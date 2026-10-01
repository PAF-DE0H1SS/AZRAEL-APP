#!/usr/bin/env bash
# ============================================================
# Дополняет .deb, собранный jpackage, тем, чего jpackage не делает.
#
#  Запуск:  ./scripts/finish-deb.sh <путь/к/xyz.azraellab.app_X_amd64.deb>
#
#  Что добавляется и почему:
#   1. /usr/bin/azrael-app -> лаунчер в /opt. jpackage DEB кладёт исполняемый
#      файл только в /opt/<pkg>/bin/ и НИКОГДА не делает симлинк в /usr/bin
#      (в шаблонах jpackage нет ни одного упоминания usr/bin для DEB). После
#      установки пакета команду нельзя набрать в терминале - только мышкой
#      через меню приложений.
#   2. /usr/share/applications/azrael-app.desktop - jpackage регистрирует
#      .desktop только вызовом `xdg-desktop-menu install` из postinst, поэтому
#      файл физически лежит не в стандартном каталоге, и если xdg-utils
#      отсутствует, запись в меню просто не появляется.
#   3. /usr/share/icons/hicolor/512x512/apps/azrael-app.png - чтобы иконка
#      была найдена по стандарту, а не абсолютным путём в /opt.
#   4. Depends в control - заменяется целиком, а не дополняется. jpackage
#      вычисляет зависимости через jdeps, но на NixOS тот не находит
#      библиотеки (вне /usr/lib), и в control попадает одинокая
#      «Depends: xdg-utils» - НИ libstdc++6, НИ libx11-6. На реальной
#      Debian-машине пакет встал бы без единой библиотеки и упал бы при
#      запуске с «libGL.so.1: cannot open shared object file».
#      Список ниже - прямые DT_NEEDED libskiko-linux-x64.so и библиотек
#      jlink-runtime, переведённые в имена пакетов Debian/Ubuntu.
#      xdg-utils здесь больше не нужен: postinst больше не вызывает
#      xdg-desktop-menu, .desktop ставится физически (пункт 2). Взамен
#      добавлены пакеты, дающие update-desktop-database и
#      gtk-update-icon-cache - без них postinst/postrm молча пропускают
#      обновление кэшей (вызовы под guard `command -v`).
#   5. prerm от jpackage снимает меню голым `xdg-desktop-menu uninstall`
#      (без `command -v`, без `|| true`) при `set -e`. xdg-utils мы из
#      Depends убрали, поэтому на машине без него `apt remove` падал бы
#      с кодом 1. Строку удаляем: свой .desktop ставится физически и в
#      меню xdg не зарегистрирован.
#   6. libasound2 записан как `libasound2 | libasound2t64`: на Debian 13
#      (trixie) реальный пакет называется libasound2t64, а на bookworm -
#      libasound2. Без альтернативы `apt install ./file.deb` на trixie
#      ругался бы на неудовлетворённую зависимость.
#   7. `libgio-2.0-0` из Depends убран: такого пакета нет НИ в bookworm,
#      НИ в trixie. Библиотека libgio-2.0.so.0 живёт в пакете
#      `libglib2.0-0`, который в списке уже есть, - дубликат был
#      несуществующим именем и делал пакет неустанавливаемым:
#      `E: Unable to correct problems, you have held broken packages` /
#      `Depends: ... but it is not installable`. Проверено реальной
#      установкой в контейнере debian:bookworm.
#   7. Добавлен AppStream-метаинфо (/usr/share/metainfo) - jpackage его не
#      кладёт, и без него дистрибутивы показывают пакет без описания.
#
#  Требует dpkg-deb. Пересобирает пакет рядом с исходным (суффикс _azrael).
#
#  Идемпотентность: готовый пакет помечается маркером внутри payload'а, и
#  повторный запуск на нём - успешный no-op. Решение принимается по ИМЕНИ
#  (до распаковки) + маркеру (по списку содержимого, тоже без распаковки):
#  наш выход *_azrael.deb с маркером → no-op; старый пакет с суффиксом, но без
#  маркера → отказ. Без этой проверки второй проход дописывал бы блок обновления
#  кэшей в postinst/postrm ещё раз, а имя разрасталось до *_azrael_azrael.deb.
# ============================================================
set -euo pipefail

DEB="${1:-}"
[ -n "$DEB" ] || { echo "usage: $0 <file.deb>" >&2; exit 1; }
[ -f "$DEB" ] || { echo "нет файла: $DEB" >&2; exit 1; }

MARKER_REL="usr/share/azrael/finish-deb.marker"
MARKER_PATH="./$MARKER_REL"

