package com.project.ftouch.entity;

public class User {

    private String phone;                           // 휴대번호 (아이디로 사용)

    private String name;
    private String password;

    private long joinTimeMillis;                    // 가입일시를 millisecond 로 표현

    public User() {}

    public User(String phone, String name, String password, long joinTimeMillis) {
        this.phone = phone;
        this.name = name;
        this.password = password;
        this.joinTimeMillis = joinTimeMillis;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getName() {
        return this.name;
    }

    public String getPassword() {
        return this.password;
    }

    public long getJoinTimeMillis() {
        return this.joinTimeMillis;
    }
}
