{
  description = "MyDog - Paper plugin that makes tamed wolves at least 10x better";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs =
    { self, nixpkgs }:
    let
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];
      forAllSystems = f: nixpkgs.lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});
    in
    {
      devShells = forAllSystems (
        pkgs:
        let
          # Matches the JDK used by CI (.github/workflows/build.yml)
          jdk = pkgs.jdk25;
        in
        {
          default = pkgs.mkShell {
            packages = [
              jdk
              (pkgs.maven.override { jdk_headless = jdk; })
            ];

            JAVA_HOME = jdk.home;
          };
        }
      );

      formatter = forAllSystems (pkgs: pkgs.nixfmt-rfc-style);
    };
}
