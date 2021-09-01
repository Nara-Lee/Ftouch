package com.project.ftouch.entity;

public class AppItem {

    public String appName;              // 앱이름
    public String packageName;          // 패키지명

    public AppItem(String appName, String packageName) {
        this.appName = appName;
        this.packageName = packageName;
    }
}
