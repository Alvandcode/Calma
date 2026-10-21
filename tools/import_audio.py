#!/usr/bin/env python3
"""
Calma - ambient sound importer.

Takes recordings you already own (or any WAV you downloaded) and turns them into
assets the app can loop forever without a click at the seam.

What it does
------------
1. Decodes the input (WAV natively; anything else via ffmpeg if it is installed).
2. Downmixes to mono, resamples to 22 050 Hz, 16-bit PCM.
3. Applies a loop cross-fade so the end of the file flows back into the beginning.
4. Removes DC offset and normalises the peak to -1.5 dBFS, so mixing four layers
   never clips.
5. Writes the result to app/src/main/res/raw/<name>.wav.

Usage
-----
    python tools/import_audio.py <input_dir> [--no-crossfade] [--keep-length]
    python tools/import_audio.py <input_dir> --dry-run

The output file name must match a sound id in
app/src/main/java/app/calma/audio/AmbientSound.kt, because the player resolves
the audio through `android.resource://<package>/raw/<id>`.

Recognised input formats
------------------------
    .wav  .wave  .aif  .aiff   -> handled natively
    .mp3  .ogg  .m4a  .flac   -> handled through ffmpeg (if available)

Requirements
------------
    numpy            (always)
    ffmpeg           (only for non-WAV input)
"""

from __future__ import annotations

import argparse
import io
import os
import subprocess
import sys
import wave
from dataclasses import dataclass

import numpy as np

SAMPLE_RATE = 22050
BIT_DEPTH = 16
PEAK_DBFS = -1.5
CROSSFADE_SECONDS = 1.5

NATIVE_EXTENSIONS = {".wav", ".wave", ".aif", ".aiff"}
FFMPEG_EXTENSIONS = {".mp3", ".ogg", ".m4a", ".aac", ".flac", ".opus", ".webm"}

# Sound ids that exist in AmbientSound.kt. The file name must match one of these.
KNOWN_SOUNDS = {
    "rain",
    "forest",
    "birds",
    "sea",
    "stream",
    "wind",
    "white_noise",
    "pink_noise",
    "brown_noise",
}


# --------------------------------------------------------------------------------------
# Decoding
# --------------------------------------------------------------------------------------
def decode_wav(path: str) -> tuple[np.ndarray, int]:
    with wave.open(path, "rb") as handle:
        channels = handle.getnchannels()
        width = handle.getsampwidth()
        rate = handle.getframerate()
        frames = handle.readframes(handle.getnframes())

    if width == 1:
        data = np.frombuffer(frames, dtype=np.uint8).astype(np.float32)
        data = (data - 128.0) / 128.0
    elif width == 2:
        data = np.frombuffer(frames, dtype="<i2").astype(np.float32) / 32768.0
    elif width == 3:
        raw = np.frombuffer(frames, dtype=np.uint8).reshape(-1, 3).astype(np.int32)
        value = (raw[:, 0] | (raw[:, 1] << 8) | (raw[:, 2] << 16))
        value = np.where(value & 0x800000, value - 0x1000000, value)
        data = value.astype(np.float32) / 8388608.0
    elif width == 4:
        data = np.frombuffer(frames, dtype="<i4").astype(np.float32) / 2147483648.0
    else:
        raise ValueError(f"unsupported sample width: {width} bytes")

    if channels > 1:
        data = data.reshape(-1, channels).mean(axis=1)

    return data.astype(np.float64), rate


def decode_with_ffmpeg(path: str) -> tuple[np.ndarray, int]:
    command = [
        "ffmpeg", "-hide_banner", "-loglevel", "error",
        "-i", path,
        "-f", "wav", "-ac", "1", "-ar", str(SAMPLE_RATE),
        "-acodec", "pcm_s16le", "-",
    ]
    result = subprocess.run(command, capture_output=True, check=True)
    with wave.open(io.BytesIO(result.stdout), "rb") as handle:
        rate = handle.getframerate()
        frames = handle.readframes(handle.getnframes())
    data = np.frombuffer(frames, dtype="<i2").astype(np.float64) / 32768.0
    return data, rate


