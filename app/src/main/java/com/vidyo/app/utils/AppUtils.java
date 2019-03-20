package com.vidyo.app.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.telephony.TelephonyManager;

import com.vidyo.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AppUtils {

    public static String writeCaCertificates(Context context) {
        try {
            InputStream caCertStream = context.getResources().openRawResource(R.raw.ca_certificates);
            File caCertDirectory;

            String pathDir = getAndroidInternalMemDir(context);
            if (pathDir == null) return null;

            caCertDirectory = new File(pathDir);

            File caFile = new File(caCertDirectory, "ca-certificates.crt");
            FileOutputStream caCertFile = new FileOutputStream(caFile);

            byte buf[] = new byte[1024];
            int len;

            while ((len = caCertStream.read(buf)) != -1) {
                caCertFile.write(buf, 0, len);
            }

            caCertStream.close();
            caCertFile.close();
            return caFile.getPath();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getAndroidInternalMemDir(Context context) {
        File fileDir = context.getFilesDir();
        return fileDir != null ? fileDir.toString() + File.separator : null;
    }

    public static String getAndroidCacheDir(Context context) {
        File cacheDir = context.getCacheDir();
        return cacheDir != null ? cacheDir.toString() + File.separator : null;
    }

    @SuppressLint("MissingPermission")
    public static String deviceId(Context context) {
        TelephonyManager telephonyManager;
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}