// Copyright (c) 2013 StrongLoop. All rights reserved.

package com.strongloop.android.remoting.adapters;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

import com.google.common.collect.ImmutableMap;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.strongloop.android.remoting.JsonUtil;

/**
 * A specific {@link Adapter} implementation for RESTful servers.
 *
 * In addition to implementing the {@link Adapter} interface,
 * <code>RestAdapter</code> contains a single {@link RestContract} to map
 * remote methods to custom HTTP routes. This is only required if the HTTP
 * settings have been customized on the server. When in doubt, try without.
 *
 * @see RestContract
 */
public class RestAdapter extends Adapter {

    private HttpClient client;
    private RestContract contract;
    private Object accessToken;

    public RestAdapter(Context context, String url) {
        super(context, url);
        this.contract = new RestContract();
    }

    /**
     * Gets this adapter's {@link RestContract}, a custom contract for
     * fine-grained route configuration.
     * @return the contract.
     */
    public RestContract getContract() {
        return contract;
    }

    /**
     * Sets this adapter's {@link RestContract}, a custom contract for
     * fine-grained route configuration.
     * @param contract The contract.
     */
    public void setContract(RestContract contract) {
        this.contract = contract;
    }

    public void setAccessToken(Object accessToken) {
    	this.accessToken = accessToken;
    }
    
    public Object getAccessToken() {
    	return accessToken;
    }

    @Override
    public void connect(Context context, String url) {
        if (url == null) {
            client = null;
        }
        else {
            client = new HttpClient(context, url);
            client.addHeader("Accept", "application/json");
        }
    }

