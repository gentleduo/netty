package org.duo.herostory.cmdhandler;

import io.netty.channel.ChannelHandlerContext;
import org.duo.herostory.User;
import org.duo.herostory.UserManager;
import org.duo.herostory.msg.GameMsgProtocol;

import java.util.Collection;

public class WhoElseIsHereCmdHandler implements ICmdHandler<GameMsgProtocol.WhoElseIsHereCmd> {

    @Override
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
            resultBuilder.addUserInfo(userInfoBuilder);
        }
        GameMsgProtocol.WhoElseIsHereResult newResult = resultBuilder.build();
        ctx.writeAndFlush(newResult);
    }
}
