package org.duo.netty.codec.protobuf;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class SubReqServer {

    /**
     * 首先向SocketChannel.pipeline添加ProtobufVarint32FrameDecoder，它主要用于粘包/半包处理，
     * 随后继续添加ProtobufDecoder解码器，它的参数是SubscribeReqProto.SubscribeReq，
     * 实际上就是要告诉ProtobufDecoder需要解码的目标类是什么，否则仅仅从字节数组中是无法判断出要解码的目标类型信息的。
     * 由于ProtobufDecoder已经对消息进行了自动解码，因此接收到的请求消息就可以直接使用了。
     * ProtobufDecoder仅仅负责解码，它并不支持读粘包/半包。因此，在ProtobufDecoder前面一定要有能够处理读粘包/半包的解码器，有以下三种选择：
     * 1.使用Netty提供的ProtobufVarint32FrameDecoder，它可以处理粘包/半包消息；
     * 2.继承Netty提供的通用粘包/半包解码器LengthFieldBasedFrameDecoder；
     * 3.继承ByteToMessageDecoder类，自己处理粘包/半包消息。
     *
     * @param port
     * @throws Exception
     */
    public void bind(int port) throws Exception {
        // 配置服务端的NIO线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new ProtobufVarint32FrameDecoder());
                            ch.pipeline().addLast(
                                    new ProtobufDecoder(
                                            SubscribeReqProto.SubscribeReq
                                                    .getDefaultInstance()));
                            ch.pipeline().addLast(
                                    new ProtobufVarint32LengthFieldPrepender());
                            ch.pipeline().addLast(new ProtobufEncoder());
                            ch.pipeline().addLast(new SubReqServerHandler());
                        }
                    });
            // 绑定端口，同步等待成功
            ChannelFuture f = b.bind(port).sync();
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }
        new SubReqServer().bind(port);
    }
}
