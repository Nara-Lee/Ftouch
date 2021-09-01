package com.project.ftouch.entity;

public class AppDataItem {

    public String id;           // AppData Doc ID
    public AppData appData;     // AppData 객체

    public AppDataItem(String id, AppData appData) {
        this.id = id;
        this.appData = appData;
    }
}
