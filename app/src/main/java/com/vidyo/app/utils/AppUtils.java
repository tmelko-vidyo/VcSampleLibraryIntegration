package com.vidyo.app.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Surface;

import com.vidyo.R;
import com.vidyo.app.ApplicationJni;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AppUtils {

    private static final int ORIENTATION_UP = 0;
    private static final int ORIENTATION_DOWN = 1;
    private static final int ORIENTATION_LEFT = 2;
    private static final int ORIENTATION_RIGHT = 3;

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

    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String deviceId(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ERROR", "No permissions granted for Telephony manager");
            return "";
        }

        String machineID = null;

        TelephonyManager tManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                machineID = tManager.getImei();
            } else {
                machineID = tManager.getDeviceId();
            }
        }

        if (machineID == null) {
            machineID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        if (machineID == null) return "";

        return machineID;
    }

    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;

        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void SetDeviceOrientation(int newRotation, ApplicationJni applicationJni) {
        int orientation = rotation2Orientation(newRotation);
        applicationJni.LmiAndroidJniSetOrientation(orientation);
    }

    private static int rotation2Orientation(int rotation) {
        switch (rotation) {
            case Surface.ROTATION_0:
                return ORIENTATION_UP;
            case Surface.ROTATION_90:
                return ORIENTATION_RIGHT;
            case Surface.ROTATION_180:
                return ORIENTATION_DOWN;
            case Surface.ROTATION_270:
                return ORIENTATION_LEFT;
            default:
                return ORIENTATION_UP;
        }
    }
}