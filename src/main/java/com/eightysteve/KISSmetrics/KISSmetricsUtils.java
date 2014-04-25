/*
 *
 * Copyright 2014 Greencopper
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.eightysteve.KISSmetrics;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class KISSmetricsUtils {

    private static final String URL_AND = "&";
    private static final String URL_EQUAL = "=";

    public static Bundle convertHashMapToBundle(HashMap<String, String> properties) {
        if (properties == null) {
            return null;
        }
        Bundle result = new Bundle();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            result.putString(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static String processKissMetricsQuery(String query, StringBuilder args) {
        if (args.length() > 0) {
            args.insert(0, URL_AND);
        }
        args.insert(0, query);
        return args.toString();
    }

    public static void appendBundleAsUrlArgs(StringBuilder builder, Bundle properties) {
        if (builder == null) {
            return;
        }
        Object[] keySetIds = properties.keySet().toArray();
        for (int i = 0; i < keySetIds.length; i++) {
            String key = (String) keySetIds[i];
            if (properties.get(key) == null) {
                continue;
            }
            appendPropertyAsUrlArgs(builder, key, properties.get(key).toString());
            if (i < keySetIds.length - 1)
                builder.append(URL_AND);
        }
    }

    public static void appendPropertyAsUrlArgs(StringBuilder builder, String key, String value) {
        if (builder != null && !TextUtils.isEmpty(key)) {
            builder.append(key).append(URL_EQUAL).append(Uri.encode(value));
        }
    }

    public static boolean isNetworkAvailable(Context context) {

        final ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
