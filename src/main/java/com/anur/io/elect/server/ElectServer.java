package com.anur.io.elect.server;

import java.util.function.BiConsumer;
import com.anur.core.util.ShutDownHooker;
import com.anur.io.core.Server;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

/**
 * Created by Anur IjuoKaruKas on 2019/1/18
 *
 * 供其他端连接的套接字服务端入口，此端口负责集群内选举
 */
public class ElectServer extends Server {

    public ElectServer(int port, BiConsumer<ChannelHandlerContext, String> msgConsumer, ShutDownHooker shutDownHooker) {
        super(port, msgConsumer, shutDownHooker);
    }

    @Override
    public ChannelPipeline channelPipelineConsumer(ChannelPipeline channelPipeline) {
        return channelPipeline;
    }
}