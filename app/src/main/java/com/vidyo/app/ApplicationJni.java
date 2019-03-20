package com.vidyo.app;

import android.util.Log;

import com.vidyo.VidyoClientLib.LmiAndroidAppJni;
import com.vidyo.VidyoClientLib.LmiAndroidJniConferenceCallbacks;
import com.vidyo.app.utils.CallStatusEvent;

import org.greenrobot.eventbus.EventBus;

public class ApplicationJni extends LmiAndroidAppJni {

    private static final String TAG = ApplicationJni.class.getCanonicalName();

    private static final String CALLBACK_CLASS_PATH = "com/vidyo/app/ApplicationJni";
    private static final String METHOD_FOR_JNI_CALLBACK = "applicationJniConferenceStatusCallback";

    private LmiAndroidJniConferenceCallbacks conferenceCallbacks;

    @Override
    public void onCreate() {
        super.onCreate();
        conferenceCallbacks = new LmiAndroidJniConferenceCallbacks(
                CALLBACK_CLASS_PATH, METHOD_FOR_JNI_CALLBACK,
                null, null, null, null, null);
    }

    @SuppressWarnings("JniCallback")
    public void applicationJniConferenceStatusCallback(int status, int error, String message) {
        Log.d(TAG, "applicationJniConferenceStatusCallback: status=" + status + ", error=" + error + ", message=" + message);

        EventBus.getDefault().post(new CallStatusEvent(status, error, message));
    }

    @Override
    protected void onLibraryStarted() {
        Log.i(ApplicationJni.class.getCanonicalName(), "Library has been started successfully!");
    }

    public void registerCallback() {
        LmiAndroidJniConferenceSetCallbacks(conferenceCallbacks);
    }
}