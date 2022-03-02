package org.duo.netty.protocol.http.json;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import java.net.InetSocketAddress;

/**
 * 服务端编解码器： 获取请求，最终解码为自定义的HttpJsonRequest对象
 * 1.HttpRequestDecoder：请求消息解码器，转换为消息对象。
 * 2.HttpObjectAggregator: 目的是将多个消息转换为单一的request或者response对象，最终得到的是FullHttpRequest对象
 * 3.需要自定义的解码器HttpJsonRequestDecoder，将FullHttpRequest转换为HttpJsonRequest对象
 * 服务器端编码器：发送响应，将生成的数据转换为DefaultFullHttpResponse对象发送出去.
 * 1.HttpResponseEncoder：响应消息编码器，已经是一个HTTP消息了
 * 2.自定义编码器 HttpJsonResponseEncoder：由于Netty的DefaultFullHttpResponse没有提供动态设置消息体content的接口。因此我们只能复制一个新的HTTP消息，将动态内容加入，生成一个DefaultFullHttpResponse对象。
 */
public class HttpJsonServer {

    public void run(final int port) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch)
                                throws Exception {
                            //接收HttpJsonRequest，需要对应解码器
                            //ByteBuf->FullHttpRequest-> HttpJsonRequestDecoder
                            //输出HttpJsonResponse，需要对应编码器
                            //HttpResponseEncoder->FullHttpResponse-> HttpJsonResponseEncoder
                            ch.pipeline().addLast("http-decoder", new HttpRequestDecoder());
                            ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
                            ch.pipeline().addLast("json-decoder", new HttpJsonRequestDecoder(Order.class, true));
                            ch.pipeline().addLast("http-encoder", new HttpResponseEncoder());
                            ch.pipeline().addLast("json-encoder", new HttpJsonResponseEncoder());
                            ch.pipeline().addLast("jsonServerHandler", new HttpJsonServerHandler());
                        }
                    });
            ChannelFuture future = b.bind(new InetSocketAddress(port)).sync();
            System.out.println("HTTP订购服务器启动，网址是 : " + "http://localhost:"
                    + port);
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        new HttpJsonServer().run(port);
    }
}