package com.alex;

import android.app.Activity;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxRewardedAd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AlexMaxRewardAd {

    private static Map<String, AlexMaxRewardAd> maxATRewardAdMap;
    MaxRewardedAd maxRewardedAd;
    MaxRewardedAdListener maxRewardedAdListener;

    MaxRewardedAdListener mLoadListener;
    MaxRewardedAdListener mImpressionListener;

    MaxAd mMaxAd;

    private AlexMaxRewardAd(String adUnitId) {
        maxRewardedAd = MaxRewardedAd.getInstance(adUnitId);
        maxRewardedAdListener = new MaxRewardedAdListener() {

            @Override
            public void onAdLoaded(MaxAd maxAd) {
                mMaxAd = maxAd;

                if (mLoadListener != null) {
                    mLoadListener.onAdLoaded(maxAd);
                }
                mLoadListener = null;
            }

            @Override
            public void onAdLoadFailed(String s, MaxError maxError) {
                if (mLoadListener != null) {
                    mLoadListener.onAdLoadFailed(s, maxError);
                }

                mLoadListener = null;
            }

            //--------------------------------------------------------------------------------------
            //@Override
            //public void onRewardedVideoStarted(MaxAd maxAd) {
            //    if (mImpressionListener != null) {
            //        mImpressionListener.onRewardedVideoStarted(maxAd);
            //    }
            //}
            //
            //@Override
            //public void onRewardedVideoCompleted(MaxAd maxAd) {
            //    if (mImpressionListener != null) {
            //        mImpressionListener.onRewardedVideoCompleted(maxAd);
            //    }
            //}

            @Override
            public void onUserRewarded(MaxAd maxAd, MaxReward maxReward) {
                if (mImpressionListener != null) {
                    mImpressionListener.onUserRewarded(maxAd, maxReward);
                }
            }


            @Override
            public void onAdDisplayed(MaxAd maxAd) {
                if (mImpressionListener != null) {
                    mImpressionListener.onAdDisplayed(maxAd);
                }
            }

            @Override
            public void onAdHidden(MaxAd maxAd) {
                if (mImpressionListener != null) {
                    mImpressionListener.onAdHidden(maxAd);
                }
            }

            @Override
            public void onAdClicked(MaxAd maxAd) {
                if (mImpressionListener != null) {
                    mImpressionListener.onAdClicked(maxAd);
                }
            }


            @Override
            public void onAdDisplayFailed(MaxAd maxAd, MaxError maxError) {
                if (mLoadListener != null) {
                    mLoadListener.onAdDisplayFailed(maxAd, maxError);
                }
            }
        };

        maxRewardedAd.setListener(maxRewardedAdListener);
    }

    public synchronized static AlexMaxRewardAd getInstance(String adUnitId) {
        if (maxATRewardAdMap == null) {
            maxATRewardAdMap = new ConcurrentHashMap<>();
        }

        AlexMaxRewardAd alexMaxRewardAd = maxATRewardAdMap.get(adUnitId);
        if (alexMaxRewardAd == null) {
            alexMaxRewardAd = new AlexMaxRewardAd(adUnitId);
            maxATRewardAdMap.put(adUnitId, alexMaxRewardAd);
        }
        return alexMaxRewardAd;
    }

    public void load(MaxRewardedAdListener loadListener) {
        mLoadListener = loadListener;
        maxRewardedAd.loadAd();
    }

    public void show(Activity activity, MaxRewardedAdListener rewardedVideoEventListener) {
        mImpressionListener = rewardedVideoEventListener;
        maxRewardedAd.showAd(activity);
    }

    public void setExtraParameter(String key, String values) {
        maxRewardedAd.setExtraParameter(key, values);
    }

    public boolean isReady() {
        if (maxRewardedAd != null) {
            return maxRewardedAd.isReady();
        }
        return false;
    }

    public MaxAd getMaxAd() {
        return mMaxAd;
    }


}
