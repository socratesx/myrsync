#!/usr/bin/env python3
"""Read and compare application versionCode / versionName in app/build.gradle."""

import pathlib
import re
import sys

CODE_RE = re.compile(r"(?m)^\s*versionCode\s*=\s*(\d+)\s*$")
NAME_RE = re.compile(r'(?m)^\s*versionName\s*=\s*"([^"]+)"\s*$')


def parse(text):
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    code = CODE_RE.search(text)
    name = NAME_RE.search(text)
    if not code or not name:
        raise ValueError(
            "Could not find versionCode = <number> and versionName = \"<name>\" "
            "in app/build.gradle"
        )
    return int(code.group(1)), name.group(1)


def read(path):
    return parse(pathlib.Path(path).read_text(encoding="utf-8"))


def cmd_print(path):
    code, name = read(path)
    print(f"version_code={code}")
    print(f"version_name={name}")
    return 0


def cmd_check(current_path, base_path):
    new_code, new_name = read(current_path)
    old_code, old_name = read(base_path)
    errors = []
    if new_code <= old_code:
        errors.append(
            f"versionCode must increase (base {old_code}, this change {new_code}). "
            "Play Console rejects an upload that reuses a versionCode."
        )
    if new_name == old_name:
        errors.append(
            f"versionName must change (still {new_name}). "
            "The GitHub release is tagged from versionName."
        )
    if errors:
        for message in errors:
            print(message, file=sys.stderr)
        return 1
    print(f"version {old_name} ({old_code}) -> {new_name} ({new_code})")
    return 0


def main(argv):
    if len(argv) < 2 or argv[1] in ("-h", "--help"):
        print(
            "usage:\n"
            "  app_version.py print <app/build.gradle>\n"
            "  app_version.py check <current app/build.gradle> <base app/build.gradle>",
            file=sys.stderr,
        )
        return 2
    command = argv[1]
    try:
        if command == "print" and len(argv) == 3:
            return cmd_print(argv[2])
        if command == "check" and len(argv) == 4:
            return cmd_check(argv[2], argv[3])
    except (OSError, ValueError) as error:
        print(error, file=sys.stderr)
        return 1
    print(f"unknown command: {command}", file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
