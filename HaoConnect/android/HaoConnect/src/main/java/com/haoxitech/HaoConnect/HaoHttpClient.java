package com.haoxitech.HaoConnect;

import android.content.Context;
import android.os.Environment;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HeaderElement;
import cz.msebera.android.httpclient.ParseException;

/**
 * Created by wangtao on 15/11/25.
 */
public class HaoHttpClient {

    private static AsyncHttpClient client = new AsyncHttpClient();

    private static String tempCacheDictionary = "temp";

    /**
     * @param actionUrl 请求地址
     * @param params    请求参数
     * @param Method    请求类型
     * @param headers   请求头
     * @param response  回调方法
     */

    public static RequestHandle loadContent(String actionUrl, RequestParams params, String Method, Map<String, Object> headers, AsyncHttpResponseHandler response, Context context) {

        RequestHeader[] headersArray = new RequestHeader[headers.entrySet().size()];

        int i = 0;
        for (Map.Entry<String, Object> header : headers.entrySet()) {
            headersArray[i] = new HaoHttpClient().new RequestHeader();
            headersArray[i].setName(header.getKey());
            headersArray[i].setValue(header.getValue().toString());
            i++;
        }

        if (Method == null || Method.equals("get")) {
            return client.get(context, actionUrl, headersArray, params, response);
        } else {
            return client.post(context, actionUrl, headersArray, params, null, response);
        }
    }

    public static RequestHandle loadJson(String actionUrl, RequestParams params, String Method, Map<String, Object> headers, JsonHttpResponseHandler response, Context context) {

        RequestHeader[] headersArray = new RequestHeader[headers.entrySet().size()];

        int i = 0;
        for (Map.Entry<String, Object> header : headers.entrySet()) {
            headersArray[i] = new HaoHttpClient().new RequestHeader();
            headersArray[i].setName(header.getKey());
            headersArray[i].setValue(header.getValue().toString());
            i++;
        }

        if (Method == null || Method.equals("get")) {
            return client.get(context, actionUrl, headersArray, params, response);
        } else {
            return client.post(context, actionUrl, headersArray, params, null, response);
        }
    }

    public static RequestHandle loadFile(final String actionUrl, RequestParams params, final HaoFileHttpResponseHandler response, Context context) {

        return client.get(context, actionUrl, params, new BinaryHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] binaryData) {

                String fileName = "";
                String fileType = "temp";
                for (int i = 0; i < headers.length; i++) {
                    if (headers[i].getName().equalsIgnoreCase("Content-disposition")) {
                        Pattern pattern = Pattern
                                .compile("^.*?filename=\"*(.+?)(\".*|$)");
                        Matcher matcher = pattern.matcher(headers[i].getValue());
                        fileName = matcher.replaceAll("$1").trim();
                    }
                    if (headers[i].getName().equalsIgnoreCase("Content-Type")) {
                        Pattern pattern = Pattern
                                .compile("^.*?/\"*(.+?)(\".*|$)");
                        Matcher matcher = pattern.matcher(headers[i].getValue());
                        fileType = matcher.replaceAll("$1").trim();
                    }

                }

                if (fileName.contains(".")) {
                    String[] fileTemp = fileName.split("\\.");
                    fileType = fileTemp[fileTemp.length - 1];
                    fileName = fileName.replace("." + fileType, "");
                }

                File file = null;

                try {
                    int repeat = 1;
                    while (true) {
                        file = new File(getTempCacheDictionaryPath() + "/" + fileName + (repeat == 1 ? "" : "(" + repeat + ")") + "." + fileType);
                        if (!file.exists()) {
                            break;
                        }
                        repeat++;
                    }
                    file.createNewFile();
                    OutputStream os = new FileOutputStream(file);
                    os.write(binaryData);
                    os.close();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (file != null) {
                    response.onSuccess(file);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable error) {
                response.onFailure(statusCode, headers, binaryData, error);
            }

            @Override
            public void onStart() {
                super.onStart();
                response.onStart();
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                super.onProgress(bytesWritten, totalSize);
                response.onProgress(bytesWritten, totalSize);
            }
        });
    }

    /**
     * 取消请求
     *
     * @param context
     */
    public static void cancelRequest(Context context) {
//        HaoUtility.print("取消请求:" + context);
        client.cancelRequests(context, true);
    }

    /**
     * 设置连接最大数
     *
     * @param maxConnects
     */
    public static void setMaxConnects(int maxConnects) {
        if (maxConnects < 1) {
            maxConnects = 10;
        }
        client.setMaxConnections(maxConnects);
    }

    public static String getTempCacheDictionaryPath() {
        String path = Environment.getExternalStorageDirectory().getPath() + "/" + tempCacheDictionary;
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
        return path;
    }

    public static void setTempCacheDictionary(String dictionaryName) {
        tempCacheDictionary = dictionaryName;
    }

    public interface HaoFileHttpResponseHandler {
        void onSuccess(File file);

        void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable error);

        void onStart();

        void onProgress(long bytesWritten, long totalSize);
    }

    class RequestHeader implements Header {
        private String name;

        public void setValue(String value) {
            this.value = value;
        }

        public void setName(String name) {
            this.name = name;
        }

        private String value;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public HeaderElement[] getElements() throws ParseException {
            return new HeaderElement[0];
        }
    }
}
