package com.alex;

import android.view.View;
import android.view.ViewGroup;

import com.anythink.nativead.unitgroup.api.CustomNativeAd;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;

import java.util.Map;

public class AlexMaxNativeAd extends CustomNativeAd {

    MaxNativeAdLoader mMaxNativeAdLoader;
    AlexMaxNativeAdapter.LoadCallbackListener mLoadCallbackListener;

    View mMediaView;
    MaxAd mMaxAd;

    private int mMediaWidth;
    private int mMediaHeight;

    public AlexMaxNativeAd(MaxNativeAdLoader maxNativeAdLoader, AlexMaxNativeAdapter.LoadCallbackListener loadCallbackListener, int mediaWidth, int mediaHeight) {
        mMaxNativeAdLoader = maxNativeAdLoader;
        mLoadCallbackListener = loadCallbackListener;
        mMediaWidth = mediaWidth;
        mMediaHeight = mediaHeight;
        mMaxNativeAdLoader.setNativeAdListener(new MaxNativeAdListener() {
            @Override
            public void onNativeAdLoaded(MaxNativeAdView maxNativeAdView, MaxAd maxAd) {
                if (maxNativeAdView == null) {
                    if (mLoadCallbackListener != null) {
                        mLoadCallbackListener.onFail("", "Max return MaxNativeAdView is null.");
                    }
                    return;
                }
                mMediaView = maxNativeAdView;
                mMaxAd = maxAd;
                Map<String, Object> networkInfoMap = AlexMaxInitManager.getInstance().handleMaxAd(maxAd);
                setNetworkInfoMap(networkInfoMap);

                if (mLoadCallbackListener != null) {
                    mLoadCallbackListener.onSuccess(AlexMaxNativeAd.this, maxAd, networkInfoMap);
                }
            }

            @Override
            public void onNativeAdLoadFailed(String s, MaxError maxError) {
                if (mLoadCallbackListener != null) {
                    mLoadCallbackListener.onFail("" + maxError.getCode(), maxError.getMessage());
                }
            }

            @Override
            public void onNativeAdClicked(MaxAd maxAd) {
                notifyAdClicked();
            }

            @Override
            public void onNativeAdExpired(MaxAd maxAd) {
            }
        });

        mMaxNativeAdLoader.setRevenueListener(new MaxAdRevenueListener() {
            @Override
            public void onAdRevenuePaid(MaxAd maxAd) {
                notifyAdImpression();
            }
        });
    }

    public void startLoad() {
        mMaxNativeAdLoader.loadAd();
    }

    @Override
    public boolean isNativeExpress() {
        return true;
    }

    @Override
    public View getAdMediaView(Object... object) {
        if (mMediaWidth != 0 && mMediaHeight != 0) {
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(mMediaWidth, mMediaHeight);
            mMediaView.setLayoutParams(layoutParams);
        }
        return mMediaView;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mMaxNativeAdLoader != null) {
            mMaxNativeAdLoader.destroy(mMaxAd);
        }
    }


}