# dpkg-deb в NixOS не в PATH по умолчанию.
if ! command -v dpkg-deb > /dev/null 2>&1; then
  for d in /nix/store/*dpkg-*/; do
    if [ -x "$d/bin/dpkg-deb" ]; then
      export PATH="$d/bin:$PATH"
      break
    fi
  done
fi
command -v dpkg-deb > /dev/null 2>&1 || { echo "dpkg-deb не найден" >&2; exit 1; }

# Защита по ИМЕНИ - до распаковки. dpkg-deb -R распаковывает ~90 МБ, а решить
# можно и без распаковки: dpkg-deb -c только перечисляет содержимое архива,
# ничего не пишет на диск.
#
# Смысл: готовый пакет всегда называется *_azrael.deb. Если суффикс уже в имени,
# то это либо наш собственный вывод (маркер есть) - успешный no-op, либо пакет,
# собранный ДО появления маркера - дополнять его нельзя, второй проход размножил
# бы суффикс и дописал блоки в postinst/postrm.
case "$(basename "$DEB")" in
  *_azrael.deb | *_azrael_azrael.deb | *_azrael_azrael_azrael.deb | *_azrael_azrael_azrael_azrael.deb)
    # grep -F без -q: -q выходит на первом совпадении, dpkg-deb ловит SIGPIPE,
    # и при `set -o pipefail` пайплайн вернёт 141, т.е. «маркера нет».
    if dpkg-deb -c "$DEB" 2>/dev/null | grep -F "$MARKER_PATH" > /dev/null; then
      echo "[AZRAEL] пакет уже дополнен, повторный запуск не требуется: $DEB"
      exit 0
    fi
    echo "[AZRAEL] отказ: имя содержит суффикс _azrael, но маркера нет (старый пакет)." >&2
    echo "         $(basename "$DEB")" >&2
    echo "         Возьмите исходный *_amd64.deb из jpackage либо удалите этот пакет." >&2
    exit 1
    ;;
esac

PKG_DIR=$(basename "$DEB" .deb | cut -d_ -f1)   # xyz.azraellab.app
CMD="azrael-app"

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

dpkg-deb -R "$DEB" "$WORK/root"

# Страховка на переименованный готовый пакет: имя чистое, а маркер внутри есть.
if [ -f "$WORK/root/$MARKER_REL" ]; then
  echo "[AZRAEL] пакет уже дополнен, повторный запуск не требуется: $DEB"
  exit 0
fi

LAUNCHER="$WORK/root/opt/$PKG_DIR/bin/$PKG_DIR"
[ -x "$LAUNCHER" ] || { echo "не найден лаунчер $LAUNCHER" >&2; exit 1; }
ICON_SRC=$(ls "$WORK/root/opt/$PKG_DIR/lib/$PKG_DIR.png" 2>/dev/null | head -1)
[ -n "$ICON_SRC" ] || { echo "не найдена иконка $PKG_DIR.png" >&2; exit 1; }

# 1. симлинок в /usr/bin
mkdir -p "$WORK/root/usr/bin"
ln -sfn "/opt/$PKG_DIR/bin/$PKG_DIR" "$WORK/root/usr/bin/$CMD"

# 2. desktop-файл в стандартном каталоге
mkdir -p "$WORK/root/usr/share/applications"
cat > "$WORK/root/usr/share/applications/$CMD.desktop" <<EOF
[Desktop Entry]
Name=AZRAEL-APP
GenericName=AZRAEL-APP
Comment=Клиент AZRAEL-APP для azrael-lab.xyz
Exec=$CMD
Icon=$CMD
Terminal=false
Type=Application
# Одна main-категория (Network) + подкатегория InstantMessaging.
# Development;Network; - это две main-категории, desktop-file-validate даёт
# hint, и в меню приложение рисуется дважды.
Categories=Network;InstantMessaging;
StartupWMClass=xyz.azraellab.app
EOF

# 3. иконка в hicolor
mkdir -p "$WORK/root/usr/share/icons/hicolor/512x512/apps"
cp "$ICON_SRC" "$WORK/root/usr/share/icons/hicolor/512x512/apps/$CMD.png"

