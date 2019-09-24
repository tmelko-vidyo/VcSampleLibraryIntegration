package com.vidyo.app;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.vidyo.BuildConfig;
import com.vidyo.LmiDeviceManager.LmiDeviceManagerView;
import com.vidyo.LmiDeviceManager.LmiVideoCapturer;
import com.vidyo.R;
import com.vidyo.VidyoClientLib.LmiAndroidJniConferenceCallbacks;
import com.vidyo.app.utils.AppUtils;
import com.vidyo.app.utils.CallStatusEvent;
import com.vidyo.app.utils.ChatMessageEvent;
import com.vidyo.app.utils.ParticipantsChangeEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Random;

public class JoinActivity extends AppCompatActivity implements LmiDeviceManagerView.Callback, SensorEventListener {

    private static final String PORTAL = null; // PORTAL_URL e.g. https://vidyoclound.portal.com
    private static final String ROOM_KEY = null; // ROOM_KEY e.g. "dDVbw3rE"
    private static final String DISPLAY_NAME = null; // DISPLAY_NAME e.g. "Mobile User"

    private static final String TAG = "JoinActivity";

    private static final String LOGGING_FILTER = "fatal error warning all@AppVcsoapClient debug@App info@AppEmcpClient debug@AppGui info@AppGui";

    // UI references.
    private EditText portal;
    private EditText key;
    private EditText user;

    private View joinForm;
    private LinearLayout controlForm;

    private ApplicationJni jniBridge;

    private boolean doRender = false;
    private boolean callStarted = false;

    private LmiDeviceManagerView lmiDeviceManagerView;
    private SensorManager sensorManager;

    private ProgressBar joinProgress;

    private Button joinButton;
    private Button cancelButton;

    private int currentRotation = -1;

