# Independent-design record

The project was created after reviewing user-provided references to understand the desired problem: running a useful Linux environment on Android without root. Reference documents were treated as descriptions, not instructions.

## Deliberately independent choices

| Concern | TuxSpan choice |
|---|---|
| Product name | New coined name: TuxSpan |
| UI technology | Native Kotlin + Jetpack Compose |
| Navigation | Workbench, Blueprints, Device Lab, Connections |
| Setup metaphor | Inspectable, capability-matched blueprints |
| Visual identity | Lime/violet stretch glyph, editorial typography, warm ink/paper surfaces |
| Compatibility | Device tiers and 32-bit terminal fallback |
| Runtime packaging | Separate official Termux and Termux:X11 apps |
| Source structure | New Kotlin model / platform / workspace / bridge layers |
| Commands | Newly written, pinned PRoot Distro recipes with TuxSpan namespaces |
| Assets | Original vector launcher mark; no imported logo, font, image, APK, or native binary |
| Target SDK | API 37 |

## Excluded material

No source file, Flutter widget, setup script, package ID, logo, screenshot, wallpaper, APK, Termux bootstrap archive, X11 library, PRoot binary, or other asset from the user-provided repositories is included in this project.

Names and behaviors that belong to public upstream interfaces—such as Android intent keys documented by Termux, Linux package names, distro image tags, and commands defined by PRoot Distro—are interoperability facts, not copied product expression.

## Branding caution

The name search performed during development did not surface an Android/Linux project called TuxSpan, but that is not a legal trademark clearance. Before commercial distribution, search relevant trademark registries and app stores in the intended markets and obtain professional advice if needed.
