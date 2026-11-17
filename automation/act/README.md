# Local releases

Requires act + Docker Desktop (Linux containers). Builds and signing verified with act.

Root `.env`:

```dotenv
ANDROID_KEYSTORE_PATH=C:\Users\yeahg\Documents\dev\enc\glider-compose-kot
ANDROID_KEY_ALIAS=key1
ANDROID_STORE_PASSWORD=<store password>
ANDROID_KEY_PASSWORD=<key password>
GITHUB_TOKEN=<Systemad/Glider token with Contents read/write>
```

The keystore stays outside the repo. `.env` is ignored by Git.

After committing/pushing the release source and matching version tag:

```powershell
./automation/act/release.ps1 -Tag v0.1 -Commit FULL_SHA -CreateDraft
```

Gradle signs the release APK. The debug APK uses Android's debug key and a separate
app ID, so both can be installed together. `-CreateDraft` uploads both APKs;
omit it for a local build. Add `-LocalSource` to build a committed local snapshot.
Artifacts: `.act-artifacts/`. Nothing runs automatically.
