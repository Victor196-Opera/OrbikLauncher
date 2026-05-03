#!/bin/bash

ANDROID_HOME=$HOME/android-sdk
ANDROID_JAR=$ANDROID_HOME/platforms/android-34/android.jar
SRC_DIR=app/src/main
BUILD_DIR=build
KEYSTORE=/storage/emulated/0/orbik.keystore
PASS="OrbikCell_Im-A-Cell"
OUTPUT=/storage/emulated/0/APP_AS/OrbikLauncher-debug.apk

echo "🔨 BUILD ORBIK LAUNCHER"
echo "========================"

rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR/classes
mkdir -p /storage/emulated/0/APP_AS

echo "⚙️  Compile Java..."
javac -source 1.8 -target 1.8 \
    -d $BUILD_DIR/classes/ \
    -cp $ANDROID_JAR \
    -bootclasspath $ANDROID_JAR \
    $SRC_DIR/java/com/orbik/launcher/*.java

if [ $? -ne 0 ]; then
    echo "❌ Compile thất bại"
    exit 1
fi

echo "📦 Tạo DEX..."
dx --dex --output=$BUILD_DIR/classes.dex $BUILD_DIR/classes/

if [ $? -ne 0 ]; then
    echo "❌ DEX thất bại"
    exit 1
fi

echo "📱 Đóng gói APK..."
aapt package -f \
    -M $SRC_DIR/AndroidManifest.xml \
    -S $SRC_DIR/res/ \
    -I $ANDROID_JAR \
    -F $BUILD_DIR/app-unsigned.apk

# Chỉ thêm classes.dex, icon và wallpaper đã được aapt thêm tự động
cd $BUILD_DIR
aapt add app-unsigned.apk classes.dex
cd ..

echo "🔐 Ký APK..."
apksigner sign \
    --ks $KEYSTORE \
    --ks-pass pass:"$PASS" \
    --key-pass pass:"$PASS" \
    --min-sdk-version 21 \
    --out "$OUTPUT" \
    $BUILD_DIR/app-unsigned.apk

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ BUILD THÀNH CÔNG!"
    echo "📱 APK: $OUTPUT"
    echo "📦 Size: $(du -h "$OUTPUT" | cut -f1)"
else
    echo "❌ Ký thất bại"
    exit 1
fi