    @Override
    public boolean isConnected() {
        return client != null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the contract is not set
     * (see {@link #setContract(RestContract)})
     * or the adapter is not connected.
     */
    @Override
    public void invokeStaticMethod(String method,
            Map<String, ? extends Object> parameters,
            Callback callback) {
        if (contract == null) {
            throw new IllegalStateException("Invalid contract");
        }

        String verb = contract.getVerbForMethod(method);
        String path = contract.getUrlForMethod(method, parameters);

        request(path, verb, parameters, callback);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the contract is not set
     * (see {@link #setContract(RestContract)})
     * or the adapter is not connected.
     */
    @Override
    public void invokeInstanceMethod(String method,
            Map<String, ? extends Object> constructorParameters,
            Map<String, ? extends Object> parameters,
            Callback callback) {
        if (contract == null) {
            throw new IllegalStateException("Invalid contract");
        }

        Map<String, Object> combinedParameters = new HashMap<String, Object>();
        if (constructorParameters != null) {
            combinedParameters.putAll(constructorParameters);
        }
        if (parameters != null) {
            combinedParameters.putAll(parameters);
        }

        String verb = contract.getVerbForMethod(method);
        String path = contract.getUrlForMethod(method, combinedParameters);

        request(path, verb, combinedParameters, callback);
    }

    private void request(String path, String verb,
    		Map<String, ? extends Object> parameters, Callback callback) {
        if (!isConnected()) {
            throw new IllegalStateException("Adapter not connected");
        }

        Map<String, Object> combinedParameters = new HashMap<String, Object>();
        if ( parameters != null )
        	combinedParameters.putAll(parameters);
        if ( accessToken != null )  
        	combinedParameters.put("access_token", accessToken);
        
        client.request(verb, path, combinedParameters,
        		HttpClient.ParameterEncoding.JSON, callback);
    }

    //
    // Mimic AFNetworking as much as possible.
    //
    // Internally, it's using "Android Asynchronous Http Client".
    // http://loopj.com/android-async-http/
    // The benefit is connection pools, persistent cookies,
    // an asynchronous API, Android bug workarounds, etc.
    // The drawback is it doesn't support HEAD or OPTION.
    //

    private static final boolean LOG = false;

    private static class HttpClient extends AsyncHttpClient {

        enum ParameterEncoding {
            FORM_URL,
            JSON
        }

        private static String getVersionName(Context context) {
            String appVersion = null;
            try {
                PackageInfo pinfo = context.getPackageManager().getPackageInfo(
                		context.getPackageName(), 0);
                appVersion = pinfo.versionName;
            }
            catch (NameNotFoundException e) {
                // Do nothing
            }
            return (appVersion != null) ? appVersion : "";
        }

        private static String getDeviceName() {
            String deviceName = android.os.Build.MODEL;
            if (deviceName == null || deviceName.length() == 0) {
                deviceName = android.os.Build.DEVICE;
                if (deviceName == null || deviceName.length() == 0) {
                    deviceName = "Unknown";
                }
            }
            return deviceName;
        }

        private Context context;
        private String baseUrl;

        public HttpClient(Context context, String baseUrl) {
            if (baseUrl == null) {
                throw new IllegalArgumentException(
                		"The baseUrl cannot be null");
            }

            this.context = context;
            this.baseUrl = baseUrl;

            // Make sure base url ends with a trailing slash.
            if (!this.baseUrl.endsWith("/")) {
                this.baseUrl += "/";
            }

            // More useful User-Agent, similar to AFNetworing.
            String appName;
            if (context != null) {
                String appPackageName = context.getPackageName();
                String appVersion = getVersionName(context);
                appName = appPackageName + "/" + appVersion;
            }
            else {
                appName = "StongLoopRemoting App";
            }
            String deviceName = getDeviceName();
            String androidVersion = android.os.Build.VERSION.RELEASE +
            		"/API-" + android.os.Build.VERSION.SDK_INT;
            String userAgent = appName + " (" + deviceName +
            		" Android " + androidVersion + ")";
            setUserAgent(userAgent);
        }

        public void request(String method, String path,
                Map<String, ? extends Object> parameters,
                ParameterEncoding parameterEncoding,
                final Callback callback) {
            Uri.Builder uri = Uri.parse(baseUrl).buildUpon();
            if (path != null) {
                if (path.startsWith("/")) {
                    uri.appendEncodedPath(path.substring(1));
                }
                else {
                    uri.appendEncodedPath(path);
                }
            }
            String contentType = null;
            HttpEntity body = null;
            String charset = "utf-8";
            AsyncHttpResponseHandler httpCallback =
            		new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(String response) {
                    if (LOG) {
                        Log.i("RestAdapter", "Success: " + response);
                    }
                    try {
                        callback.onSuccess(response);
                    } catch (Throwable t) {
                        callback.onError(t);
                    }
                }

                @Override
                public void onFailure(Throwable e, String response) {
                    if (LOG) {
                        Log.i("RestAdapter", "Error: " + response);
                    }
                    callback.onError(e);
                }
            };

            if (parameters != null) {
                if ("GET".equalsIgnoreCase(method) ||
                		"HEAD".equalsIgnoreCase(method) ||
                		"DELETE".equalsIgnoreCase(method)) {

                    for (Map.Entry<String, ? extends Object> entry :
                            buildUrlQueryParameters(parameters).entrySet()) {
                        uri.appendQueryParameter(entry.getKey(),
                        		String.valueOf(entry.getValue()));
                    }
                }
                else if (parameterEncoding == ParameterEncoding.FORM_URL) {
                	// NOTE: Code for "x-www-form-urlencoded" is not used
                	// and is untested.
                    contentType =
                    		"application/x-www-form-urlencoded; charset=" +
                    		charset;

                    List<NameValuePair> nameValuePairs =
                    		new ArrayList<NameValuePair>();
                    for (Map.Entry<String, ? extends Object> entry :
                    	parameters.entrySet()) {
                        nameValuePairs.add(
                        		new BasicNameValuePair(entry.getKey(),
                        				String.valueOf(entry.getValue())));
                    }
                    try {
                        body = new UrlEncodedFormEntity(nameValuePairs,
                        		charset);
                    }
                    catch (UnsupportedEncodingException e) {
                        // Won't happen
                        Log.e("RestAdapter", "Couldn't encode url params", e);
                    }
                }
                else if (parameterEncoding == ParameterEncoding.JSON) {
                    contentType = "application/json; charset=" + charset;
                    String s = "";
                    try {
                        s = String.valueOf(JsonUtil.toJson(parameters));
                    }
                    catch (JSONException e) {
                        Log.e("RestAdapter",
                        		"Couldn't convert parameters to JSON", e);
                    }
                    try {
                        body = new StringEntity(s, charset);
                    }
                    catch (UnsupportedEncodingException e) {
                        // Won't happen
                        Log.e("RestAdapter", "Couldn't encode JSON params", e);
                    }
                }
            }

            Header[] headers = {
                    new BasicHeader("Accept", "application/json"),
            };

            String url = uri.build().toString();
            if (LOG) {
                Log.i("RestAdapter", method + " " + url);
            }
            if ("GET".equalsIgnoreCase(method)) {
                get(context, url, headers, null, httpCallback);
            }
            else if ("DELETE".equalsIgnoreCase(method)) {
                delete(context, url, headers, httpCallback);
            }
            else if ("POST".equalsIgnoreCase(method)) {
                post(context, url, headers, body, contentType, httpCallback);
            }
            else if ("PUT".equalsIgnoreCase(method)) {
                put(context, url, headers, body, contentType, httpCallback);
            }
            else {
                throw new IllegalArgumentException("Illegal method: " +
                		method + ". Only GET, POST, PUT, DELETE supported.");
            }
        }

        private Map<String, Object> buildUrlQueryParameters(
                final Map<String, ? extends Object> parameters) {
            return buildUrlQueryParameters(null, parameters);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> buildUrlQueryParameters(
                final String keyPrefix,
                final Map<String, ? extends Object> parameters) {

            // This method converts nested maps into a flat list
            //   Input:  { "here": { "lat": 10, "lng": 20 }
            //   Output: { "here[lat]": 10, "here[lng]": 20 }

            Map<String, Object> result = new HashMap<String, Object>();

            for (Map.Entry<String, ? extends Object> entry
                    : parameters.entrySet()) {

                String key = keyPrefix != null
                        ? keyPrefix + "[" + entry.getKey() + "]"
                        : entry.getKey();

                Object value = entry.getValue();

                if (value instanceof Map) {
                    result.putAll(buildUrlQueryParameters(key, (Map)value));
                } else {
                    result.put(key, value);
                }
            }

            return result;
        }
    }
}
