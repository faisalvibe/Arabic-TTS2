# Arabic-TTS2

## Build APK on GitHub Actions (no Android Studio)

This repo includes a CI workflow that builds an Android debug APK and uploads it as an artifact.

What the current APK does:

- Local on-device speech (no server required for playback)
- Type text, choose English/Arabic, and tap **Speak**
- Uses ONNX model inference only (no Android system TTS)
- Includes demo English + Arabic ONNX models in `android-app/app/src/main/assets/voice/en` and `android-app/app/src/main/assets/voice/ar`

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
- Android app uses on-device ONNX synthesis.
- Replace bundled demo models with your own exported models for personal voice.

Practical path:

1. Record more high-quality clips (target 30-60+ minutes for decent quality).
2. Export dataset with `scripts/mini_tts/export_dataset.py`.
3. Train/export a mobile-ready model from that dataset (`model.onnx` + `tokens.txt`, and `espeak-ng-data` when required).
4. Put model files in `android-app/app/src/main/assets/voice/en/` or `android-app/app/src/main/assets/voice/ar/`.
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
