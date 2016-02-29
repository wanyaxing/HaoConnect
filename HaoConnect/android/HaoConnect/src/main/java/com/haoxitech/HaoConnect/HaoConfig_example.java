package com.haoxitech.HaoConnect;

/**
 * Created by wangtao on 15/12/21.
 */
public class HaoConfig_example {

    private static String Clientversion = "";

    public static String getClientInfo() {
        return "???";
    }

    public static void setClientVersion(String clientVersion) {
        Clientversion = clientVersion;
    }

    public static String getClientVersion() {
        return Clientversion;
    }

    public static String getSecretHax() {
        return "secret=???";
    }

    public static String getApiHost() {
        return "api.???.com";
    }
}
