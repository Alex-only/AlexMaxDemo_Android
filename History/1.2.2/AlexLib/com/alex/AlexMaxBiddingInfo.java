package com.alex;

import com.applovin.mediation.MaxAd;

public class AlexMaxBiddingInfo {
    protected MaxAd maxAd;
    protected Object adObject;

    protected AlexMaxBiddingInfo(Object adObject, MaxAd maxAd){
        this.maxAd = maxAd;
        this.adObject = adObject;
    }
}
