package com.alex;

import android.content.Context;
import android.text.TextUtils;

import com.anythink.core.api.ATInitMediation;
import com.anythink.core.api.ATSDK;
import com.anythink.core.api.MediationInitCallback;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AlexMaxInitManager extends ATInitMediation {

    private static final String TAG = AlexMaxInitManager.class.getSimpleName();
    private volatile static AlexMaxInitManager sInstance;

    private String mSdkKey;
    private AppLovinSdk mAppLovinSdk;
    private Object mLock = new Object();
    private Boolean mMute = null;

    private AtomicBoolean mIsLoading = new AtomicBoolean(false);


    Map<String, Map<String, AlexMaxBiddingInfo>> mAdCacheMap;

    private AlexMaxInitManager() {
    }

    public static AlexMaxInitManager getInstance() {
        if (sInstance == null) {
            synchronized (AlexMaxInitManager.class) {
                if (sInstance == null)
                    sInstance = new AlexMaxInitManager();
            }
        }
        return sInstance;
    }

    protected AppLovinSdk getApplovinSdk() {
        return mAppLovinSdk;
    }


    public synchronized void initSDK(Context context, Map<String, Object> serviceExtras) {
        this.initSDK(context, serviceExtras, null);
    }

    private List<MediationInitCallback> mListeners;

    public synchronized void initSDK(Context context, Map<String, Object> serviceExtras, final MediationInitCallback initListener) {

        String sdkKey = (String) serviceExtras.get("sdk_key");
        if (TextUtils.isEmpty(mSdkKey) || !TextUtils.equals(mSdkKey, sdkKey)) {
            mSdkKey = sdkKey;
        }

        try {
            boolean coppaSwitch = (boolean) serviceExtras.get("app_coppa_switch");
            if (coppaSwitch) {
                AppLovinPrivacySettings.setIsAgeRestrictedUser(true, context);
            } else {
                AppLovinPrivacySettings.setIsAgeRestrictedUser(false, context);
            }
        } catch (Throwable e) {

        }
        try {
            boolean ccpaSwitch = (boolean) serviceExtras.get("app_ccpa_switch");
            if (ccpaSwitch) {
                AppLovinPrivacySettings.setDoNotSell(true, context);
            } else {
                AppLovinPrivacySettings.setDoNotSell(false, context);
            }
        } catch (Throwable e) {

        }

        if (mAppLovinSdk != null) {
            prepareUserId(mAppLovinSdk);
            prepareDynameicUnit(mAppLovinSdk, serviceExtras);
            if (initListener != null) {
                initListener.onSuccess();
            }
            return;
        }

        final AppLovinSdk appLovinSdk = AppLovinSdk.getInstance(sdkKey, new AppLovinSdkSettings(context), context);

        prepareUserId(appLovinSdk);
        prepareDynameicUnit(appLovinSdk, serviceExtras);
        appLovinSdk.getSettings().setVerboseLogging(ATSDK.isNetworkLogDebug());
        appLovinSdk.setMediationProvider("max");
        if (mMute != null) {
            appLovinSdk.getSettings().setMuted(mMute);
        }
        if (appLovinSdk != null && appLovinSdk.isInitialized()) {
            mAppLovinSdk = appLovinSdk;
            if (initListener != null) {
                initListener.onSuccess();
            }
            callbackResult();
            return;
        }

        synchronized (mLock) {
            if (mListeners == null) {
                mListeners = new ArrayList<>();
            }
            if (initListener != null) {
                mListeners.add(initListener);
            }

        }


        if (mIsLoading.get()) {
            return;
        }

        mIsLoading.set(true);

        appLovinSdk.initializeSdk(new AppLovinSdk.SdkInitializationListener() {
            @Override
            public void onSdkInitialized(AppLovinSdkConfiguration appLovinSdkConfiguration) {
                mAppLovinSdk = appLovinSdk;
                mIsLoading.set(false);
                callbackResult();
            }
        });

    }

    private void prepareUserId(AppLovinSdk appLovinSdk) {
        try {
            String userId = getUserId();
            if (!TextUtils.isEmpty(userId)) {
                appLovinSdk.setUserIdentifier(userId);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void prepareDynameicUnit(AppLovinSdk appLovinSdk, Map<String, Object> serviceExtras) {
        JSONObject unitJSONObject = null;
        try {
            Object maxUnitInfoObj = AlexMaxConst.getMaxUnitInfoObj(serviceExtras);
            if (maxUnitInfoObj != null) {
                unitJSONObject = (JSONObject) maxUnitInfoObj;
            }
        } catch (Throwable e) {

        }

        if (unitJSONObject == null) {
            return;
        }

        String unitIds = "";
        JSONArray nativeUnitId = unitJSONObject.optJSONArray("0");
        JSONArray rewardedVideoUnitId = unitJSONObject.optJSONArray("1");
        JSONArray interstitlUnitIds = unitJSONObject.optJSONArray("3");
        JSONArray splashUnitIds = unitJSONObject.optJSONArray("4");
        if (nativeUnitId != null && nativeUnitId.length() > 0) {
            for (int i = 0; i < nativeUnitId.length(); i++) {
                unitIds += nativeUnitId.optString(i) + ",";
            }
        }

        if (rewardedVideoUnitId != null && rewardedVideoUnitId.length() > 0) {
            for (int i = 0; i < rewardedVideoUnitId.length(); i++) {
                unitIds += rewardedVideoUnitId.optString(i) + ",";
            }
        }

        if (interstitlUnitIds != null && interstitlUnitIds.length() > 0) {
            for (int i = 0; i < interstitlUnitIds.length(); i++) {
                unitIds += interstitlUnitIds.optString(i) + ",";
            }
        }
        if (splashUnitIds != null && splashUnitIds.length() > 0) {
            for (int i = 0; i < splashUnitIds.length(); i++) {
                unitIds += splashUnitIds.optString(i) + ",";
            }
        }

        if (unitIds.endsWith(",")) {
            unitIds = unitIds.substring(0, unitIds.length() - 1);
        }

        //Disable auto cache
        AppLovinSdkSettings sdkSettings = appLovinSdk.getSettings();
        sdkSettings.setExtraParameter("disable_b2b_ad_unit_ids", unitIds);

        String disableRetryFormat = "";
        if (unitJSONObject.has("0")) {
            disableRetryFormat += MaxAdFormat.NATIVE.getLabel() + ",";
        }
        if (unitJSONObject.has("1")) {
            disableRetryFormat += MaxAdFormat.REWARDED.getLabel() + ",";
        }
        if (unitJSONObject.has("2")) {
            disableRetryFormat += MaxAdFormat.BANNER.getLabel() + ",";
            disableRetryFormat += MaxAdFormat.MREC.getLabel() + ",";
        }
        if (unitJSONObject.has("3")) {
            disableRetryFormat += MaxAdFormat.INTERSTITIAL.getLabel() + ",";
        }
        if (unitJSONObject.has("4")) {
            disableRetryFormat += MaxAdFormat.APP_OPEN.getLabel() + ",";
        }

        if (disableRetryFormat.endsWith(",")) {
            disableRetryFormat = disableRetryFormat.substring(0, disableRetryFormat.length() - 1);
        }

        //Disable Retry request
        sdkSettings.setExtraParameter("disable_auto_retry_ad_formats", disableRetryFormat);
    }

    private void callbackResult() {

        List<MediationInitCallback> Listeners;
        synchronized (mLock) {
            if (mListeners == null) {
                return;
            }
            int size = mListeners.size();
            if (size <= 0) {
                return;
            }

            Listeners = new ArrayList<>(mListeners);

            mListeners.clear();
        }

        for (MediationInitCallback initListener : Listeners) {
            try {
                if (initListener != null) {
                    initListener.onSuccess();
                }
            } catch (Throwable e) {
                if (initListener != null) {
                    initListener.onFail(e.getMessage());
                }
            }
        }

    }


    @Override
    public boolean setUserDataConsent(Context context, boolean isConsent, boolean isEUTraffic) {
        AppLovinPrivacySettings.setHasUserConsent(isConsent, context);
        return true;
    }

    @Override
    public String getNetworkName() {
        return "Max";
    }

    @Override
    public String getNetworkVersion() {
        return AlexMaxConst.getNetworkVersion();
    }

    @Override
    public String getNetworkSDKClass() {
        return "com.applovin.sdk.AppLovinSdk";
    }

    protected synchronized String saveC2SOffer(String adUnitId, Object adCacheObject, MaxAd maxAd) {
        if (mAdCacheMap == null) {
            mAdCacheMap = new ConcurrentHashMap<>(3);
        }
        Map<String, AlexMaxBiddingInfo> unitCacheMap = mAdCacheMap.get(adUnitId);
        if (unitCacheMap == null) {
            unitCacheMap = new ConcurrentHashMap<>(2);
            mAdCacheMap.put(adUnitId, unitCacheMap);
        }
        unitCacheMap.clear();

        String cacheId = UUID.randomUUID().toString();
        AlexMaxBiddingInfo biddingInfo = new AlexMaxBiddingInfo(adCacheObject, maxAd);
        unitCacheMap.put(cacheId, biddingInfo);

        return cacheId;
    }

    protected synchronized AlexMaxBiddingInfo requestC2SOffer(String adUnitId, String cacheId) {
        if (mAdCacheMap != null) {
            Map<String, AlexMaxBiddingInfo> unitCacheMap = mAdCacheMap.get(adUnitId);
            if (unitCacheMap != null) {
                AlexMaxBiddingInfo alexMaxBiddingInfo = unitCacheMap.remove(cacheId);
                return alexMaxBiddingInfo;
            }
        }
        return null;
    }

    protected synchronized Map.Entry<String, AlexMaxBiddingInfo> checkC2SCacheOffer(String adUnitId) {
        if (mAdCacheMap != null) {
            Map<String, AlexMaxBiddingInfo> unitCacheMap = mAdCacheMap.get(adUnitId);
            if (unitCacheMap != null) {
                Iterator<Map.Entry<String, AlexMaxBiddingInfo>> iterator = unitCacheMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    return iterator.next();
                }
            }
        }
        return null;
    }

    protected String getToken() {
        return UUID.randomUUID().toString();
    }

    protected Map<String, Object> handleMaxAd(MaxAd maxAd) {
        if (maxAd == null) {
            return null;
        }
        HashMap<String, Object> maxAdMap = new HashMap<>();
        maxAdMap.put("Revenue", maxAd.getRevenue());
        maxAdMap.put("AdUnitId", maxAd.getAdUnitId());
        maxAdMap.put("CreativeId", maxAd.getCreativeId());
        maxAdMap.put("Format", maxAd.getFormat().getLabel());
        maxAdMap.put("NetworkName", maxAd.getNetworkName());
        maxAdMap.put("NetworkPlacement", maxAd.getNetworkPlacement());
        maxAdMap.put("Placement", maxAd.getPlacement());
        String countryCode = mAppLovinSdk != null ? mAppLovinSdk.getConfiguration().getCountryCode() : "";
        maxAdMap.put("CountryCode", countryCode);
        return maxAdMap;
    }

    protected double getMaxAdEcpm(MaxAd maxAd) {
        if (maxAd == null) {
            return 0;
        }
        return maxAd.getRevenue() * 1000.0;
    }

    public void setMute(boolean isMuted) {
        mMute = isMuted;
        if (mAppLovinSdk != null) {
            mAppLovinSdk.getSettings().setMuted(isMuted);
        }
    }
}
