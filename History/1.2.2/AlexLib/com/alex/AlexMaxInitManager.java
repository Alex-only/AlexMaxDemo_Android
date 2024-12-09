package com.alex;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.HandlerThread;
import android.text.TextUtils;

import com.anythink.core.api.ATAdapterLogUtil;
import com.anythink.core.api.ATInitMediation;
import com.anythink.core.api.ATSDK;
import com.anythink.core.api.MediationInitCallback;
import com.applovin.impl.sdk.AppLovinSdkInitializationConfigurationImpl;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.sdk.AppLovinMediationProvider;
import com.applovin.sdk.AppLovinPrivacySettings;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkInitializationConfiguration;
import com.applovin.sdk.AppLovinSdkSettings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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

    AtomicBoolean mHasCallbackInit = new AtomicBoolean();

    private final String APPLOVIN_MEDIATION_INIT_MANAGER = "com.anythink.network.applovin.ApplovinATInitManager";

    private Object initManagerInstance;
    private Method initSDKMediationAppLovinMethod;
    private Method getAppLovinSDKMethod;
    private Method setIsAgeRestrictedUserMethod;


    private AlexMaxInitManager() {
        initByATInitManagerState();
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
        if (mAppLovinSdk != null) {
            return mAppLovinSdk;
        }

        try {
            if (getAppLovinSDKMethod != null) {
                Object object = getAppLovinSDKMethod.invoke(initManagerInstance);
                ATAdapterLogUtil.i(TAG, "getApplovinSdk, by applovin init manager, obj=" + object);
                if (object instanceof AppLovinSdk) {
                    mAppLovinSdk = ((AppLovinSdk) object);
                }
            }
        } catch (Throwable e) {
            ATAdapterLogUtil.e(TAG, "getApplovinSdk error", e);
        }

        return mAppLovinSdk;
    }


    public synchronized void initSDK(Context context, Map<String, Object> serviceExtras) {
        this.initSDK(context, serviceExtras, null);
    }

    private List<MediationInitCallback> mListeners;

    public synchronized void initSDK(Context context, Map<String, Object> serviceExtras, final MediationInitCallback initListener) {
        String sdkKey = (String) serviceExtras.get("sdk_key");

        if (initSDKMediationAppLovinMethod != null) {
            //AppLovin存在时，优先通过AppLovin 去初始化sdk
            try {
                serviceExtras.put("sdkkey", sdkKey);//ApplovinATInitManager使用的是sdkkey

                ATAdapterLogUtil.i(TAG, "initSDK, by applovin init manager");
                initSDKMediationAppLovinMethod.invoke(initManagerInstance, context, serviceExtras, initListener);
                return;
            } catch (Throwable t) {
                ATAdapterLogUtil.e(TAG, "initSDK", t);
            }
        }

        try {
            boolean coppaSwitch = getBooleanFromMap(serviceExtras, "app_coppa_switch");
            if (AppLovinSdk.VERSION_CODE >= 13000000) {
                //从v13.0.0开始，不支持儿童用户
                if (coppaSwitch) {
                    if (initListener != null) {
                        initListener.onFail("AppLovin SDK 13.0.0 or higher does not support child users.");
                    }
                    return;
                }
            } else {
                //AppLovinPrivacySettings.setIsAgeRestrictedUser(true, context);
                try {
                    if (setIsAgeRestrictedUserMethod == null) {
                        setIsAgeRestrictedUserMethod = AppLovinPrivacySettings.class.getDeclaredMethod("setIsAgeRestrictedUser", boolean.class, Context.class);
                    }
                    setIsAgeRestrictedUserMethod.setAccessible(true);
                    setIsAgeRestrictedUserMethod.invoke(null, coppaSwitch, context);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable e) {
        }


        if (TextUtils.isEmpty(mSdkKey) || !TextUtils.equals(mSdkKey, sdkKey)) {
            mSdkKey = sdkKey;
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
            AppLovinSdkSettings settings = mAppLovinSdk.getSettings();
            if (settings != null) {
                prepareUserId(settings);
                prepareDynameicUnit(settings, serviceExtras);
            }
            if (initListener != null) {
                initListener.onSuccess();
            }
            return;
        }

        final AppLovinSdk appLovinSdk = AppLovinSdk.getInstance(context);
        AppLovinSdkSettings settings = appLovinSdk.getSettings();
        if (settings != null) {
            prepareUserId(settings);
            prepareDynameicUnit(settings, serviceExtras);
            settings.setVerboseLogging(ATSDK.isNetworkLogDebug());
            if (mMute != null) {
                settings.setMuted(mMute);
            }
        }

        if (appLovinSdk != null && appLovinSdk.isInitialized()) {
            mAppLovinSdk = appLovinSdk;
            if (initListener != null) {
                initListener.onSuccess();
            }
            callbackResult("");
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

        // Create the initialization configuration
        AppLovinSdkInitializationConfiguration initConfig = new AppLovinSdkInitializationConfigurationImpl.BuilderImpl( sdkKey, context )
                .setMediationProvider( AppLovinMediationProvider.MAX )
                // Perform any additional configuration/setting changes
                .build();


        final CountDownTimer[] countDownTimer = new CountDownTimer[1];
        HandlerThread mHandlerThread = new HandlerThread("alex_max_init") {
            @Override
            protected void onLooperPrepared() {
                countDownTimer[0] = new CountDownTimer(5000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if (mHasCallbackInit.get()) {
                            ATAdapterLogUtil.i(TAG, "onTick: has callback init, return");
                            return;
                        }

                        if (appLovinSdk.isInitialized()) {
                            ATAdapterLogUtil.i(TAG, "onTick: callback init");
                            if (mHasCallbackInit.compareAndSet(false, true)) {
                                mAppLovinSdk = appLovinSdk;
                                mIsLoading.set(false);
                                callbackResult("");
                            }
                        } else {
                            ATAdapterLogUtil.i(TAG, "onTick: isInitialized = false");
                        }
                    }

                    @Override
                    public void onFinish() {
                        if (mHasCallbackInit.compareAndSet(false, true)) {
                            ATAdapterLogUtil.e(TAG, "onFinish: callback timeout");
                            mIsLoading.set(false);
                            callbackResult("init timeout");
                        }
                    }
                };
                countDownTimer[0].start();
            }
        };
        mHandlerThread.start();

        appLovinSdk.initialize(initConfig, new AppLovinSdk.SdkInitializationListener() {
            @Override
            public void onSdkInitialized(AppLovinSdkConfiguration appLovinSdkConfiguration) {
                try {
                    if (countDownTimer[0] != null) {
                        countDownTimer[0].cancel();
                    }

                    mHandlerThread.quit();
                } catch (Throwable e) {
                }

                if (mHasCallbackInit.compareAndSet(false, true)) {
                    mAppLovinSdk = appLovinSdk;
                    mIsLoading.set(false);
                    callbackResult("");
                }
            }
        });

    }

    private void prepareUserId(AppLovinSdkSettings appLovinSdkSettings) {
        try {
            String userId = getUserId();
            if (!TextUtils.isEmpty(userId)) {
                appLovinSdkSettings.setUserIdentifier(userId);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void prepareDynameicUnit(AppLovinSdkSettings sdkSettings, Map<String, Object> serviceExtras) {
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

    private void callbackResult(String errorMsg) {
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
                if (!TextUtils.isEmpty(errorMsg)) {
                    if (initListener != null) {
                        initListener.onFail(errorMsg);
                    }
                    return;
                }

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


        maxAdMap.put(AlexMaxConst.KEY_REVENUE, maxAd.getRevenue());
        maxAdMap.put(AlexMaxConst.KEY_AD_UNIT_ID, maxAd.getAdUnitId());
        maxAdMap.put(AlexMaxConst.KEY_CREATIVE_ID, maxAd.getCreativeId());
        maxAdMap.put(AlexMaxConst.KEY_FORMAT, maxAd.getFormat().getLabel());
        maxAdMap.put(AlexMaxConst.KEY_NETWORK_NAME, maxAd.getNetworkName());
        maxAdMap.put(AlexMaxConst.KEY_NETWORK_PLACEMENT_ID, maxAd.getNetworkPlacement());
        maxAdMap.put(AlexMaxConst.KEY_PLACEMENT, maxAd.getPlacement());
        maxAdMap.put(AlexMaxConst.KEY_COUNTRY_CODE, countryCode);


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

        AppLovinSdk applovinSdk = getApplovinSdk();
        if (applovinSdk != null) {
            applovinSdk.getSettings().setMuted(isMuted);
        }
    }

    private Object getAppLovinInitManagerInstance(){
        if (initManagerInstance != null) {
            return initManagerInstance;
        }

        try {
            Class<? extends ATInitMediation> nativeClass = Class.forName(
                    APPLOVIN_MEDIATION_INIT_MANAGER).asSubclass(ATInitMediation.class);
            final Constructor<?> nativeConstructor = nativeClass.getDeclaredConstructor(
                    (Class[]) null);
            nativeConstructor.setAccessible(true);
            //get ApplovinATInitManager
            ATInitMediation ttInitManagerObj = (ATInitMediation) nativeConstructor.newInstance();
            Method getInstanceAppLovinMethod = ttInitManagerObj.getClass().getMethod("getInstance");
            //调用 getInstance 方法
            Object applovinInitManagerInstance = getInstanceAppLovinMethod.invoke(ttInitManagerObj);
            return applovinInitManagerInstance;
        } catch (Throwable t){
            ATAdapterLogUtil.e(TAG, "initSDK", t);
        }
        return null;
    }

    private void initByATInitManagerState() {
        try {
            //获取 InitManager Instance
            initManagerInstance = getAppLovinInitManagerInstance();
            if (initManagerInstance != null) {
                initSDKMediationAppLovinMethod = initManagerInstance.getClass().getMethod("initSDK", Context.class, Map.class, MediationInitCallback.class);
                getAppLovinSDKMethod = initManagerInstance.getClass().getMethod("getAppLovinSDK");
            }
        } catch (Throwable t) {
            if (ATSDK.isNetworkLogDebug()) {
                t.printStackTrace();
            }
        }
    }

}