# 3a. AppStream-метаинфо. jpackage его не кладёт, а без него GNOME Software /
#     KDE Discover показывают пакет просто как «xyz.azraellab.app» без описания,
#     лицензии и ссылок. Файл необязательный, поэтому пишем его в /usr/share,
#     а не в /opt: тогда AppStream увидит его у любого установленного пакета.
#     Версию берём из control, чтобы metainfo не расходился с .deb.
PKGVER=$(dpkg-deb -f "$DEB" Version 2>/dev/null || true)
[ -n "$PKGVER" ] || PKGVER="1.0.0"
# Дата релиза - mtime собираемого .deb, а не зашитая константа: иначе через
# год в метаданных будет стоять сегодняшняя дата сборки 1.3.3, а appstreamcli
# ругается на <release> без date (release-time-missing).
PKGDATE=$(date -u -r "$DEB" +%Y-%m-%d 2>/dev/null || true)
case "$PKGDATE" in
  [0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]) ;;
  *) PKGDATE="1970-01-01" ;;
esac
mkdir -p "$WORK/root/usr/share/metainfo"
cat > "$WORK/root/usr/share/metainfo/$PKG_DIR.metainfo.xml" <<META_EOF
<?xml version="1.0" encoding="UTF-8"?>
<!-- Сгенерировано scripts/finish-deb.sh, правьте его, а не этот файл. -->
<component type="desktop-application">
  <id>$PKG_DIR</id>
  <metadata_license>CC0-1.0</metadata_license>
  <!-- SPDX-идентификатора нет: лицензия двойная (PolyForm Noncommercial
       для некоммерческого + коммерческая по соглашению), см. LICENSE.
       Блок <custom> с текстом лицензии сюда НЕ кладём: appstream-glib
       1.1.x считает любой <custom> ошибкой custom-key-missing. -->
  <project_license>LicenseRef-custom</project_license>
  <name>AZRAEL-APP</name>
  <name xml:lang="ru">AZRAEL-APP</name>
  <name xml:lang="zh">AZRAEL-APP</name>
  <summary>Client for the azrael-lab.xyz platform</summary>
  <summary xml:lang="ru">Клиент платформы azrael-lab.xyz</summary>
  <summary xml:lang="zh">azrael-lab.xyz 平台客户端</summary>
  <description>
    <p>AZRAEL-APP is a Compose Multiplatform client for the azrael-lab.xyz platform: chats with attachments, device management, shortener, VPN, and an admin console. One code base, Android and desktop.</p>
    <p xml:lang="ru">AZRAEL-APP - клиент платформы azrael-lab.xyz на Compose Multiplatform: чаты с вложениями, управление устройствами, сокращатель, VPN и админ-консоль. Один код, Android и десктоп.</p>
    <p xml:lang="zh">AZRAEL-APP 是 azrael-lab.xyz 平台的 Compose Multiplatform 客户端：带附件的聊天、设备管理、短链、VPN 和管理后台。一套代码，Android 与桌面端。</p>
  </description>
  <launchable type="desktop-id">$CMD.desktop</launchable>
  <provides>
    <binary>$CMD</binary>
  </provides>
  <url type="homepage">https://azrael-lab.xyz</url>
  <url type="vcs-browser">https://github.com/PAF-DE0H1SS/AZRAEL-APP</url>
  <!-- id обязан быть доменоподобным (иначе developer-id-invalid), поэтому
       берём id компонента, а не слово azrael. -->
  <developer id="$PKG_DIR">
    <name>AZRAEL Lab</name>
  </developer>
  <content_rating type="oars-1.1"/>
  <releases>
    <release version="$PKGVER" date="$PKGDATE"/>
  </releases>
</component>
META_EOF


# Проверяем, что XML well-formed и внутри есть обязательные для AppStream поля.
# appstreamcli есть не везде (и в CI-образе может не быть), поэтому полагаемся
# только на то, что гарантированно: XML разбирается и id/name/summary на месте.
if command -v xmllint > /dev/null 2>&1; then
  xmllint --noout "$WORK/root/usr/share/metainfo/$PKG_DIR.metainfo.xml" \
    || { echo "metainfo: некорректный XML" >&2; exit 1; }
fi

# 4. maintainer-скрипты.
#    postinst от jpackage регистрирует в меню СВОЙ .desktop
#    (Name=xyz.azraellab.app, Exec с путём в /opt) - рядом с нашей записью
#    AZRAEL-APP это даёт две строки в меню, поэтому вызов убираем.
#    postrm у jpackage пустой: после `apt remove` запись в меню остаётся
#    навсегда. Добавляем обновление кэшей в оба скрипта.
CACHE_BLOCK="$WORK/cache-updates.sh"
cat > "$CACHE_BLOCK" <<'CACHE_EOF'

    # AZRAEL-CACHE-UPDATE
    if command -v update-desktop-database > /dev/null 2>&1; then
        update-desktop-database -q /usr/share/applications || true
    fi
    if command -v gtk-update-icon-cache > /dev/null 2>&1; then
        gtk-update-icon-cache -f -t /usr/share/icons/hicolor > /dev/null 2>&1 || true
    fi
