package org.duo.herostory.cmdhandler;

import io.netty.channel.ChannelHandlerContext;
import org.duo.herostory.model.User;
import org.duo.herostory.model.UserManager;
import org.duo.herostory.msg.GameMsgProtocol;

import java.util.Collection;

/**
 * 处理还有谁在场消息
 */
public class WhoElseIsHereCmdHandler implements ICmdHandler<GameMsgProtocol.WhoElseIsHereCmd> {

    /**
     * 遍历用户字典，获取每一个用户的移动状态，把所有用户的移动状态获取后，推送给客户端
     *
     * @param ctx
     * @param cmd
     */
    public void handle(ChannelHandlerContext ctx, GameMsgProtocol.WhoElseIsHereCmd cmd) {

        if (ctx == null || cmd == null) {
            return;
        }
        GameMsgProtocol.WhoElseIsHereResult.Builder resultBuilder = GameMsgProtocol.WhoElseIsHereResult.newBuilder();
        Collection<User> userList = UserManager.listUser();
        for (User user : userList) {
            if (user == null) {
                continue;
            }
            GameMsgProtocol.WhoElseIsHereResult.UserInfo.Builder userInfoBuilder = GameMsgProtocol.WhoElseIsHereResult.UserInfo.newBuilder();
            userInfoBuilder.setUserId(user.userID);
            userInfoBuilder.setHeroAvatar(user.heroAvatar);

            //构建移动状态
            GameMsgProtocol.WhoElseIsHereResult.UserInfo.MoveState.Builder mvStateBuilder
                    = GameMsgProtocol.WhoElseIsHereResult.UserInfo.MoveState.newBuilder();
            mvStateBuilder.setFromPosX(user.moveState.fromPosX);
            mvStateBuilder.setFromPosY(user.moveState.fromPosY);
            mvStateBuilder.setToPosX(user.moveState.toPosX);
            mvStateBuilder.setToPosY(user.moveState.toPosY);
            mvStateBuilder.setStartTime(user.moveState.startTime);
            userInfoBuilder.setMoveState(mvStateBuilder);

            resultBuilder.addUserInfo(userInfoBuilder);
        }
        // 把用户字典中的用户广播
        GameMsgProtocol.WhoElseIsHereResult newResult = resultBuilder.build();
        ctx.writeAndFlush(newResult);
    }
}