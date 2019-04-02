package com.anur.core.coordinate.sender;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.anur.core.command.modle.FetchResponse;
import com.anur.core.command.modle.Register;
import com.anur.core.command.common.AbstractCommand;
import com.anur.core.coordinate.model.RequestProcessor;
import com.anur.core.command.common.OperationTypeEnum;
import com.anur.core.lock.ReentrantReadWriteLocker;
import com.anur.core.util.ChannelManager;
import com.anur.core.util.ChannelManager.ChannelType;
import com.anur.exception.HanabiException;
import com.anur.timewheel.TimedTask;
import com.anur.timewheel.Timer;
import io.netty.channel.Channel;
import io.netty.util.internal.StringUtil;

/**
 * Created by Anur IjuoKaruKas on 2019/3/27
 *
 * 此管理器负责消息的重发、并保证这种消息类型，在收到回复之前，无法继续发同一种类型的消息
 *
 * 1、消息在没有收到回复之前，会定时重发。
 * 2、那么如何保证数据不被重复消费：我们以时间戳作为 key 的一部分，应答方需要在消费消息后，需要记录此时间戳，并不再消费比此时间戳小的消息。
 */
public class InFlightRequestManager extends ReentrantReadWriteLocker {

    private static volatile InFlightRequestManager INSTANCE;

    private static Map<OperationTypeEnum, OperationTypeEnum> RequestAndResponseType = new HashMap<>();

    private static Map<OperationTypeEnum, OperationTypeEnum> ResponseAndRequestType = new HashMap<>();

    static {
        RequestAndResponseType.put(OperationTypeEnum.REGISTER, OperationTypeEnum.NONE);
        RequestAndResponseType.put(OperationTypeEnum.FETCH, OperationTypeEnum.FETCH_RESPONSE);

        RequestAndResponseType.forEach((ek, ev) -> ResponseAndRequestType.put(ev, ek));
    }

    public static InFlightRequestManager getINSTANCE() {
        if (INSTANCE == null) {
            synchronized (InFlightRequestManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new InFlightRequestManager();
                }
            }
        }
        return INSTANCE;
    }

    public static void main(String[] args) {
    }

    private final Logger logger = LoggerFactory.getLogger(InFlightRequestManager.class);

    /**
     * 此 map 确保对一个服务发送某个消息，在收到回复之前，不可以再次对其发送消息。（有自动重发机制）
     */
    private volatile Map<String, Map<OperationTypeEnum, RequestProcessor>> inFlight = new HashMap<>();

    /**
     * 重启此类，用于在重新选举后，刷新所有任务，不再执着于上个世代的任务
     */
    public void reboot() {
        this.writeLockSupplier(() -> {
            inFlight.forEach((s, m) -> m.forEach((o, t) -> t.cancel()));
            inFlight = new HashMap<>();
            return null;
        });
    }

    /**
     * 接收到消息如何处理
     */
    public void receive(ByteBuffer msg, OperationTypeEnum typeEnum, Channel channel) {
        switch (typeEnum) {
        case REGISTER:
            Register register = new Register(msg);
            logger.info("协调节点 {} 已注册到本节点", register.getServerName());
            ChannelManager.getInstance(ChannelType.COORDINATE)
                          .register(register.getServerName(), channel);
            break;

        case FETCH:

            break;
        /*
         * DEFAULT 都当做 response 处理
         */
        default:
            OperationTypeEnum requestType = ResponseAndRequestType.get(typeEnum);
            String serverName = ChannelManager.getInstance(ChannelType.COORDINATE)
                                              .getChannelName(channel);
            if (StringUtil.isNullOrEmpty(serverName)) {
                throw new HanabiException("收到了来自已断开连接节点 " + serverName + " 关于 " + requestType.name() + " 的无效 response");
            }

            this.readLockSupplier(() -> {
                RequestProcessor requestProcessor = Optional.ofNullable(inFlight.get(serverName))
                                                            .map(m -> m.get(requestType))
                                                            .orElse(null);
                if (requestProcessor == null || requestProcessor.isComplete()) {
                    throw new HanabiException("收到了来自节点 " + serverName + " 关于 " + requestType.name() + " 的无效 response");
                }

                this.writeLockSupplier(() -> {
                    requestProcessor.complete(msg);
                    inFlight.get(serverName)
                            .remove(requestType);
                    logger.info("成功收到来自节点 {} 关于 {} 的 response", serverName, requestType.name());
                    return null;
                });
                return null;
            });
        }
    }

    /**
     * 此发送器保证【一个类型的消息】只能在收到回复前发送一次，类似于仅有 1 容量的Queue
     */
    public boolean send(String serverName, AbstractCommand command, RequestProcessor requestProcessor) {
        OperationTypeEnum typeEnum = command.getOperationTypeEnum();

        // 第一次不锁检查
        if (Optional.ofNullable(inFlight.get(serverName))
                    .map(enums -> enums.containsKey(typeEnum))
                    .orElse(false)) {
            return false;
        }

        return this.writeLockSupplier(() -> {

            // 双重锁检查
            if (Optional.ofNullable(inFlight.get(serverName))
                        .map(enums -> enums.containsKey(typeEnum))
                        .orElse(false)) {

                logger.debug("尝试创建发送到节点 {} 的 {} 任务失败，上次的指令还未收到 response", serverName, typeEnum.name());
                return false;
            } else {
                logger.debug("发送到节点 {} 的 {} 任务创建成功", serverName, typeEnum.name());
                inFlight.compute(serverName, (s, enums) -> {
                    if (enums == null) {
                        enums = new HashMap<>();
                    }
                    enums.put(typeEnum, requestProcessor);
                    sendImpl(serverName, command, requestProcessor, typeEnum);
                    return enums;
                });

                return true;
            }
        });
    }

    /**
     * 真正发送消息的方法，内置了重发机制
     */
    private void sendImpl(String serverName, AbstractCommand command, RequestProcessor requestProcessor, OperationTypeEnum operationTypeEnum) {
        this.readLockSupplier(() -> {
            if (!requestProcessor.isComplete()) {
                CoordinateSender.send(serverName, command);

                if (RequestAndResponseType.get(operationTypeEnum)
                                          .equals(OperationTypeEnum.NONE)) {
                    // 是不需要回复的类型
                    requestProcessor.complete();
                    writeLockSupplier(() -> inFlight.get(serverName)
                                                    .remove(operationTypeEnum)
                    );
                } else {
                    TimedTask task = new TimedTask(1000, () -> sendImpl(serverName, command, requestProcessor, operationTypeEnum));

                    inFlight.get(serverName)
                            .get(operationTypeEnum)
                            .registerTask(task);

                    Timer.getInstance()// 扔进时间轮不断重试，直到收到此消息的回复
                         .addTask(task);
                }
            }
            return null;
        });
    }
}
