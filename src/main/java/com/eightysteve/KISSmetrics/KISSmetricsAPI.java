/*
 * 
 * Copyright 2012 Steve Chan, http://80steve.com
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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class KISSmetricsAPI implements KISSmetricsURLConnectionCallbackInterface {

    private static final String LOG_TAG = KISSmetricsAPI.class.getSimpleName();

    public static final String BASE_URL = "//trk.KISSmetrics.com";

    public static final String EVENT_PATH = "/e";
    public static final String PROP_PATH = "/s";
    public static final String ALIAS_PATH = "/a";

    private static final String URL_FMT = "%s:%s%s?%s";

    private static final String KISS_METRICS_QUERY_PROPERTY_FMT = "_k=%s&_p=%s&_d=1&_t=%d";
    private static final String KISS_METRICS_QUERY_IDENTITY_FMT = "_k=%s&_p=%s&_n=%s";
    private static final String KISS_METRICS_QUERY_EVENT_FMT = "_k=%s&_p=%s&_d=1&_t=%d&_n=%s";

    public static final String ACTION_FILE = "KISSmetricsAction";
    public static final String IDENTITY_PREF = "KISSmetricsIdentityPreferences";

    public static final String HTTP = "http";
    public static final String HTTPS = "https";

    private static final String KEY_SYSTEM_VERSION = "systemVersion";
    private static final String KEY_SYSTEM_NAME = "systemName";
    private static final String KEY_IDENTITY = "identity";
    private static final String VALUE_SYSTEM_NAME = "android";

    private String mApiKey;
    private String mIdentity;
    private List<String> mSendQueue;
    private Context mContext;
    private String mCurrentScheme;
    private static boolean sIsDebug = false;

    private static KISSmetricsAPI sSharedAPI = null;

    private KISSmetricsAPI(String apiKey, Context context, Bundle initialPropsToSend, boolean secure) {
        this.mApiKey = apiKey;
        this.mContext = context;
        if (this.mContext == null) return;
        this.mCurrentScheme = (secure) ? HTTPS : HTTP;
        SharedPreferences pref = this.mContext.getSharedPreferences(IDENTITY_PREF, Activity.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = null;
        this.mIdentity = pref.getString(KEY_IDENTITY, null);
        if (this.mIdentity == null) {
            this.clearIdentity();
        }

        boolean shouldSendProps = true;
        final Bundle propsToSend = new Bundle();
        for (Map.Entry entry : pref.getAll().entrySet()) {
            if (entry.getValue() != null) {
                propsToSend.putString((String) entry.getKey(), entry.getValue().toString());
            }
        }
        if (!propsToSend.isEmpty()) {
            shouldSendProps = false;
            if (!propsToSend.containsKey(KEY_SYSTEM_VERSION) || !android.os.Build.VERSION.RELEASE.equals(propsToSend.get(KEY_SYSTEM_VERSION))) {
                shouldSendProps = true;
            }
        }

        if (shouldSendProps) {
            propsToSend.clear();
            propsToSend.putString(KEY_SYSTEM_NAME, VALUE_SYSTEM_NAME);
            propsToSend.putString(KEY_SYSTEM_VERSION, android.os.Build.VERSION.RELEASE);
            prefEditor = pref.edit();
            for (String s : propsToSend.keySet()) {
                Object value = propsToSend.get(s);
                if (value != null) {
                    prefEditor.putString(s, value.toString());
                }
            }
            prefEditor.commit();
            propsToSend.putAll(initialPropsToSend);
        } else {
            propsToSend.clear();
        }

        this.unarchiveData();
        if (propsToSend.size() > 0) { // avoid log
            this.setProperties(propsToSend);
        }
    }

    public static synchronized KISSmetricsAPI sharedAPI(String apiKey, Context context) {
        if (sSharedAPI == null) {
            sSharedAPI = new KISSmetricsAPI(apiKey, context, Bundle.EMPTY, true);
        }
        return sSharedAPI;
    }

    public static synchronized KISSmetricsAPI sharedAPI(String apiKey, Context context, boolean secure) {
        if (sSharedAPI == null) {
            sSharedAPI = new KISSmetricsAPI(apiKey, context, Bundle.EMPTY, secure);
        }
        return sSharedAPI;
    }

    public static synchronized KISSmetricsAPI sharedAPI(String apiKey, Context context, Bundle initialPropsToSend) {
        if (sSharedAPI == null) {
            sSharedAPI = new KISSmetricsAPI(apiKey, context, initialPropsToSend, true);
        }
        return sSharedAPI;
    }

    public static synchronized KISSmetricsAPI sharedAPI(String apiKey, Context context, Bundle initialPropsToSend, boolean secure) {
        if (sSharedAPI == null) {
            sSharedAPI = new KISSmetricsAPI(apiKey, context, initialPropsToSend, secure);
        }
        return sSharedAPI;
    }


    public static synchronized KISSmetricsAPI sharedAPI() {
        if (sSharedAPI == null) {
            if (sIsDebug) {
                Log.w(LOG_TAG, "KISSmetricsAPI has not been initialized, please call the method new KISSmetricsAPI(<API_KEY>)");
            }
        }
        return sSharedAPI;
    }

    public static void setIsDebug(boolean isDebug) {
        sIsDebug = isDebug;
    }

    public void send() {
        synchronized (this) {
            if (this.mSendQueue.size() == 0)
                return;
            if (KISSmetricsUtils.isNetworkAvailable(mContext)) {
                String nextAPICall = this.mSendQueue.get(0);
                KISSmetricsURLConnection connector = KISSmetricsURLConnection.initializeConnector(this);
                connector.connectURL(nextAPICall);
            }
        }
    }

    @Deprecated
    public void recordEvent(String name, HashMap<String, String> properties) {
        recordEvent(name, KISSmetricsUtils.convertHashMapToBundle(properties));
    }

    public void recordEvent(String name, Bundle properties) {
        if (name == null || name.length() == 0) {
            if (sIsDebug) {
                Log.w(LOG_TAG, "Name cannot be null");
            }
            return;
        }

        long timeOfEvent = System.currentTimeMillis() / 1000;
        String query = String.format(KISS_METRICS_QUERY_EVENT_FMT, this.mApiKey, this.mIdentity, timeOfEvent, name);

        StringBuilder builder = new StringBuilder();
        if (properties != null) {
            KISSmetricsUtils.appendBundleAsUrlArgs(builder, properties);
        }

        final String theURL = String.format(Locale.US, URL_FMT, this.mCurrentScheme, BASE_URL, EVENT_PATH, KISSmetricsUtils.processKissMetricsQuery(query, builder));

        synchronized (this) {
            addUrlToQueue(theURL);
            this.archiveData();
        }
        this.send();
    }

    public void alias(String firstIdentity, String secondIdentity) {
        if (firstIdentity == null || firstIdentity.length() == 0 || secondIdentity == null || secondIdentity.length() == 0) {
            if (sIsDebug) {
                Log.w(LOG_TAG, String.format("Attempted to use nil or empty identities in alias (%s and %s). Ignoring.", firstIdentity, secondIdentity));
            }
        }

        String query = String.format(KISS_METRICS_QUERY_IDENTITY_FMT, this.mApiKey, firstIdentity, secondIdentity);
        final String theURL = String.format(Locale.US, URL_FMT, this.mCurrentScheme, BASE_URL, ALIAS_PATH, query);

        synchronized (this) {
            addUrlToQueue(theURL);
            this.archiveData();
        }
        this.send();
    }

    public void identify(String identity) {
        if (identity == null || identity.length() == 0) {
            if (sIsDebug) {
                Log.w(LOG_TAG, "Attempted to use nil or empty identity. Ignoring.");
            }
            return;
        }

        String query = String.format(KISS_METRICS_QUERY_IDENTITY_FMT, this.mApiKey, this.mIdentity, identity);
        final String theURL = String.format(Locale.US, URL_FMT, this.mCurrentScheme, BASE_URL, ALIAS_PATH, query);

        synchronized (this) {
            this.setIdentity(identity);

            addUrlToQueue(theURL);
            this.archiveData();
        }
        this.send();
    }

    public void clearIdentity() {
        String identity = UUID.randomUUID().toString();

        this.setIdentity(identity);
    }

    synchronized private void setIdentity(String identity) {
        this.mIdentity = identity;

        SharedPreferences pref = this.mContext.getSharedPreferences(IDENTITY_PREF, Activity.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = pref.edit();
        prefEditor.putString("identity", this.mIdentity);
        prefEditor.commit();
    }

    @Deprecated
    public void setProperties(HashMap<String, String> properties) {
        setProperties(KISSmetricsUtils.convertHashMapToBundle(properties));
    }

    public void setProperties(Bundle properties) {
        if (properties == null || properties.size() == 0) {
            if (sIsDebug) {
                Log.w(LOG_TAG, "Tried to set properties with no properties in it..");
            }
            return;
        }

        StringBuilder builder = new StringBuilder();
        KISSmetricsUtils.appendBundleAsUrlArgs(builder, properties);

        if (builder.length() == 0) {
            if (sIsDebug) {
                Log.w(LOG_TAG, "No valid properties in setProperties:. Ignoring call");
            }
            return;
        }

        long timeOfEvent = System.currentTimeMillis() / 1000;

        String query = String.format(KISS_METRICS_QUERY_PROPERTY_FMT, this.mApiKey, this.mIdentity, timeOfEvent);
        final String theURL = String.format(Locale.US, URL_FMT, this.mCurrentScheme, BASE_URL, PROP_PATH, KISSmetricsUtils.processKissMetricsQuery(query, builder));

        synchronized (this) {
            addUrlToQueue(theURL);
            this.archiveData();
        }
        this.send();
    }

    public void setProperty(String name, String value) {
        if (name == null || name.length() == 0) {
            if (sIsDebug) {
                Log.w(LOG_TAG, "Name cannot be null");
            }
            return;
        }

        StringBuilder builder = new StringBuilder();
        KISSmetricsUtils.appendPropertyAsUrlArgs(builder, name, value);

        long timeOfEvent = System.currentTimeMillis() / 1000;

        String query = String.format(KISS_METRICS_QUERY_PROPERTY_FMT, this.mApiKey, this.mIdentity, timeOfEvent);
        final String theURL = String.format(Locale.US, URL_FMT, this.mCurrentScheme, BASE_URL, PROP_PATH, KISSmetricsUtils.processKissMetricsQuery(query, builder));

        synchronized (this) {
            addUrlToQueue(theURL);
            this.archiveData();
        }
        this.send();
    }


    private void addUrlToQueue(String url) {
        if (null != url) {
            this.mSendQueue.add(url);
        }
    }

    public void archiveData() {
        try {
            FileOutputStream fos = this.mContext.openFileOutput(ACTION_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this.mSendQueue);
            oos.close();
        } catch (Exception e) {
            if (sIsDebug) {
                Log.w(LOG_TAG, "Unable to archive data");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void unarchiveData() {
        try {
            FileInputStream fis = this.mContext.openFileInput(ACTION_FILE);
            ObjectInputStream ois = new ObjectInputStream(fis);
            this.mSendQueue = (List<String>) ois.readObject();
            ois.close();
            fis.close();
        } catch (Exception e) {
            if (sIsDebug) {
                Log.w(LOG_TAG, "Unable to unarchive data");
            }
        }

        if (this.mSendQueue == null)
            this.mSendQueue = new ArrayList<String>();
        else {
            this.mSendQueue.removeAll(Collections.singleton(null));
            List<String> newSendQueue = new ArrayList<String>();
            for (String url : this.mSendQueue) {
                newSendQueue.add(url.replace("&_d=0", "&_d=1"));
            }
            this.mSendQueue = newSendQueue;
            this.send();
        }
    }

    public void finished(int statusCode) {
        this.send();
    }

    public void setSecure(boolean secure) {
        this.mCurrentScheme = (secure) ? HTTPS : HTTP;
    }

    public List<String> getSendQueue() {
        return mSendQueue;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }
}
