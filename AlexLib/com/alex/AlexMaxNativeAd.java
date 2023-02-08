package com.alex;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.anythink.nativead.unitgroup.api.CustomNativeAd;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;

public class AlexMaxNativeAd extends CustomNativeAd {

    MaxNativeAdLoader mMaxNativeAdLoader;
    LoadCallbackListener mLoadCallbackListener;

    View mMediaView;
    MaxAd mMaxAd;

    private int mMediaWidth;
    private int mMediaHeight;

    public AlexMaxNativeAd(MaxNativeAdLoader maxNativeAdLoader, LoadCallbackListener loadCallbackListener, int mediaWidth, int mediaHeight) {
        mMaxNativeAdLoader = maxNativeAdLoader;
        mLoadCallbackListener = loadCallbackListener;
        mMediaWidth = mediaWidth;
        mMediaHeight = mediaHeight;
        mMaxNativeAdLoader.setNativeAdListener(new MaxNativeAdListener() {
            @Override
            public void onNativeAdLoaded(@Nullable MaxNativeAdView maxNativeAdView, MaxAd maxAd) {
                mMediaView = maxNativeAdView;
                mMaxAd = maxAd;

                if (mLoadCallbackListener != null) {
                    mLoadCallbackListener.onSuccess(AlexMaxNativeAd.this, maxAd);
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

    protected interface LoadCallbackListener {
        void onSuccess(CustomNativeAd customNativeAd, MaxAd maxAd);

        void onFail(String errorCode, String errorMsg);
    }
}
