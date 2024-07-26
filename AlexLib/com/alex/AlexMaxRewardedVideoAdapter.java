package com.alex;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.anythink.core.api.ATAdConst;
import com.anythink.core.api.ATBiddingListener;
import com.anythink.core.api.ATBiddingResult;
import com.anythink.core.api.MediationInitCallback;
import com.anythink.rewardvideo.unitgroup.api.CustomRewardVideoAdapter;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.sdk.AppLovinSdk;

import java.util.HashMap;
import java.util.Map;

public class AlexMaxRewardedVideoAdapter extends CustomRewardVideoAdapter {

    static final String TAG = AlexMaxRewardedVideoAdapter.class.getSimpleName();

    String mAdUnitId;
    AlexMaxRewardAd mMaxRewardedAd;
    String mSdkKey;

    String mPayload;

    Map<String, Object> mExtraMap;

    boolean isDynamicePrice;

    double dynamicPrice;

    @Override
    public void show(Activity activity) {
        if (mMaxRewardedAd != null) {
            mMaxRewardedAd.show(activity, createImpressionListener());
        }
    }

    @Override
    public void loadCustomNetworkAd(final Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra) {
        initRequestParams(serverExtra);

        //Bidding Request
        if (!TextUtils.isEmpty(mPayload)) {
            AlexMaxBiddingInfo alexMaxBiddingInfo = AlexMaxInitManager.getInstance().requestC2SOffer(mAdUnitId, mPayload);
            AppLovinSdk appLovinSdk = AlexMaxInitManager.getInstance().getApplovinSdk();
            if (appLovinSdk != null) {
                appLovinSdk.setUserIdentifier(mUserId);
            }
            if (alexMaxBiddingInfo != null && alexMaxBiddingInfo.adObject instanceof AlexMaxRewardAd && ((AlexMaxRewardAd) alexMaxBiddingInfo.adObject).isReady()) {
                mMaxRewardedAd = (AlexMaxRewardAd) alexMaxBiddingInfo.adObject;
                createLoadListener(false);
                if (mLoadListener != null) {
                    mLoadListener.onAdCacheLoaded();
                }
            } else {
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadError("", "Max: Bidding Cache is Empty or not ready.");
                }
            }
            return;
        }


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
                appLovinSdk.setUserIdentifier(mUserId);

                startLoadAd(context, appLovinSdk, false);
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


    private void startLoadAd(Context context, AppLovinSdk appLovinSdk, final boolean isBiddingRequest) {

        mMaxRewardedAd = AlexMaxRewardAd.getInstance(context, appLovinSdk, mAdUnitId);
        if (isDynamicePrice) {
            mMaxRewardedAd.setExtraParameter("jC7Fp", String.valueOf(dynamicPrice));
        }

        mMaxRewardedAd.load(createLoadListener(isBiddingRequest));
    }


    private MaxRewardedAdListener createLoadListener(final boolean isBiddingRequest) {
        MaxRewardedAdListener rewardedAdListener = new MaxRewardedAdListener() {

            @Override
            public void onAdLoaded(final MaxAd maxAd) {
                if (!isBiddingRequest) {
                    if (mLoadListener != null) {
                        mLoadListener.onAdCacheLoaded();
                    }
                } else {
                    runOnNetworkRequestThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mBiddingListener != null) {
                                String cacheId = AlexMaxInitManager.getInstance().saveC2SOffer(mAdUnitId, mMaxRewardedAd, maxAd);
                                mBiddingListener.onC2SBidResult(ATBiddingResult.success(AlexMaxInitManager.getInstance().getMaxAdEcpm(maxAd), cacheId, null));
                                mBiddingListener = null;
                            }
                        }
                    });

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
            public void onRewardedVideoStarted(MaxAd maxAd) {

            }

            @Override
            public void onRewardedVideoCompleted(MaxAd maxAd) {

            }

            @Override
            public void onUserRewarded(MaxAd maxAd, MaxReward maxReward) {

            }

            @Override
            public void onAdDisplayed(MaxAd maxAd) {

            }

            @Override
            public void onAdHidden(MaxAd maxAd) {

            }

            @Override
            public void onAdClicked(MaxAd maxAd) {

            }


