package org.duo.herostory.cmdhandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.duo.herostory.Broadcaster;
import org.duo.herostory.model.User;
import org.duo.herostory.model.UserManager;
import org.duo.herostory.msg.GameMsgProtocol;

public class UserEntryCmdHandler implements ICmdHandler<GameMsgProtocol.UserEntryCmd> {

    @Override
    public void handle(ChannelHandlerContext ctx, GameMsgProtocol.UserEntryCmd cmd) {

        if (ctx == null || cmd == null) {
            return;
        }
        int userID = cmd.getUserId();
        String heroAvatar = cmd.getHeroAvatar();

        User newUser = new User();
        newUser.userID = userID;
        newUser.heroAvatar = heroAvatar;
        UserManager.addUser(newUser);

        // 将用户ID保存至session
        ctx.channel().attr(AttributeKey.valueOf("userId")).set(userID);

        // 构造结果并广播
        GameMsgProtocol.UserEntryResult.Builder resultBuilder = GameMsgProtocol.UserEntryResult.newBuilder();
        resultBuilder.setUserId(userID);
        resultBuilder.setHeroAvatar(heroAvatar);
        GameMsgProtocol.UserEntryResult newResult = resultBuilder.build();
        Broadcaster.broadcast(newResult);
    }
}
