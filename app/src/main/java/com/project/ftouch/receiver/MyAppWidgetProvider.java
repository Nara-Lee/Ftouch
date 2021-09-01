package com.project.ftouch.receiver;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.project.ftouch.R;
import com.project.ftouch.service.TouchService;
import com.project.ftouch.util.Constants;
import com.project.ftouch.util.SharedPreferencesUtils;

public class MyAppWidgetProvider extends AppWidgetProvider {
    private static String TAG = MyAppWidgetProvider.class.getSimpleName();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);

            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // 26 이후 버전
                intent = new Intent(context, MyAppWidgetProvider.class);
                intent.setAction(Constants.APP_WIDGET_ACTION_TOUCH);
            } else {
                intent = new Intent(Constants.APP_WIDGET_ACTION_TOUCH);
            }
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.layWidgetTouch, pendingIntent);

            appWidgetManager.updateAppWidget(id, views);

            Log.d(TAG, "onUpdate");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        //RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);

        final String action = intent.getAction();
        // 터치
        if (action.equals(Constants.APP_WIDGET_ACTION_TOUCH)) {
            int count;                                  // 터치수
            long time = System.currentTimeMillis();     // 터치 시간 (ms)

            // 이전 터치시간 (ms)
            long beforeTime = SharedPreferencesUtils.getInstance(context).get(Constants.SharedPreferencesName.APP_WIDGET_TOUCH_TIME, (long) 0);
            if ((time - beforeTime) > (Constants.TOUCH_DELAY_SECOND * 1000)) {
                // 터치 딜레이 시간 (2초)이 지났으면 터치수 초기화
                count = 0;
            } else {
                count = SharedPreferencesUtils.getInstance(context).get(Constants.SharedPreferencesName.APP_WIDGET_TOUCH_COUNT, 0);
            }
            // 터치수 증가
            count++;

            // 터치시간 저장
            SharedPreferencesUtils.getInstance(context).put(Constants.SharedPreferencesName.APP_WIDGET_TOUCH_TIME, time);
            // 터치수 저장
            SharedPreferencesUtils.getInstance(context).put(Constants.SharedPreferencesName.APP_WIDGET_TOUCH_COUNT, count);

            // 터치 체크 Service
            Intent serviceIntent = new Intent(context, TouchService.class);
            serviceIntent.putExtra("touch_time", time);
            serviceIntent.putExtra("touch_count", count);

            // Oreo 버전 이후부터는 Background 에서 실행을 금지하기 때문에 Foreground 에서 실행해야 함
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }

        Log.d(TAG, "onReceive:" + action);
    }
}
