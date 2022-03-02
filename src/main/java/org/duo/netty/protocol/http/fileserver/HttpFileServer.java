package org.duo.netty.protocol.http.fileserver;

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
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 文件服务器功能：
 * 文件服务器使用HTTP协议对外提供服务，当客户端通过浏览器访问文件服务器时，
 * 对访问路径进行检查，检查失败时返回HTTP403错误，该页无法访问；如果检验通过，以链接的方式打开当前文件目录，每个目录或者文件都是个超链接，可以递归访问。
 * 如果是目录，可以继续递归访问它下面的子目录或者文件，如果是文件且可读，则可以在浏览器直接打开，或者通过【目标另存为】下载该文件。
 */
public class HttpFileServer {

    // 允许访问的URL：DEFAULT_URL最终会作为参数传入HttpFileServerHandler类中，对客户请求中的URL进行合法性判断。
    private static final String DEFAULT_URL = "/src/main/java/org/duo/netty/";
//    private static final String DEFAULT_URL = "/";

    /**
     * 首先向SocketChannel.pipeline中添加HTTP请求消息解码器，随后又添加了HttpObjectAggregator解码器；
     * HttpObjectAggregator的作用是将多个消息转化为单一的FullHttpRequest或者FullHttpResponse，原因是HTTP解码器在每个HTTP消息中会生成多个消息对象：
     * 1.HttpRequest/HttpResponse；
     * 2.HttpContent；
     * 3.LastHttpContent。
     * 接着添加HttpResponseEncoder(响应编码器)和ChunkedWriteHandler(支持异步大文件传输，但不占用过多的内存，防止发生Java内存溢出)
     * 最后添加HttpFileServerHandler，用于文件服务器的业务逻辑处理。
     *
     * @param port
     * @param url
     * @throws Exception
     */
    public void run(final int port, final String url) throws Exception {
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
                            ch.pipeline().addLast("http-decoder",
                                    new HttpRequestDecoder()); // 请求消息解码器
                            ch.pipeline().addLast("http-aggregator",
                                    new HttpObjectAggregator(65536));// 目的是将多个消息转换为单一的request或者response对象
                            ch.pipeline().addLast("http-encoder",
                                    new HttpResponseEncoder());//响应编码器
                            ch.pipeline().addLast("http-chunked",
                                    new ChunkedWriteHandler());//目的是支持异步大文件传输
                            ch.pipeline().addLast("fileServerHandler",
                                    new HttpFileServerHandler(url));// 业务逻辑
                        }
                    });
            ChannelFuture future = b.bind("127.0.0.1", port).sync();
            System.out.println("HTTP文件目录服务器启动，网址是 : " + "http://127.0.0.1:"
                    + port + url);
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
        String url = DEFAULT_URL;
        if (args.length > 1)
            url = args[1];
        new HttpFileServer().run(port, url);
    }
}
