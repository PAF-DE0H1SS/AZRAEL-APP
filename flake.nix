{
  description = "AZRAEL-APP - client for azrael-lab.xyz (Android 13+ / Windows 10/11 / Linux)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
      pkgsFor = system: import nixpkgs { inherit system; };

      src = nixpkgs.lib.cleanSourceWith {
        src = self;
        filter = path: type:
          let base = baseNameOf path;
          in !(type == "directory" && (base == ".gradle" || base == ".kotlin" || base == ".idea" || base == "build"));
      };

      appVersion =
        let
          line = builtins.match ".*azraelVersion=([^ \r\n]*).*"
            (builtins.readFile (self + "/gradle.properties"));
        in
        if line == null then "0.0.0-unknown"
        else builtins.head line;

      mkApp = system:
        let
          pkgs = pkgsFor system;
          jdk = pkgs.jdk21;
        in
        pkgs.stdenvNoCC.mkDerivation {
          pname = "azrael-app";
          version = appVersion;
          src = src;

          nativeBuildInputs = with pkgs; [ jdk cmake ninja binutils ];

          buildPhase = ''
            JAVA_HOME=${jdk.home} ./gradlew :desktopApp:packageUberJarForCurrentOS \
              --no-daemon --console=plain
          '';

          installPhase = ''
            install -dm755 $out/bin $out/share/azrael $out/share/applications
            cp desktopApp/build/compose/jars/*.jar $out/share/azrael/azrael-app.jar

            cat > $out/bin/azrael <<EOF
            #!/bin/sh
            exec ${jdk.home}/bin/java -Xms64m -Xmx512m -jar $out/share/azrael/azrael-app.jar "\$@"
            EOF
            chmod +x $out/bin/azrael

            cat > $out/share/applications/azrael.desktop <<EOF
            [Desktop Entry]
            Type=Application
            Name=AZRAEL-APP
            Comment=Client for azrael-lab.xyz
            Exec=$out/bin/azrael
            Terminal=false
            Categories=Utility;
            EOF
          '';

          meta = with pkgs.lib; {
            description = "Client for azrael-lab.xyz";
            license = {
              shortName = "PolyForm-Noncommercial-1.0.0";
              fullName = "Dual licensing: PolyForm Noncommercial 1.0.0 + author terms (revenue share, back-feed)";
              url = "https://polyformproject.org/licenses/noncommercial/1.0.0";
              free = false;
            };
            platforms = platforms.linux;
          };
        };
    in
    {
      packages = forAllSystems (system: {
        default = mkApp system;
      });

      apps = forAllSystems (system: {
        default = {
          type = "app";
          program = "${mkApp system}/bin/azrael";
        };
      });

      devShells = forAllSystems (system:
        let pkgs = pkgsFor system;
        in {
          default = pkgs.mkShell {
            packages = with pkgs; [ jdk21 jdk17 cmake ninja binutils ];
          };
        });
    };
}
