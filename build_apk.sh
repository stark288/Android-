#!/data/data/com.termux/files/usr/bin/bash
echo "[*] Building APK (no zipalign)"

rm -rf build/ output/
mkdir -p build/classes output/

# Package resources
aapt package -f -M app/src/main/AndroidManifest.xml \
  -I $PREFIX/lib/android-sdk/platforms/android-34/android.jar \
  -S app/src/main/res \
  -F build/unsigned.apk \
  app/src/main/java/

# Compile (use ecj or javac)
javac -d build/classes \
  -cp $PREFIX/lib/android-sdk/platforms/android-34/android.jar \
  app/src/main/java/com/anonymity/toolkit/*.java 2>/dev/null || \
ecj -d build/classes \
  -cp $PREFIX/lib/android-sdk/platforms/android-34/android.jar \
  app/src/main/java/com/anonymity/toolkit/*.java

# Convert to DEX
dx --dex --output=build/classes.dex build/classes/

# Add DEX to APK
aapt add build/unsigned.apk build/classes.dex

# Sign directly (no align)
apksigner sign --ks keystore/anonymity.keystore \
  --ks-pass pass:anon2024 \
  --key-pass pass:anon2024 \
  --out output/AnonymityToolkit.apk \
  build/unsigned.apk

echo "[+] APK: output/AnonymityToolkit.apk"
ls -lh output/
