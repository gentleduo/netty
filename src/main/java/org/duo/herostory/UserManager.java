package org.duo.herostory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户管理
 */
public final class UserManager {

    /**
     * 用户字典
     */
    static private final Map<Integer, User> _userMap = new ConcurrentHashMap<>();

    /**
     * 私有化类默认构造器
     */
    private UserManager() {
    }

    /**
     * 添加用户
     *
     * @param u
     */
    public static void addUser(User u) {
        if (u != null) {
            _userMap.putIfAbsent(u.userID, u);
        }
    }

    /**
     * 删除用户
     *
     * @param userId
     */
    public static void removeByUserId(int userId) {
        _userMap.remove(userId);
    }

    /**
     * 列表用户
     *
     * @return
     */
    public static Collection<User> listUser() {
        return _userMap.values();
    }
}
