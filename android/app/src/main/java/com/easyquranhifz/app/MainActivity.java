package com.easyquranhifz.app;

import android.content.IntentSender;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "MainActivity";
    private static final String BANNER_AD_UNIT_ID = "ca-app-pub-1448372299256521/6027622764";
    private static final int UPDATE_REQUEST_CODE = 500;
    
    private FrameLayout adContainer;
    private AppUpdateManager appUpdateManager;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setNavigationBarColor(0xFF000000);
        window.setStatusBarColor(0xFF050505);
        
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.setAppearanceLightNavigationBars(false);
        controller.setAppearanceLightStatusBars(false);

        // 1. Initialize Mobile Ads SDK and load persistent native banner
        initNativeAdMobBanner();

        // 2. Check for In-App Updates
        checkForAppUpdate();
    }

    private void initNativeAdMobBanner() {
        try {
            MobileAds.initialize(this, initializationStatus -> {
                Log.d(TAG, "Native MobileAds initialized.");
                runOnUiThread(this::loadBanner);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MobileAds", e);
        }
    }

    private void loadBanner() {
        try {
            if (adView != null) {
                return;
            }

            // Find or create the dedicated ad container at bottom
            ViewGroup rootView = findViewById(android.R.id.content);
            if (rootView == null) return;

            // Get Bridge WebView
            android.webkit.WebView webView = getBridge() != null ? getBridge().getWebView() : null;

            adContainer = new FrameLayout(this);
            FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            containerParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            adContainer.setLayoutParams(containerParams);
            adContainer.setBackgroundColor(0xFF000000);
            adContainer.setVisibility(android.view.View.GONE); // Initially 0 height until loaded

            adView = new AdView(this);
            adView.setAdUnitId(BANNER_AD_UNIT_ID);
            adView.setAdSize(getAdaptiveAdSize());

            adView.setAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    Log.d(TAG, "AdMob banner loaded successfully.");
                    runOnUiThread(() -> updateBannerVisibility(true, webView));
                }

                @Override
                public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    Log.w(TAG, "AdMob banner failed to load: " + loadAdError.getMessage() + ". Retrying in 30s...");
                    runOnUiThread(() -> updateBannerVisibility(false, webView));
                    
                    // Automatically retry loading after 30 seconds
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (adView != null) {
                            AdRequest retryRequest = new AdRequest.Builder().build();
                            adView.loadAd(retryRequest);
                        }
                    }, 30000);
                }

                @Override
                public void onAdOpened() {
                    super.onAdOpened();
                }

                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                }
            });

            adContainer.addView(adView);
            rootView.addView(adContainer);

            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
            Log.d(TAG, "AdMob native banner loadAd() requested for " + BANNER_AD_UNIT_ID);

        } catch (Exception e) {
            Log.e(TAG, "Failed to create/load native AdView", e);
        }
    }

    private void updateBannerVisibility(boolean isLoaded, android.webkit.WebView webView) {
        if (adContainer != null && adView != null) {
            if (isLoaded) {
                adContainer.setVisibility(android.view.View.VISIBLE);
                // Calculate exact height of the adaptive banner in pixels
                adContainer.post(() -> {
                    int bannerHeight = adContainer.getHeight();
                    if (bannerHeight == 0) {
                        bannerHeight = (int) (getAdaptiveAdSize().getHeightInPixels(this));
                    }
                    if (webView != null) {
                        // Apply bottom margin/padding to WebView so no UI is covered
                        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) webView.getLayoutParams();
                        if (lp != null && lp.bottomMargin != bannerHeight) {
                            lp.bottomMargin = bannerHeight;
                            webView.setLayoutParams(lp);
                        }
                    }
                });
            } else {
                adContainer.setVisibility(android.view.View.GONE);
                if (webView != null) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) webView.getLayoutParams();
                    if (lp != null && lp.bottomMargin != 0) {
                        lp.bottomMargin = 0;
                        webView.setLayoutParams(lp);
                    }
                }
            }
        }
    }

    private AdSize getAdaptiveAdSize() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int widthPixels = displayMetrics.widthPixels;
        float density = displayMetrics.density;
        int adWidth = (int) (widthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
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
                        Log.e(TAG, "Update flow failed: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "In-App update check skipped: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
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
                        Log.e(TAG, "Resume update failed: " + e.getMessage());
                    }
                }
            });
        }
    }

    @Override
    public void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }
}
