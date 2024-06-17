package com.alex;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;

import com.anythink.core.api.ATAdConst;
import com.anythink.core.api.ATBiddingListener;
import com.anythink.core.api.ATBiddingResult;
import com.anythink.core.api.ErrorCode;
import com.anythink.core.api.MediationInitCallback;
import com.anythink.splashad.unitgroup.api.CustomSplashAdapter;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAppOpenAd;
import com.applovin.sdk.AppLovinSdk;

import java.util.Map;

public class AlexMaxSplashAdapter extends CustomSplashAdapter {

    static final String TAG = AlexMaxSplashAdapter.class.getSimpleName();

    String mAdUnitId;
    String mSdkKey;
    String mPayload;

    private MaxAppOpenAd mMaxAppOpenAd;

    Map<String, Object> mExtraMap;

    boolean isDynamicePrice;

    double dynamicPrice;

    @Override
    public void loadCustomNetworkAd(Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra) {
        initRequestParams(serverExtra);

        //Bidding Request
//        if (!TextUtils.isEmpty(mPayload)) {
//            AlexMaxBiddingInfo alexMaxBiddingInfo = AlexMaxInitManager.getInstance().requestC2SOffer(mAdUnitId, mPayload);
//            if (alexMaxBiddingInfo != null && alexMaxBiddingInfo.adObject instanceof MaxAppOpenAd && ((MaxAppOpenAd) alexMaxBiddingInfo.adObject).isReady()) {
//                mMaxAppOpenAd = (MaxAppOpenAd) alexMaxBiddingInfo.adObject;
//                registerListener(false);
//                if (mLoadListener != null) {
//                    mLoadListener.onAdCacheLoaded();
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

                startLoadAd(appLovinSdk, false);
            }

            @Override
            public void onFail(String errorMsg) {

            }
        });
    }

    private void startLoadAd(AppLovinSdk appLovinSdk, final boolean isBidding) {

        mMaxAppOpenAd = new MaxAppOpenAd(mAdUnitId, appLovinSdk);
        if (isDynamicePrice) {
            mMaxAppOpenAd.setExtraParameter("jC7Fp", String.valueOf(dynamicPrice));
        }

        registerListener(isBidding);

        mMaxAppOpenAd.loadAd();
    }

    private void registerListener(final boolean isBidding) {
        mMaxAppOpenAd.setListener(new MaxAdListener() {
            @Override
            public void onAdLoaded(final MaxAd maxAd) {
                if (!isBidding) {
                    if (mLoadListener != null) {
                        mLoadListener.onAdCacheLoaded();
                    }
                } else {
                    runOnNetworkRequestThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mBiddingListener != null) {
                                String token = AlexMaxInitManager.getInstance().getToken();
                                mBiddingListener.onC2SBiddingResultWithCache(ATBiddingResult.success(AlexMaxInitManager.getInstance().getMaxAdEcpm(maxAd), token, null), null);
                                mBiddingListener = null;
                            }
                        }
                    });
                }
            }

            @Override
            public void onAdDisplayed(MaxAd maxAd) {
                if (mExtraMap == null) {
                    mExtraMap = AlexMaxInitManager.getInstance().handleMaxAd(maxAd);
                }
                if (mImpressionListener != null) {
                    mImpressionListener.onSplashAdShow();
                }
            }

            @Override
            public void onAdHidden(MaxAd maxAd) {
                if (mImpressionListener != null) {
                    mImpressionListener.onSplashAdDismiss();
                }
            }

            @Override
            public void onAdClicked(MaxAd maxAd) {
                if (mImpressionListener != null) {
                    mImpressionListener.onSplashAdClicked();
                }
            }

            @Override
            public void onAdLoadFailed(String s, MaxError maxError) {
                notifyATLoadFail(maxError.getCode() + "", maxError.getMessage());
            }

            @Override
            public void onAdDisplayFailed(MaxAd maxAd, MaxError maxError) {
                if (mImpressionListener != null) {
                    mDismissType = ATAdConst.DISMISS_TYPE.SHOWFAILED;
                    mImpressionListener.onSplashAdShowFail(ErrorCode.getErrorCode(ErrorCode.adShowError, maxError.getCode() + "", maxError.getMessage()));
                    mImpressionListener.onSplashAdDismiss();
                }
            }
        });
    }

    @Override
    public boolean isAdReady() {
        return mMaxAppOpenAd != null && mMaxAppOpenAd.isReady();
    }

    @Override
    public void show(Activity activity, ViewGroup container) {
        mMaxAppOpenAd.showAd();
    }


    private void initRequestParams(Map<String, Object> serverExtra) {
        mSdkKey = "";
        mAdUnitId = "";

        if (serverExtra.containsKey("sdk_key")) {
            mSdkKey = (String) serverExtra.get("sdk_key");
        }
        if (serverExtra.containsKey("unit_id")) {
            mAdUnitId = (String) serverExtra.get("unit_id");
        }
        if (serverExtra.containsKey("payload")) {
            mPayload = serverExtra.get("payload").toString();
        }

        double maxPriceValue = AlexMaxConst.getMaxPriceValue(serverExtra);
        if (maxPriceValue != -1) {
            isDynamicePrice = true;
            dynamicPrice = maxPriceValue;
        }
    }

    @Override
    public boolean startBiddingRequest(final Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra, ATBiddingListener biddingListener) {
        initRequestParams(serverExtra);
        this.mBiddingListener = biddingListener;

        AlexMaxInitManager.getInstance().initSDK(context, serverExtra, new MediationInitCallback() {
            @Override
            public void onSuccess() {
//                if (checkBiddingCache()) return;
                startLoadAd(AlexMaxInitManager.getInstance().getApplovinSdk(), true);
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
//            if (alexMaxBiddingInfo != null && alexMaxBiddingInfo.adObject instanceof MaxAppOpenAd && ((MaxAppOpenAd) alexMaxBiddingInfo.adObject).isReady()) {
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

    @Override
    public void destory() {
        if (mMaxAppOpenAd != null) {
            mMaxAppOpenAd.destroy();
            mMaxAppOpenAd = null;
        }
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
}
