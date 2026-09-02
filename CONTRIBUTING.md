# Contributing

1. Keep TuxSpan independent in product language, UI, assets, and source structure.
2. Do not submit copied APKs, bootstraps, native binaries, artwork, or code without a documented compatible license, exact source provenance, and reproducible build path.
3. Keep every generated command visible and deterministic.
4. Add unit tests when changing recipe generation or removal behavior.
5. Test with `./gradlew test assembleDebug lint`.
6. Update compatibility and third-party notices when behavior or dependencies change.

For device reports, include Android version, manufacturer/model, primary ABI, total RAM, free space, Termux source/version, Termux:X11 version, selected blueprint, and sanitized error output. Never include tokens, SSH keys, shell history, or private filenames.

