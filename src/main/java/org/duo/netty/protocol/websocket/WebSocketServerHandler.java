package org.duo.netty.protocol.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 第一次握手请求由HTTP协议承载，所以它是一个HTTP消息，执行handleHttpRequest方法来处理WebSocket握手请求：
 * 首先对握手请求消息进行判断，如果消息头中没有包含Upgrade字段或者它的值不是websocket，则返回HTTP400响应。
 * 握手请求简单校验通过后，开始构建握手工厂，创建握手处理类WebSocketServerHandshaker，通过它构造握手响应消息返回给客户端，
 * 同时在WebSocketServerHandshaker的handshake方法中会将WebSocket相关的编码和解码类动态添加到ChannelPipeline中，用于WebSocket消息的编解码。
 * <p>
 * 在添加WebSocket Encoder和WebSocket Decoder之后，服务端就可以自动对WebSocket消息进行编解码了，后面的业务handler可以直接对WebSocket对象进行操作。
 * 1.客户端提交消息给服务端，WebSocketServerHandler接收到的是已经解码后的WebSocketFrame消息，对WebSocket请求消息处理前需要对控制帧进行判断：
 * 2.如果是关闭链路的控制消息，就调用WebSocketServerHandshaker的close方法关闭WebSocket连接；
 * 3.如果是维持链路的Ping消息，则构造Pong消息返回。
 * 接着对请求消息的类型进行判断：
 * 1.二进制消息（即：图片消息）
 * 2.文本消息
 */
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = Logger
            .getLogger(WebSocketServerHandler.class.getName());

    private WebSocketServerHandshaker handshaker;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        // 传统的HTTP接入
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        }
        // WebSocket接入
        else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx,
                                   FullHttpRequest req) throws Exception {

        // 如果HTTP解码失败，返回HHTP异常
        if (!req.decoderResult().isSuccess()
                || (!"websocket".equals(req.headers().get("Upgrade")))) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
                    BAD_REQUEST));
            return;
        }

        // 构造握手响应返回，本机测试
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8080/websocket", null, false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx,
                                      WebSocketFrame frame) {

        // 判断是否是关闭链路的指令
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(),
                    (CloseWebSocketFrame) frame.retain());
            return;
        }
        // 判断是否是Ping消息
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(
                    new PongWebSocketFrame(frame.content().retain()));
            return;
        }

//        // 本例程仅支持文本消息，不支持二进制消息
//        if (!(frame instanceof TextWebSocketFrame)) {
//            throw new UnsupportedOperationException(String.format(
//                    "%s frame types not supported", frame.getClass().getName()));
//        }

        //二进制消息（即：图片消息），将客户端发过来的图片返回给客户端。
        if (frame instanceof BinaryWebSocketFrame) {

            BinaryWebSocketFrame img = (BinaryWebSocketFrame) frame.copy();
            ctx.channel().writeAndFlush(img);
        }

        //文本消息
        if (frame instanceof TextWebSocketFrame) {

            // 返回应答消息
            String request = ((TextWebSocketFrame) frame).text();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(String.format("%s received %s", ctx.channel(), request));
            }
            ctx.channel().write(
                    new TextWebSocketFrame(request
                            + " , 欢迎使用Netty WebSocket服务，现在时刻："
                            + new java.util.Date().toString()));
        }
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx,
                                         FullHttpRequest req, FullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
                    CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }

        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
