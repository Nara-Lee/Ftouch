package com.project.ftouch.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.project.ftouch.IntroActivity;
import com.project.ftouch.R;
import com.project.ftouch.entity.AppData;
import com.project.ftouch.util.Constants;
import com.project.ftouch.util.SharedPreferencesUtils;

public class TouchService extends Service {
    private static final String TAG = TouchService.class.getSimpleName();

    private SoundPool soundPool;            // 사운드 풀 (앱 실행시 소리)
    private int soundId;                    // 사운드 id

    public TouchService() {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 사운드 초기화
        // 사운드 생성
        this.soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .build();

        // 사운드 로드
        this.soundId = soundPool.load(this, R.raw.sound, 1);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final long touchTime = intent.getLongExtra("touch_time", 0);    // 터치시간 (ms)
        final int touchCount = intent.getIntExtra("touch_count", 0);    // 터치수

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 26 이후 버전부터는 channel 이 필요함
            String channelId = createNotificationChannel();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
            Notification notification = builder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(true)
                    .build();

            startForeground(1, notification);
        }

        Log.d(TAG, "onStartCommand");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // 서비스가 구동되고 나서 터치가 있었는지 확인하기 위함
                long time = SharedPreferencesUtils.getInstance(TouchService.this)
                        .get(Constants.SharedPreferencesName.APP_WIDGET_TOUCH_TIME, (long) 0);

                if (touchTime == time) {
                    // 일정시간 (2초) 동안 터치가 없으면 실행할 앱 체크

                    // 사용자 등록 Doc ID 값으로 자동 로그인 인지 체크
                    String id = SharedPreferencesUtils.getInstance(TouchService.this)
                            .get(Constants.SharedPreferencesName.USER_DOCUMENT_ID);

                    if (TextUtils.isEmpty(id)) {
                        // 로그인이 안된 상태
                        Intent intent1 = new Intent(TouchService.this, IntroActivity.class);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent1);
                    } else {
                        // 앱 실행
                        executeApp(id, touchCount);
                    }

                    Log.d(TAG, "executeApp");
                }
            }
        }, Constants.TOUCH_DELAY_SECOND * 1000);

        Log.d(TAG, "onStartCommand");

        stopSelf();

        return START_NOT_STICKY;
    }

    /* 앱 실행 */
    private void executeApp(String id, int touchCount) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(id).collection(Constants.FirestoreCollectionName.APP);

        // 터치수 확인
        Query query = reference.whereEqualTo("touchCount", touchCount).limit(1);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult() != null) {
                        if (task.getResult().size() > 0) {
                            // 터치수와 매핑되는 앱이 있음
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                AppData appData = document.toObject(AppData.class);

                                // 소리
                                if (appData.isSound()) {
                                    soundPool.play(soundId, 0.5f, 0.5f, 1, 0, 1.0f);
                                }

                                // 진동
                                if (appData.isVibration()) {
                                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                    if (vibrator != null) {
                                        vibrator.vibrate(500);
                                    }
                                }

                                // 실행할 앱 패키지명
                                String packageName = appData.getPackageName();

                                Intent intent;
                                try {
                                    // 앱 실행
                                    intent = getPackageManager().getLaunchIntentForPackage(packageName);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                } catch (Exception e) {
                                    // 앱이 없으면 (서비스에서는 플레이스토어로 이동 안됨)
                                    Toast.makeText(TouchService.this, getString(R.string.msg_app_not_installed), Toast.LENGTH_SHORT).show();
                                }
                                break;
                            }
                        } else {
                            // 앱이 없음
                            Toast.makeText(TouchService.this, getString(R.string.msg_app_empty), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(TouchService.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 오류
                    Toast.makeText(TouchService.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelId = "ftouch";
        String channelName = getString(R.string.app_name);
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        //channel.setDescription(channelName);
        channel.setSound(null, null);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        return channelId;
    }
}