def decode(path: str) -> tuple[np.ndarray, int]:
    extension = os.path.splitext(path)[1].lower()
    if extension in NATIVE_EXTENSIONS:
        return decode_wav(path)
    if extension in FFMPEG_EXTENSIONS:
        return decode_with_ffmpeg(path)
    raise ValueError(f"unsupported file type: {extension}")


# --------------------------------------------------------------------------------------
# Processing
# --------------------------------------------------------------------------------------
def resample(signal: np.ndarray, source_rate: int) -> np.ndarray:
    if source_rate == SAMPLE_RATE:
        return signal
    if source_rate <= 0:
        raise ValueError("invalid sample rate")
    target_length = int(round(signal.shape[0] * SAMPLE_RATE / source_rate))
    if target_length < 1:
        raise ValueError("audio too short")
    source_positions = np.linspace(0.0, signal.shape[0] - 1.0, num=target_length)
    return np.interp(source_positions, np.arange(signal.shape[0]), signal)


def loop_crossfade(signal: np.ndarray, seconds: float) -> np.ndarray:
    """
    Makes the recording loopable.

    The last `seconds` of audio are cross-faded over the first `seconds`, so the
    player can jump from the last sample back to the first one without a click.
    """
    total = signal.shape[0]
    fade = int(seconds * SAMPLE_RATE)
    if fade <= 0 or total < 4 * fade:
        return signal

    head = signal[:fade]
    tail = signal[total - fade:]
    middle = signal[fade:total - fade]

    # Equal-power cross-fade: the sum of the two squared weights stays constant,
    # so the volume does not dip in the middle of the blend.
    t = np.linspace(0.0, np.pi / 2.0, fade)
    fade_in = np.sin(t)
    fade_out = np.cos(t)
    blend = head * fade_in + tail * fade_out

    return np.concatenate([blend, middle])


def normalise(signal: np.ndarray, peak_dbfs: float = PEAK_DBFS) -> np.ndarray:
    mean = float(np.mean(signal))
    if abs(mean) > 1e-9:
        signal = signal - mean
    peak = float(np.max(np.abs(signal)))
    if peak < 1e-9:
        return signal
    target = 10.0 ** (peak_dbfs / 20.0)
    return signal * (target / peak)


def write_wav(path: str, signal: np.ndarray) -> None:
    data = np.clip(signal, -1.0, 1.0)
    pcm = (data * 32767.0).astype("<i2")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with wave.open(path, "wb") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(BIT_DEPTH // 8)
        handle.setframerate(SAMPLE_RATE)
        handle.writeframes(pcm.tobytes())


# --------------------------------------------------------------------------------------
# Driver
# --------------------------------------------------------------------------------------
@dataclass
class Report:
    source: str
    target: str
    seconds_before: float
    seconds_after: float
    size_kb: float
    warning: str | None = None


def default_output_dir() -> str:
    return os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "app", "src", "main", "res", "raw",
    )


def process_file(path: str, out_dir: str, crossfade: bool, dry_run: bool) -> Report:
    name = os.path.splitext(os.path.basename(path))[0].lower()
    if name not in KNOWN_SOUNDS:
        warning = (
            f"'{name}' is not a known sound id. Add it to AmbientSound.kt "
            f"(and to strings.xml) or rename the file."
        )
    else:
        warning = None

    signal, rate = decode(path)
    seconds_before = signal.shape[0] / rate
    signal = resample(signal, rate)

    if crossfade:
        signal = loop_crossfade(signal, CROSSFADE_SECONDS)

    signal = normalise(signal)

    target = os.path.join(out_dir, f"{name}.wav")
    seconds_after = signal.shape[0] / SAMPLE_RATE

    if not dry_run:
        write_wav(target, signal)

    size_kb = 0.0
    if os.path.exists(target):
        size_kb = os.path.getsize(target) / 1024.0

    return Report(
        source=os.path.basename(path),
        target=os.path.basename(target),
        seconds_before=seconds_before,
        seconds_after=seconds_after,
        size_kb=size_kb,
        warning=warning,
    )


