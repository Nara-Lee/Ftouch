package com.project.ftouch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.project.ftouch.entity.AppData;
import com.project.ftouch.util.Constants;
import com.project.ftouch.util.GlobalVariable;
import com.project.ftouch.util.SharedPreferencesUtils;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static String TAG = MainActivity.class.getSimpleName();

    private BackPressHandler backPressHandler;

    private Timer timer;
    private TimerTask timerTask;

    private LinearLayout layTouch;

    private SoundPool soundPool;            // 사운드 풀 (앱 실행시 소리)
    private int soundId;                    // 사운드 id

    private boolean startTouch = false;     // 터치 시작여부
    private int touchCount;                 // 터치수
    private int touchTime;                  // 터치시간(초)

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 제목 표시
        setTitle(getString(R.string.activity_title_main));

        this.layTouch = findViewById(R.id.layTouch);

        this.layTouch.setOnClickListener(mClickListener);
        findViewById(R.id.btnAppList).setOnClickListener(mClickListener);

        // 회원정보
        ((TextView) findViewById(R.id.txtName)).setText(GlobalVariable.user.getName());
        ((TextView) findViewById(R.id.txtPhone)).setText(GlobalVariable.user.getPhone());

        // 사운드 초기화
        // 사운드 생성
        this.soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .build();

        // 사운드 로드
        this.soundId = this.soundPool.load(this, R.raw.sound, 1);

        // 종료 핸들러
        this.backPressHandler = new BackPressHandler(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // TimerTask stop
        stopTimerTask();

        if (this.soundPool != null) {
            this.soundPool.release();
            this.soundPool = null;
        }
    }

    @Override
    public void onBackPressed() {
        this.backPressHandler.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // main 메뉴 생성
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                // 로그아웃
                new AlertDialog.Builder(this)
                        .setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(@NonNull DialogInterface dialog, int which) {
                                // 로그아웃
                                logout();
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_cancel), null)
                        .setCancelable(false)
                        .setTitle(getString(R.string.dialog_title_logout))
                        .setMessage(getString(R.string.dialog_msg_logout))
                        .show();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* 로그아웃 */
    private void logout() {
        // Document Id 값 clear
        SharedPreferencesUtils.getInstance(this)
                .put(Constants.SharedPreferencesName.USER_DOCUMENT_ID, "");

        // 로그인화면으로 이동
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    /* 앱 실행 */
    private void executeApp() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.documentId).collection(Constants.FirestoreCollectionName.APP);

        // 터치수
        Query query = reference.whereEqualTo("touchCount", this.touchCount).limit(1);
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
                                final String packageName = appData.getPackageName();

                                try {
                                    // 앱 실행
                                    Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                } catch (Exception e) {
                                    // 앱이 없으면 플레이스토어로 이동
                                    new AlertDialog.Builder(MainActivity.this)
                                            .setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(@NonNull DialogInterface dialog, int which) {
                                                    String url = "market://details?id=" + packageName;
                                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                                    startActivity(intent);
                                                }
                                            })
                                            .setNegativeButton(getString(R.string.dialog_cancel), null)
                                            .setCancelable(false)
                                            .setTitle(getString(R.string.dialog_title_play_store))
                                            .setMessage(getString(R.string.dialog_msg_play_store))
                                            .show();
                                }
                                break;
                            }
                        } else {
                            // 앱이 없음
                            Toast.makeText(MainActivity.this, getString(R.string.msg_app_empty), Toast.LENGTH_SHORT).show();
                        }

                        layTouch.setEnabled(true);
                    } else {
                        layTouch.setEnabled(true);
                        Toast.makeText(MainActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 오류
                    layTouch.setEnabled(true);
                    Toast.makeText(MainActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /* Timer 시작 */
    private void startTimer() {
        this.timer = new Timer();

        // TimerTask 초기화
        initTimerTask();

        // 1초마다 체크
        this.timer.schedule(timerTask, 1000, 1000);
    }

    /* TimerTask 초기화 */
    private void initTimerTask() {
        this.timerTask = new TimerTask() {
            @Override
            public void run() {
                // 위젯을 컨트롤 하기 위해 Main Thread 가 필요함
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        touchTime++;

                        Log.d(TAG, "time:" + touchTime);

                        if (touchTime == Constants.TOUCH_DELAY_SECOND) {
                            // TimerTask stop
                            stopTimerTask();

                            // 터치 못하게 막음
                            layTouch.setEnabled(false);
                            startTouch = false;

                            Log.d(TAG, "executeApp");

                            // 앱 실행
                            executeApp();
                        }
                    }
                });
            }
        };
    }

    /* TimerTask stop */
    private void stopTimerTask() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }
    }

    /* 종료 */
    public void end() {
        moveTaskToBack(true);
        finish();
        // 프로세스까지 강제 종료
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btnAppList:
                    // 앱목록
                    Intent intent = new Intent(MainActivity.this, AppListActivity.class);
                    startActivity(intent);
                    break;
                case R.id.layTouch:
                    // 터치영역

                    if (startTouch) {
                        touchTime = 0;
                        touchCount++;
                    } else {
                        // 터치 시작
                        touchCount = 1;
                        touchTime = 0;
                        startTouch = true;

                        Log.d(TAG, "startTimer");

                        // Timer 시작
                        startTimer();
                    }
                    break;
            }
        }
    };

    /* Back Press Class */
    private class BackPressHandler {
        private Context context;
        private Toast toast;

        private final long FINISH_INTERVAL_TIME = 2000;
        private long backPressedTime = 0;

        public BackPressHandler(Context context) {
            this.context = context;
        }

        public void onBackPressed() {
            if (System.currentTimeMillis() > this.backPressedTime + FINISH_INTERVAL_TIME) {
                this.backPressedTime = System.currentTimeMillis();

                this.toast = Toast.makeText(this.context, R.string.msg_back_press_end, Toast.LENGTH_SHORT);
                this.toast.show();

                return;
            }

            if (System.currentTimeMillis() <= this.backPressedTime + FINISH_INTERVAL_TIME) {
                // 종료
                end();
                this.toast.cancel();
            }
        }
    }
}