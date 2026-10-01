#!/usr/bin/env bash
# ============================================================
# AZRAEL: автосборка всех платформ (Android + Desktop).
#
#  Запуск:  ./build-all.sh [release|dev|linux]
#    release  - релиз: debug+release APK + desktop uber-JAR (по умолчанию)
#    dev      - быстрая: только debug APK + desktop uber-JAR
#    linux    - Linux-форматы: AppImage + .deb (Ubuntu/Debian/Arch/NixOS)
#
#  Код/дизайн/функционал общие (composeApp). Нативный C++ (JNI) -
#  только Android, на desktop передаётся заглушка.
#
#  Java: ищем полный JDK 17+ (нужен jpackage для createDistributable);
#        иначе JBR Android Studio (только для APK, без дистрибутива).
# ============================================================
set -euo pipefail
cd "$(dirname "$0")"

MODE="${1:-release}"

find_jdk_with_jpackage() {
  # Полный JDK (с jpackage) в Nix store
  for d in /nix/store/*openjdk-21*/ /nix/store/*openjdk-17*/; do
    [ -x "$d/bin/jpackage" ] && { echo "${d%/}"; return; }
  done
  # Вручную установленный JDK
  [ -x "/usr/lib/jvm/java/bin/jpackage" ] && { echo "/usr/lib/jvm/java"; return; }
  [ -x "$HOME/.jdks/java/jpackage" ] && { echo "$HOME/.jdks/java"; return; }
  return 1
}

if [ -n "${JAVA_HOME:-}" ] && [ ! -x "$JAVA_HOME/bin/jpackage" ] && [[ "$MODE" == "release" ]]; then
  # Указанный JAVA_HOME не даёт jpackage, а нужен дистрибутив — ищем полный JDK.
  unset JAVA_HOME
fi

if [ -z "${JAVA_HOME:-}" ]; then
  if JDK=$(find_jdk_with_jpackage 2>/dev/null); then
    export JAVA_HOME="$JDK"
  elif compgen -G "/nix/store/*android-studio-unwrapped*/jbr" > /dev/null 2>&1; then
    export JAVA_HOME=$(echo /nix/store/*android-studio-unwrapped*/jbr | tr ' ' '\n' | head -1)
  else
    echo "[AZRAEL] JAVA_HOME не задан, полный JDK и JBR не найдены" >&2
    exit 1
  fi
  export PATH="$JAVA_HOME/bin:$PATH"
  echo "[AZRAEL] JAVA_HOME=$JAVA_HOME"
fi

# jlink/jpackage на Linux требуют objcopy (binutils), их нет в PATH по умолчанию на NixOS.
if ! command -v objcopy > /dev/null 2>&1; then
  for d in /nix/store/*binutils-*/; do
    if [ -x "$d/bin/objcopy" ]; then
      export PATH="$d/bin:$PATH"
      echo "[AZRAEL] binutils=$d"
      break
    fi
  done
fi

# .deb (jpackage DEB-бандлер) требует fakeroot и dpkg-deb — на NixOS их тоже нет в PATH.
if [ "$MODE" = "linux" ] || [ "$MODE" = "release" ]; then
  if ! command -v fakeroot > /dev/null 2>&1; then
    for d in /nix/store/*fakeroot-*/; do
      if [ -x "$d/bin/fakeroot" ]; then
        export PATH="$d/bin:$PATH"
        echo "[AZRAEL] fakeroot=$d"
        break
      fi
    done
  fi
  if ! command -v dpkg-deb > /dev/null 2>&1; then
    for d in /nix/store/*dpkg-*/; do
      if [ -x "$d/bin/dpkg-deb" ]; then
        export PATH="$d/bin:$PATH"
        echo "[AZRAEL] dpkg=$d"
        break
      fi
    done
  fi
fi

