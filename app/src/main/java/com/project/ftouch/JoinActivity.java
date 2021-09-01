package com.project.ftouch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.project.ftouch.entity.User;
import com.project.ftouch.util.Constants;
import com.project.ftouch.util.GlobalVariable;
import com.project.ftouch.util.Utils;

public class JoinActivity extends AppCompatActivity {
    private static String TAG = JoinActivity.class.getSimpleName();

    private LinearLayout layLoading;

    private EditText editPhone, editName, editPassword1, editPassword2;

    private InputMethodManager imm;                 // 키보드를 숨기기 위해 필요함

    // 비밀번호 최소 자리수
    private final static int PASSWORD_MIN_SIZE = 6;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 툴바 안보이게 하기 위함
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_join);

        // 로딩 레이아웃
        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        this.editPhone = findViewById(R.id.editPhone);
        this.editPhone.setHint("휴대번호를 입력하세요.");

        this.editName = findViewById(R.id.editName);
        this.editName.setHint("이름을 입력하세요.");

        this.editPassword1 = findViewById(R.id.editPassword1);
        this.editPassword1.setHint("6글자 이상 입력하세요.");

        this.editPassword2 = findViewById(R.id.editPassword2);
        this.editPassword2.setHint("비밀번호를 확인하세요.");

        findViewById(R.id.btnJoin).setOnClickListener(mClickListener);
        findViewById(R.id.layLoading).setOnClickListener(mClickListener);

        // 키보드를 숨기기 위해 필요함
        this.imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        // 자신의 휴대번호를 표시
        this.editPhone.setText(Utils.getPhoneNumber(this));
        this.editName.requestFocus();
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

        // 이름 입력 체크
        String name = this.editName.getText().toString();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, getString(R.string.msg_user_name_check_empty), Toast.LENGTH_SHORT).show();
            this.editName.requestFocus();
            return false;
        }

        // 비밀번호 입력 체크
        String password1 = this.editPassword1.getText().toString();
        if (TextUtils.isEmpty(password1)) {
            Toast.makeText(this, getString(R.string.msg_password_check_empty), Toast.LENGTH_SHORT).show();
            this.editPassword1.requestFocus();
            return false;
        }

        // 비밀번호 자리수 체크
        if (password1.length() < PASSWORD_MIN_SIZE) {
            Toast.makeText(this, getString(R.string.msg_password_check_length), Toast.LENGTH_SHORT).show();
            this.editPassword1.requestFocus();
            return false;
        }

        // 비밀번호 확인 체크
        String password2 = this.editPassword2.getText().toString();
        if (!password1.equals(password2)) {
            Toast.makeText(this, getString(R.string.msg_password_check_confirm), Toast.LENGTH_SHORT).show();
            this.editPassword2.requestFocus();
            return false;
        }

        // 키보드 숨기기
        this.imm.hideSoftInputFromWindow(this.editPassword2.getWindowToken(), 0);

        return true;
    }

    /* 회원가입 */
    private void join() {
        final String phone = this.editPhone.getText().toString();
        final String name = this.editName.getText().toString();
        final String password = this.editPassword1.getText().toString();

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.USER);

        // 휴대번호 중복 체크
        Query query = reference.whereEqualTo("phone", phone);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult() != null) {
                        if (task.getResult().size() == 0) {
                            // 휴대번호 중복 아님

                            // 자동 문서 ID 값 생성 (컬렉션에 add 하면 document 가 자동 생성됨)
                            final User user = new User(phone, name, password, System.currentTimeMillis());

                            // 회원가입 하기
                            db.collection(Constants.FirestoreCollectionName.USER)
                                    .add(user)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            // 성공

                                            // Document Id 저장
                                            GlobalVariable.documentId = documentReference.getId();

                                            // 사용자 객체 생성
                                            GlobalVariable.user = user;

                                            layLoading.setVisibility(View.GONE);

                                            // 로그인 Activity 에 전달 (바로 로그인 되게 하기 위함)
                                            Intent intent = new Intent();
                                            intent.putExtra("phone", user.getPhone());
                                            intent.putExtra("password", user.getPassword());
                                            setResult(Activity.RESULT_OK, intent);

                                            finish();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // 회원가입 실패
                                            layLoading.setVisibility(View.GONE);
                                            Toast.makeText(JoinActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            // 휴대번호 중복
                            layLoading.setVisibility(View.GONE);
                            Toast.makeText(JoinActivity.this, getString(R.string.msg_phone_number_check_overlap), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        layLoading.setVisibility(View.GONE);
                        Toast.makeText(JoinActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 오류
                    layLoading.setVisibility(View.GONE);
                    Toast.makeText(JoinActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /* 클릭 리스너 */
    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnJoin:
                    // 회원가입
                    if (checkData()) {
                        layLoading.setVisibility(View.VISIBLE);
                        // 로딩 레이아웃을 표시하기 위해 딜레이를 줌
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // 휻대번호 중복체크 후 가입
                                join();
                            }
                        }, Constants.LoadingDelay.SHORT);
                    }

                    break;
                case R.id.layLoading:
                    // 로딩중 클릭 방지
                    break;
            }
        }
    };
}
