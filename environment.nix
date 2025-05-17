# environment.nix
{
  pkgs = import (fetchTarball "https://github.com/NixOS/nixpkgs/archive/nixos-unstable.tar.gz") {};
}

{
  appEnvironment = pkgs.mkShellNoCC {
    packages = with pkgs; [
      sqlite
      clojure
      jdk24_headless
      nodejs_24
    ];
  };
} 