package com.vidyo.app;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * A login screen that offers login via email/password.
 */
public class JoinActivity extends AppCompatActivity implements LmiDeviceManagerView.Callback {

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

    private ProgressBar joinProgress;

    private Button joinButton;
    private Button cancelButton;

    @Override
    protected void onResume() {
        super.onResume();

        if (callStarted) {
            resumeCall();
            LmiVideoCapturer.onActivityResume();

            jniBridge.LmiAndroidJniSetBackground(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (callStarted) {
            pauseCall();
            LmiVideoCapturer.onActivityPause();

            jniBridge.LmiAndroidJniSetBackground(true);
        }
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
            controlForm.setOrientation(LinearLayout.VERTICAL);

            lmiDeviceManagerView = new LmiDeviceManagerView(this, this);
            lmiDeviceManagerView.setVisibility(View.GONE);

            frame.addView(lmiDeviceManagerView);

            frame.addView(controlForm);
            addEndCallView(controlForm);

            controlForm.setVisibility(View.GONE);
        } else throw new RuntimeException("Init failed!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);

        stopDevices();

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
        boolean isHttps = URLUtil.isHttpsUrl(String.valueOf(portalParam));
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

        endCall.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        endCall.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
    }
}