package org.duo.herostory;

import com.google.protobuf.GeneratedMessageV3;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.duo.herostory.msg.GameMsgProtocol;

/**
 * 消息解码器
 */
public class GameMsgDecoder extends ChannelInboundHandlerAdapter {

    static private final Logger LOGGER = LoggerFactory.getLogger(GameMsgDecoder.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (ctx == null || msg == null) {
            return;
        }
        if (!(msg instanceof BinaryWebSocketFrame)) {
            return;
        }

        try {
            BinaryWebSocketFrame inputFrame = (BinaryWebSocketFrame) msg;
            ByteBuf byteBuf = inputFrame.content();

            byteBuf.readShort(); //读取消息的长度
            int msgCode = byteBuf.readShort(); //读取消息编号

            byte[] msgBody = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(msgBody);

            GeneratedMessageV3 cmd = null;

            switch (msgCode) {
                case GameMsgProtocol.MsgCode.USER_ENTRY_CMD_VALUE:
                    cmd = GameMsgProtocol.UserEntryCmd.parseFrom(msgBody);
                    break;
                case GameMsgProtocol.MsgCode.WHO_ELSE_IS_HERE_CMD_VALUE:
                    cmd = GameMsgProtocol.WhoElseIsHereCmd.parseFrom(msgBody);
                    break;
                case GameMsgProtocol.MsgCode.USER_MOVE_TO_CMD_VALUE:
                    cmd = GameMsgProtocol.UserMoveToCmd.parseFrom(msgBody);
                    break;
                default:
                    break;
            }

            if (cmd != null) {
                ctx.fireChannelRead(cmd);
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
}
