package com.anur;

import com.anur.engine.api.constant.CommandTypeConst;
import com.anur.engine.api.constant.TransactionTypeConst;
import com.anur.engine.api.constant.str.StrApiConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.anur.config.InetSocketAddressConfiguration;
import com.anur.core.coordinate.apis.fetch.LeaderCoordinateManager;
import com.anur.core.coordinate.apis.recovery.FollowerClusterRecoveryManager;
import com.anur.core.coordinate.apis.recovery.LeaderClusterRecoveryManager;
import com.anur.core.coordinate.apis.fetch.FollowerCoordinateManager;
import com.anur.core.coordinate.operator.CoordinateServerOperator;
import com.anur.core.elect.operator.ElectOperator;
import com.anur.core.elect.operator.ElectServerOperator;
import com.anur.core.struct.OperationTypeEnum;
import com.anur.core.struct.base.Operation;
import com.anur.engine.EngineFacade;
import com.anur.util.HanabiExecutors;
import com.anur.engine.common.core.HanabiCommand;
import com.anur.io.hanalog.log.LogManager;
import com.anur.io.hanalog.log.CommitProcessManager;

/**
 * Created by Anur IjuoKaruKas on 2019/3/13
 */
public class Bootstrap {
    
    private static Logger logger = LoggerFactory.getLogger(Bootstrap.class);
    
    private static volatile boolean RUNNING = true;
    
    public static void main(String[] args) throws InterruptedException {
        
        String serverName = null;
        if (args.length > 0) {
            serverName = args[0];
        }
        InetSocketAddressConfiguration.INSTANCE.init(serverName);
        
        logger.info(
                "\n\n" +
                        " _     _                   _     _ \n" +
                        "| |   | |                 | |   (_)\n" +
                        "| |__ | | ____ ____   ____| | _  _ \n" +
                        "|  __)| |/ _  |  _ \\ / _  | || \\| |\n" +
                        "| |   | ( ( | | | | ( ( | | |_) ) |\n" +
                        "|_|   |_|\\_||_|_| |_|\\_||_|____/|_|\n" +
                        "           Hanabi     (ver 0.0.1)\n" +
                        "                                         A distributed key-value store \n\n" +
                        "node - " + InetSocketAddressConfiguration.INSTANCE.getServerName() + "\n");
        
        HanabiExecutors.INSTANCE.execute(() -> {
            
            try {
                /*
                 * 日志一致性控制器
                 */
                FollowerCoordinateManager forInitial01 = FollowerCoordinateManager.INSTANCE;
                
                /*
                 * 日志一致性控制器
                 */
                LeaderCoordinateManager forInitial02 = LeaderCoordinateManager.INSTANCE;
                
                /*
                 * 集群日志恢复器
                 */
                LeaderClusterRecoveryManager forInitial03 = LeaderClusterRecoveryManager.INSTANCE;
                
                /*
                 * 集群日志恢复器
                 */
                FollowerClusterRecoveryManager forInitial04 = FollowerClusterRecoveryManager.INSTANCE;
                
                /*
                 * 提交记录管理者（仅leader）
                 */
                CommitProcessManager forInitial05 = CommitProcessManager.INSTANCE;
                
                /*
                 * 初始化日志管理
                 */
                LogManager logManager = LogManager.INSTANCE;
                
                /*
                 * 启动协调服务器
                 */
                CoordinateServerOperator.getInstance()
                        .start();
                
                /*
                 * 启动选举服务器，没什么主要的操作，这个服务器主要就是应答选票以及应答成为 Flower 用
                 */
                ElectServerOperator.getInstance()
                        .start();
                
                /*
                 * 启动选举客户端，初始化各种投票用的信息，以及启动成为候选者的定时任务
                 */
                ElectOperator.getInstance()
                        .start();
                
                /*
                 * 启动存储引擎
                 */
                EngineFacade forInitial06 = EngineFacade.INSTANCE;
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
            
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            try {
                
                for (int i = 0; i < 100000000; i++) {
                    
                    Operation operation = new Operation(OperationTypeEnum.COMMAND, "AnurKey",
                                                        HanabiCommand.Companion.generator(
                                                            99, TransactionTypeConst.SHORT, CommandTypeConst.STR, StrApiConst.SET, "HanabiValue-中文-"));
                    LogManager.INSTANCE.appendWhileClusterValid(operation);
                }
                
                System.out.println("append complete");
            } catch (Exception e) {
            }
        });
        
        while (RUNNING) {
            Thread.sleep(1000);
        }
    }
}
