package org.duo.herostory.model;

public class User {

    /**
     * 用户ID
     */
    public int userID;
    /**
     * 用户形象
     */
    public String heroAvatar;
    /**
     * 移动状态
     */
    public final MoveState moveState = new MoveState();
}
