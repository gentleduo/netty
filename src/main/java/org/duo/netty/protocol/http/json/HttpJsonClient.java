package org.duo.netty.protocol.http.json;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;

import java.net.InetSocketAddress;

/**
 * 客户端解码器：
 * 1.使用netty自带的HttpResponseDecoder和HttpObjectAggregator将响应解码成FullHttpResponse
 * 2.使用自定义的解码器HttpJsonResponseDecoder封装为使用的HttpJsonResponse
 * 客户端编码器:
 * 1.使用netty自带的HttpRequestEncoder写入HttpJsonRequest
 * 2.使用自定义的编码器HttpJsonRequestEncoder封装成FullHttpRequest然后发送
 */
public class HttpJsonClient {

    public void connect(int port) throws Exception {
        // 配置客户端NIO线程组
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            ch.pipeline().addLast("http-decoder",
                                    new HttpResponseDecoder());
                            ch.pipeline().addLast("http-aggregator",
                                    new HttpObjectAggregator(65536));
                            // json解码器
                            ch.pipeline().addLast("json-decoder", new HttpJsonResponseDecoder(Order.class, true));
                            ch.pipeline().addLast("http-encoder",
                                    new HttpRequestEncoder());
                            ch.pipeline().addLast("json-encoder",
                                    new HttpJsonRequestEncoder());
                            ch.pipeline().addLast("jsonClientHandler",
                                    new HttpJsonClientHandler());
                        }
                    });

            // 发起异步连接操作
            ChannelFuture f = b.connect(new InetSocketAddress(port)).sync();

            // 当代客户端链路关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅退出，释放NIO线程组
            group.shutdownGracefully();
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
        new HttpJsonClient().connect(port);
    }
}