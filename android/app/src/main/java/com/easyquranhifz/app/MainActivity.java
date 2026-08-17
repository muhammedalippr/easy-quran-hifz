package com.easyquranhifz.app;

import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.community.admob.AdMob;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;

public class MainActivity extends BridgeActivity {
    private static final int UPDATE_REQUEST_CODE = 500;
    private AppUpdateManager appUpdateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        registerPlugin(AdMob.class);
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(0xFF000000);
        window.setStatusBarColor(0xFF050505);
        
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightNavigationBars(false);
        controller.setAppearanceLightStatusBars(false);

        // Check for In-App Updates
        checkForAppUpdate();
    }

    private void checkForAppUpdate() {
        try {
            appUpdateManager = AppUpdateManagerFactory.create(this);
            Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

            appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this,
                                UPDATE_REQUEST_CODE
                        );
                    } catch (IntentSender.SendIntentException e) {
                        Log.e("InAppUpdate", "Update flow failed: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.w("InAppUpdate", "In-App update check skipped: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appUpdateManager != null) {
            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this,
                                UPDATE_REQUEST_CODE
                        );
                    } catch (IntentSender.SendIntentException e) {
                        Log.e("InAppUpdate", "Resume update failed: " + e.getMessage());
                    }
                }
            });
        }
    }
}
