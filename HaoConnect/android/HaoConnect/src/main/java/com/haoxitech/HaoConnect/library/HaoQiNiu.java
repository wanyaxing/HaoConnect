package com.haoxitech.HaoConnect.library;

import android.content.Context;

import com.haoxitech.HaoConnect.HaoResultHttpResponseHandler;
import com.haoxitech.HaoConnect.HaoResult;
import com.haoxitech.HaoConnect.HaoUtility;
import com.haoxitech.HaoConnect.connects.QiniuConnect;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

/**
 * Created by wangtao on 16/1/21.
 */
public class HaoQiNiu {

    private static AsyncHttpClient client = new AsyncHttpClient();

    private static final Map<String, RequestHandle> requestHandleMap = new HashMap<>();

    public static void cancel(RequestHandle handle)
    {
        if (requestHandleMap.containsKey(handle.getTag()))
        {
            RequestHandle qiniuHandle = requestHandleMap.get(handle.getTag());
            qiniuHandle.cancel(true);
            requestHandleMap.remove(handle.getTag());
        }

        handle.cancel(true);
    }

    public static void cancel(List<RequestHandle> handles)
    {
        for (RequestHandle handle : handles)
        {
            cancel(handle);
        }
    }

    public static RequestHandle requestGetUploadTokenForQiniu(Map<String, Object> params, final JsonHttpResponseHandler response, Context context) {
        return QiniuConnect.requestGetUploadTokenForQiniu(params, new HaoResultHttpResponseHandler() {
            @Override
            public void onSuccess(HaoResult result) {

                if (result.isResultsOK()) {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("urlPreview", result.findAsString("urlPreview"));
                        jsonObject.put("isFileExistInQiniu", result.findAsString("isFileExistInQiniu"));
                        jsonObject.put("uploadToken", result.findAsString("uploadToken"));
                        response.onSuccess(200, null, jsonObject);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        response.onFailure(200, null, e.toString(), null);
                    }
                } else {
                    response.onFailure(200, null, result.errorStr, null);
                }
            }

            @Override
            public void onStart() {
                response.onStart();
            }

            @Override
            public void onFail(HaoResult result) {
                response.onFailure(200, null, result.errorStr, null);
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                super.onProgress(bytesWritten, totalSize);

            }

        }, context);
    }

    public static RequestHandle requestGetUploadTokenForQiniuWithFile(final File file, final JsonHttpResponseHandler response, final Context context) {
        String fileType = "tmp";
        if (file.getName().contains(".")) {
            String name = file.getName();
            String[] fileTemp = name.split("\\.");
            fileType = fileTemp[fileTemp.length - 1];
        }

        Map<String, Object> params = new HashMap<>();
        params.put("md5", HaoUtility.getFileMD5(file));
        params.put("filesize", file.length());
        params.put("filetype", fileType);
        return requestGetUploadTokenForQiniu(params, response, context);
    }

    public static RequestHandle requestUploadToQiNiuWithFile(final File file, final JsonHttpResponseHandler response, final Context context) {
        final Map<String, Long> processMap = new HashMap<>();
        processMap.put("totalSizeOfUpload", 0L);

        final long fileLength = file.length();

        final String fileTag = HaoUtility.getFileMD5(file) + System.currentTimeMillis() / 1000 + "";

        RequestHandle handle = requestGetUploadTokenForQiniuWithFile(file, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject resultResponse) {
                super.onSuccess(statusCode, headers, resultResponse);
                try {
                    if (Boolean.parseBoolean(resultResponse.get("isFileExistInQiniu") + "")) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("urlPreview", resultResponse.get("urlPreview"));
                        response.onSuccess(statusCode, headers, jsonObject);
                        response.onProgress(fileLength, fileLength);
                    } else {
                        try {
                            RequestParams qiniuParams = new RequestParams();
                            qiniuParams.put("file", file);
                            qiniuParams.put("token", resultResponse.get("uploadToken").toString());

                            final RequestHandle handle = client.post(context, "http://upload.qiniu.com", qiniuParams, new JsonHttpResponseHandler() {

                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject resultResponse) {
                                    super.onSuccess(statusCode, headers, resultResponse);
                                    response.onProgress(fileLength, fileLength);

                                    checkRequestHandles();
                                    try {
                                        JSONObject jsonObject = new JSONObject();
                                        jsonObject.put("urlPreview", resultResponse.get("urlPreview"));
                                        response.onSuccess(statusCode, headers, jsonObject);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        response.onFailure(200, null, e.toString(), null);
                                    }
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                                    super.onFailure(statusCode, headers, responseString, throwable);
                                    response.onProgress(fileLength, fileLength);
                                    response.onFailure(statusCode, headers, responseString, throwable);

                                    checkRequestHandles();
                                }

                                public void checkRequestHandles() {
                                    if (requestHandleMap.containsKey(fileTag)) {
                                        requestHandleMap.remove(fileTag);
                                    }
                                }

                                @Override
                                public void onProgress(long bytesWritten, long totalSize) {
                                    super.onProgress(bytesWritten, totalSize);
                                    if (processMap.get("totalSizeOfUpload") == 0) {
                                        processMap.put("totalSizeOfUpload", totalSize);
                                    }

                                    if (totalSize == processMap.get("totalSizeOfUpload")) {
                                        response.onProgress((long) (0.2 * fileLength + 0.78 * bytesWritten / totalSize * fileLength - 1), fileLength);
                                    } else {
                                        response.onProgress((long) (0.98 * fileLength + 0.02 * bytesWritten / totalSize * fileLength - 1), fileLength);
                                    }
                                }

                                @Override
                                public void onStart() {
                                    super.onStart();
                                }

                                @Override
                                public void onCancel() {
                                    super.onCancel();
                                    HaoUtility.print("qiniu oncancel");
                                    response.onCancel();
                                }
                            });
                            requestHandleMap.put(fileTag, handle);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    response.onFailure(200, null, e.toString(), null);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                response.onFailure(statusCode, headers, responseString, throwable);
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                super.onProgress(bytesWritten, totalSize);
                response.onProgress((long) (0.2 * bytesWritten / totalSize * fileLength), fileLength);
            }

            @Override
            public void onStart() {
                super.onStart();
                response.onStart();
            }

            @Override
            public void onCancel() {
                super.onCancel();
                HaoUtility.print("onCancel");
                response.onCancel();
            }
        }, context);

        handle.setTag(fileTag);
        return handle;
    }

    public interface QiNiuRequestResponseHandler {
        void onStart();

        void onStartSingle(int index, File file);

        void onProgressTotal(float process);

        void onProcessSingle(long bytesWritten, long totalSize, int index, File file);

        void onComplete(List<Object> results);

        void onFailSingle(String result, int index, File file);

        void onFailTotal(List<String> results);

        void onSuccessSingle(JSONObject result, int index, File file);

        void onSuccessTotal(List<JSONObject> results);
    }

    public static List<RequestHandle> requestUploadToQiNiuWithFiles(final List<File> files, final QiNiuRequestResponseHandler response, final Context context) {

        final Map<String, Object> resultsMap = new HashMap<>();
        final List<String> flagList = new ArrayList<>();
        List<RequestHandle> requestList = new ArrayList<>();
        Map<String, File> fileMap = new HashMap<>();
        final Map<String, List<Integer>> indexMap = new HashMap<>();

        final Map<String, Float> processMap = new HashMap<>();

        for (int i = 0; i < files.size(); i++) {
            String key = HaoUtility.getFileMD5(files.get(i));

            flagList.add(key);
            fileMap.put(key, files.get(i));
            processMap.put(key, 0f);
            ArrayList<Integer> indexList = (ArrayList<Integer>) indexMap.get(key);
            if (indexList == null) {
                indexList = new ArrayList<>();
            }
            indexList.add(i);
            indexMap.put(key, indexList);
        }

        response.onStart();

        for (final String key : fileMap.keySet()) {
            final File file = fileMap.get(key);

            RequestHandle requestHandle = requestUploadToQiNiuWithFile(file, new JsonHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject resultResponse) {
                    super.onSuccess(statusCode, headers, resultResponse);

                    ArrayList<Integer> indexList = (ArrayList<Integer>) indexMap.get(key);
                    for (int j = 0; j < indexList.size(); j++) {
                        int i = indexList.get(j);
                        response.onSuccessSingle(resultResponse, i, files.get(i));
                    }

                    resultsMap.put(key, resultResponse);
                    checkIfComplete();
                }

                public void checkIfComplete() {
                    if (resultsMap.keySet().size() == processMap.keySet().size()) {
                        List<JSONObject> tempSuccessList = new ArrayList<JSONObject>();
                        List<String> tempFailList = new ArrayList<String>();
                        List<Object> tempCompleteList = new ArrayList<Object>();

                        ArrayList<Integer> indexList = (ArrayList<Integer>) indexMap.get(key);
                        for (int j = 0; j < indexList.size(); j++) {
                            int i = indexList.get(j);
                            Object resultTemp = resultsMap.get(flagList.get(i));
                            if (resultTemp instanceof JSONObject) {
                                tempSuccessList.add((JSONObject) resultTemp);
                            } else {
                                tempFailList.add((String) resultTemp);
                            }
                            tempCompleteList.add(resultTemp);
                        }

                        if (tempSuccessList.size() > 0) {
                            response.onSuccessTotal(tempSuccessList);
                        }

                        if (tempFailList.size() > 0) {
                            response.onFailTotal(tempFailList);
                        }

                        if (tempCompleteList.size() > 0) {
                            response.onComplete(tempCompleteList);
                        }
                    }
                }

                @Override
                public void onStart() {
                    super.onStart();

                    ArrayList<Integer> indexList = (ArrayList<Integer>) indexMap.get(key);
                    for (int j = 0; j < indexList.size(); j++) {
                        int i = indexList.get(j);
                        response.onStartSingle(i, files.get(i));
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);

                    ArrayList<Integer> indexList = (ArrayList<Integer>) indexMap.get(key);
                    for (int j = 0; j < indexList.size(); j++) {
                        int i = indexList.get(j);
                        response.onFailSingle(responseString, i, files.get(i));
                    }

                    processMap.put(key, 1f);
                    avgTheProgress();

                    resultsMap.put(key, responseString);
                    checkIfComplete();
                }

                @Override
                public void onProgress(long bytesWritten, long totalSize) {
                    super.onProgress(bytesWritten, totalSize);
                    ArrayList<Integer> indexList = (ArrayList<Integer>) indexMap.get(key);
                    for (int j = 0; j < indexList.size(); j++) {
                        int i = indexList.get(j);
                        response.onProcessSingle(bytesWritten, totalSize, i, files.get(i));
                    }

                    processMap.put(key, 1f * bytesWritten / totalSize);
                    avgTheProgress();
                }

                public void avgTheProgress() {
                    float processTemp = 0;
                    for (Float num : processMap.values()) {
                        processTemp += num;
                    }
                    response.onProgressTotal(processTemp / processMap.values().size());
                }
            }, context);
            requestList.add(requestHandle);
        }

        return requestList;
    }
}
