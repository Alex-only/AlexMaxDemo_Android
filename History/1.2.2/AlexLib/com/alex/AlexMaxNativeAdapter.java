package com.alex;

import static com.anythink.core.api.ATInitMediation.getIntFromMap;

import android.content.Context;
import android.text.TextUtils;

import com.anythink.core.api.ATAdConst;
import com.anythink.core.api.ATBiddingListener;
import com.anythink.core.api.ATBiddingResult;
import com.anythink.core.api.MediationInitCallback;
import com.anythink.nativead.unitgroup.api.CustomNativeAd;
import com.anythink.nativead.unitgroup.api.CustomNativeAdapter;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.sdk.AppLovinSdk;

import java.util.Map;

public class AlexMaxNativeAdapter extends CustomNativeAdapter {

    static final String TAG = AlexMaxNativeAdapter.class.getSimpleName();

    String mAdUnitId;
    String mSdkKey;
    String mPayload;

    String mUnitType;
    Map<String, Object> mExtraMap;

    boolean isDynamicePrice;

    double dynamicPrice;

    private MaxNativeAdLoader nativeAdLoader;

    private int mMediaWidth = 0;
    private int mMediaHeight = 0;

    @Override
    public void loadCustomNetworkAd(final Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra) {
        initRequestParams(serverExtra, localExtra);
        //Bidding Request
//        if (!TextUtils.isEmpty(mPayload)) {
//            AlexMaxBiddingInfo alexMaxBiddingInfo = AlexMaxInitManager.getInstance().requestC2SOffer(mAdUnitId, mPayload);
//            if (alexMaxBiddingInfo != null && alexMaxBiddingInfo.adObject instanceof CustomNativeAd) {
//                mExtraMap = AlexMaxInitManager.getInstance().handleMaxAd(alexMaxBiddingInfo.maxAd);
//
//                if (mLoadListener != null) {
//                    mLoadListener.onAdCacheLoaded((CustomNativeAd) alexMaxBiddingInfo.adObject);
//                }
//            } else {
//                if (mLoadListener != null) {
//                    mLoadListener.onAdLoadError("", "Max: Bidding Cache is Empty or not ready.");
//                }
//            }
//            return;
//        }

        if (TextUtils.isEmpty(mSdkKey) || TextUtils.isEmpty(mAdUnitId)) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadError("", "Max: sdk_key、ad_unit_id could not be null.");
            }
            return;
        }

        AlexMaxInitManager.getInstance().initSDK(context, serverExtra, new MediationInitCallback() {
            @Override
            public void onSuccess() {
                AppLovinSdk appLovinSdk = AlexMaxInitManager.getInstance().getApplovinSdk();

                startLoadAd(context.getApplicationContext(), appLovinSdk, false, localExtra);
            }

            @Override
            public void onFail(String errorMsg) {
                notifyATLoadFail("", "Max: " + errorMsg);
            }
        });
    }

    private void startLoadAd(Context context, AppLovinSdk appLovinSdk, final boolean isBidding, Map<String, Object> localExtras) {
        nativeAdLoader = new MaxNativeAdLoader(mAdUnitId, appLovinSdk, context);
        if (isDynamicePrice) {
            nativeAdLoader.setExtraParameter("jC7Fp", String.valueOf(dynamicPrice));
        }

        //TODO Test

        LoadCallbackListener loadCallbackListener = new LoadCallbackListener() {
            @Override
            public void onSuccess(final CustomNativeAd customNativeAd, final MaxAd maxAd, Map<String, Object> networkInfoMap) {
                mExtraMap = networkInfoMap;
                if (isBidding) {
                    runOnNetworkRequestThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mBiddingListener != null) {
                                String token = AlexMaxInitManager.getInstance().getToken();
                                mBiddingListener.onC2SBiddingResultWithCache(ATBiddingResult.success(AlexMaxInitManager.getInstance().getMaxAdEcpm(maxAd), token, null), customNativeAd);
                                mBiddingListener = null;
                            }
                        }
                    });
                } else {
                    if (mLoadListener != null) {
                        mLoadListener.onAdCacheLoaded(customNativeAd);
                    }
                }
            }

            @Override
            public void onFail(String errorCode, String errorMsg) {
                notifyATLoadFail(errorCode, errorMsg);
            }
        };

        if (TextUtils.equals(mUnitType, "2")) {
            AlexMaxManualNativeAd alexMaxNativeAd = new AlexMaxManualNativeAd(context, nativeAdLoader, loadCallbackListener);
            alexMaxNativeAd.startLoad(localExtras);
        } else {
            AlexMaxNativeAd alexMaxNativeAd = new AlexMaxNativeAd(nativeAdLoader, loadCallbackListener, mMediaWidth, mMediaHeight);
            alexMaxNativeAd.startLoad();
        }

    }

    @Override
    public boolean startBiddingRequest(final Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra, ATBiddingListener biddingListener) {
        initRequestParams(serverExtra, localExtra);
        this.mBiddingListener = biddingListener;

        AlexMaxInitManager.getInstance().initSDK(context, serverExtra, new MediationInitCallback() {
            @Override
            public void onSuccess() {
//                if (checkBiddingCache()) return;
                startLoadAd(context, AlexMaxInitManager.getInstance().getApplovinSdk(), true, localExtra);
            }

            @Override
            public void onFail(String errorMsg) {
                if (mBiddingListener != null) {
                    ATBiddingResult biddingResult = ATBiddingResult.fail("Max: " + errorMsg);
                    mBiddingListener.onC2SBidResult(biddingResult);
                    mBiddingListener = null;
                }
            }
        });
        return true;
    }

