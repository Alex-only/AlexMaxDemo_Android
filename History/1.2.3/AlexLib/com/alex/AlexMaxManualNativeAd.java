package com.alex;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.anythink.core.api.ATAdConst;
import com.anythink.nativead.api.ATNativePrepareInfo;
import com.anythink.nativead.unitgroup.api.CustomNativeAd;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;

import java.util.Map;

public class AlexMaxManualNativeAd extends CustomNativeAd {

    MaxNativeAdLoader mMaxNativeAdLoader;
    AlexMaxNativeAdapter.LoadCallbackListener mLoadCallbackListener;

    MaxAd mMaxAd;

    Context mContext;


    MaxNativeAd mMaxNativeAd;

    public AlexMaxManualNativeAd(Context context, MaxNativeAdLoader maxNativeAdLoader, AlexMaxNativeAdapter.LoadCallbackListener loadCallbackListener) {
        mMaxNativeAdLoader = maxNativeAdLoader;
        mLoadCallbackListener = loadCallbackListener;
        mContext = context.getApplicationContext();
        mMaxNativeAdLoader.setNativeAdListener(new MaxNativeAdListener() {
            @Override
            public void onNativeAdLoaded(@Nullable MaxNativeAdView maxNativeAdView, MaxAd maxAd) {
                mMaxAd = maxAd;
                mMaxNativeAd = mMaxAd.getNativeAd();
                if (mMaxNativeAd == null) {
                    if (mLoadCallbackListener != null) {
                        mLoadCallbackListener.onFail("", "Max Manual Native Ad return empty.");
                    }
                    return;
                }


                setData(mMaxNativeAd);
                Map<String, Object> networkInfoMap = AlexMaxInitManager.getInstance().handleMaxAd(maxAd);
                setNetworkInfoMap(networkInfoMap);

                if (mLoadCallbackListener != null) {
                    mLoadCallbackListener.onSuccess(AlexMaxManualNativeAd.this, maxAd, networkInfoMap);
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

    View mAdIconView;
    View mAdMediaView;

    boolean useMaxAdView = false;
    private void setData(MaxNativeAd maxNativeAd) {
        String networkName = mMaxAd.getNetworkName();
        if (networkName != null && networkName.toLowerCase().contains("applovin")) {
            useMaxAdView = true;
        }
        setTitle(maxNativeAd.getTitle());
        setDescriptionText(maxNativeAd.getBody());
        setCallToActionText(maxNativeAd.getCallToAction());
        setAdLogoView(maxNativeAd.getOptionsView());
        setAdvertiserName(maxNativeAd.getAdvertiser());
        setStarRating(maxNativeAd.getStarRating());

        mAdIconView = maxNativeAd.getIconView();
        if (mAdIconView == null) {
            MaxNativeAd.MaxNativeAdImage imageIcon = maxNativeAd.getIcon();
            if (imageIcon != null) {
                if (imageIcon.getDrawable() != null) {
                    ImageView iconView = new ImageView(mContext);
                    iconView.setImageDrawable(imageIcon.getDrawable());
                    mAdIconView = iconView;
                }
                Uri uri = imageIcon.getUri();
                if (uri != null) {
                    String url = uri.toString();
                    if (url.startsWith("file")) {
                        ImageView iconView = new ImageView(mContext);
                        iconView.setImageURI(uri);
                        mAdIconView = iconView;
                    }
                    setIconImageUrl(url);
                }

            }
        }


        MaxNativeAd.MaxNativeAdImage imageMain = maxNativeAd.getMainImage();
        mAdMediaView = mMaxNativeAd.getMediaView();
        if (mAdMediaView == null) {
            if (imageMain != null) {
                Drawable mainDrawable = imageMain.getDrawable();
                if (mainDrawable != null) {
                    ImageView mainImageView = new ImageView(mContext);
                    mainImageView.setImageDrawable(mainDrawable);
                    mAdMediaView = mainImageView;
                } else if (imageMain.getUri() != null) {
                    setMainImageUrl(imageMain.getUri().toString());
                }
            }
        }

    }

    @Override
    public void prepare(View view, ATNativePrepareInfo nativePrepareInfo) {
        super.prepare(view, nativePrepareInfo);
        try {
            //For Admob click
            View iconView = nativePrepareInfo.getIconView();
            if (iconView != null) {
                iconView.setTag(3);
//                if (iconView instanceof ImageView) {
//                    frameLayout.setIconImageView((ImageView) iconView);
//                }
            }
            View titleView = nativePrepareInfo.getTitleView();
            if (titleView != null) {
                titleView.setTag(1);
//                if (titleView instanceof TextView) {
//                    frameLayout.setTitleTextView((TextView) titleView);
//                }
            }
            View bodyView = nativePrepareInfo.getDescView();
            if (bodyView != null) {
                bodyView.setTag(4);
//                if (bodyView instanceof TextView) {
//                    frameLayout.setBodyTextView((TextView) bodyView);
//                }
            }
            View ctaView = nativePrepareInfo.getCtaView();
            if (ctaView != null) {
                ctaView.setTag(5);
//                if (ctaView instanceof Button) {
//                    frameLayout.setCallToActionButton((Button) ctaView);
//                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        prepareForInteraction(nativePrepareInfo);

    }

    private void prepareForInteraction(ATNativePrepareInfo nativePrepareInfo) {
        if (frameLayout instanceof AlexMaxNativeAdView) {
            ((AlexMaxNativeAdView) frameLayout).setClickableList(nativePrepareInfo.getClickViewList());
            mMaxNativeAdLoader.render(((AlexMaxNativeAdView) frameLayout), mMaxAd);
        } else {
            try {
                mMaxNativeAdLoader.a(nativePrepareInfo.getClickViewList(), frameLayout, mMaxAd);
                return;
            } catch (Throwable e) {
                e.printStackTrace();
                Log.e("AlexMaxManualNativeAd", "prepare fail:" + e.getMessage());
            }
//
            try {
                mMaxNativeAd.prepareForInteraction(nativePrepareInfo.getClickViewList(), frameLayout);
            } catch (Throwable e) {
                e.printStackTrace();
                Log.e("AlexMaxManualNativeAd", "prepare fail:" + e.getMessage());
            }
        }
    }

    FrameLayout frameLayout;

    @Override

    public ViewGroup getCustomAdContainer() {
        if (useMaxAdView) {
            frameLayout = new AlexMaxNativeAdView(mContext);
        } else {
            frameLayout = new FrameLayout(mContext);
        }
//        frameLayout = new FrameLayout(mContext);
        return frameLayout;
    }

    private static final int ADCHOICES_TOP_LEFT = 0;
    private static final int ADCHOICES_TOP_RIGHT = 1;
    private static final int ADCHOICES_BOTTOM_RIGHT = 2;
    private static final int ADCHOICES_BOTTOM_LEFT = 3;

    private int admob_adchoices = ADCHOICES_TOP_RIGHT;

    public void startLoad(Map<String, Object> localExtras) {
        try {
            if (localExtras.containsKey(ATAdConst.KEY.AD_CHOICES_PLACEMENT)) {
                int tempAdChoicePlacement = Integer.parseInt(localExtras.get(ATAdConst.KEY.AD_CHOICES_PLACEMENT).toString());
                switch (tempAdChoicePlacement) {
                    case ATAdConst.AD_CHOICES_PLACEMENT_TOP_LEFT:
                        admob_adchoices = ADCHOICES_TOP_LEFT;
                        break;
                    case ATAdConst.AD_CHOICES_PLACEMENT_TOP_RIGHT:
                        admob_adchoices = ADCHOICES_TOP_RIGHT;
                        break;
                    case ATAdConst.AD_CHOICES_PLACEMENT_BOTTOM_RIGHT:
                        admob_adchoices = ADCHOICES_BOTTOM_RIGHT;
                        break;
                    case ATAdConst.AD_CHOICES_PLACEMENT_BOTTOM_LEFT:
                        admob_adchoices = ADCHOICES_BOTTOM_LEFT;
                        break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        mMaxNativeAdLoader.setLocalExtraParameter("admob_ad_choices_placement", admob_adchoices);
        mMaxNativeAdLoader.loadAd();
    }

    @Override
    public View getAdIconView() {
        return mAdIconView;
    }

    @Override
    public View getAdMediaView(Object... object) {
        return mAdMediaView;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mMaxNativeAdLoader != null) {
            mMaxNativeAdLoader.destroy(mMaxAd);
        }
    }

}
