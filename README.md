# KISSmetrics-4-Android -- KISSmetrics For Android

This version of the library is based on the original project:

KISSmetrics-4-Android is an Android Library which helps interacting with the KISSmetrics Analytics Service.
http://80steve.com/posts/2012/02/01/kissmetrics-library-for-android/

## What has been changed in this version:

- Change URL in order to align with iOS SDK : timestamp is used with _d=1
- Fix issue with URL encoding of parameters (if the property value contains a & for example)
- Utils to format URL, and usage of StringBuilder to optimise the code
- Ability to set a Bundle of property at initialisation
- In airplane mode the data were removed. Fixed that by checking network availability before trying to send
- Add a Broadcastreceiver in order to send the data when the network is back (need to be registered in the manifest)
- Use Android Bundle instead of HashMap<String, String> - deprecated methods
- Logs only in Debug

## TODO:

- Defragment network traffic
- Use Android Service

## Setup

mvn install or mvn package

## Usage

```java
KISSmetricsAPI kiss = KISSmetricsAPI.sharedAPI(<API KEY>, <Application Context>);

// Track Event
kiss.recordEvent("Activated", null);

// Track Event with Parameters
HashMap<String, String> properties = new HashMap<String, String>();
properties.put("Item", "Potion");
properties.put("Amount", "10");
kiss.recordEvent("Purchase", properties);

```

```xml
		<!-- KISS Metrics receiver -->
        <receiver android:name="com.eightysteve.KISSmetrics.ConnectivityReceiver"  >
            <intent-filter>
                <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
            </intent-filter>
        </receiver>
```

## License

Copyright 2012 Steve Chan, http://80steve.com
Copyright 2014 Greencopper

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