//    private boolean checkBiddingCache() {
//        Map.Entry<String, AlexMaxBiddingInfo> cacheEntry = AlexMaxInitManager.getInstance().checkC2SCacheOffer(mAdUnitId);
//        if (cacheEntry != null) {
//            AlexMaxBiddingInfo alexMaxBiddingInfo = cacheEntry.getValue();
//            if (alexMaxBiddingInfo != null && alexMaxBiddingInfo.adObject instanceof CustomNativeAd) {
//                MaxAd maxAd = alexMaxBiddingInfo.maxAd;
//                String cacheId = cacheEntry.getKey();
//                if (mBiddingListener != null) {
//                    mBiddingListener.onC2SBidResult(ATBiddingResult.success(AlexMaxInitManager.getInstance().getMaxAdEcpm(maxAd), cacheId, null));
//                    mBiddingListener = null;
//                }
//                return true;
//            }
//
//        }
//
//        return false;
//    }

    private void initRequestParams(Map<String, Object> serverExtra, Map<String, Object> localExtra) {
        mSdkKey = "";
        mAdUnitId = "";
        mUnitType = "";

        if (serverExtra.containsKey("sdk_key")) {
            mSdkKey = (String) serverExtra.get("sdk_key");
        }
        if (serverExtra.containsKey("unit_id")) {
            mAdUnitId = (String) serverExtra.get("unit_id");
        }
        if (serverExtra.containsKey("payload")) {
            mPayload = serverExtra.get("payload").toString();
        }
        if (serverExtra.containsKey("unit_type")) {
            mUnitType = serverExtra.get("unit_type").toString();
        }

        double maxPriceValue = AlexMaxConst.getMaxPriceValue(serverExtra);
        if (maxPriceValue != -1) {
            isDynamicePrice = true;
            dynamicPrice = maxPriceValue;
        }

        mMediaWidth = getIntFromMap(localExtra, ATAdConst.KEY.AD_WIDTH, 0);
        mMediaHeight = getIntFromMap(localExtra, ATAdConst.KEY.AD_HEIGHT, 0);
    }

    @Override
    public void destory() {

    }

    @Override
    public String getNetworkPlacementId() {
        return mAdUnitId;
    }

    @Override
    public String getNetworkSDKVersion() {
        return AlexMaxInitManager.getInstance().getNetworkVersion();
    }

    @Override
    public String getNetworkName() {
        return AlexMaxInitManager.getInstance().getNetworkName();
    }

    @Override
    public Map<String, Object> getNetworkInfoMap() {
        return mExtraMap;
    }

    @Override
    public boolean setUserDataConsent(Context context, boolean isConsent, boolean isEUTraffic) {
        return AlexMaxInitManager.getInstance().setUserDataConsent(context, isConsent, isEUTraffic);
    }

    protected interface LoadCallbackListener {
        void onSuccess(CustomNativeAd customNativeAd, MaxAd maxAd, Map<String, Object> networkInfoMap);

        void onFail(String errorCode, String errorMsg);
    }
}
