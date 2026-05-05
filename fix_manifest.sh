#!/bin/bash

APK=/storage/emulated/0/APP_AS/OrbikLauncher-debug.apk
TMP_DIR=/data/data/com.termux/files/home/tmp_apk
PASS="OrbikCell_Im-A-Cell"
KEYSTORE=/storage/emulated/0/orbik.keystore
APK_TMP=/storage/emulated/0/APP_AS/OrbikLauncher-tmp.apk

echo "🔧 FIX MANIFEST & SIGN"
echo "======================"

if [ ! -f "$APK" ]; then
    echo "❌ Không tìm thấy $APK"
    exit 1
fi

rm -rf $TMP_DIR
mkdir -p $TMP_DIR

# Giải nén APK
echo "📦 Giải nén APK..."
cd $TMP_DIR
unzip -o "$APK" > /dev/null 2>&1

# Tạo Manifest đầy đủ
echo "📝 Tạo Manifest mới..."
cat > AndroidManifest.xml << 'MANIFEST'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.orbik.launcher"
    android:versionCode="1000"
    android:versionName="10.0 Cinnamon Bun">
    <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="36" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
    <application android:label="Orbik Launcher" android:icon="@mipmap/ic_launcher">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <receiver android:name=".PackageChangeReceiver" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.PACKAGE_ADDED" />
                <action android:name="android.intent.action.PACKAGE_REMOVED" />
                <action android:name="android.intent.action.PACKAGE_REPLACED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
MANIFEST

# Đóng gói lại (tạo file tạm)
echo "📦 Đóng gói APK mới..."
if command -v zip &> /dev/null; then
    zip -r "$APK_TMP" . > /dev/null 2>&1
else
    jar cf "$APK_TMP" .
fi

# Kiểm tra file tạm có được tạo không
if [ ! -f "$APK_TMP" ]; then
    echo "❌ Không thể tạo APK mới"
    rm -rf $TMP_DIR
    exit 1
fi

# Thay thế APK cũ
mv "$APK_TMP" "$APK"
rm -rf $TMP_DIR

# Ký lại APK
echo "🔐 Ký APK..."
apksigner sign \
    --ks $KEYSTORE \
    --ks-pass pass:"$PASS" \
    --key-pass pass:"$PASS" \
    --min-sdk-version 21 \
    "$APK"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ HOÀN TẤT!"
    echo "📱 $APK"
else
    echo "❌ Ký thất bại"
    exit 1
fi
