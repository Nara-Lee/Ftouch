package com.project.ftouch.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.ftouch.R;
import com.project.ftouch.entity.AppDataItem;
import com.project.ftouch.util.Constants;
import com.project.ftouch.util.IAdapterOnClickListener;

import java.util.ArrayList;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
    private static final String TAG = AppAdapter.class.getSimpleName();

    private IAdapterOnClickListener listener;
    private ArrayList<AppDataItem> items;

    public AppAdapter(IAdapterOnClickListener listener, ArrayList<AppDataItem> items) {
        this.listener = listener;
        this.items = items;
    }

    /* 추가 */
    public void add(AppDataItem data, int position) {
        position = position == -1 ? getItemCount()  : position;
        // 앱 추가
        this.items.add(position, data);
        // 추가된 앱을 리스트에 적용하기 위함
        notifyItemInserted(position);
    }

    /* 삭제 */
    public AppDataItem remove(int position){
        AppDataItem data = null;

        if (position < getItemCount()) {
            data = this.items.get(position);
            // 앱 삭제
            this.items.remove(position);
            // 삭제된 앱을 리스트에 적용하기 위함
            notifyItemRemoved(position);
        }

        return data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, null);

        // Item 사이즈 조절
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);

        // ViewHolder 생성
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtAppName.setText(this.items.get(position).appData.getAppName());           // 앱이름
        holder.txtPackageName.setText(this.items.get(position).appData.getPackageName());   // 패키지명
        holder.txtTouchCount.setText(String.valueOf(this.items.get(position).appData.getTouchCount()));     // 터치수

        // 소리여부
        if (this.items.get(position).appData.isSound()) {
            holder.txtSoundState.setText("ON");
        } else {
            holder.txtSoundState.setText("OFF");
        }

        // 진동여부
        if (this.items.get(position).appData.isVibration()) {
            holder.txtVibrationState.setText("ON");
        } else {
            holder.txtVibrationState.setText("OFF");
        }
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView txtAppName, txtPackageName, txtTouchCount, txtSoundState, txtVibrationState;

        ViewHolder(View view) {
            super(view);

            this.txtAppName = view.findViewById(R.id.txtAppName);
            this.txtPackageName = view.findViewById(R.id.txtPackageName);
            this.txtTouchCount = view.findViewById(R.id.txtTouchCount);
            this.txtSoundState = view.findViewById(R.id.txtSoundState);
            this.txtVibrationState = view.findViewById(R.id.txtVibrationState);

            view.setOnClickListener(this);          // 클릭 리스너 등록
            view.setOnLongClickListener(this);      // 롱클릭 리스너 등록
        }

        @Override
        public void onClick(View view) {
            Bundle bundle = new Bundle();
            int position = getAdapterPosition();

            bundle.putInt("position", position);
            bundle.putInt("click_mode", Constants.ClickMode.NORMAL);
            bundle.putString("id", items.get(position).id);
            listener.onItemClick(bundle, view.getId());
        }

        @Override
        public boolean onLongClick(View view) {
            // 롱클릭시 삭제 처리 하기
            Bundle bundle = new Bundle();
            int position = getAdapterPosition();

            bundle.putInt("position", position);
            bundle.putInt("click_mode", Constants.ClickMode.LONG);
            bundle.putString("id", items.get(position).id);
            listener.onItemClick(bundle, view.getId());

            // 다른데서는 처리할 필요없음 true
            return true;
        }
    }
}
