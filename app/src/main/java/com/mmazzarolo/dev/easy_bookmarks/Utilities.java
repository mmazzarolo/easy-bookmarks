package com.mmazzarolo.dev.easy_bookmarks;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.util.Patterns;

import org.ocpsoft.prettytime.PrettyTime;

import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Matteo on 10/07/2015.
 */
public class Utilities {

    public static String encodeUrlForFirebase(String urlToEncode) {
        String urlEncoded = null;
        try {
            urlEncoded = URLEncoder.encode(urlToEncode, "UTF-8");
            urlEncoded = urlEncoded.replaceAll("\\.", "%2E");
        } catch (Exception e) {
            Log.v("encodeUrlForFirebase", "Catched encoding exception: " + e.getMessage());
        }
        return urlEncoded;
    }

    public static boolean isUrlValid(String urlToValidate) {
        return Patterns.WEB_URL.matcher(urlToValidate).matches();
    }

    public static void saveSharedPreference(Context context, String settingName, String settingValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(settingName, settingValue);
        editor.apply();
    }

    public static String readSharedPreference(Context context, String settingName, String defaultValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString(settingName, defaultValue);
    }

    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static String epochToString(Long epoch) {
        Date date = new Date(epoch);
        PrettyTime p = new PrettyTime();
        return p.format(date);
    }

    public static FirebaseFragment setupFirebase(FragmentManager fm) {
        FirebaseFragment ff =
                (FirebaseFragment) fm.findFragmentByTag(FirebaseFragment.TAG_FIREBASE_FRAGMENT);
        if (ff == null) {
            ff = new FirebaseFragment();
            fm.beginTransaction().add(ff, FirebaseFragment.TAG_FIREBASE_FRAGMENT).commit();
            fm.executePendingTransactions();
        }
        return ff;
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static String getFaviconUrl(String s) {
        return "http://www.google.com/s2/favicons?domain_url=" + s;
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static void stringListToUppercase(List<String> strings) {
        ListIterator<String> iterator = strings.listIterator();
        while (iterator.hasNext()) {
            iterator.set(iterator.next().toUpperCase());
        }
    }

    public static boolean isExpired(long expirationDate) {
        return expirationDate <= System.currentTimeMillis() / 1000;
    }

    public static boolean onClickExpand(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getResources().getString(R.string.setting_on_click_expand_key);
        return sharedPref.getBoolean(key, false);
    }

    public static String getListOrder(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String key = context.getResources().getString(R.string.setting_list_order);
        return sharedPref.getString(key, "lastSaved");
    }

    public static void switchOrder(Context context) {
        String currentOrder = getListOrder(context);
        String nextOrder;
        if (isNullOrEmpty(currentOrder) || currentOrder.equals("numSaved")) {
            nextOrder = "lastSaved";
        } else {
            nextOrder = "numSaved";
        }
        String key = context.getResources().getString(R.string.setting_list_order);
        saveSharedPreference(context, key, nextOrder);
    }

    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.isTablet);
    }


}
