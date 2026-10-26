#!/bin/bash

# Script to check obfuscation percentage and memory usage
# Run this after building the release APK

echo "==================================="
echo "Checking Chatile APK Optimization"
echo "==================================="
echo ""

# Check if APK exists
APK_PATH="app/build/outputs/apk/release/app-release.apk"
BUNDLE_PATH="app/build/outputs/bundle/release/app-release.aab"
MAPPING_FILE="app/build/outputs/mapping/release/mapping.txt"
USAGE_FILE="app/build/outputs/mapping/release/usage.txt"

if [ ! -f "$APK_PATH" ]; then
    echo "⚠️  Release APK not found at: $APK_PATH"
    echo "Building release APK..."
    ./gradlew assembleRelease
fi

if [ ! -f "$APK_PATH" ]; then
    echo "❌ Failed to build release APK"
    exit 1
fi

echo "✅ Release APK found: $APK_PATH"
echo ""

# Check APK size
echo "📦 APK Size:"
du -sh "$APK_PATH"
echo ""

# Check if mapping file exists
if [ -f "$MAPPING_FILE" ]; then
    echo "✅ Mapping file exists"
    MAPPING_LINES=$(wc -l < "$MAPPING_FILE")
    echo "   Lines in mapping: $MAPPING_LINES"
else
    echo "⚠️  Mapping file not found at: $MAPPING_FILE"
fi

echo ""

# Check usage file
if [ -f "$USAGE_FILE" ]; then
    echo "✅ Usage file exists"
    USAGE_LINES=$(wc -l < "$USAGE_FILE")
    echo "   Classes/methods kept: $USAGE_LINES"
    
    echo ""
    echo "🔍 Top kept packages:"
    echo "   Google/Firebase:"
    grep -c "com\.google\." "$USAGE_FILE" | xargs echo "   - com.google.*: lines"
    grep -c "com\.google\.firebase" "$USAGE_FILE" | xargs echo "   - Firebase: lines"
    grep -c "com\.google\.android\.gms" "$USAGE_FILE" | xargs echo "   - Play Services: lines"
    
    echo "   AndroidX:"
    grep -c "androidx\." "$USAGE_FILE" | xargs echo "   - androidx.*: lines"
    grep -c "androidx\.compose" "$USAGE_FILE" | xargs echo "   - Compose: lines"
    
    echo "   Kotlin:"
    grep -c "kotlin" "$USAGE_FILE" | xargs echo "   - kotlin: lines"
    
    echo "   Your code:"
    grep -c "com\.chatile" "$USAGE_FILE" | xargs echo "   - com.chatile: lines"
else
    echo "⚠️  Usage file not found at: $USAGE_FILE"
fi

echo ""
echo "==================================="
echo "Analyzing DEX files"
echo "==================================="
echo ""

# Extract APK to temp directory
TEMP_DIR="temp_apk_analysis"
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

# Unzip APK
unzip -q "$APK_PATH" -d "$TEMP_DIR" classes.dex classes2.dex classes3.dex 2>/dev/null || true

# Count DEX files
DEX_COUNT=$(ls -1 "$TEMP_DIR"/classes*.dex 2>/dev/null | wc -l)
echo "📄 Number of DEX files: $DEX_COUNT"

# Calculate total DEX size
TOTAL_DEX_SIZE=$(du -sb "$TEMP_DIR"/classes*.dex 2>/dev/null | awk '{sum+=$1} END {print sum}')
if [ -n "$TOTAL_DEX_SIZE" ]; then
    echo "💾 Total DEX size: $(numfmt --to=iec "$TOTAL_DEX_SIZE")"
fi

# Count methods in DEX (approximate)
TOTAL_METHODS=0
for dex in "$TEMP_DIR"/classes*.dex; do
    if [ -f "$dex" ]; then
        # Use dexdump to count methods (requires Android SDK)
        if command -v dexdump &>/dev/null; then
            METHOD_COUNT=$(dexdump -f "$dex" 2>/dev/null | grep "method_ids" | awk '{print $2}')
            if [ -n "$METHOD_COUNT" ]; then
                TOTAL_METHODS=$((TOTAL_METHODS + METHOD_COUNT))
            fi
        fi
    fi
done

if [ $TOTAL_METHODS -gt 0 ]; then
    echo "📊 Total methods in DEX: ~$TOTAL_METHODS"
fi

# Cleanup
rm -rf "$TEMP_DIR"

echo ""
echo "==================================="
echo "Checking for common issues"
echo "==================================="
echo ""

# Check if Broadcom/VMware SDK is being used (common issue)
if grep -q "vmware" "$USAGE_FILE" 2>/dev/null; then
    echo "⚠️  WARNING: VMware/Broadcom SDK detected - this prevents obfuscation"
fi

# Check for excessive -keep rules
if [ -f "proguard-rules.pro" ]; then
    KEEP_COUNT=$(grep -c "^-keep" proguard-rules.pro)
    DONTWARN_COUNT=$(grep -c "^-dontwarn" proguard-rules.pro)
    echo "📝 ProGuard rules:"
    echo "   -keep rules: $KEEP_COUNT"
    echo "   -dontwarn rules: $DONTWARN_COUNT"
    
    if [ $KEEP_COUNT -gt 10 ]; then
        echo "   ⚠️  Consider reducing -keep rules for better obfuscation"
    fi
fi

echo ""
echo "==================================="
echo "Recommendations"
echo "==================================="
echo ""

if [ -f "$USAGE_FILE" ]; then
    USAGE_LINES=$(wc -l < "$USAGE_FILE")
    if [ $USAGE_LINES -gt 5000 ]; then
        echo "⚠️  HIGH: $USAGE_LINES classes/methods are kept"
        echo "   → Review usage.txt to identify unnecessary kept classes"
        echo "   → Look for broad -keep rules that can be narrowed"
    elif [ $USAGE_LINES -gt 2000 ]; then
        echo "⚡ MEDIUM: $USAGE_LINES classes/methods are kept"
        echo "   → Consider narrowing -keep rules"
    else
        echo "✅ LOW: $USAGE_LINES classes/methods are kept - good!"
    fi
fi

# Check for play-services-ads (heavy library)
if grep -q "play-services-ads" build.gradle.kts; then
    if grep -q "play-services-ads-lite" build.gradle.kts; then
        echo "✅ Using play-services-ads-lite (lighter version)"
    else
        echo "⚠️  Consider switching to play-services-ads-lite"
    fi
fi

echo ""
echo "==================================="
echo "Next Steps"
echo "==================================="
echo ""
echo "1. Run: ./gradlew assembleRelease"
echo "2. Check mapping.txt and usage.txt in app/build/outputs/mapping/release/"
echo "3. Upload to Google Play Console"
echo "4. Monitor obfuscation percentage (target: >25%)"
echo "5. If still low, refine proguard-rules.pro based on usage.txt"
echo ""
echo "For detailed analysis:"
echo "  - Use Android Studio: Build > Analyze APK..."
echo "  - Use apkanalyzer: apkanalyzer app-release.apk"
echo ""

# Cleanup temp files
rm -rf "$TEMP_DIR"

echo "✅ Analysis complete!"