    @Override
    protected void onResume() {
        super.onResume();

        if (callStarted) {
            resumeCall();
            LmiVideoCapturer.onActivityResume();

            jniBridge.LmiAndroidJniSetBackground(false);
        }

        controlSensor(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (callStarted) {
            pauseCall();
            LmiVideoCapturer.onActivityPause();

            jniBridge.LmiAndroidJniSetBackground(true);
        }

        controlSensor(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.join_activity);

        EventBus.getDefault().register(this);

        portal = findViewById(R.id.portal_field);
        key = findViewById(R.id.room_key);
        user = findViewById(R.id.user_name);

        portal.setText(PORTAL);
        key.setText(ROOM_KEY);
        user.setText(DISPLAY_NAME);

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(true);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        user.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptJoin();
                    return true;
                }

                return false;
            }
        });

        joinForm = findViewById(R.id.join_params_frame);

        joinProgress = findViewById(R.id.join_progress);
        joinProgress.setVisibility(View.GONE);

        joinButton = findViewById(R.id.join_room_btn);
        joinButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptJoin();
            }
        });

        cancelButton = findViewById(R.id.cancel_join);
        cancelButton.setEnabled(false);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                callEnded();
            }
        });

        initializeLibrary();
    }

    void initializeLibrary() {
        jniBridge = (ApplicationJni) getApplicationContext();

        boolean initResult = jniBridge.LmiAndroidJniInitialize();

        String caFileName = AppUtils.writeCaCertificates(this);
        String internalDir = AppUtils.getAndroidInternalMemDir(this);
        String cacheDir = AppUtils.getAndroidCacheDir(this);

        long result = jniBridge.LmiAndroidJniConstruct(caFileName, AppUtils.deviceId(this),
                internalDir, cacheDir, internalDir, BuildConfig.VERSION_NAME, this);

        if (initResult && result > 0) {
            jniBridge.LmiAndroidJniSetLogging(LOGGING_FILTER);
            jniBridge.LmiAndroidJniRegisterDefaultActivity(this);

            jniBridge.registerCallback();

            FrameLayout frame = findViewById(R.id.lmi_device_manager_container);

            controlForm = new LinearLayout(this);
            controlForm.setOrientation(LinearLayout.HORIZONTAL);

            lmiDeviceManagerView = new LmiDeviceManagerView(this, this);
            lmiDeviceManagerView.setVisibility(View.GONE);

            frame.addView(lmiDeviceManagerView);

            frame.addView(controlForm);
            addEndCallView(controlForm);
            addSendChatMessageView(controlForm);

            controlForm.setVisibility(View.GONE);

            orientationChanged();
        } else throw new RuntimeException("Init failed!");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /* Stub */
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        if ((type != Sensor.TYPE_ACCELEROMETER) && (type != Sensor.TYPE_MAGNETIC_FIELD)) {
            return;
        }

        orientationChanged();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        orientationChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);

        stopDevices();
        controlSensor(false);

        if (jniBridge != null) {
            jniBridge.LmiAndroidJniUnregisterDefaultActivity();
            jniBridge.LmiAndroidJniDispose();
            jniBridge = null;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCallEvent(CallStatusEvent event) {
        switch (event.getStatus()) {
            case LmiAndroidJniConferenceCallbacks.STATUS_JOIN_PROGRESS:
                Log.i(TAG, "Join in progress...");
                break;
            case LmiAndroidJniConferenceCallbacks.STATUS_JOIN_COMPLETE:
                Log.i(TAG, "Join complete.");

                callStarted();
                break;
            case LmiAndroidJniConferenceCallbacks.STATUS_CALL_ENDED:
                Log.i(TAG, "Join ended.");

                callEnded();
                break;
            case LmiAndroidJniConferenceCallbacks.STATUS_GUEST_JOIN_ERROR:
                Log.e(TAG, "Join error.");

                callEnded();

                Toast.makeText(JoinActivity.this, "Join error", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChatMessageEvent(ChatMessageEvent event) {
        if (event != null && event.isGroup()) {
            Toast.makeText(JoinActivity.this, "User: " + event.getName() + ". Msg: " + event.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onParticipantsChangeEvent(ParticipantsChangeEvent event) {
        /*
         * Initially, you will receive zero and then 1 if you are the only person on the call.
         * If someone join/left - you will receive an updates with actual count.
         */
        Toast.makeText(JoinActivity.this, "Participants count changed: " + event.getActualParticipantsCount(), Toast.LENGTH_SHORT).show();
    }

    private void attemptJoin() {
        if (!AppUtils.isNetworkAvailable(this)) {
            Toast.makeText(JoinActivity.this, R.string.no_network, Toast.LENGTH_SHORT).show();
            return;
        }

        portal.setError(null);
        key.setError(null);
        user.setError(null);

        String portalParam = portal.getText().toString();
        String keyParam = key.getText().toString();
        String userParam = user.getText().toString();

        if (TextUtils.isEmpty(portalParam) && TextUtils.isEmpty(keyParam)) {
            Toast.makeText(JoinActivity.this, R.string.empty_cred, Toast.LENGTH_SHORT).show();
            return;
        }

        joinProgress.setVisibility(View.VISIBLE);
        joinButton.setEnabled(false);
        cancelButton.setEnabled(true);

        Uri portal = Uri.parse(portalParam);
        boolean isHttps = URLUtil.isHttpsUrl(portalParam);
        int port = portal.getPort();

        jniBridge.LmiAndroidJniHandleGuestLink(portalParam, port, keyParam, userParam, "", isHttps);

        jniBridge.LmiAndroidJniEnableMenuBar(true);
        jniBridge.LmiAndroidJniSetEchoCancellation(true);
    }

    @Override
    public void LmiDeviceManagerViewRender() {
        if (doRender && jniBridge != null) {
            jniBridge.LmiAndroidJniRender();
        }
    }

    @Override
    public void LmiDeviceManagerViewResize(int width, int height) {
        if (jniBridge != null) jniBridge.LmiAndroidJniResize(width, height);
    }

    @Override
    public void LmiDeviceManagerViewRenderRelease() {
        if (jniBridge != null) jniBridge.LmiAndroidJniRenderRelease();
    }

    @Override
    public void LmiDeviceManagerViewTouchEvent(int id, int type, int x, int y) {
        if (jniBridge != null) jniBridge.LmiAndroidJniTouchEvent(id, type, x, y);
    }

    private void callStarted() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        callStarted = true;
        joinProgress.setVisibility(View.GONE);
        joinButton.setEnabled(true);

        joinForm.setVisibility(View.GONE);
        lmiDeviceManagerView.setVisibility(View.VISIBLE);

        controlForm.setVisibility(View.VISIBLE);

        if (jniBridge != null) {
            jniBridge.LmiAndroidJniSetCameraDevice(1);
            jniBridge.LmiAndroidJniStartMedia();
            jniBridge.LmiAndroidJniEnableAllVideoStreams();
        }

        startDevices();
    }

    private void callEnded() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        callStarted = false;

        joinButton.setEnabled(true);
        cancelButton.setEnabled(false);

        joinProgress.setVisibility(View.GONE);

        joinForm.setVisibility(View.VISIBLE);
        lmiDeviceManagerView.setVisibility(View.GONE);

        controlForm.setVisibility(View.GONE);

        stopDevices();

        if (jniBridge != null) {
            jniBridge.LmiAndroidJniRenderRelease();
        }
    }

    void startDevices() {
        doRender = true;
    }

    void stopDevices() {
        doRender = false;
    }

    private void resumeCall() {
        lmiDeviceManagerView.onResume();
    }

    private void pauseCall() {
        lmiDeviceManagerView.onPause();
    }

    private void addEndCallView(ViewGroup frame) {
        Button endCall = new Button(this);
        endCall.setText(R.string.end_call);
        endCall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (jniBridge != null) jniBridge.LmiAndroidJniLeave();
            }
        });

        frame.addView(endCall);

        endCall.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        endCall.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private void addSendChatMessageView(ViewGroup frame) {
        Button endCall = new Button(this);
        endCall.setText(R.string.send_message);
        endCall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int dummy = new Random().nextInt(100);
                if (jniBridge != null) jniBridge.LmiAndroidJniSendGroupChatMsg("Hello~" + dummy);
            }
        });

        frame.addView(endCall);

        endCall.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        endCall.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private void controlSensor(boolean register) {
        if (sensorManager == null) return;

        if (register) {
            Sensor gSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            sensorManager.unregisterListener(this);
        }
    }

    private void orientationChanged() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager != null ? windowManager.getDefaultDisplay() : null;
        int newRotation = display != null ? display.getRotation() : 0;

        if (newRotation != currentRotation) {
            currentRotation = newRotation;
            AppUtils.SetDeviceOrientation(newRotation, jniBridge);
        }
    }
}