package com.vidyo.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.vidyo.R;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.RECORD_AUDIO;

public class HomeActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS_LIST = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE
    };

    private static final int PERMISSIONS_RC = 0x85ba;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        if (requestPermissions()) {
            granted();
        }
    }

    public void guestJoin(View view) {
        startActivity(new Intent(this, JoinActivity.class));
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_RC) {
            if (grantResults.length == PERMISSIONS_LIST.length && loopPermissions(grantResults)) {
                // Granted
                granted();
            } else {
                // Go over again.
                requestPermissions();
            }
        }
    }

    private void granted() {
        Toast.makeText(this, "All has been granted.", Toast.LENGTH_SHORT).show();
    }

    private boolean requestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (loopPermissions()) {
            return true;
        }

        if (shouldShowRequestPermissionRationale(CAMERA) && shouldShowRequestPermissionRationale(READ_PHONE_STATE)
                && shouldShowRequestPermissionRationale(RECORD_AUDIO)) {
            Snackbar.make(findViewById(R.id.main_content), R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(PERMISSIONS_LIST, PERMISSIONS_RC);
                        }
                    });
        } else {
            requestPermissions(PERMISSIONS_LIST, PERMISSIONS_RC);
        }

        return false;
    }

    private boolean loopPermissions(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) return false;
        }

        // fine
        return true;
    }

    private boolean loopPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        for (String permission : PERMISSIONS_LIST) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) return false;
        }

        // fine
        return true;
    }
}