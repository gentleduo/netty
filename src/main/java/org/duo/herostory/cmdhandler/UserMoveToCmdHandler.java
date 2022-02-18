package org.duo.herostory.cmdhandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.duo.herostory.Broadcaster;
import org.duo.herostory.model.User;
import org.duo.herostory.model.UserManager;
import org.duo.herostory.msg.GameMsgProtocol;

/**
 * 用户移动指令处理器
 */
public class UserMoveToCmdHandler implements ICmdHandler<GameMsgProtocol.UserMoveToCmd> {

    /**
     * 更新用户字典中的移动状态，并把用户移动的消息广播给所有用户
     *
     * @param ctx
     * @param cmd
     */
    public void handle(ChannelHandlerContext ctx, GameMsgProtocol.UserMoveToCmd cmd) {

        if (ctx == null || cmd == null) {
            return;
        }
        Integer userId = (Integer) ctx.channel().attr(AttributeKey.valueOf("userId")).get();
        if (userId == null) {
            return;
        }

        User existUser = UserManager.getByUserId(userId);
        if (null == existUser) {
            return;
        }
        long nowTime = System.currentTimeMillis();

        //更新用户的移动状态
        existUser.moveState.fromPosX = cmd.getMoveFromPosX();
        existUser.moveState.fromPosY = cmd.getMoveFromPosY();
        existUser.moveState.toPosX = cmd.getMoveToPosX();
        existUser.moveState.toPosY = cmd.getMoveToPosY();
        existUser.moveState.startTime = nowTime;

        GameMsgProtocol.UserMoveToResult.Builder resultBuilder = GameMsgProtocol.UserMoveToResult.newBuilder();
        resultBuilder.setMoveUserId(userId);
        resultBuilder.setMoveToPosX(cmd.getMoveToPosX());
        resultBuilder.setMoveToPosY(cmd.getMoveToPosY());

        //添加启动起始，终止时间
        resultBuilder.setMoveFromPosX(cmd.getMoveFromPosX());
        resultBuilder.setMoveFromPosY(cmd.getMoveFromPosY());
        resultBuilder.setMoveStartTime(nowTime);

        GameMsgProtocol.UserMoveToResult newResult = resultBuilder.build();
        //把用户移动消息发送给所有用户
        Broadcaster.broadcast(newResult);

    }
}
