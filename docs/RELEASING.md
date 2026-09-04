# Publishing a release

A locally built or device-installed APK is not a published release. The public download is updated only after these checks are complete.

1. Review all changes, update the version and changelog, and run the JVM tests and Android lint.
2. Build `assembleRelease` with the existing private production signing credentials. Never generate a new signing key for an update. Keep all signing material outside the repository.
3. Verify the APK with `apksigner verify --verbose --print-certs`. The production certificate SHA-256 must be `4764899DEC5E840DF9A8E814DD09732255E7B312129990DEA164B4C0329C6D6C`.
4. Inspect the packaged manifest: application ID `dev.tuxspan.mobile`, correct version, minimum SDK 26, and no debuggable flag. Development/CI APKs are not release assets.
5. Test installation, app startup, and updating over the previous production release without clearing app data. Separately test any changed feature and record the limits of verification. Do not claim universal error-free operation.
6. Record test results in `VERIFICATION.md`, commit the matching source changes, and build the final APK from that commit. Record its SHA-256 in the release checksum asset. Push the source commit and tag that exact commit as `v<version>`.
7. Create a draft GitHub release for the tag. Attach `TuxSpan-<version>.apk` and `SHA256SUMS.txt`, with general app information, official companion links, and relevant upgrade notes. Do not upload private signing material or device logs.
8. After checking the draft assets, publish it as the latest standard release (not a prerelease). Download the public APK again and compare its hash and signing certificate with the verified local artifact.
9. Check that `/releases/latest` points to the new release before announcing availability. Keep prior production releases available for reference; do not replace an older release's APK with a different version.
