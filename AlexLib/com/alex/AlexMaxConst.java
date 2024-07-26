package com.alex;

import com.applovin.sdk.AppLovinSdk;

import java.util.Map;

public class AlexMaxConst {

    private static final String ADAPTER_VERSION = "1.1.9";
    public static final String IS_ADAPTIVE = "is_adaptive";

    public static class REWARD_EXTRA {
        public static final String REWARD_EXTRA_KEY_REWARD_AMOUNT = "alex_reward_amount";
        public static final String REWARD_EXTRA_KEY_REWARD_LABEL = "alex_reward_label";
    }

    public static String getNetworkVersion() {
        try {
            return AppLovinSdk.VERSION;
        } catch (Throwable e) {

        }
        return "";
    }

    public static Object getMaxUnitInfoObj(Map<String, Object> serverExtra) {
        try {
            for (String key : serverExtra.keySet()) {
                try {
                    if (key.contains("dynamic_unit_info")) {
                        return serverExtra.get(key);
                    }
                } catch (Throwable e) {
                }
            }
        } catch (Throwable e) {
        }

        return null;
    }


    public static double getMaxPriceValue(Map<String, Object> serverExtra) {
        try {
            for (String key : serverExtra.keySet()) {
                try {
                    if (key.contains("dynamic_max_price")) {
                        return Double.parseDouble(serverExtra.get(key).toString());
                    }
                } catch (Throwable e) {
                }
            }
        } catch (Throwable e) {
        }

        return -1;
    }

}
