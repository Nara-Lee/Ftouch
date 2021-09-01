package com.project.ftouch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.project.ftouch.entity.User;
import com.project.ftouch.util.Constants;
import com.project.ftouch.util.GlobalVariable;
import com.project.ftouch.util.SharedPreferencesUtils;
import com.project.ftouch.util.Utils;

public class LoginActivity extends AppCompatActivity {
    private static String TAG = LoginActivity.class.getSimpleName();

    private LinearLayout layLoading;

    private EditText editPhone, editPassword;
    private CheckBox ckSave;

    private InputMethodManager imm;                 // 키보드를 숨기기 위해 필요함

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 툴바 안보이게 하기 위함
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);

        // 로딩 레이아웃
        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        this.editPhone = findViewById(R.id.editPhone);
        this.editPhone.setHint("휴대번호를 입력하세요.");

        this.editPassword = findViewById(R.id.editPassword);
        this.editPassword.setHint("비밀번호를 입력하세요.");

        // 로그인 상태 유지
        this.ckSave = findViewById(R.id.ckSave);

        findViewById(R.id.btnLogin).setOnClickListener(mClickListener);
        findViewById(R.id.btnJoin).setOnClickListener(mClickListener);
        findViewById(R.id.layLoading).setOnClickListener(mClickListener);

        // 키보드를 숨기기 위해 필요함
        this.imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // 자신의 휴대번호를 표시
        this.editPhone.setText(Utils.getPhoneNumber(this));
        this.editPhone.requestFocus();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        finish();
        // 프로세스까지 강제 종료
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case Constants.RequestCode.JOIN:
                    // 회원가입 이후 로그인하기
                    this.editPhone.setText(data.getStringExtra("phone"));
                    this.editPassword.setText(data.getStringExtra("password"));

                    this.layLoading.setVisibility(View.VISIBLE);
                    // 로딩 레이아웃을 표시하기 위해 딜레이를 줌
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // 로그인
                            login();
                        }
                    }, Constants.LoadingDelay.SHORT);

                    break;
            }
        }
    }

    /* 입력 데이터 체크 */
    private boolean checkData() {
        // 휴대번호 입력 체크
        String phone = this.editPhone.getText().toString();
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, getString(R.string.msg_phone_number_check_empty), Toast.LENGTH_SHORT).show();
            this.editPhone.requestFocus();
            return false;
        }

        // 휴대번호 유효성 체크
        if (!Utils.isPhoneNumber(phone)) {
            Toast.makeText(this, getString(R.string.msg_phone_number_check_wrong), Toast.LENGTH_SHORT).show();
            this.editPhone.requestFocus();
            return false;
        }

        // 비밀번호 입력 체크
        String password = this.editPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.msg_password_check_empty), Toast.LENGTH_SHORT).show();
            this.editPassword.requestFocus();
            return false;
        }

        // 키보드 숨기기
        this.imm.hideSoftInputFromWindow(this.editPassword.getWindowToken(), 0);

        return true;
    }

    /* 로그인 */
    private void login() {
        final String phone = this.editPhone.getText().toString();
        final String password = this.editPassword.getText().toString();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.USER);

        // 로그인
        Query query = reference.whereEqualTo("phone", phone).limit(1);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                layLoading.setVisibility(View.GONE);

                if (task.isSuccessful()) {
                    if (task.getResult() != null) {
                        if (task.getResult().size() == 0) {
                            // 로그인 실패 (회원이 아님)
                            Toast.makeText(LoginActivity.this, getString(R.string.msg_login_user_none), Toast.LENGTH_SHORT).show();
                        } else {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());

                                User user = document.toObject(User.class);
                                if (user.getPassword().equals(password)) {
                                    // 로그인 성공

                                    // Document Id 저장
                                    GlobalVariable.documentId = document.getId();

                                    // 사용자 객체 생성
                                    GlobalVariable.user = user;

                                    // 로그인 상태 유지이면
                                    if (ckSave.isChecked()) {
                                        // SharedPreferences 에 저장
                                        SharedPreferencesUtils.getInstance(LoginActivity.this)
                                                .put(Constants.SharedPreferencesName.USER_DOCUMENT_ID, GlobalVariable.documentId);
                                    }

                                    // 메인 화면으로 이동
                                    goMain();
                                } else {
                                    // 로그인 실패 (비밀번호 틀림)
                                    Toast.makeText(LoginActivity.this, getString(R.string.msg_login_password_wrong), Toast.LENGTH_SHORT).show();
                                }
                                break;
                            }
                        }
                    } else {
                        // 오류
                        Toast.makeText(LoginActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 오류
                    Toast.makeText(LoginActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /* 메인화면으로 이동 */
    private void goMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        finish();
    }

    /* 회원가입화면으로 이동 */
    private void goJoin() {
        Intent intent = new Intent(this, JoinActivity.class);
        startActivityForResult(intent, Constants.RequestCode.JOIN);
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnLogin:
                    // 로그인
                    if (checkData()) {
                        layLoading.setVisibility(View.VISIBLE);
                        // 로딩 레이아웃을 표시하기 위해 딜레이를 줌
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // 로그인
                                login();
                            }
                        }, Constants.LoadingDelay.SHORT);
                    }

                    break;
                case R.id.btnJoin:
                    // 회원가입 화면으로 이동
                    goJoin();

                    break;
                case R.id.layLoading:
                    // 로딩중 클릭 방지
                    break;
            }
        }
    };
}
