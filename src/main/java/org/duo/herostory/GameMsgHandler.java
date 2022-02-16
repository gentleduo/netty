package org.duo.herostory;

import com.google.protobuf.GeneratedMessageV3;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.duo.herostory.cmdhandler.*;
import org.duo.herostory.msg.GameMsgProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GameMsgHandler extends SimpleChannelInboundHandler<Object> {

    static private Logger LOGGER = LoggerFactory.getLogger(GameMsgHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (ctx == null) {
            return;
        }
        try {
            super.channelActive(ctx);
            Broadcaster.addChannel(ctx.channel());
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

        if (ctx == null) {
            return;
        }
        try {
            super.handlerRemoved(ctx);

            Integer userId = (Integer) ctx.channel().attr(AttributeKey.valueOf("userId")).get();
            if (userId == null) {
                return;
            }

            UserManager.removeByUserId(userId);
            Broadcaster.broadcast(ctx.channel());

            GameMsgProtocol.UserQuitResult.Builder resultBuilder = GameMsgProtocol.UserQuitResult.newBuilder();
            resultBuilder.setQuitUserId(userId);

            GameMsgProtocol.UserQuitResult newResult = resultBuilder.build();
            Broadcaster.broadcast(newResult);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (ctx == null || msg == null) {

            return;
        }
        LOGGER.info("收到客户端消息，msgClazz = {}, msgBody = {}", msg.getClass().getSimpleName(), msg);

        try {
            ICmdHandler<? extends GeneratedMessageV3> iCmdHandler = CmdHandlerFactory.create(msg.getClass());

            if (iCmdHandler != null) {
//                iCmdHandler.handle(ctx, (GeneratedMessageV3) msg); // 编译报错？原因不理解
                iCmdHandler.handle(ctx, cast(msg));
            }

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    static private <T extends GeneratedMessageV3> T cast(Object msg) {

        if (msg == null) {
            return null;
        } else {
            return (T) msg;
        }
    }
}
