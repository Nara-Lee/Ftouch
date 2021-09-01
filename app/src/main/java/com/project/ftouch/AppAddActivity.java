package com.project.ftouch;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.project.ftouch.adapter.MySpinnerAdapter;
import com.project.ftouch.entity.AppData;
import com.project.ftouch.entity.AppItem;
import com.project.ftouch.util.Constants;
import com.project.ftouch.util.GlobalVariable;
import com.project.ftouch.util.Utils;

import java.util.ArrayList;

public class AppAddActivity extends AppCompatActivity {
    private static String TAG = AppAddActivity.class.getSimpleName();

    protected LinearLayout layLoading;

    private Spinner spApp;
    private TextView txtTouchCount;
    private Switch switchSound, switchVibration;

    private ArrayList<String> packageNames;         // 등록된 앱 패키지명 array
    private ArrayList<AppItem> appItems;            // 설치된 앱 array (등록된 앱 제외)

    private int touchCount;                         // 터치수

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_add);

        // 등록된 앱 패키지명 array 정보
        Intent intent = getIntent();
        this.packageNames = intent.getStringArrayListExtra("package_name_list");

        // 제목 표시
        setTitle(getString(R.string.activity_title_app_add));

        // 홈버튼(<-) 표시
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // 로딩 레이아웃
        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        this.spApp = findViewById(R.id.spApp);
        this.txtTouchCount = findViewById(R.id.txtTouchCount);
        this.switchSound = findViewById(R.id.switchSound);
        this.switchVibration = findViewById(R.id.switchVibration);

        findViewById(R.id.btnSave).setOnClickListener(mClickListener);
        findViewById(R.id.imgPlus).setOnClickListener(mClickListener);
        findViewById(R.id.imgMinus).setOnClickListener(mClickListener);
        findViewById(R.id.layLoading).setOnClickListener(mClickListener);

        // 초기화
        init();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* 초기화 */
    private void init() {
        this.touchCount = 0;

        // 설치된 앱 목록 가져오기
        ArrayList<AppItem> items = Utils.getApps(getPackageManager());

        this.appItems = new ArrayList<>();
        // Spinner 구성하기 위한 String array
        ArrayList<String> apps = new ArrayList<>();
        apps.add("선택");
        for (AppItem item : items) {
            boolean exist = false;
            for(String str : this.packageNames) {
                // 등록된 앱인지 체크
                if (str.equals(item.packageName)) {
                    exist = true;
                    break;
                }
            }

            // 등록되지 않은 앱만 Spinner 에 추가
            if (!exist) {
                apps.add(item.appName);
                this.appItems.add(item);
            }
        }

        // 앱 Adapter 구성
        spApp.setAdapter(new MySpinnerAdapter(this, apps, MySpinnerAdapter.TYPE_NORMAL, true));
    }

    /* 입력 데이터 체크 */
    private boolean checkData() {
        // 앱선택 체크
        if (this.spApp.getSelectedItemPosition() == 0) {
            Toast.makeText(this, getString(R.string.msg_app_select_empty), Toast.LENGTH_SHORT).show();
            return false;
        }

        // 터치수 체크
        if (this.touchCount <= 0) {
            Toast.makeText(this, getString(R.string.msg_touch_count_empty), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /* 앱 저장 */
    private void save() {
        int position = this.spApp.getSelectedItemPosition() - 1;
        String appName = this.appItems.get(position).appName;           // 앱이름
        String packageName = this.appItems.get(position).packageName;   // 패키지이름
        boolean sound = this.switchSound.isChecked();                   // 소리여부
        boolean vibration = this.switchVibration.isChecked();           // 진동여부

        // 앱정보
        final AppData appData = new AppData(appName, packageName, this.touchCount, sound, vibration);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.documentId).collection(Constants.FirestoreCollectionName.APP);

        // 터치수 중복 체크
        Query query = reference.whereEqualTo("touchCount", this.touchCount).limit(1);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult() != null) {
                        if (task.getResult().size() == 0) {
                            // 터치수 중복 아님
                            // 앱 등록
                            db.collection(Constants.FirestoreCollectionName.USER)
                                    .document(GlobalVariable.documentId).collection(Constants.FirestoreCollectionName.APP)
                                    .add(appData)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            // 성공
                                            layLoading.setVisibility(View.GONE);
                                            // 앱목록이 push 방식 이라 파이어스토어 수신 리스너에서 자동 추가됨
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // 실패
                                            layLoading.setVisibility(View.GONE);
                                            Toast.makeText(AppAddActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            // 터치수 중복
                            layLoading.setVisibility(View.GONE);
                            Toast.makeText(AppAddActivity.this, getString(R.string.msg_touch_count_check_overlap), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        layLoading.setVisibility(View.GONE);
                        Toast.makeText(AppAddActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 오류
                    layLoading.setVisibility(View.GONE);
                    Toast.makeText(AppAddActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /* 클릭 리스너 */
    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnSave:
                    // 저장
                    // 입력 체크
                    if (checkData()) {
                        // 저장
                        layLoading.setVisibility(View.VISIBLE);
                        // 로딩 레이아웃을 표시하기 위해 딜레이 적용
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // 앱 저장
                                save();
                            }
                        }, Constants.LoadingDelay.SHORT);
                    }
                    break;
                case R.id.imgPlus:
                    // 터치수 +
                    touchCount++;
                    txtTouchCount.setText(String.valueOf(touchCount));
                    break;
                case R.id.imgMinus:
                    // 터치수 -
                    if (touchCount == 0) {
                        return;
                    }
                    touchCount--;
                    txtTouchCount.setText(String.valueOf(touchCount));
                    break;
                case R.id.layLoading:
                    // 로딩중 클릭 방지
                    break;
            }
        }
    };
}
