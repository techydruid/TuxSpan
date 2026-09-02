# Security policy

## Trust model

TuxSpan can request `com.termux.permission.RUN_COMMAND`. When granted and paired with Termux's `allow-external-apps=true` property, that permission can execute commands and obtain results within the user's Termux environment. Treat a modified TuxSpan build as highly trusted software.

TuxSpan reduces risk by:

- generating commands only from built-in recipe identifiers;
- showing the full setup command before dispatch;
- keeping setup visible in a Termux terminal;
- limiting background result collection to short verification commands;
- using a `tuxspan-<recipe>` namespace;
- avoiding remote configuration, analytics, ad, and update-execution SDKs;
- avoiding root, broad storage permissions, and bundled executable files.

## Maintainer rules

- Never accept a remote string and pass it to `TermuxBridge`.
- Never add shell interpolation from unrestricted user input.
- Keep OCI image versions pinned.
- Show materially changed recipes to the user before execution.
- Keep removals namespaced and confirmation-gated.
- Review dependency changes and generated manifests before each release.
- Sign public releases with a protected release key; never publish a release signed with a debug key.

## Reporting

Until a public repository and security address exist, do not describe this alpha as accepting confidential vulnerability reports. Add a monitored private security contact before publishing a stable release.

