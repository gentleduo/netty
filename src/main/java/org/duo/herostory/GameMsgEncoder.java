package org.duo.herostory;

import com.google.protobuf.GeneratedMessageV3;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.duo.herostory.msg.GameMsgProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 消息编码器
 */
public class GameMsgEncoder extends ChannelOutboundHandlerAdapter {

    static private final Logger LOGGER = LoggerFactory.getLogger(GameMsgEncoder.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

        if (ctx == null || msg == null) {
            return;
        }

        try {
            if (!(msg instanceof GeneratedMessageV3)) {
                super.write(ctx, msg, promise);
                return;
            }
            // 消息编码
            int msgCode = -1;
            if (msg instanceof GameMsgProtocol.UserEntryResult) {
                msgCode = GameMsgProtocol.MsgCode.USER_ENTRY_RESULT_VALUE;
            } else if (msg instanceof GameMsgProtocol.WhoElseIsHereResult) {
                msgCode = GameMsgProtocol.MsgCode.WHO_ELSE_IS_HERE_RESULT_VALUE;
            } else if (msg instanceof GameMsgProtocol.UserMoveToResult) {
                msgCode = GameMsgProtocol.MsgCode.USER_MOVE_TO_RESULT_VALUE;
            } else if (msg instanceof GameMsgProtocol.UserQuitResult) {
                msgCode = GameMsgProtocol.MsgCode.USER_QUIT_RESULT_VALUE;
            } else {
                LOGGER.info("无法识别的消息类型，msgClazz = {}", msg.getClass().getSimpleName());
                super.write(ctx, msg, promise);
                return;
            }
            // 消息体
            byte[] msgBody = ((GeneratedMessageV3) msg).toByteArray();
            ByteBuf byteBuf = ctx.alloc().buffer();
            byteBuf.writeShort((short) msgBody.length); //消息长度
            byteBuf.writeShort((short) msgCode); //消息编码
            byteBuf.writeBytes(msgBody); //消息体

            BinaryWebSocketFrame binaryWebSocketFrame = new BinaryWebSocketFrame(byteBuf);
            super.write(ctx, binaryWebSocketFrame, promise);

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
}