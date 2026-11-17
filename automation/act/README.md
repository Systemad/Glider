# Local releases

Requires act + Docker Desktop (Linux containers). Local checks passed on 2026-11-17; signed release/upload remains untested.

Root `.env`:

```dotenv
ANDROID_KEYSTORE_PATH=C:\Users\yeahg\Documents\dev\enc\glider-compose
ANDROID_KEY_ALIAS=key0
ANDROID_STORE_PASSWORD=<store password>
ANDROID_KEY_PASSWORD=<key password>
ANDROID_CERT_SHA256=<certificate fingerprint>
GITHUB_TOKEN=<Systemad/Glider token with Contents read/write>
```

The keystore stays outside the repo. `.env` is ignored by Git.

After committing/pushing the release source and matching version tag:

```powershell
./automation/act/release.ps1 -Tag v3.0.0 -Commit FULL_SHA -CreateDraft
```

The script mounts the keystore read-only into Docker. Builds/signs the remote tag
and uploads a draft GitHub Release. Omit `-CreateDraft` for a local build only.
Artifacts: `.act-artifacts/`. Nothing runs automatically.
