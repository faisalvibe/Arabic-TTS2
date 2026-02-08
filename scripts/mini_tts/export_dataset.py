#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import json
from pathlib import Path
from typing import Any

import requests


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Export recorder sessions to train/val manifests")
    p.add_argument("--api-base", required=True)
    p.add_argument("--speaker-id", required=True)
    p.add_argument("--session-id", action="append", required=True)
    p.add_argument("--out-dir", default="dataset-export")
    p.add_argument("--val-ratio", type=float, default=0.1)
    return p.parse_args()


def fetch_session(api_base: str, speaker_id: str, session_id: str) -> list[dict[str, Any]]:
    url = f"{api_base.rstrip('/')}/api/tts-recordings?speakerId={speaker_id}&sessionId={session_id}"
    r = requests.get(url, timeout=60)
    r.raise_for_status()
    clips = r.json().get("clips", [])
    if not isinstance(clips, list):
        return []
    return [c for c in clips if isinstance(c, dict) and c.get("fileUrl")]


def stable_bucket(value: str) -> float:
    digest = hashlib.sha1(value.encode("utf-8")).hexdigest()
    return int(digest[:8], 16) / 0xFFFFFFFF


def infer_language(raw: dict[str, Any]) -> str:
    lang = str(raw.get("language") or "").strip().lower()
    if lang in {"ar", "en"}:
        return lang
    text = str(raw.get("promptText") or "")
    return "ar" if any("\u0600" <= ch <= "\u06FF" for ch in text) else "en"


def download(url: str, target: Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    r = requests.get(url, timeout=120)
    r.raise_for_status()
    target.write_bytes(r.content)


def main() -> int:
    args = parse_args()
    out = Path(args.out_dir).resolve()
    clips_dir = out / "clips"
    manifests = out / "manifests"
    manifests.mkdir(parents=True, exist_ok=True)

    rows: list[dict[str, Any]] = []
    for session in args.session_id:
        for clip in fetch_session(args.api_base, args.speaker_id, session):
            file_url = str(clip.get("fileUrl") or "").strip()
            if not file_url:
                continue
            name = str(clip.get("fileName") or "") or Path(file_url.split("?", 1)[0]).name
            local = clips_dir / args.speaker_id / session / name
            download(file_url, local)
            text = str(clip.get("promptText") or "").strip()
            if not text:
                continue
            rows.append(
                {
                    "speaker": args.speaker_id,
                    "session": session,
                    "prompt_index": int(clip.get("promptIndex", -1)),
                    "language": infer_language(clip),
                    "text": text,
                    "duration_ms": int(clip.get("durationMs") or 0),
                    "audio_path": str(local.resolve()),
                    "file_url": file_url,
                }
            )

    if not rows:
        raise SystemExit("No clips found.")

    rows.sort(key=lambda r: (r["session"], r["prompt_index"], r["audio_path"]))

    csv_path = manifests / "metadata.csv"
    with csv_path.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)

    train_path = manifests / "train.jsonl"
    val_path = manifests / "val.jsonl"
    with train_path.open("w", encoding="utf-8") as tr, val_path.open("w", encoding="utf-8") as va:
        for row in rows:
            sample = {
                "audio_file": row["audio_path"],
                "text": row["text"],
                "speaker": row["speaker"],
                "language": row["language"],
                "session": row["session"],
                "duration_ms": row["duration_ms"],
            }
            key = f"{row['session']}|{row['prompt_index']}|{row['audio_path']}"
            (va if stable_bucket(key) < args.val_ratio else tr).write(
                json.dumps(sample, ensure_ascii=False) + "\n"
            )

    print(f"Export complete: {out}")
    print(f"Metadata CSV: {csv_path}")
    print(f"Train manifest: {train_path}")
    print(f"Val manifest: {val_path}")
    print(f"Total clips: {len(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
