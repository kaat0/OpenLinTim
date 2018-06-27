#!/usr/bin/env bash
set -e

cd "$(dirname "$0")"

exec nix-shell -p gurobi python3Packages.gurobipy python3Packages.numpy --run "source ../src/nix-env.sh;source c7-env.sh;bash run_all_tests.sh"