def verify_folder(out_dir: str) -> int:
    """Reports duration, peak level and loop continuity of every asset in the folder."""
    files = sorted(
        os.path.join(out_dir, name)
        for name in os.listdir(out_dir)
        if os.path.splitext(name)[1].lower() == ".wav"
    )
    if not files:
        print(f"No .wav files in {out_dir}")
        return 1

    print(f"{'file':<22}{'length':>9}{'peak dBFS':>11}{'seam':>9}  status")
    problems = 0
    for path in files:
        signal, rate = decode(path)
        seconds = signal.shape[0] / rate
        peak = 20.0 * np.log10(max(float(np.max(np.abs(signal))), 1e-9))
        seam = abs(signal[0] - signal[-1])
        typical = float(np.mean(np.abs(np.diff(signal)))) if signal.shape[0] > 1 else 0.0

        issues = []
        if rate != SAMPLE_RATE:
            issues.append(f"rate {rate} != {SAMPLE_RATE}")
        if seconds < 2.0:
            issues.append("too short")
        if seam > max(typical * 6.0, 0.05):
            issues.append("click at loop point")
        if peak > -0.5:
            issues.append("clipping risk")

        status = "ok" if not issues else ", ".join(issues)
        if issues:
            problems += 1
        print(
            f"  {os.path.basename(path):<20}{seconds:>8.1f}s{peak:>11.2f}"
            f"{seam:>9.4f}  {status}"
        )

    print()
    if problems:
        print(f"{problems} file(s) need attention.")
    else:
        print("All assets look good.")
    return 0 if problems == 0 else 1


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Import ambient sounds into Calma.")
    parser.add_argument("input_dir", nargs="?", help="folder containing the source recordings")
    parser.add_argument("--out", default=None, help="output folder (default: res/raw)")
    parser.add_argument(
        "--verify",
        action="store_true",
        help="check the assets in the output folder instead of importing",
    )
    parser.add_argument(
        "--no-crossfade",
        action="store_true",
        help="skip the loop cross-fade (use for already-seamless loops)",
    )
    parser.add_argument("--dry-run", action="store_true", help="only report, do not write")
    args = parser.parse_args(argv[1:])

    out_dir = args.out or default_output_dir()

    if args.verify:
        return verify_folder(out_dir)

    if not args.input_dir:
        parser.error("input_dir is required unless --verify is used")
    if not os.path.isdir(args.input_dir):
        print(f"Not a folder: {args.input_dir}")
        return 1
    files = sorted(
        os.path.join(args.input_dir, name)
        for name in os.listdir(args.input_dir)
        if os.path.splitext(name)[1].lower() in NATIVE_EXTENSIONS | FFMPEG_EXTENSIONS
    )

    if not files:
        print(f"No audio files found in {args.input_dir}")
        return 1

    reports: list[Report] = []
    for path in files:
        try:
            reports.append(
                process_file(
                    path,
                    out_dir,
                    crossfade=not args.no_crossfade,
                    dry_run=args.dry_run,
                )
            )
        except FileNotFoundError as exc:
            if "ffmpeg" in str(exc).lower():
                print("ffmpeg is required for non-WAV input but was not found.")
                print("Install ffmpeg, or convert the file to WAV first.")
                return 1
            print(f"Failed on {os.path.basename(path)}: {exc}")
            return 1
        except Exception as exc:  # noqa: BLE001 - report and continue
            print(f"Failed on {os.path.basename(path)}: {exc}")
            return 1

    print()
    for report in reports:
        print(
            f"  {report.source:<20} -> {report.target:<20} "
            f"{report.seconds_before:>6.1f}s -> {report.seconds_after:>6.1f}s  "
            f"{report.size_kb:>8.1f} KiB"
        )
        if report.warning:
            print(f"      ! {report.warning}")

    verb = "Would write" if args.dry_run else "Wrote"
    print(f"\n{verb} {len(reports)} file(s) to {out_dir}")
    print("Next: rebuild and commit the .wav files.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