echo "[AZRAEL] === Сборка $(date -u +%FT%TZ) / mode=$MODE ==="
case "$MODE" in
  dev)
    ./gradlew :app:assembleDebug :desktopApp:packageUberJarForCurrentOS
    ;;
  release)
    ./gradlew :app:assembleDebug :app:assembleRelease :desktopApp:createDistributable :desktopApp:packageUberJarForCurrentOS
    ;;
  linux)
    ./gradlew :desktopApp:packageAppImage :desktopApp:packageDeb
    ;;
  *)
    echo "Неизвестный режим: $MODE (release|dev|linux)" >&2
    exit 1
    ;;
esac

DIST="build/dist"
mkdir -p "$DIST"

cp app/build/outputs/apk/debug/app-debug.apk "$DIST/AZRAEL-android-debug.apk"
# Release: подписанный APK, если доступен signing/keystore.properties, иначе unsigned.
if [ -f app/build/outputs/apk/release/app-release.apk ]; then
  cp app/build/outputs/apk/release/app-release.apk "$DIST/AZRAEL-android-release.apk"
  rm -f "$DIST/AZRAEL-android-release-unsigned.apk"
  echo "[AZRAEL] release APK подписан (signing/keystore.properties)"
elif [ -f app/build/outputs/apk/release/app-release-unsigned.apk ]; then
  cp app/build/outputs/apk/release/app-release-unsigned.apk "$DIST/AZRAEL-android-release-unsigned.apk"
  echo "[AZRAEL] ВНИМАНИЕ: release APK без подписи (нет signing/keystore.properties)"
fi

DESKTOP_JAR=$(ls desktopApp/build/compose/jars/*.jar 2>/dev/null | head -1 || true)
[ -n "$DESKTOP_JAR" ] && cp "$DESKTOP_JAR" "$DIST/AZRAEL-desktop-$(uname -m).jar"

# Linux-форматы: .deb — готовый файл; AppImage в Compose 1.12 — каталог app-image
# (настоящий .AppImage собирает appimagetool в CI, см. .github/workflows/build.yml).
# Сырой jpackage-пакет публиковать нельзя: в нём нет Depends на реальные
# библиотеки и нет .desktop/иконки. scripts/finish-deb.sh дополняет его и
# даёт *_azrael.deb — в dist кладём только его.
# Именно *_amd64.deb: общий *.deb подхватывает уже дополненный *_azrael.deb,
# и повторный прогон давал *_azrael_azrael.deb.
DEB=$(ls desktopApp/build/compose/binaries/main/deb/*_amd64.deb 2>/dev/null | head -1 || true)
if [ -n "$DEB" ]; then
  if ./scripts/finish-deb.sh "$DEB"; then
    cp "${DEB%.deb}_azrael.deb" "$DIST/"
    rm -f "${DEB%.deb}_azrael_azrael.deb"
  else
    echo "[AZRAEL] ВНИМАНИЕ: finish-deb.sh не отработал, .deb в dist не будет" >&2
  fi
fi

# jpackage/dpkg-deb от root оставляют root-owned файлы — ломают сборку azrael.
# Владельца не хардкодим: берём того, от кого реально запущен скрипт
# (`sudo -u …` → SUDO_USER), иначе владельца репозитория, иначе ничего.
if [ "$(id -u)" = "0" ]; then
  OWNER="${SUDO_USER:-${SUDO_USER_NAME:-}}"
  if [ -z "$OWNER" ] && command -v stat > /dev/null 2>&1; then
    OWNER=$(stat -c '%U' . 2>/dev/null || true)
  fi
  if [ -n "$OWNER" ] && [ "$OWNER" != "root" ]; then
    chown -R "$OWNER" "$DIST" 2>/dev/null || \
      echo "[AZRAEL] ВНИМАНИЕ: не удалось вернуть владельца $OWNER на $DIST" >&2
  else
    echo "[AZRAEL] ВНИМАНИЕ: владелец артефактов остался root ($DIST)" >&2
  fi
fi

echo "[AZRAEL] === Готово. Артефакты: ==="
ls -la "$DIST"