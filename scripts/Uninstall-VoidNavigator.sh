#!/usr/bin/env bash

set -euo pipefail

usage() {
    cat <<'EOF'
Usage: Uninstall-VoidNavigator.sh [--app PATH] [--remove-saves] [--yes] [--dry-run]

Removes the VoidNavigator app beside this script, or the app at --app PATH.
Saved games are kept unless --remove-saves is supplied.
EOF
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
app_path=""
remove_saves=false
assume_yes=false
dry_run=false

while (($#)); do
    case "$1" in
        --app)
            (($# >= 2)) || { usage >&2; exit 2; }
            app_path="$2"
            shift 2
            ;;
        --remove-saves) remove_saves=true; shift ;;
        --yes) assume_yes=true; shift ;;
        --dry-run) dry_run=true; shift ;;
        -h|--help) usage; exit 0 ;;
        *) usage >&2; exit 2 ;;
    esac
done

if [[ -z "$app_path" ]]; then
    if [[ -d "$script_dir/VoidNavigator.app" ]]; then
        app_path="$script_dir/VoidNavigator.app"
    else
        app_path="$script_dir/VoidNavigator"
    fi
fi

[[ -d "$app_path" && ! -L "$app_path" ]] || {
    echo "VoidNavigator app not found at: $app_path" >&2
    echo "If you moved the app, pass --app followed by its path." >&2
    exit 1
}
app_path="$(cd "$app_path" && pwd -P)"

if [[ -f "$app_path/Contents/Info.plist" &&
      -f "$app_path/Contents/app/AsteroidGame.jar" &&
      -x "$app_path/Contents/MacOS/VoidNavigator" ]]; then
    app_kind="macOS app"
elif [[ -f "$app_path/AsteroidGame.jar" &&
        -f "$app_path/Play-VoidNavigator.bat" ]]; then
    app_kind="portable download"
else
    echo "Refusing to remove an unrecognized folder: $app_path" >&2
    exit 1
fi

if [[ "$(uname -s)" == "Darwin" ]]; then
    save_dir="${HOME:?HOME must be set}/Library/Application Support/VoidNavigator"
else
    data_home="${XDG_DATA_HOME:-${HOME:?HOME must be set}/.local/share}"
    if "$remove_saves" && [[ "$data_home" != /* ]]; then
        echo "XDG_DATA_HOME must be an absolute path to remove saves safely." >&2
        exit 1
    fi
    save_dir="$data_home/VoidNavigator"
fi

echo "App to remove ($app_kind): $app_path"
if "$remove_saves"; then
    echo "Save data to remove: $save_dir"
else
    echo "Save data will be kept: $save_dir"
fi

if "$dry_run"; then
    echo "Dry run: nothing removed."
    exit 0
fi

if ! "$assume_yes"; then
    read -r -p "Quit the game, then type REMOVE to continue: " reply || exit 1
    [[ "$reply" == "REMOVE" ]] || { echo "Cancelled."; exit 0; }
fi

rm -rf -- "$app_path"
if "$remove_saves" && [[ -e "$save_dir" || -L "$save_dir" ]]; then
    [[ -d "$save_dir" && ! -L "$save_dir" ]] || {
        echo "App removed; save path is not a normal directory, so it was kept: $save_dir" >&2
        exit 1
    }
    rm -rf -- "$save_dir"
fi
echo "VoidNavigator removed."
