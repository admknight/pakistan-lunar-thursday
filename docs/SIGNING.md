# Permanent Android signing — owner setup

**Public repository:** `admknight/pakistan-lunar-thursday`

This repository never contains the private release keystore or any signing passwords. The maintainer owns one persistent PKCS#12 (`.p12`) release keystore and must store two **encrypted GitHub Actions repository secrets** before publishing the first non-preview signed APK.

## First-time setup

1. Download the private **LunarThursdaySigningKit.zip** recovery kit supplied directly to the maintainer. Store it **offline, encrypted**, with a second secure backup. **Never upload the kit, `.p12` file, password file, or base64 keystore to the public repository, an issue, PR, discussion or release.**
2. On GitHub, open **Settings → Secrets and variables → Actions → New repository secret** in this repository.
3. Create `ANDROID_RELEASE_KEYSTORE_B64` with the **single-line contents** of `ANDROID_RELEASE_KEYSTORE_B64.txt` from the private kit (the text is base64-encoded private key material and is secret).
4. Create `ANDROID_RELEASE_PASSWORD` with the complete **single-line contents** of `ANDROID_RELEASE_PASSWORD.txt` from the private kit.
5. Open **Actions → Publish permanently signed APK → Run workflow** on `main` (not an untrusted PR). The job checks both secrets, verifies the locked certificate fingerprint, runs the scheduling tests, builds `assembleRelease`, verifies the APK signature and publishes **GitHub Releases → `v<version>`** as a normal public release.

GitHub Actions cannot read the repository secret's plaintext back. The maintainer must preserve the offline recovery kit for disaster recovery. Changing or losing the private key can break Android in-place updates. Keep the same certificate for all future sideloaded APK releases. Increment both `versionCode` and `versionName` when a new signed version is published; the workflow never replaces existing release assets.

## Public release certificate identity (not a secret)

- Algorithm: RSA 4096, SHA-256
- Keystore format: PKCS#12
- Alias: `lunarthursday`
- Certificate SHA-256 fingerprint: `D2:8B:98:2A:62:5B:08:64:34:73:43:E5:66:F1:F9:64:00:AD:5F:39:9B:43:C2:33:95:4B:C9:91:1C:B9:96:F1`

The release workflow compares both the keystore certificate and the signed APK against this fixed fingerprint. The certificate fingerprint is safe to share; it is not the private key.

## Release policy and practical limits

- **`Build Android APK`** continues generating debug-signed *preview* builds for CI and early testing, without accessing private signing secrets. A preview can still be downloaded publicly from Releases.
- **`Publish permanently signed APK`** runs only when explicitly initiated by a trusted maintainer on `main` and fails closed if either secret is missing or the certificate is wrong. Only it may publish an ordinary non-preview GitHub Release.
- The first transition from a previously installed debug-signed APK **requires uninstall/reinstall**; after installing a stable-signed APK, later signed APKs using this same key and higher `versionCode` can update in place.
- Persistent signing validates APK identity, **not** physical-device functionality, exact timing, official lunar-date correctness or Google Play readiness. The user must approve installation of sideloaded updates.
- Security caveat: Repository admins and trusted workflows can use the signing secrets; enable branch protection and review workflow modifications before merging. Repository secrets are not provided to forked pull requests.

See [Android Developers — app signing](https://developer.android.com/studio/publish/app-signing) and [GitHub Actions secrets](https://docs.github.com/en/actions/concepts/security/secrets).
