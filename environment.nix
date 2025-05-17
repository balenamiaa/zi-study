# environment.nix
# Defines the Nix environment for the application, using nixpkgs-unstable for packages.
let
  pkgs = import (fetchTarball "https://github.com/NixOS/nixpkgs/archive/nixos-24.11.tar.gz") {};
in
pkgs.mkShellNoCC {
  name = "zi-study-env";

  packages = with pkgs; [
    sqlite
    clojure
    jdk23_headless
    nodejs_24
  ];
} 