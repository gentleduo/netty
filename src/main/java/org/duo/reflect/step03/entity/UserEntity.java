package org.duo.reflect.step03.entity;

public class UserEntity {

    @Column(name = "user_id")
    public int _userId;

    @Column(name = "user_name")
    public String _userName;

    @Column(name = "password")
    public String _password;
}
