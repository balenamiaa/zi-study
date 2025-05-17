# environment.nix
# Defines the Nix environment for the application, using nixpkgs-unstable for packages.
let
  # Import nixpkgs-unstable channel.
  # For more reproducible builds, consider pinning to a specific commit:
  # pkgs = import (fetchTarball "https://github.com/NixOS/nixpkgs/archive/YOUR_COMMIT_HASH.tar.gz") {};
  pkgs = import (fetchTarball "https://github.com/NixOS/nixpkgs/archive/nixos-unstable.tar.gz") {};
in
pkgs.mkShellNoCC {
  # Name is not strictly necessary for Nixpacks but good practice.
  name = "zi-study-env";

  packages = with pkgs; [
    sqlite
    clojure
    # Ensure this package name is correct for OpenJDK 24 headless in nixpkgs-unstable.
    # Search search.nixos.org/packages if issues persist.
    # Common alternatives could be 'jdk24', 'openjdk24', 'temurin-bin-24'.
    jdk24_headless
    nodejs_24 # Or just 'nodejs' for the latest from unstable.
  ];
} 