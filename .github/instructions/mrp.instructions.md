---
applyTo: '**'
---
- Analyse this repository by using repomix mcp server to get a context of what it does.
- Always use sequential thinking when answering.
- Always provide sources or evidence for your answers.
- If you are unsure about something, say "I don't know".
- Update all the md (markdown) files and other documentation after every significant change to document technical metrics, success criteria, next steps, and development notes.
- I will perform build.

## Version Management
Before every app build/release, update both version fields in `app/build.gradle.kts`:
- **`versionCode`** - Integer that MUST increase with each release (Google Play Store requirement)
- **`versionName`** - User-friendly version string (follows semantic versioning: Major.Minor.Patch)

Example update:
```kotlin
versionCode = 12          // Increment by 1 or more
versionName = "1.2.0"     // Update based on changes (Major.Minor.Patch)
```

Version naming convention:
- Major: Breaking changes (1.0.0 → 2.0.0)
- Minor: New features (1.1.0 → 1.2.0) 
- Patch: Bug fixes (1.1.0 → 1.1.1)

Note: MainActivity.kt automatically displays version using `BuildConfig.VERSION_NAME`, so no code changes needed.