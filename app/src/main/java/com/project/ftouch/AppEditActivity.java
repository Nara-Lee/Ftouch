package com.project.ftouch;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.project.ftouch.entity.AppData;
import com.project.ftouch.util.Constants;
import com.project.ftouch.util.GlobalVariable;

public class AppEditActivity extends AppCompatActivity {
    private static String TAG = AppAddActivity.class.getSimpleName();

    protected LinearLayout layLoading;

    private TextView txtAppName, txtTouchCount;
    private Switch switchSound, switchVibration;
    private Button btnSave;

    private String appId;                           // 앱 id
    private AppData appData;                        // 앱 객체

    private int touchCount;                         // 터치수

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_edit);

        // 앱 id 정보
        Intent intent = getIntent();
        this.appId = intent.getStringExtra("app_id");

        // 제목 표시
        setTitle(getString(R.string.activity_title_app_edit));

        // 홈버튼(<-) 표시
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // 로딩 레이아웃
        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        this.txtAppName = findViewById(R.id.txtAppName);
        this.txtTouchCount = findViewById(R.id.txtTouchCount);
        this.switchSound = findViewById(R.id.switchSound);
        this.switchVibration = findViewById(R.id.switchVibration);

        this.btnSave = findViewById(R.id.btnSave);

        this.btnSave.setOnClickListener(mClickListener);
        findViewById(R.id.imgPlus).setOnClickListener(mClickListener);
        findViewById(R.id.imgMinus).setOnClickListener(mClickListener);
        findViewById(R.id.layLoading).setOnClickListener(mClickListener);

        // 앱 정보 보기
        infoApp();
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

    /* 앱 정보 */
    private void infoApp() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.documentId).collection(Constants.FirestoreCollectionName.APP)
                .document(this.appId);
        reference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    // 성공
                    DocumentSnapshot document = task.getResult();
                    if (document != null) {
                        appData = document.toObject(AppData.class);
                        if (appData != null) {
                            txtAppName.setText(appData.getAppName());           // 앱이름

                            // 터치수
                            touchCount = appData.getTouchCount();
                            txtTouchCount.setText(String.valueOf(touchCount));

                            switchSound.setChecked(appData.isSound());          // 소리여부
                            switchVibration.setChecked(appData.isVibration());  // 진동여부
                        }
                    } else {
                        btnSave.setEnabled(false);
                        Toast.makeText(AppEditActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 실패
                    btnSave.setEnabled(false);
                    Toast.makeText(AppEditActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /* 입력 데이터 체크 */
    private boolean checkData() {
        // 터치수 체크
        if (this.touchCount <= 0) {
            Toast.makeText(this, getString(R.string.msg_touch_count_empty), Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /* 앱 저장 */
    private void save() {
        final boolean sound = this.switchSound.isChecked();                 // 소리여부
        final boolean vibration = this.switchVibration.isChecked();         // 진동여부

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.documentId).collection(Constants.FirestoreCollectionName.APP);

        // 터치수 중복 체크
        Query query = reference.whereEqualTo("touchCount", this.touchCount);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult() != null) {
                        boolean overlap = false;
                        switch (task.getResult().size()) {
                            case 0:
                                break;
                            case 1:
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    // 수정할 앱인지 체크
                                    String packageName = document.getData().get("packageName").toString();
                                    if (!packageName.equals(appData.getPackageName())) {
                                        overlap = true;
                                    }
                                }
                                break;
                            default:
                                overlap = true;
                                break;
                        }

                        if (!overlap) {
                            // 터치수 중복 아님
                            // 앱 수정
                            db.collection(Constants.FirestoreCollectionName.USER)
                                    .document(GlobalVariable.documentId).collection(Constants.FirestoreCollectionName.APP)
                                    .document(appId)
                                    .update("touchCount", touchCount, "sound", sound, "vibration", vibration)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // 성공
                                            layLoading.setVisibility(View.GONE);
                                            // 앱목록이 push 방식 이라 파이어스토어 수신 리스너에서 자동 수정됨
                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // 실패
                                            layLoading.setVisibility(View.GONE);
                                            Toast.makeText(AppEditActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            // 터치수 중복
                            layLoading.setVisibility(View.GONE);
                            Toast.makeText(AppEditActivity.this, getString(R.string.msg_touch_count_check_overlap), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        layLoading.setVisibility(View.GONE);
                        Toast.makeText(AppEditActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 오류
                    layLoading.setVisibility(View.GONE);
                    Toast.makeText(AppEditActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
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
                    // 저장 (변경내용 저장)
                    if (appData  == null) {
                        return;
                    }

                    // 입력 체크
                    if (checkData()) {
                        // 저장 (변경내용 저장)
                        new AlertDialog.Builder(AppEditActivity.this)
                                .setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(@NonNull DialogInterface dialog, int which) {
                                        layLoading.setVisibility(View.VISIBLE);
                                        // 로딩 레이아웃을 표시하기 위해 딜레이를 줌
                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                // 앱 변경내역 저장
                                                save();
                                            }
                                        }, Constants.LoadingDelay.SHORT);
                                    }
                                })
                                .setNegativeButton(getString(R.string.dialog_cancel), null)
                                .setCancelable(false)
                                .setTitle(getString(R.string.dialog_title_app_edit))
                                .setMessage(getString(R.string.dialog_msg_app_edit))
                                .show();
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
