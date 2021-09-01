package com.project.ftouch;

import android.content.DialogInterface;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.project.ftouch.adapter.AppAdapter;
import com.project.ftouch.entity.AppData;
import com.project.ftouch.entity.AppDataItem;
import com.project.ftouch.util.Constants;
import com.project.ftouch.util.GlobalVariable;
import com.project.ftouch.util.IAdapterOnClickListener;

import java.util.ArrayList;

public class AppListActivity extends AppCompatActivity {
    private static String TAG = AppListActivity.class.getSimpleName();

    // Firestore 수신대기 리스너
    private ListenerRegistration registration;

    protected LinearLayout layLoading;

    private RecyclerView recyclerView;
    protected AppAdapter adapter;

    private ArrayList<AppDataItem> items;

    // 데이터 없을때 표시할 레이아웃
    private LinearLayout layNoData;

    private TextView txtCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        // 제목 표시
        setTitle(getString(R.string.activity_title_app_list));

        // 홈버튼(<-) 표시
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // 로딩 레이아웃
        this.layLoading = findViewById(R.id.layLoading);
        ((ProgressBar) findViewById(R.id.progressBar)).setIndeterminateTintList(ColorStateList.valueOf(Color.WHITE));

        // 리사이클러뷰
        this.recyclerView = findViewById(R.id.recyclerView);
        this.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        this.layNoData = findViewById(R.id.layNoData);

        this.txtCount = findViewById(R.id.txtCount);

        findViewById(R.id.fabAdd).setOnClickListener(mClickListener);
        findViewById(R.id.layLoading).setOnClickListener(mClickListener);

        this.items = new ArrayList<>();
        this.adapter = new AppAdapter(mAdapterListener, items);
        this.recyclerView.setAdapter(adapter);

        this.layNoData.setVisibility(View.VISIBLE);
        this.txtCount.setText("0건");

        // 앱목록 (push 방식)
        listApp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Firestore 수신대기 리스너 제거
        if (this.registration != null) {
            this.registration.remove();
        }
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

    /* 앱목록 (push 방식) */
    private void listApp() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 앱목록 가져오기
        CollectionReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.documentId).collection(Constants.FirestoreCollectionName.APP);
        // 앱이름으로 정렬
        Query query = reference.orderBy("appName");
        this.registration = query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    // 오류
                    Toast.makeText(AppListActivity.this, getString(R.string.msg_listener_error), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, e.toString());
                    return;
                }

                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    switch (dc.getType()) {
                        case ADDED:
                            Log.d(TAG, "ADDED");

                            // 앱 item 구성
                            AppData appData = dc.getDocument().toObject(AppData.class);

                            layNoData.setVisibility(View.GONE);

                            // 최 상단에 추가
                            adapter.add(new AppDataItem(dc.getDocument().getId(), appData), 0);
                            recyclerView.scrollToPosition(0);

                            txtCount.setText(items.size() + "건");

                            break;
                        case MODIFIED:
                            Log.d(TAG, "MODIFIED");

                            // 앱 해당 위치 찾음
                            for (int i=0; i<items.size(); i++) {
                                if (items.get(i).id.equals(dc.getDocument().getId())) {
                                    AppDataItem dataItem = items.get(i);
                                    dataItem.appData = dc.getDocument().toObject(AppData.class);

                                    // 리스트에 적용
                                    adapter.notifyItemChanged(i);
                                    break;
                                }
                            }

                            break;
                        case REMOVED:
                            Log.d(TAG, "REMOVED");

                            // 앱 해당 위치 찾음
                            for (int i=0; i<items.size(); i++) {
                                if (items.get(i).id.equals(dc.getDocument().getId())) {
                                    // 앱 삭제
                                    adapter.remove(i);
                                    break;
                                }
                            }

                            txtCount.setText(items.size() + "건");

                            // 앱이 없으면
                            if (items.size() == 0) {
                                layNoData.setVisibility(View.VISIBLE);
                            }

                            break;
                    }
                }
            }
        });
    }

    /* 앱 삭제 */
    private void deleteApp(String appId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // 앱 document 참조
        DocumentReference reference = db.collection(Constants.FirestoreCollectionName.USER)
                .document(GlobalVariable.documentId).collection(Constants.FirestoreCollectionName.APP)
                .document(appId);
        // 앱 삭제
        reference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // 성공
                        layLoading.setVisibility(View.GONE);

                        // 앱목록이 push 방식 이라 파이어스토어 수신 리스너에서 자동 삭제됨
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // 실패
                layLoading.setVisibility(View.GONE);
                Toast.makeText(AppListActivity.this, getString(R.string.msg_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /* 리스트 클릭 리스너 */
    private IAdapterOnClickListener mAdapterListener = new IAdapterOnClickListener() {
        @Override
        public void onItemClick(Bundle bundle, int id) {
            // 선택
            int mode = bundle.getInt("click_mode");
            final String appId = bundle.getString("id");

            if (mode == Constants.ClickMode.LONG) {
                // 롱클릭 (삭제)
                new AlertDialog.Builder(AppListActivity.this)
                        .setPositiveButton(getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(@NonNull DialogInterface dialog, int which) {
                                layLoading.setVisibility(View.VISIBLE);
                                // 로딩 레이아웃을 표시하기 위해 딜레이를 줌
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 앱 삭제
                                        deleteApp(appId);
                                    }
                                }, Constants.LoadingDelay.SHORT);
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_cancel), null)
                        .setCancelable(false)
                        .setTitle(getString(R.string.dialog_title_app_delete))
                        .setMessage(getString(R.string.dialog_msg_app_delete))
                        .show();
            } else {
                // 정보보기 (수정모드)
                Intent intent = new Intent(AppListActivity.this, AppEditActivity.class);
                intent.putExtra("app_id", appId);
                startActivity(intent);
            }
        }
    };

    /* 클릭 리스너 */
    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.fabAdd:
                    // 등록
                    // 등록된 앱 패키지명 구성
                    ArrayList<String> packageNames = new ArrayList<>();
                    for (AppDataItem item : items) {
                        packageNames.add(item.appData.getPackageName());
                    }
                    Intent intent = new Intent(AppListActivity.this, AppAddActivity.class);
                    intent.putExtra("package_name_list", packageNames);
                    startActivity(intent);
                    break;
                case R.id.layLoading:
                    // 로딩중 클릭 방지
                    break;
            }
        }
    };
}
