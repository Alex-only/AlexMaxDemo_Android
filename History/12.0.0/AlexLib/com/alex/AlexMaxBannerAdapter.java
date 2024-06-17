package com.alex;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.anythink.banner.unitgroup.api.CustomBannerAdapter;
import com.anythink.core.api.ATBiddingListener;
import com.anythink.core.api.ATBiddingResult;
import com.anythink.core.api.MediationInitCallback;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;

import java.util.Map;

public class AlexMaxBannerAdapter extends CustomBannerAdapter {

    static final String TAG = AlexMaxBannerAdapter.class.getSimpleName();

    String mAdUnitId;
    MaxAdView mMaxAdView;
    String mUnitType;//0: banner , 1: MREc

    String mSdkKey;

    String mPayload;

    Map<String, Object> mExtraMap;

    boolean isDynamicePrice;

    double dynamicPrice;

    @Override
    public View getBannerView() {
        return mMaxAdView;
    }

    @Override
    public void loadCustomNetworkAd(final Context context, final Map<String, Object> serverExtra, final Map<String, Object> localExtra) {
        initRequestParams(serverExtra);

        //Bidding Request
//        if (!TextUtils.isEmpty(mPayload)) {
//            AlexMaxBiddingInfo alexMaxBiddingInfo = AlexMaxInitManager.getInstance().requestC2SOffer(mAdUnitId, mPayload);
//            if (alexMaxBiddingInfo != null && alexMaxBiddingInfo.adObject instanceof MaxAdView) {
//                mMaxAdView = (MaxAdView) alexMaxBiddingInfo.adObject;
//                mExtraMap = AlexMaxInitManager.getInstance().handleMaxAd(alexMaxBiddingInfo.maxAd);
//                registerListener(false);
//                registerImpressionListener();
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
                mLoadListener.onAdLoadError("", "Max: sdk_key、unit_id could not be null.");
            }
            return;
        }

        runOnNetworkRequestThread(new Runnable() {
            @Override
            public void run() {
                AlexMaxInitManager.getInstance().initSDK(context, serverExtra, new MediationInitCallback() {
                    @Override
                    public void onSuccess() {
                        postOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    startLoadAd(context, AlexMaxInitManager.getInstance().getApplovinSdk(), localExtra, false);
                                } catch (Throwable e) {
                                    if (mLoadListener != null) {
                                        mLoadListener.onAdLoadError("", e.getMessage());
                                    }
                                }

                            }
                        });

                    }

                    @Override
                    public void onFail(String errorMsg) {

                    }
                });
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

        try {
            if (serverExtra.containsKey("unit_type")) {
                mUnitType = serverExtra.get("unit_type").toString();
            }
        } catch (Throwable e) {
        }

    }

    private void startLoadAd(Context context, AppLovinSdk appLovinSdk, Map<String, Object> localExtra, final boolean isBiddingRequest) {
        if (!(context instanceof Activity)) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadError("", "Max: context must be activity");
            }
            return;
        }

        if (TextUtils.equals("1", mUnitType)) { //MREC
            mMaxAdView = new MaxAdView(mAdUnitId, MaxAdFormat.MREC, appLovinSdk, ((Activity) context));
        } else { //Banner
            mMaxAdView = new MaxAdView(mAdUnitId, appLovinSdk, ((Activity) context));
        }

        if (isDynamicePrice) {
            mMaxAdView.setExtraParameter("disable_precache", "true");
            mMaxAdView.setExtraParameter("jC7Fp", String.valueOf(dynamicPrice));
            mMaxAdView.setExtraParameter("allow_pause_auto_refresh_immediately", "true");
            mMaxAdView.stopAutoRefresh();
        }

        registerListener(isBiddingRequest);

        int defaultWidth = ViewGroup.LayoutParams.MATCH_PARENT;
        boolean tablet = AppLovinSdkUtils.isTablet(context);
        int defaultHeight = AppLovinSdkUtils.dpToPx(context, tablet ? 90 : 50);
        int width = 0;
        int height = 0;


        if (TextUtils.equals("1", mUnitType)) { //MREC
            width = AppLovinSdkUtils.dpToPx(context, 300);
            height = AppLovinSdkUtils.dpToPx(context, 250);
        } else { //Banner
            if (localExtra.containsKey(AlexMaxConst.IS_ADAPTIVE)) {

                try {
                    if ((Boolean) localExtra.get(AlexMaxConst.IS_ADAPTIVE)) {
                        width = defaultWidth;

                        int adaptiveHeight = MaxAdFormat.BANNER.getAdaptiveSize(((Activity) context)).getHeight();
                        height = AppLovinSdkUtils.dpToPx(context, adaptiveHeight);
                    }
                } catch (Throwable e) {
                }
            }
        }

        if (width == 0) {
            width = defaultWidth;
        }

        if (height == 0) {
            height = defaultHeight;
        }

        mMaxAdView.setLayoutParams(new FrameLayout.LayoutParams(width, height));

        mMaxAdView.stopAutoRefresh();
        mMaxAdView.loadAd();
    }

    private void registerListener(final boolean isBiddingRequest) {
        mMaxAdView.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(MaxAd maxAd) {
            }

            @Override
            public void onAdCollapsed(MaxAd maxAd) {
            }

            @Override
            public void onAdLoaded(final MaxAd maxAd) {
                registerImpressionListener();
                if (!isBiddingRequest) {
                    mExtraMap = AlexMaxInitManager.getInstance().handleMaxAd(maxAd);

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
                /* DO NOT USE - THIS IS RESERVED FOR FULLSCREEN ADS ONLY AND WILL BE REMOVED IN A FUTURE SDK RELEASE */
            }

            @Override
            public void onAdHidden(MaxAd maxAd) {
                /* DO NOT USE - THIS IS RESERVED FOR FULLSCREEN ADS ONLY AND WILL BE REMOVED IN A FUTURE SDK RELEASE */
            }

            @Override
            public void onAdClicked(MaxAd maxAd) {
                if (mImpressionEventListener != null) {
                    mImpressionEventListener.onBannerAdClicked();
                }
            }

            @Override
            public void onAdLoadFailed(String s, MaxError maxError) {
                if (!isBiddingRequest) {
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
                Log.e(TAG, "onAdDisplayFailed: errorCode: " + maxError.getCode() + ",errorMessage: " + maxError.getMessage());
            }
        });
    }

    private void registerImpressionListener() {
        if (mMaxAdView == null) {
            return;
        }
        if (mMaxAdView.isShown()) {
            if (mImpressionEventListener != null) {
                mImpressionEventListener.onBannerAdShow();
            }
        } else {
            mMaxAdView.stopAutoRefresh();
            mMaxAdView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (mMaxAdView != null && mMaxAdView.isShown()) {
                        mMaxAdView.getViewTreeObserver().removeOnPreDrawListener(this);
                        if (mImpressionEventListener != null) {
                            mImpressionEventListener.onBannerAdShow();
                        }
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public void destory() {
        if (mMaxAdView != null) {
            mMaxAdView.setListener(null);
            mMaxAdView.destroy();
            mMaxAdView = null;
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

//    @Override
//    public boolean supportImpressionCallback() {
//        return false;
//    }


    ATBiddingListener mBiddingListener;

    @Override
    public boolean startBiddingRequest(final Context context, Map<String, Object> serverExtra, final Map<String, Object> localExtra, ATBiddingListener biddingListener) {
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
                postOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        startLoadAd(context, AlexMaxInitManager.getInstance().getApplovinSdk(), localExtra, true);
                    }
                });

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
//            if (alexMaxBiddingInfo != null && alexMaxBiddingInfo.adObject instanceof MaxAdView) {
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
