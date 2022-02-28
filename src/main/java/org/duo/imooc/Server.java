package org.duo.imooc;

//import com.imooc.netty.ch6.AuthHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;

/**
 * @author
 */
public final class Server {

    public static void main(String[] args) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    // ServerBootstrap.channel方法，会给ServerBootstrap的成员变量:channelFactory赋初始化值(channelFactory定义在父类AbstractBootstrap中)
                    // 即：ReflectiveChannelFactory<NioServerSocketChannel.class>
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childAttr(AttributeKey.newInstance("childAttr"), "childAttrValue")
                    .handler(new ServerHandler())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            //ch.pipeline().addLast(new AuthHandler());
                            //..
                        }
                    });

            /**
             * ServerBootstrap的bind方法中，首先会先通过ServerBootstrap的channelFactory(即：ReflectiveChannelFactory)的newChannel方法，和构建ReflectiveChannelFactory传入的泛型类：NioServerSocketChannel，通过反射来实例化NioServerSocketChannel的类对象
             * 而在通过反射调用的NioServerSocketChannel的无参构造函数中，主要完成以下几步：
             * 第一步：创建JDK底层的ServerSocketChannel：
             * 调用newSocket中通过调用SelectorProvider的openServerSocketChannel方法创建JDK底层的channel，再将创建好的ServerSocketChannel传入NioServerSocketChannel有参的构造方法中；
             * SelectorProvider是JDK底层的类，位于：java.nio.channels.spi包下
             * SelectorProvider的openServerSocketChannel方法中会创建JDK底层的channel：sun.nio.ch.ServerSocketChannelImpl对象
             * ServerSocketChannelImpl是ServerSocketChannel的子类
             * 第二步：设置ServerSocketChannel的configureBlocking为false·
             *
             * 第三步：NioServerSocketChannelConfig[tcp参数配置类]
             */
            ChannelFuture f = b.bind(8888).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}