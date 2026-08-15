// Google AdMob Integration Helper
const ADMOB_BANNER_ID = 'ca-app-pub-1448372299256521/6027622764';

let admobInitialized = false;

async function initAdMobBanner() {
    try {
        if (!window.Capacitor || !window.Capacitor.Plugins) {
            console.log("AdMob: Not in native environment, skipping.");
            return;
        }

        const { AdMob, BannerAdSize, BannerAdPosition } = window.Capacitor.Plugins;
        if (!AdMob) {
            console.log("AdMob plugin not loaded.");
            return;
        }

        if (!admobInitialized) {
            try {
                // Request user consent if required
                await AdMob.requestConsentInfo();
            } catch (e) {
                console.log("Consent info note:", e);
            }

            await AdMob.initialize({
                initializeForTesting: false
            });
            admobInitialized = true;
        }

        // Show Adaptive Banner at bottom
        await AdMob.showBanner({
            adId: ADMOB_BANNER_ID,
            adSize: BannerAdSize.ADAPTIVE_BANNER,
            position: BannerAdPosition.BOTTOM_CENTER,
            margin: 0,
            isTesting: false
        });

        console.log("AdMob banner request sent.");
    } catch (err) {
        console.warn("AdMob live banner failed (common on newly created ad units, testing fallback):", err);
        // Fallback to Google test banner if live unit is still propagating
        try {
            const { AdMob, BannerAdSize, BannerAdPosition } = window.Capacitor.Plugins;
            await AdMob.showBanner({
                adId: 'ca-app-pub-3940256099942544/6300978111', // Official Google Test Banner ID
                adSize: BannerAdSize.ADAPTIVE_BANNER,
                position: BannerAdPosition.BOTTOM_CENTER,
                margin: 0,
                isTesting: true
            });
            console.log("AdMob test banner displayed.");
        } catch (testErr) {
            console.warn("AdMob test banner error:", testErr);
        }
    }
}

// Auto-run when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        setTimeout(initAdMobBanner, 1500);
    });
} else {
    setTimeout(initAdMobBanner, 1500);
}
