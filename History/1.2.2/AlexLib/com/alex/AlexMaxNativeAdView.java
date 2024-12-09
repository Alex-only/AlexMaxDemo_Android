package com.alex;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder;

import java.util.List;

public class AlexMaxNativeAdView extends MaxNativeAdView {

    public AlexMaxNativeAdView(Context context) {
        super(new MaxNativeAdViewBinder.Builder(new FrameLayout(context)).build(), context);
    }

//    public AlexMaxNativeAdView(MaxNativeAd maxNativeAd, Activity activity) {
//        super(maxNativeAd, activity);
//    }
//
//    public AlexMaxNativeAdView(String s, Context context) {
//        super(s, context);
//    }
//
//    public AlexMaxNativeAdView(@Nullable MaxNativeAd maxNativeAd, @Nullable String s, Activity activity) {
//        super(maxNativeAd, s, activity);
//    }
//
//    public AlexMaxNativeAdView(@Nullable MaxNativeAd maxNativeAd, @Nullable String s, Context context) {
//        super(maxNativeAd, s, context);
//    }
//
//    public AlexMaxNativeAdView(MaxNativeAdViewBinder maxNativeAdViewBinder, Context context) {
//        super(maxNativeAdViewBinder, context);
//    }
//
//    public AlexMaxNativeAdView(@Nullable MaxNativeAd maxNativeAd, MaxNativeAdViewBinder maxNativeAdViewBinder, Context context) {
//        super(maxNativeAd, maxNativeAdViewBinder, context);
//    }

    ImageView iconImageView;
    TextView titleTextView;
    TextView bodyTextView;
    Button callActionView;
    public void setIconImageView(ImageView iconImageView){
        this.iconImageView = iconImageView;
    }

    public void setTitleTextView(TextView titleTextView){
        this.titleTextView = titleTextView;
    }

    public void setBodyTextView(TextView bodyTextView){
        this.bodyTextView = bodyTextView;
    }

    public void setCallToActionButton(Button callActionView){
        this.callActionView = callActionView;
    }


    @Override
    public ImageView getIconImageView() {
        return iconImageView;
    }

    @Override
    public TextView getTitleTextView() {
        return titleTextView;
    }

    @Override
    public TextView getBodyTextView() {
        return bodyTextView;
    }

    @Override
    public Button getCallToActionButton() {
        return callActionView;
    }



    boolean hasAttachWindow;

    public boolean isHasAttachWindow() {
        return hasAttachWindow;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        hasAttachWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        hasAttachWindow = false;
    }

    List<View> clickableList;

    public void setClickableList(List<View> clickableList) {
        this.clickableList = clickableList;
    }

    @Override
    public List<View> getClickableViews() {
        return clickableList;
    }
}
