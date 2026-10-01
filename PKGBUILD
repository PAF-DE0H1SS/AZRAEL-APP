# Maintainer: azrael <typ.onepatop@gmail.com>
# Build: makepkg -si (see https://wiki.archlinux.org/title/PKGBUILD)
pkgname=azrael-app
pkgver=1.3.3
pkgrel=1
pkgdesc="AZRAEL-APP client for azrael-lab.xyz (Compose Multiplatform, Android 13+/Windows 10/11/Linux)"
arch=('x86_64')
url="https://github.com/PAF-DE0H1SS/AZRAEL-APP"
license=('custom:PolyForm-Noncommercial-1.0.0')
makedepends=('git' 'jdk21-openjdk' 'cmake' 'ninja' 'binutils')
depends=('jre21-openjdk')
source=("$pkgname-$pkgver.tar.gz::https://github.com/PAF-DE0H1SS/AZRAEL-APP/archive/refs/tags/v$pkgver.tar.gz")
sha256sums=('b4b3118a500ba52ed61765858c3e03a7664fefaebddc34c5c8f246f2a815f703')

_srcdir="AZRAEL-APP-$pkgver"

build() {
  export JAVA_HOME=/usr/lib/jvm/default
  cd "$srcdir/$_srcdir"
  ./gradlew :desktopApp:packageUberJarForCurrentOS --no-daemon --console=plain
}

package() {
  cd "$srcdir/$_srcdir"

  install -dm755 "$pkgdir/usr/bin" "$pkgdir/usr/share/azrael" \
    "$pkgdir/usr/share/applications" "$pkgdir/usr/share/licenses/azrael-app"

  local jar found=1
  for jar in desktopApp/build/compose/jars/*.jar; do
    [ -f "$jar" ] || continue
    install -Dm644 "$jar" "$pkgdir/usr/share/azrael/azrael-app.jar"
    found=0
  done
  if [ "$found" -ne 0 ]; then
    echo "no jar produced in desktopApp/build/compose/jars/" >&2
    return 1
  fi

  install -Dm644 LICENSE "$pkgdir/usr/share/licenses/azrael-app/LICENSE"

  cat > "$pkgdir/usr/bin/azrael" <<EOF
#!/bin/sh
exec /usr/bin/java -Xms64m -Xmx512m -jar /usr/share/azrael/azrael-app.jar "\$@"
EOF
  chmod 755 "$pkgdir/usr/bin/azrael"

  cat > "$pkgdir/usr/share/applications/azrael.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=AZRAEL-APP
Comment=Client for azrael-lab.xyz
Exec=/usr/bin/azrael
Terminal=false
Categories=Utility;
EOF
}
