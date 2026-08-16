#!/usr/bin/env bash
# JustDid APK 打包脚本
# 用法: ./build-apk.sh
# 产出: dist/just-did-v<版本号>-debug.apk 和 dist/just-did-v<版本号>-release.apk
# 说明: release 签名密钥位于 keystore/release.jks，密码在 app/keystore.properties（均已加入 .gitignore，勿提交）
set -euo pipefail
cd "$(dirname "$0")"

VERSION_NAME=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts | head -1)
echo "开始打包 v${VERSION_NAME} ..."

./gradlew --console=plain assembleDebug assembleRelease

mkdir -p dist
cp -f app/build/outputs/apk/debug/app-debug.apk "dist/just-did-v${VERSION_NAME}-debug.apk"
cp -f app/build/outputs/apk/release/app-release*.apk "dist/just-did-v${VERSION_NAME}-release.apk"

echo "打包完成:"
ls -lh dist/
