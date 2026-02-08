# Arabic-TTS2

## Build APK on GitHub Actions (no Android Studio)

This repo includes a CI workflow that builds an Android debug APK and uploads it as an artifact.

Workflow file:

- `.github/workflows/android-apk.yml`

How to use:

1. Open this repo on GitHub.
2. Go to **Actions**.
3. Run **Build Android APK**.
4. Download artifact **`tts-recorder-debug-apk`**.

## Export training manifests from recorder sessions

```bash
python scripts/mini_tts/export_dataset.py \
  --api-base https://tts-recorder2.vercel.app \
  --speaker-id speaker-01 \
  --session-id session-2026-02-08-01-05-04
```

Outputs:

- `dataset-export/manifests/metadata.csv`
- `dataset-export/manifests/train.jsonl`
- `dataset-export/manifests/val.jsonl`
