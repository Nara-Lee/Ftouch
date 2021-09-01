package com.project.ftouch.adapter;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.project.ftouch.R;

import java.util.ArrayList;
import java.util.Arrays;

public class MySpinnerAdapter extends BaseAdapter implements SpinnerAdapter {
    private static String TAG = MySpinnerAdapter.class.getSimpleName();

    private final Context context;
    private ArrayList<String> items;

    // db 사이즈를 구하기 위함
    private DisplayMetrics dm;

    private byte type;

    private int normalColor;
    private int dropDownColor;
    private int selectedColor;

    // 풀 사이즈가 아니면 getView()를 항상 아이템 수 만큼 호출함
    private boolean fullSize;
    private boolean visibleIcon = true;

    // 화살표 아이콘
    private int resIcon;

    // DropDown 텍스트 색상 적용 시작위치 (-1은 적용안함)
    private int selectedDropDownStartPosition;
    // 선택 텍스트 색상 적용 시작위치 (-1은 적용안함)
    private int selectedTextStartPosition;

    private int selectedPosition = 0;

    public static final byte TYPE_NORMAL = 0;
    public static final byte TYPE_SMALL = 1;
    public static final byte TYPE_LARGE = 2;

    private static final byte PADDING_TEXT_SIZE_NORMAL = 8;
    private static final byte PADDING_TEXT_SIZE_SMALL = 4;
    private static final byte PADDING_TEXT_SIZE_LARGE = 12;

    private static final byte PADDING_DROPDOWN_SIZE_NORMAL = 12;
    private static final byte PADDING_DROPDOWN_SIZE_SMALL = 8;
    private static final byte PADDING_DROPDOWN_SIZE_LARGE = 16;

    private static final byte FONT_SIZE_NORMAL = 14;
    private static final byte FONT_SIZE_SMALL = 12;
    private static final byte FONT_SIZE_LARGE = 16;

    public MySpinnerAdapter(Context context, String[] items, byte type, boolean fullSize) {
        this(context, new ArrayList<>(Arrays.asList(items)), type, fullSize);
    }

    public MySpinnerAdapter(Context context, String[] items, byte type, boolean fullSize, int selectedDropDownStartPosition, int selectedTextStartPosition) {
        this(context, new ArrayList<>(Arrays.asList(items)), type, fullSize, selectedDropDownStartPosition, selectedTextStartPosition);
    }

    public MySpinnerAdapter(Context context, ArrayList<String> items, byte type, boolean fullSize) {
        this(context, items, type, fullSize, 1, 1);
    }

    public MySpinnerAdapter(Context context, ArrayList<String> items, byte type, boolean fullSize, int selectedDropDownStartPosition, int selectedTextStartPosition) {
        this.context = context;
        this.items = items;
        this.type = type;
        this.fullSize = fullSize;
        this.selectedDropDownStartPosition = selectedDropDownStartPosition;
        this.selectedTextStartPosition = selectedTextStartPosition;

        init();
    }

    /* 초기화 */
    private void init() {
        this.dm = context.getResources().getDisplayMetrics();

        setTextColor(ContextCompat.getColor(context, R.color.default_text_color),
                ContextCompat.getColor(context, R.color.default_text_color),
                ContextCompat.getColor(context, R.color.selected_text_color));

        this.resIcon = R.drawable.ic_menu_down_24_black;
    }

    /* 텍스트 색상 설정 */
    public void setTextColor(int normalColor, int dropDownColor, int selectedColor) {
        this.normalColor = normalColor;
        this.dropDownColor = dropDownColor;
        this.selectedColor = selectedColor;
    }

    /* 아이콘 표시 여부 */
    public void setIconVisible(boolean visible) {
        this.visibleIcon = visible;
    }

    /* 아이콘 res */
    public void setIconRes(int res) {
        this.resIcon = res;
    }

    /* 선택 텍스트 색상 적용 시작위치 (-1은 적용안함) */
    public void setSelectedTextStartPosition(int position) {
        this.selectedTextStartPosition = position;
    }

    /* DropDown 텍스트 색상 적용 시작위치 (-1은 적용안함) */
    public void setSelectedDropDownStartPosition(int position) {
        this.selectedDropDownStartPosition = position;
    }

    /* position 설정 */
    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return (long) i;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView txt = createDropDownView(position);

        return  txt;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (this.fullSize) {
            this.selectedPosition = i;
        }

        TextView txt = createTextView(i);

        return  txt;
    }

    /* DropDown TextView 만들기 */
    private TextView createDropDownView(int position) {
        TextView txt = new TextView(this.context);

        int paddingSize = 0;
        int fontSize = 0;

        switch (this.type) {
            case TYPE_SMALL:
                paddingSize = Math.round(PADDING_DROPDOWN_SIZE_SMALL * this.dm.density);
                fontSize = FONT_SIZE_SMALL;

                break;
            case TYPE_NORMAL:
                paddingSize = Math.round(PADDING_DROPDOWN_SIZE_NORMAL * this.dm.density);
                fontSize = FONT_SIZE_NORMAL;

                break;
            case TYPE_LARGE:
                paddingSize = Math.round(PADDING_DROPDOWN_SIZE_LARGE * this.dm.density);
                fontSize = FONT_SIZE_LARGE;

                break;
        }

        txt.setPadding(paddingSize, paddingSize, paddingSize, paddingSize);
        txt.setTextSize(fontSize);

        txt.setGravity(Gravity.CENTER_VERTICAL);
        txt.setText(this.items.get(position));
        if (this.selectedDropDownStartPosition == -1) {
            txt.setTextColor(this.dropDownColor);
        } else {
            if (position < this.selectedDropDownStartPosition) {
                txt.setTextColor(this.dropDownColor);
            } else {
                if (this.selectedPosition == position) {
                    txt.setTextColor(this.selectedColor);
                } else {
                    txt.setTextColor(this.dropDownColor);
                }
            }
        }

        return txt;
    }

    /* TextView 만들기 */
    private TextView createTextView(int position) {
        TextView txt = new TextView(this.context);

        int paddingSize = 0;
        int fontSize = 0;

        switch (this.type) {
            case TYPE_SMALL:
                paddingSize = Math.round(PADDING_TEXT_SIZE_SMALL * this.dm.density);
                fontSize = FONT_SIZE_SMALL;

                break;
            case TYPE_NORMAL:
                paddingSize = Math.round(PADDING_TEXT_SIZE_NORMAL * this.dm.density);
                fontSize = FONT_SIZE_NORMAL;

                break;
            case TYPE_LARGE:
                paddingSize = Math.round(PADDING_TEXT_SIZE_LARGE * this.dm.density);
                fontSize = FONT_SIZE_LARGE;

                break;
        }

        if (this.visibleIcon) {
            txt.setCompoundDrawablesWithIntrinsicBounds(0, 0, this.resIcon, 0);
        }

        txt.setPadding(paddingSize, paddingSize, paddingSize, paddingSize);
        txt.setTextSize(fontSize);

        txt.setGravity(Gravity.CENTER_VERTICAL);
        txt.setText(this.items.get(position));
        if (this.selectedTextStartPosition == -1) {
            txt.setTextColor(this.normalColor);
        } else {
            if (position < this.selectedTextStartPosition) {
                txt.setTextColor(this.normalColor);
            } else {
                txt.setTextColor(this.selectedColor);
            }
        }

        return txt;
    }
}
