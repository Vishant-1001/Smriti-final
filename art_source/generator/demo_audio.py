"""Generates SMRITI's two demo radio tunes: original, synthesised here, no third-party material.

Usage (repo root): python -I art_source/generator/demo_audio.py
Needs ffmpeg on PATH to encode OGG Vorbis.
"""
import math
import struct
import subprocess
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "app" / "src" / "main" / "assets" / "audio" / "radio"
RATE = 22050

# Gentle pentatonic phrases (semitone offsets from the tonic), each note: (offset, beats)
TUNES = {
    "demo_tune_1": (220.0, 76, [(0, 2), (2, 1), (4, 1), (7, 2), (4, 2), (2, 1), (4, 1), (2, 2), (0, 4),
                                (4, 2), (7, 1), (9, 1), (7, 2), (4, 2), (2, 2), (4, 2), (0, 4)]),
    "demo_tune_2": (196.0, 66, [(7, 2), (9, 2), (7, 1), (4, 1), (2, 2), (4, 4), (0, 2), (2, 2), (4, 1), (7, 1),
                                (4, 2), (2, 4), (0, 2), (-3, 2), (0, 4)]),
}


def tone(freq, seconds, amp=0.28):
    n = int(seconds * RATE)
    out = []
    for i in range(n):
        t = i / RATE
        env = min(1.0, t / 0.04) * math.exp(-2.2 * t / max(seconds, 0.3))
        v = (math.sin(2 * math.pi * freq * t) + 0.35 * math.sin(4 * math.pi * freq * t)
             + 0.12 * math.sin(6 * math.pi * freq * t))
        out.append(amp * env * v / 1.47)
    return out


def drone(freq, seconds, amp=0.06):
    return [amp * (math.sin(2 * math.pi * freq * i / RATE) + 0.5 * math.sin(2 * math.pi * freq * 1.5 * i / RATE))
            for i in range(int(seconds * RATE))]


def build(name, tonic, bpm, phrase):
    beat = 60.0 / bpm
    melody = []
    for _ in range(3):
        for off, beats in phrase:
            melody += tone(tonic * 2 ** (off / 12), beats * beat)
    bed = drone(tonic / 2, len(melody) / RATE)
    total = len(melody)
    fade = int(2.5 * RATE)
    samples = []
    for i in range(total):
        g = min(1.0, i / fade, (total - i) / fade)
        samples.append(max(-1.0, min(1.0, (melody[i] + bed[i]) * g)))
    wav = OUT / f"{name}.wav"
    with wave.open(str(wav), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(s * 32000)) for s in samples))
    ogg = OUT / f"{name}.ogg"
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", str(wav), "-c:a", "libvorbis", "-q:a", "3", str(ogg)], check=True)
    wav.unlink()
    print(f"{ogg.name}: {total / RATE:.1f}s, {ogg.stat().st_size // 1024} KB")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    for k, (tonic, bpm, phrase) in TUNES.items():
        build(k, tonic, bpm, phrase)