CACHE_EOF

# Вставка блока только если его ещё нет: иначе частично прогнанный пакет
# получил бы два одинаковых вызова подряд.
insert_cache_block() {
  _file="$1"; _pattern="$2"
  [ -f "$_file" ] || return 0
  if grep -q 'AZRAEL-CACHE-UPDATE' "$_file"; then return 0; fi
  sed -i -e '/xdg-desktop-menu install/d' -e "/$_pattern/r $CACHE_BLOCK" "$_file"
  sh -n "$_file" || { echo "maintainer-скрипт сломан: $_file" >&2; exit 1; }
}

# prerm от jpackage снимает регистрацию СВОЕГО .desktop через
# `xdg-desktop-menu uninstall` - голым вызовом, без `command -v` и без `|| true`,
# при `set -e`. Но xdg-utils мы убрали из Depends (пункт 4), а на минимальной
# Debian-машине xdg-desktop-menu нет вовсе: prerm возвращал бы 1 и
# `apt remove`/`apt upgrade` падал бы на удалении пакета. Поэтому вызов надо
# либо убрать, либо закрыть тем же guard'ом, что и вызовы в postinst/postrm.
# Наш .desktop лежит физически в /usr/share/applications, а не в меню xdg, так
# что uninstall для него и не нужен - снимаем строку, а не просто guard.
PRERM="$WORK/root/DEBIAN/prerm"
if [ -f "$PRERM" ]; then
  if grep -q 'xdg-desktop-menu uninstall' "$PRERM"; then
    sed -i -e '/xdg-desktop-menu uninstall/d' "$PRERM"
    sh -n "$PRERM" || { echo "maintainer-скрипт сломан: $PRERM" >&2; exit 1; }
  fi
fi

POSTINST="$WORK/root/DEBIAN/postinst"
insert_cache_block "$POSTINST" '^    configure)$'

POSTRM="$WORK/root/DEBIAN/postrm"
# в postrm строка с purge|remove|... единственная, начинающаяся с "    purge"
insert_cache_block "$POSTRM" '^    purge'

# 5. Depends: реальные библиотеки, а не то, что насчитал jdeps на Nix.
#    desktop-file-utils -> update-desktop-database, libgtk-3-bin -> gtk-update-icon-cache.
CONTROL="$WORK/root/DEBIAN/control"
DEPENDS="libc6, libgcc-s1, libstdc++6, zlib1g, \
libgl1, libglx0, libglib2.0-0, \
libx11-6, libxext6, libxrender1, libxi6, libxrandr2, libxtst6, libxinerama1, libxcursor1, libxfixes3, libxkbcommon0, libxcb1, libxau6, libxdmcp6, \
libfreetype6, libfontconfig1, libexpat1, libpng16-16, libjpeg62-turbo, libgif7, libbrotli1, \
libasound2 | libasound2t64, liblcms2-2, libcups2, libgdk-pixbuf-2.0-0, libmagic1, libgtk-3-0, \
desktop-file-utils, libgtk-3-bin"

if [ -f "$CONTROL" ]; then
  # Разделитель s|…|…| тут не годится: в Depends есть «libasound2 | libasound2t64»,
  # и вертикальная черта закрыла бы команду sed. Поэтому `#`.
  if grep -q "^Depends:" "$CONTROL"; then
    sed -i "s#^Depends:.*#Depends: $DEPENDS#" "$CONTROL"
  else
    printf 'Depends: %s\n' "$DEPENDS" >> "$CONTROL"
  fi
fi

OUT="${DEB%.deb}_azrael.deb"
[ "$OUT" != "$DEB" ] || { echo "имя результата совпало с исходным: $OUT" >&2; exit 1; }

# 6. маркер «пакет дополнен этим скриптом» - по нему следующий запуск узнает,
#    что работа уже сделана (см. проверку выше).
mkdir -p "$WORK/root/usr/share/azrael"
printf 'AZRAEL-APP deb finished by scripts/finish-deb.sh\n' > "$WORK/root/$MARKER_REL"

dpkg-deb --root-owner-group --build "$WORK/root" "$OUT" > /dev/null
chmod 644 "$OUT"

echo "[AZRAEL] deb дополнен: $OUT"
dpkg-deb -c "$OUT" | grep -E "usr/bin/|usr/share/applications/|usr/share/icons/" || true
