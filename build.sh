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
mkdir -p $BUILD_DIR/{classes,gen}
mkdir -p $(dirname $OUTPUT)

# 1. Tạo R.java
echo "📝 [1/4] Tạo R.java..."
aapt package -f -m \
    -J $BUILD_DIR/gen/ \
    -M $SRC_DIR/AndroidManifest.xml \
    -S $SRC_DIR/res/ \
    -I $ANDROID_JAR || exit 1

# 2. Compile Java
echo "⚙️  [2/4] Compile Java..."
javac -source 1.8 -target 1.8 \
    -d $BUILD_DIR/classes/ \
    -cp $ANDROID_JAR \
    $SRC_DIR/java/com/orbik/launcher/*.java \
    $BUILD_DIR/gen/com/orbik/launcher/R.java || exit 1

# 3. Tạo DEX
echo "📦 [3/4] Tạo DEX..."
dx --dex --output=$BUILD_DIR/classes.dex $BUILD_DIR/classes/ || exit 1

# 4. Đóng gói & Ký APK
echo "📱 [4/4] Đóng gói & Ký..."

aapt package -f \
    -M $SRC_DIR/AndroidManifest.xml \
    -S $SRC_DIR/res/ \
    -I $ANDROID_JAR \
    -F $BUILD_DIR/app-unsigned.apk

cd $BUILD_DIR
aapt add app-unsigned.apk classes.dex
cd ..

apksigner sign \
    --ks $KEYSTORE \
    --ks-pass pass:"$PASS" \
    --key-pass pass:"$PASS" \
    --min-sdk-version 21 \
    --out "$OUTPUT" \
    $BUILD_DIR/app-unsigned.apk

if [ -f "$OUTPUT" ]; then
    echo "✅ BUILD THÀNH CÔNG!"
    echo "📱 $OUTPUT"
else
    echo "❌ Thất bại"
    exit 1
fi
