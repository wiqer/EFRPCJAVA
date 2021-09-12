package com.wiqer.rpc.nettyiml.netty;

import com.wiqer.rpc.nettyiml.NettyMsg;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class NodeServer {
    Queue<NettyMsg> nettyMsgQueue ;

    public void setNettyMsgQueue(Queue<NettyMsg> nettyMsgQueue) {
        this.nettyMsgQueue = nettyMsgQueue;
    }

    public void startNettyServer(int port) throws Exception {
        //boss单线程
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        ///HashedWheelTimer
        // EventLoopGroup workerGroup = new NioEventLoopGroup(CpuNum.workerCount());
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, bossGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    //保持长连接
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    //出来网络io事件，如记录日志、对消息编解码等
                    .childHandler(new ChildChannelHandler());
            //绑定端口，同步等待成功
            ChannelFuture future = bootstrap.bind(port).sync();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                bossGroup.shutdownGracefully (1000, 3000, TimeUnit.MILLISECONDS);
                // bossGroup.shutdownGracefully (1000, 3000, TimeUnit.MILLISECONDS);
            }));
            //等待服务器监听端口关闭
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            //do nothing
            System.out.println("netty stop");
        } finally {
            //优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            //workerGroup.shutdownGracefully();
        }
    }

    /**
     * handler类
     */
    private class ChildChannelHandler extends ChannelInitializer<Channel> {

        @Override
        protected void initChannel(Channel ch) {
            NodeServerHandler serverHandler = new NodeServerHandler();
            serverHandler.setNettyMsgQueue(nettyMsgQueue);

            ByteBuf delimiter = Unpooled.copiedBuffer("!0.0".getBytes());
            ch.pipeline()
                    .addLast(new DelimiterBasedFrameDecoder(0xffffff, delimiter))
                    .addLast(new StringDecoder())
                    .addLast(serverHandler);
        }
    }
}
