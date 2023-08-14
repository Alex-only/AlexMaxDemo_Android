package com.alex;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.anythink.core.api.ATAdConst;
import com.anythink.core.api.ATBiddingListener;
import com.anythink.core.api.ATBiddingResult;
import com.anythink.core.api.MediationInitCallback;
import com.anythink.interstitial.unitgroup.api.CustomInterstitialAdapter;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.sdk.AppLovinSdk;

import java.util.Map;

public class AlexMaxInterstitialAdapter extends CustomInterstitialAdapter {

    static final String TAG = AlexMaxInterstitialAdapter.class.getSimpleName();

    String mAdUnitId;
    String mSdkKey;
    MaxInterstitialAd mMaxInterstitialAd;

    String mPayload;

    Map<String, Object> mExtraMap;

    boolean isDynamicePrice;

    double dynamicPrice;

    @Override
    public void show(Activity activity) {
        if (mMaxInterstitialAd != null) {
            mMaxInterstitialAd.showAd();
        }
    }

    @Override
    public void loadCustomNetworkAd(final Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra) {
        initRequestParams(serverExtra);

        //Bidding Request
//        if (!TextUtils.isEmpty(mPayload)) {
//            AlexMaxBiddingInfo alexMaxBiddingInfo = AlexMaxInitManager.getInstance().requestC2SOffer(mAdUnitId, mPayload);
//            if (alexMaxBiddingInfo != null && alexMaxBiddingInfo.adObject instanceof MaxInterstitialAd && ((MaxInterstitialAd) alexMaxBiddingInfo.adObject).isReady()) {
//                mMaxInterstitialAd = (MaxInterstitialAd) alexMaxBiddingInfo.adObject;
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
                startLoadAd((Activity) context, AlexMaxInitManager.getInstance().getApplovinSdk(), false);
            }

            @Override
            public void onFail(String errorMsg) {

            }
        });
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

    private void startLoadAd(Context context, AppLovinSdk appLovinSdk, final boolean isBidding) {
        if (!(context instanceof Activity)) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadError("", "Max: context must be activity");
            }
            return;
        }

        mMaxInterstitialAd = new MaxInterstitialAd(mAdUnitId, appLovinSdk, (Activity) context);
        if (isDynamicePrice) {
            mMaxInterstitialAd.setExtraParameter("jC7Fp", String.valueOf(dynamicPrice));
        }
        registerListener(isBidding);

        mMaxInterstitialAd.loadAd();
    }

    private void registerListener(final boolean isBidding) {
        mMaxInterstitialAd.setListener(new MaxAdListener() {

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
                if (mImpressListener != null) {
                    mImpressListener.onInterstitialAdShow();
                }
            }

            @Override
            public void onAdHidden(MaxAd maxAd) {
                if (mImpressListener != null) {
                    mImpressListener.onInterstitialAdClose();
                }
            }

            @Override
            public void onAdClicked(MaxAd maxAd) {
                if (mImpressListener != null) {
                    mImpressListener.onInterstitialAdClicked();
                }
            }

            @Override
            public void onAdLoadFailed(String s, MaxError maxError) {
                if (!isBidding) {
                    if (mLoadListener != null) {
                        mLoadListener.onAdLoadError(maxError.getCode() + "", maxError.getMessage());
                    }
                } else {
                    if (mBiddingListener != null) {
                        mBiddingListener.onC2SBidResult(ATBiddingResult.fail("Max: error code:" + maxError.getCode() + " | error msg:" + maxError.getMessage()));
                        mBiddingListener = null;
                    }
                }
            }

            @Override
            public void onAdDisplayFailed(MaxAd maxAd, MaxError maxError) {
                mDismissType = ATAdConst.DISMISS_TYPE.SHOWFAILED;
                Log.e(TAG, "onAdDisplayFailed: errorCode: " + maxError.getCode() + ",errorMessage: " + maxError.getMessage());
            }
        });
    }

    @Override
    public void destory() {
        if (mMaxInterstitialAd != null) {
            mMaxInterstitialAd.setListener(null);
            mMaxInterstitialAd.destroy();
            mMaxInterstitialAd = null;
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
    public boolean isAdReady() {
        if (mMaxInterstitialAd != null) {
            return mMaxInterstitialAd.isReady();
        }
        return false;
    }


    ATBiddingListener mBiddingListener;

    @Override
    public boolean startBiddingRequest(final Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra, ATBiddingListener biddingListener) {
        initRequestParams(serverExtra);
        if (!(context instanceof Activity)) {
            if (biddingListener != null) {
                ATBiddingResult biddingResult = ATBiddingResult.fail("Max: context must be activity");
                biddingListener.onC2SBidResult(biddingResult);
            }
            return true;
        }
        this.mBiddingListener = biddingListener;

        AlexMaxInitManager.getInstance().initSDK(context, serverExtra, new MediationInitCallback() {
            @Override
            public void onSuccess() {
//                if (checkBiddingCache()) return;
                startLoadAd(context, AlexMaxInitManager.getInstance().getApplovinSdk(), true);
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
//            if (alexMaxBiddingInfo != null && alexMaxBiddingInfo.adObject instanceof MaxInterstitialAd && ((MaxInterstitialAd) alexMaxBiddingInfo.adObject).isReady()) {
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
    public Map<String, Object> getNetworkInfoMap() {
        return mExtraMap;
    }

    @Override
    public boolean setUserDataConsent(Context context, boolean isConsent, boolean isEUTraffic) {
        return AlexMaxInitManager.getInstance().setUserDataConsent(context, isConsent, isEUTraffic);
    }
}
