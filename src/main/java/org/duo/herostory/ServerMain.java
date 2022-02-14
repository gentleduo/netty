package org.duo.herostory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http://cdn0001.afrxvk.cn/hero_story/demo/step010/index.html?serverAddr=127.0.0.1:12345&userId=1
 * Protobuf 命令行工具
 * https://github.com/protocolbuffers/protobuf
 * 在releases页面中找到下载链接；
 * 解压后设置path环境变量；
 * 执行命令protoc;
 * protoc --java_out=.\ .\GameMsgProtocol.proto
 */
public class ServerMain {

    static private final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) {

        PropertyConfigurator.configure(ServerMain.class.getClassLoader().getResourceAsStream("log4j.properties"));

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new HttpServerCodec(), // htttp 服务器编解码
                            new HttpObjectAggregator(65536), // 内容长度限制
                            new WebSocketServerProtocolHandler("/websocket"), // websocket协议处理器
                            new GameMsgDecoder(), // 自定义消息解码器
                            new GameMsgEncoder(), // 自定义消息编码器
                            new GameMsgHandler()); // 自定义消息处理器
                }
            });

//            bootstrap.option(ChannelOption.SO_BACKLOG, 128);
//            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture sync = bootstrap.bind(12345).sync();

            if (sync.isSuccess()) {
                LOGGER.info("游戏服务器启动成功！");
            }
            // 等待服务器channel关闭，也就是阻塞等待不退出主程序
            sync.channel().closeFuture().sync();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
