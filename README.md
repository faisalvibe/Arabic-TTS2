# Arabic-TTS2

## Build APK on GitHub Actions (no Android Studio)

This repo includes a CI workflow that builds an Android debug APK and uploads it as an artifact.

What the current APK does:

- Local on-device speech (no server required for playback)
- Type text, choose English/Arabic, and tap **Speak**
- Uses Android's built-in offline TTS engine on the phone
- Includes **My Voice (Model)** mode with model-file check (`app/src/main/assets/voice/`)

Workflow file:

- `.github/workflows/android-apk.yml`

How to use:

1. Open this repo on GitHub.
2. Go to **Actions**.
3. Run **Build Android APK**.
4. Download artifact **`tts-recorder-debug-apk`**.

## Personalized Voice (your own voice) on device

Your custom voice requires a dedicated exported mobile model (ONNX/other) trained on your recordings.

Current status:

- Recorder + dataset export is ready.
- Android app UI is ready.
- Custom on-device voice engine/model integration is the next step.

Practical path:

1. Record more high-quality clips (target 30-60+ minutes for decent quality).
2. Export dataset with `scripts/mini_tts/export_dataset.py`.
3. Train/export a mobile-ready model from that dataset (`model.onnx` + `tokens.txt`).
4. Put model files in `android-app/app/src/main/assets/voice/`.
5. Rebuild APK in Actions.

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
