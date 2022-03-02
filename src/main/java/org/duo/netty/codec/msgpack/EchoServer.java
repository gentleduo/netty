package org.duo.netty.codec.msgpack;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * MessagePack编解码框架不支持粘包/半包的处理，所以通过在消息头中新增报文长度字段，并利用该字段进行粘包/半包的编解码。
 * 本程序利用Netty提供的LengthFieldPrepender和LengthFieldBasedFrameDecoder结合MessagePack编解码框架，实现对TCP粘包/半包的支持。
 * 在MessagePack编码器之前增加LengthFieldPrepender，它将在ByteBuf之前增加2个字节的消息长度字段，其原理如下：
 * +----------------+       +--------+----------------+
 * | "HELLO, WORLD" | ===>  | 0x000C | "HELLO, WORLD" |
 * +----------------+       +--------+----------------+
 * 在MessagePack编码器之前增加LengthFieldBasedFrameDecoder，用于处理半包消息，这样后对面的MessagePack接收到的永远是整包消息：
 * +--------+----------------+      +----------------+
 * | Length | Actual Content |      | Actual Content |
 * | 0x000C | "HELLO, WORLD" | ===> | "HELLO, WORLD" |
 * +--------+----------------+      +----------------+
 */
public class EchoServer {

    public void bind(int port) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            // TODO Auto-generated method stub
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(65535, 0,
                                    2, 0, 2));
                            ch.pipeline().addLast(new MsgPackDecoder());
                            ch.pipeline().addLast(new LengthFieldPrepender(2));
                            ch.pipeline().addLast(new MsgPackEncoder());
                            ch.pipeline().addLast(new EchoServerHandler());
                        }
                    });

            //bind port
            ChannelFuture f = b.bind(port).sync();
            //wait
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        int port = 8080;
        try {
            new EchoServer().bind(port);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