            @Override
            public void onAdDisplayFailed(MaxAd maxAd, MaxError maxError) {

            }

        };

        return rewardedAdListener;
    }

    private MaxRewardedAdListener createImpressionListener() {
        MaxRewardedAdListener maxRewardedAdListener = new MaxRewardedAdListener() {

            @Override
            public void onAdLoaded(MaxAd maxAd) {

            }

            @Override
            public void onAdDisplayed(MaxAd maxAd) {
                if (mExtraMap == null) {
                    mExtraMap = AlexMaxInitManager.getInstance().handleMaxAd(maxAd);
                }
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdPlayStart();
                }
            }

            @Override
            public void onRewardedVideoStarted(MaxAd maxAd) {

            }

            @Override
            public void onRewardedVideoCompleted(MaxAd maxAd) {
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdPlayEnd();
                }
            }

            @Override
            public void onUserRewarded(MaxAd maxAd, MaxReward maxReward) {
                if (mExtraMap == null) {
                    mExtraMap = AlexMaxInitManager.getInstance().handleMaxAd(maxAd);
                }

                try {
                    if (maxReward != null) {
                        if (mExtraMap == null) {
                            mExtraMap = new HashMap<>();
                        }
                        Map<String, Object> rewardMap = new HashMap<>();
                        rewardMap.put(AlexMaxConst.REWARD_EXTRA.REWARD_EXTRA_KEY_REWARD_AMOUNT, maxReward.getAmount());
                        rewardMap.put(AlexMaxConst.REWARD_EXTRA.REWARD_EXTRA_KEY_REWARD_LABEL, maxReward.getLabel());

                        mExtraMap.put(ATAdConst.REWARD_EXTRA.REWARD_INFO, rewardMap);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                if (mImpressionListener != null) {
                    mImpressionListener.onReward();
                }
            }


            @Override
            public void onAdHidden(MaxAd maxAd) {
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdClosed();
                }
            }

            @Override
            public void onAdClicked(MaxAd maxAd) {
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdPlayClicked();
                }
            }

            @Override
            public void onAdLoadFailed(String s, MaxError maxError) {

            }


            @Override
            public void onAdDisplayFailed(MaxAd maxAd, MaxError maxError) {
                Log.e(TAG, "onAdDisplayFailed: errorCode: " + maxError.getCode() + ",errorMessage: " + maxError.getMessage());
                if (mImpressionListener != null) {
                    mImpressionListener.onRewardedVideoAdPlayFailed("" + maxError.getCode(), maxError.getMessage());
                }
            }
        };

        return maxRewardedAdListener;
    }

    @Override
    public void destory() {
        if (mMaxRewardedAd != null) {
//            mMaxRewardedAd.setListener(null);
//            mMaxRewardedAd.destroy();
            mMaxRewardedAd = null;
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
        if (mMaxRewardedAd != null) {
            return mMaxRewardedAd.isReady();
        }
        return false;
    }

    ATBiddingListener mBiddingListener;

    @Override
    public boolean startBiddingRequest(final Context context, Map<String, Object> serverExtra, Map<String, Object> localExtra, ATBiddingListener biddingListener) {
        initRequestParams(serverExtra);

        this.mBiddingListener = biddingListener;

        AlexMaxInitManager.getInstance().initSDK(context, serverExtra, new MediationInitCallback() {
            @Override
            public void onSuccess() {
                if (checkBiddingCache()) return;
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

    private boolean checkBiddingCache() {
        Map.Entry<String, AlexMaxBiddingInfo> cacheEntry = AlexMaxInitManager.getInstance().checkC2SCacheOffer(mAdUnitId);
        if (cacheEntry != null) {
            AlexMaxBiddingInfo alexMaxBiddingInfo = cacheEntry.getValue();
            if (alexMaxBiddingInfo != null && alexMaxBiddingInfo.adObject instanceof MaxRewardedAd && ((MaxRewardedAd) alexMaxBiddingInfo.adObject).isReady()) {
                MaxAd maxAd = alexMaxBiddingInfo.maxAd;
                String cacheId = cacheEntry.getKey();
                if (mBiddingListener != null) {
                    mBiddingListener.onC2SBidResult(ATBiddingResult.success(AlexMaxInitManager.getInstance().getMaxAdEcpm(maxAd), cacheId, null));
                    mBiddingListener = null;
                }
                return true;
            }

        }

        return false;
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
