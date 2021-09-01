package com.project.ftouch.util;

public class Constants {

    public static final String APP_WIDGET_ACTION_TOUCH = "com.project.ftouch.TOUCH";

    public static final int TOUCH_DELAY_SECOND = 2;         // 터치 딜레이 (2초)

    /* SharedPreferences 관련 상수 */
    public static class SharedPreferencesName {
        // 사용자 Fire store Document ID
        public static final String USER_DOCUMENT_ID = "user_document_id";

        public static final String APP_WIDGET_TOUCH_TIME = "app_widget_touch_time";
        public static final String APP_WIDGET_TOUCH_COUNT = "app_widget_touch_count";
    }

    /* Activity 요청 코드 */
    public static class RequestCode {
        public static final int JOIN = 0;
    }

    /* Fire store Collection 이름 */
    public static class FirestoreCollectionName {
        public static final String USER = "users";          // 사용자
        public static final String APP = "apps";            // 앱 설정정보
    }

    /* 클릭 모드 */
    public static class ClickMode {
        public static final int NORMAL = 0;                 // 클릭
        public static final int LONG = 1;                   // 롱클릭
    }

    /* 로딩 딜레이 */
    public static class LoadingDelay {
        public static final int SHORT = 300;
        public static final int LONG = 1000;
    }

}
