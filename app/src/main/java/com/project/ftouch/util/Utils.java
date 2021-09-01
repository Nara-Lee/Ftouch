package com.project.ftouch.util;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.project.ftouch.entity.AppItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class Utils {

    /* 휴대번호 체크 */
    public static boolean isPhoneNumber(String number) {
        String regEx = "^(010)-?(\\d{4})-?(\\d{4})$";
        if (number.indexOf("010") != 0) {
            regEx = "^(01(?:1|[6-9]))-?(\\d{3})-?(\\d{4})$";
        }

        return Pattern.matches(regEx, number);
    }

    /* 휴대번호 얻기 */
    public static String getPhoneNumber(Activity activity) {
        String number = "";

        try {
            TelephonyManager tel = (TelephonyManager) activity.getSystemService(Activity.TELEPHONY_SERVICE);
            number = tel.getLine1Number();

            if (!TextUtils.isEmpty(number)) {
                // "-", "+" 제거
                number = number.replace("-", "").replace("+", "");

                if (number.indexOf("82") == 0) {
                    number = "0" + number.substring(2);
                }
            }
        } catch (SecurityException e) {
        } catch (Exception e) {
        }

        return number;
    }

    /* 설치된 앱 목록 가져오기 */
    public static ArrayList<AppItem> getApps(PackageManager packageManager) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        // 실행가능한 Package 만 추출
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, 0);

        // 정렬
        Collections.sort(list, new ResolveInfo.DisplayNameComparator(packageManager));

        ArrayList<AppItem> apps = new ArrayList<>();
        for (ResolveInfo info : list) {
            ActivityInfo ai = info.activityInfo;
            AppItem data = new AppItem(ai.loadLabel(packageManager).toString(), ai.packageName);

            apps.add(data);
        }

        return apps;
    }
}
