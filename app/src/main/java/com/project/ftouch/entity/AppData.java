package com.project.ftouch.entity;

public class AppData {

    private String appName;             // 앱이름
    private String packageName;         // 패키지명

    private int touchCount;             // 터치수

    private boolean sound;              // 소리여부
    private boolean vibration;          // 진동여부

    // 파이어 스토어를 사용하기 위해 필요한 생성자
    public AppData() {}

    public AppData(String appName, String packageName, int touchCount,
                   boolean sound, boolean vibration) {
        this.appName = appName;
        this.packageName = packageName;
        this.touchCount = touchCount;
        this.sound = sound;
        this.vibration = vibration;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getTouchCount() {
        return touchCount;
    }

    public boolean isSound() {
        return sound;
    }

    public boolean isVibration() {
        return vibration;
    }
}
