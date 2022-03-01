/*
 * Copyright 2013-2018 Lilinfeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.duo.netty.basic;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class TimeServer {

    public void bind(int port) throws Exception {
        // 配置服务端的NIO线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            /**
             * ServerBootstrap是Netty用于启动NIO服务端的辅助启动类，目的是降低服务端的开发复杂度。
             * group方法将两个NIO线程组当作入参传递到ServerBootstrap中。
             * 接着，设置创建的channel为NioServerSocketChannel，它的功能对应于JDK NIO类库中的ServerSocketChannel。
             * 然后配置NioServerSocketChannel的TCP参数，此处将它的backlog设置为1024（当请求的连接数量非常大，资源不够时，允许排队的连接数），
             * 最后绑定I/O事件的处理类ChildChannelHandler，它的作用类似于Reactor模式中的Handler类，主要用于处理网络I/O事件，例如记录日志、对消息进行编解码等。
             */
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChildChannelHandler());
            /**
             * 服务器启动辅助类配置完成之后，调用它的bind方法绑定监听端口，随后，
             * 调用它的同步阻塞方法sync等待绑定操作完成。完成之后Netty会返回一个ChannelFuture，
             * 它的功能类似于JDK的java.util.concurrent.Future，主要用于异步操作的通知回调。
             */
            ChannelFuture f = b.bind(port).sync();
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel arg0) throws Exception {
            arg0.pipeline().addLast(new TimeServerHandler());
        }

    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }
        new TimeServer().bind(port);
    }
}
