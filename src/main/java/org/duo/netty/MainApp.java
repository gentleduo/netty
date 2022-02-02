package org.duo.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class MainApp {

    public static void main(String[] args) {

//        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(8, 20);
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.heapBuffer(8, 20);
//        ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.buffer(8,20);

        MainApp.printBufferInfo(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        MainApp.printBufferInfo(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        MainApp.printBufferInfo(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        MainApp.printBufferInfo(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        MainApp.printBufferInfo(buf);
        buf.writeBytes(new byte[]{1, 2, 3, 4});
        MainApp.printBufferInfo(buf);
    }

    public static void printBufferInfo(ByteBuf buf) {
        System.out.println("buf.isReadable() " + buf.isReadable()); //是否可读
        System.out.println("buf.readerIndex() " + buf.readerIndex());//读位置
        System.out.println("buf.readableBytes() " + buf.readableBytes());//能读多少

        System.out.println("buf.isWritable() " + buf.isWritable());//是否可写
        System.out.println("buf.writerIndex() " + buf.writerIndex());//写位置
        System.out.println("buf.writableBytes() " + buf.writableBytes());//能写多少

        System.out.println("buf.capacity() " + buf.capacity()); //已经分配的空间
        System.out.println("buf.maxCapacity() " + buf.maxCapacity()); //可分配的空间(上限)

        System.out.println("buf.isDirect() " + buf.isDirect()); //true:堆外；false:堆内
        System.out.println("-------------------------------------");
    }

    /**
     * 客户端
     */
    @Test
    public void loopExecutor() throws IOException {

        // group线程池
        NioEventLoopGroup selector = new NioEventLoopGroup(1);
        selector.execute(() -> {
            try {
                for (; ; ) {
                    Thread.sleep(1000);
                    System.out.println("hello world");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.in.read();
    }

    @Test
    public void clientMode() throws IOException, InterruptedException {

        // 相当于多路复用器
        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        // 相当于socket channel
        NioSocketChannel client = new NioSocketChannel();
        //将客户端注册到eventLoop中，也就是将socket channel注册到selector中
        thread.register(client);

        ChannelPipeline pipeline = client.pipeline();
        pipeline.addLast(new InputHandler());

        ChannelFuture connect = client.connect(new InetSocketAddress("192.168.56.112", 9090));
        ChannelFuture sync = connect.sync();
        ByteBuf buf = Unpooled.copiedBuffer("hello world".getBytes());
        ChannelFuture send = client.writeAndFlush(buf);
        send.sync();

        sync.channel().closeFuture().sync();
        System.out.println("client over......");
    }

    @Test
    public void serverMode() throws IOException, InterruptedException {

        // 相当于多路复用器
        NioEventLoopGroup thread = new NioEventLoopGroup(1);
        // 相当于server socket channel
        NioServerSocketChannel serverSocketChannel = new NioServerSocketChannel();
        // 将listen注册到eventLoop中，也就是将server socket channel注册到selector中
        thread.register(serverSocketChannel);
        ChannelFuture bind = serverSocketChannel.bind(new InetSocketAddress("192.168.56.1", 9090));
        ChannelPipeline pipeline = serverSocketChannel.pipeline();
        pipeline.addLast(new AcceptHandler(thread, new ChannelInit())); //accept接受客户端，并且注册到selector中
        bind.sync().channel().closeFuture().sync();
    }

    @Test
    public void nettyClient() throws InterruptedException {

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        ChannelFuture connect = bootstrap.group(group)
                .channel(NioSocketChannel.class)
//                .handler(new ChannelInit())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new InputHandler());
                    }
                })
                .connect(new InetSocketAddress("192.168.56.112", 9090));
        Channel client = connect.sync().channel();
        ByteBuf buf = Unpooled.copiedBuffer("hello world".getBytes());
        ChannelFuture send = client.writeAndFlush(buf);
        send.sync();
        client.closeFuture().sync();
    }

    @Test
    public void nettyServer() throws InterruptedException {

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        ChannelFuture bind = serverBootstrap.group(group, group)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new InputHandler());
                    }
                })
                .bind(new InetSocketAddress("192.168.56.1", 9090));

        bind.sync().channel().closeFuture().sync();
    }

}

class AcceptHandler extends ChannelInboundHandlerAdapter {

    EventLoopGroup selector;
    ChannelHandler inputHandler;

    public AcceptHandler(EventLoopGroup thread, ChannelHandler inputHandler) {
        this.selector = thread;
        this.inputHandler = inputHandler;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("server registered......");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        SocketChannel client = (SocketChannel) msg; // accept
        ChannelPipeline pipeline = client.pipeline();
        pipeline.addLast(inputHandler);
        // 将ChannelInit注册到selector中
        selector.register(client);
    }
}

/**
 * ChannelInit:
 * 在AcceptHandler初始化的时候会传入一个初始化好的InputHandler，以后每次有客户端连接进来都会用的是同一个InputHandler
 * 所以InputHandler必须加@ChannelHandler.Sharable注解，
 * 由于全部client共享一个InputHandler，所以在InputHandler中就不能加入业务逻辑了（不能有成员变量，必须是无状态的）
 * 但是实际在业务场景下，InputHandler一般都是需要加入业务逻辑的，比如统计客户端的平均处理耗时等。。。
 * 所以会增加一个无状态的初始化handler，这个handler是Sharable并且无状态的，
 * 客户端连接进来后会add这个Sharable到ChannelPipeline中，并且在注册事件中在初始化真正的业务handler，同样的将业务handler再add到ChannelPipeline中
 */
@ChannelHandler.Sharable
class ChannelInit extends ChannelInboundHandlerAdapter {

    /**
     * 在ChannelInit被注册后，会初始化一个新的InputHandler然后add到ChannelPipeline中，
     * ChannelInit的目的是为了让每个client都使用自定的handler，它自己本身没有作用，
     * 所以在完成将每个客户端new出来的handler添加到pipeline后，可以remove掉ChannelInit
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Channel client = ctx.channel();
        ChannelPipeline pipeline = client.pipeline();
        pipeline.addLast(new InputHandler());
        ctx.pipeline().remove(this);
    }
}

class InputHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client registered......");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client active......");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        //从buffer中读取完数据后会移动标志位，此时再通过writeAndFlush往channel里面写的话，由于buffer中的readerIndex已经到了数据的末尾，所以数据写入会失败
//        CharSequence charSequence = buf.readCharSequence(buf.readableBytes(), CharsetUtil.UTF_8);
        // 采用getCharSequence的方式读取的话不会移动index的位置，但是要自己控制读取数据的起始位置
        CharSequence charSequence = buf.getCharSequence(0, buf.readableBytes(), CharsetUtil.UTF_8);
        System.out.println(charSequence);
        ctx.writeAndFlush(buf);
    }
}
