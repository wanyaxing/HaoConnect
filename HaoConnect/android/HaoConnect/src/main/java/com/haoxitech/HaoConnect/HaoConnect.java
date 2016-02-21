package com.haoxitech.HaoConnect;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wangtao on 15/12/21.
 */
public class HaoConnect {

    private static String Clientinfo = "";
    private static String Clientversion = "0.1";
    private static String SECRET_HAX_CONNECT = "";
    private static String ApiHost = "";

    private static String Devicetype = "3";
    private static String Devicetoken = "";
    private static String Requesttime = "0";

    private static String Userid = "";
    private static String Logintime = "";
    private static String Checkcode = "";

    public static String METHOD_GET = "get";
    public static String METHOD_POST = "post";

    /**
     * 是否开启调试模式
     */
    private static String Isdebug = "0";

    private static Context Ctx;

    /**
     * 请求加密后的校验串，服务器会使用同样规则加密请求后，比较校验串是否一致，从而防止请求内容被纂改。
     * 取头信息里Clientversion,Devicetype,Requesttime,Devicetoken,Userid,Logintime,Checkcode,Clientinfo,Isdebug  和 表单数据
     * 每个都使用key=value（空则空字符串）格式组合成字符串然后放入同一个数组
     * 再放入请求地址（去除http://和末尾/?#之后的字符）后
     * 并放入私钥字符串后自然排序
     * 连接为字符串后进行MD5加密，获得Signature
     * 将Signature也放入头信息，进行传输。
     */
    private static String Signature = "";

    /**
     * 使用前必须调用该初始化方法
     */
    public static void init(Context context) {
        if (Clientinfo == null || Clientinfo.length() == 0) {
            Clientinfo = HaoConfig.HAOCONNECT_CLIENTINFO;
            SECRET_HAX_CONNECT = HaoConfig.HAOCONNECT_SECRET_HAX;
            ApiHost = HaoConfig.HAOCONNECT_APIHOST;
            Ctx = context;
        }
    }

    /**
     * 设置版本号
     */
    public void setClientVersion(String clientVersion) {
        Clientversion = clientVersion;
    }

    /**
     * 设置用户相关信息
     *
     * @param userID    用户id
     * @param loginTime 登录时间
     * @param checkCode
     */
    public static void setCurrentUserInfo(String userID, String loginTime, String checkCode) {
        Userid = userID;
        Logintime = loginTime;
        Checkcode = checkCode;

        putString("userID", Userid);
        putString("loginTime", Logintime);
        putString("checkCode", Checkcode);
    }

    public static String getUserid() {
        return Userid;
    }

    /**
     * 推送token
     *
     * @param deviceToken
     */
    public static void setCurrentDeviceToken(String deviceToken) {
        Devicetoken = deviceToken;
    }

    /**
     * @param requestData
     * @param urlParam
     * @return 请求头数据，里面包括加密字段
     */
    public static Map<String, Object> getSecretHeaders(Map<String, Object> requestData, String urlParam) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Clientinfo", Clientinfo);
        headers.put("Clientversion", Clientversion);
        headers.put("Devicetype", Devicetype);
        headers.put("Requesttime", (System.currentTimeMillis() / 1000) + "");
        headers.put("Devicetoken", Devicetoken);
        headers.put("Isdebug", "0");

        if (Userid == null || Userid.equals("")) {
            Userid = getString("userID");
            Logintime = getString("loginTime");
            Checkcode = getString("checkCode");
        }
        headers.put("Userid", Userid);
        headers.put("Logintime", Logintime);
        headers.put("Checkcode", Checkcode);

        Map<String, Object> signMap = new HashMap<>();
        signMap.putAll(headers);
        if (requestData != null) {
            signMap.putAll(requestData);
        }
        Map<String, Object> linkMap = new HashMap<String, Object>();
        linkMap.put("link", HaoUtility.httpStringFilter("http://" + ApiHost + "/" + urlParam));
        signMap.putAll(linkMap);
        headers.put("Signature", getSignature(signMap));

        return headers;
    }

    /**
     * 加密算法
     */
    private static String getSignature(Map<String, Object> map) {
        List<String> tmpArr = new ArrayList<String>();
        String secret = "";
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String data = entry.getKey() + "=" + entry.getValue();
            tmpArr.add(data);
        }
        tmpArr.add(SECRET_HAX_CONNECT);
        Collections.sort(tmpArr);
        for (String string : tmpArr) {
            secret += string;
        }
        return HaoUtility.encodeMD5String(secret);
    }

    public static RequestHandle loadContent(String urlParam, Map<String, Object> params, String method, TextHttpResponseHandler resonpse, Context context) {

        if (Clientinfo == null || Clientinfo.length() == 0) {
            Toast.makeText(context, "请先初始化HaoConnect,在程序开始的地方调用init()方法", Toast.LENGTH_SHORT).show();
            return null;
        }
        RequestParams requestParams = new RequestParams();
        if (params != null)
        {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                requestParams.put(entry.getKey(), entry.getValue() + "");
            }
        }
        return HaoHttpClient.loadContent("http://" + ApiHost + "/" + urlParam, requestParams, method, getSecretHeaders(params, urlParam), resonpse, context);
    }

    public static RequestHandle loadJson(String urlParam, Map<String, Object> params, String method, JsonHttpResponseHandler response, Context context) {
        return loadContent(urlParam, params, method, response, context);
    }

    public static RequestHandle request(String urlParam, Map<String, Object> params, String method, HaoResultHttpResponseHandler response, Context context) {
        return loadContent(urlParam, params, method, response, context);
    }

    public static void cancelRequest(Context context) {
        HaoHttpClient.cancelRequest(context);
    }

    public static void putString(String key, String value) {
        try {
            //AppContext 这里是Demo里面的Application子类，开发时候需要替换成自己相关的类
            SharedPreferences sharedPreferences = Ctx.getSharedPreferences("config",
                    0);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, value);
            editor.commit();
        } catch (Exception e) {
            Log.e("putStringInfo", e.getMessage());
        }
    }

    public static String getString(String key) {
        try {
            SharedPreferences sharedPreferences = Ctx.getSharedPreferences("config",
                    0);
            return sharedPreferences.getString(key, null);
        } catch (Exception e) {
            return "";
        }
    }


}
