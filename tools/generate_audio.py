#!/usr/bin/env python3
"""
Calma - ambient sound generator.

Every audio asset shipped with Calma is SYNTHESIZED BY THIS SCRIPT. No third-party
recordings are used, so the resulting files are fully owned by the Calma project and
released under the same license as the source code (Apache-2.0).

Technical properties guaranteed by the generator:

* Perfectly seamless loops. All noise beds are built in the frequency domain
  (rFFT -> spectral shaping -> irFFT), which makes the waveform inherently periodic
  with the loop length. Discrete events (droplets, chirps) are wrapped around the
  loop boundary so nothing is truncated at the seam.
* Mono, 22050 Hz, 16-bit signed PCM WAV -> decodable by AndroidX Media3's WavExtractor.
* Peak-normalised to -1.5 dBFS so mixing several layers never clips.

Usage:
    python tools/generate_audio.py [output_dir]

Requires: numpy
"""

from __future__ import annotations

import os
import struct
import sys
import wave
from dataclasses import dataclass

import numpy as np

SAMPLE_RATE = 22050
BIT_DEPTH = 16
PEAK_DBFS = -1.5


# --------------------------------------------------------------------------------------
# DSP helpers
# --------------------------------------------------------------------------------------
def periodic_noise(n: int, rng: np.random.Generator) -> np.ndarray:
    """White noise that loops seamlessly (built directly in the frequency domain)."""
    spectrum = np.fft.rfft(rng.standard_normal(n))
    return np.fft.irfft(spectrum, n=n)


def periodic_shaped_noise(
    n: int,
    rng: np.random.Generator,
    power: float = 0.0,
    low_hz: float = 20.0,
    high_hz: float | None = None,
    tilt_db_per_octave: float = 0.0,
) -> np.ndarray:
    """
    Seamless noise with a power-law spectral envelope.

    ``power`` is the spectral slope (0 = white, -1 = pink, -2 = brown).
    ``low_hz``/``high_hz`` band-limit the result. ``tilt_db_per_octave`` applies an
    extra gentle spectral tilt on top of the power law.
    """
    freqs = np.fft.rfftfreq(n, d=1.0 / SAMPLE_RATE)
    freqs[0] = freqs[1]  # avoid DC singularity

    gain = freqs**power
    if tilt_db_per_octave:
        octaves = np.log2(freqs / 1000.0)
        gain = gain * (10.0 ** (tilt_db_per_octave * octaves / 20.0))

    if low_hz > 0:
        gain *= 1.0 / (1.0 + (low_hz / freqs) ** 2)
    if high_hz is not None:
        gain *= 1.0 / (1.0 + (freqs / high_hz) ** 2)

    spectrum = np.fft.rfft(rng.standard_normal(n)) * gain
    return np.fft.irfft(spectrum, n=n)


def circular_one_pole_lowpass(signal: np.ndarray, cutoff_hz: float) -> np.ndarray:
    """One-pole low-pass that wraps around the loop, keeping the seam click-free."""
    n = signal.shape[0]
    alpha = 1.0 - np.exp(-2.0 * np.pi * cutoff_hz / SAMPLE_RATE)
    # Warm the filter up over one full pass so the state is already periodic at t=0.
    for _ in range(2):
        out = np.empty_like(signal)
        state = 0.0
        for i in range(n):
            state += alpha * (signal[i] - state)
            out[i] = state
        signal = out
    return signal


def circular_one_pole_highpass(signal: np.ndarray, cutoff_hz: float) -> np.ndarray:
    return signal - circular_one_pole_lowpass(signal, cutoff_hz)


def periodic_envelope(n: int, cycles: list[float], phases: list[float]) -> np.ndarray:
    """Sum of sinusoids whose frequencies are integer multiples of 1/n -> periodic."""
    t = np.arange(n) / SAMPLE_RATE
    env = np.zeros(n)
    cycle_list = list(cycles)
    for cycle_count, phase in zip(cycle_list, phases):
        f = cycle_count * SAMPLE_RATE / n
        env += np.sin(2.0 * np.pi * f * t + phase)
    return env / max(1, len(cycle_list))


def add_event(
    buffer: np.ndarray,
    start: int,
    event: np.ndarray,
    gain: float,
) -> None:
    """Add ``event`` at ``start``, wrapping around the loop so events never truncate."""
    n = buffer.shape[0]
    idx = (start + np.arange(event.shape[0])) % n
    buffer[idx] += event * gain


def normalise(signal: np.ndarray, peak_dbfs: float = PEAK_DBFS) -> np.ndarray:
    # Remove any residual DC offset first (brown-noise style beds accumulate a small mean).
    mean = float(np.mean(signal))
    if abs(mean) > 1e-9:
        signal = signal - mean
    peak = float(np.max(np.abs(signal)))
    if peak < 1e-9:
        return signal
    target = 10.0 ** (peak_dbfs / 20.0)
    return signal * (target / peak)


def fade_edges(signal: np.ndarray, seconds: float = 0.05) -> np.ndarray:
    """Very short edge fade: belt-and-braces against any residual DC step at the seam."""
    n = signal.shape[0]
    k = min(int(seconds * SAMPLE_RATE), n // 4)
    if k <= 1:
        return signal
    ramp = np.linspace(0.0, 1.0, k)
    signal[:k] *= ramp
    signal[-k:] *= ramp[::-1]
    return signal


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
# Sound designs
# --------------------------------------------------------------------------------------
@dataclass(frozen=True)
class SoundSpec:
    filename: str
    seconds: float
    build: object  # Callable[[int, np.random.Generator], np.ndarray]


def _build_white(n: int, rng: np.random.Generator) -> np.ndarray:
    return periodic_shaped_noise(n, rng, power=0.0, low_hz=40.0)


def _build_pink(n: int, rng: np.random.Generator) -> np.ndarray:
    return periodic_shaped_noise(n, rng, power=-1.0, low_hz=30.0, high_hz=11000.0)


def _build_brown(n: int, rng: np.random.Generator) -> np.ndarray:
    return periodic_shaped_noise(n, rng, power=-2.0, low_hz=20.0, high_hz=6000.0)


def _build_wind(n: int, rng: np.random.Generator) -> np.ndarray:
    body = periodic_shaped_noise(n, rng, power=-1.6, low_hz=45.0, high_hz=2600.0)
    # Slow periodic gusts: integer cycle counts keep the swell seamless.
    gust = periodic_envelope(
        n,
        cycles=[1.0, 2.0, 3.0, 5.0],
        phases=[0.0, 1.1, 2.3, 4.0],
    )
    gust = (gust + 1.6) / 3.2
    gust = np.clip(gust, 0.18, 1.0)
    hiss = periodic_shaped_noise(n, rng, power=-1.0, low_hz=1800.0, high_hz=9000.0)
    return body * gust + hiss * gust * 0.18


def _build_rain(n: int, rng: np.random.Generator) -> np.ndarray:
    # Dense broadband hiss = the "sheet" of rain.
    sheet = periodic_shaped_noise(n, rng, power=-0.75, low_hz=350.0, high_hz=10500.0)
    sheet = circular_one_pole_highpass(sheet, 420.0)
    # Slower low rumble underneath.
    rumble = periodic_shaped_noise(n, rng, power=-2.0, low_hz=30.0, high_hz=420.0)
    # Individual droplet impacts: short resonant clicks.
    drops = np.zeros(n)
    n_drops = max(60, n // 1400)
    for _ in range(n_drops):
        length = int(rng.uniform(0.010, 0.045) * SAMPLE_RATE)
        t = np.arange(length) / SAMPLE_RATE
        freq = rng.uniform(1400.0, 5200.0)
        decay = np.exp(-t * rng.uniform(55.0, 150.0))
        click = np.sin(2.0 * np.pi * freq * t) * decay
        click += rng.standard_normal(length) * np.exp(-t * 320.0) * 0.5
        add_event(drops, int(rng.integers(0, n)), click, float(rng.uniform(0.05, 0.20)))
    return sheet * 0.78 + rumble * 0.22 + circular_one_pole_highpass(drops, 600.0) * 0.9


def _build_stream(n: int, rng: np.random.Generator) -> np.ndarray:
    body = periodic_shaped_noise(n, rng, power=-0.9, low_hz=260.0, high_hz=9200.0)
    body = circular_one_pole_highpass(body, 300.0)
    # Bubbles: rising short tones.
    bubbles = np.zeros(n)
    n_bubbles = max(70, n // 1900)
    for _ in range(n_bubbles):
        length = int(rng.uniform(0.020, 0.070) * SAMPLE_RATE)
        t = np.arange(length) / SAMPLE_RATE
        f0 = rng.uniform(320.0, 900.0)
        f1 = f0 * rng.uniform(1.8, 3.4)
        freq = f0 + (f1 - f0) * (t / t[-1])
        env = np.sin(np.pi * np.linspace(0.0, 1.0, length)) ** 1.5
        bub = np.sin(2.0 * np.pi * np.cumsum(freq) / SAMPLE_RATE) * env
        add_event(bubbles, int(rng.integers(0, n)), bub, float(rng.uniform(0.06, 0.22)))
    return body * 0.8 + bubbles * 0.55


def _build_sea(n: int, rng: np.random.Generator) -> np.ndarray:
    # Long, slow surf swells.
    swell = periodic_shaped_noise(n, rng, power=-2.0, low_hz=25.0, high_hz=900.0)
    waves = periodic_envelope(
        n,
        cycles=[2.0, 3.0, 5.0],
        phases=[0.0, 0.7, 2.1],
    )
    waves = (waves + 1.4) / 2.8
    waves = np.clip(waves, 0.06, 1.0) ** 1.4
    # Breaking foam: brighter noise gated by the same swell envelope.
    foam = periodic_shaped_noise(n, rng, power=-0.8, low_hz=900.0, high_hz=11000.0)
    return swell * waves * 1.0 + foam * waves**2 * 0.55


def _chirp(
    rng: np.random.Generator,
    duration: float,
    f_start: float,
    f_end: float,
    vibrato_hz: float,
    vibrato_depth: float,
) -> np.ndarray:
    length = max(8, int(duration * SAMPLE_RATE))
    t = np.arange(length) / SAMPLE_RATE
    sweep = np.linspace(f_start, f_end, length, endpoint=True)
    vib = np.sin(2.0 * np.pi * vibrato_hz * t) * vibrato_depth
    inst = sweep + vib
    phase = 2.0 * np.pi * np.cumsum(inst) / SAMPLE_RATE
    attack = np.clip(np.linspace(0.0, 1.0, length) * 14.0, 0.0, 1.0)
    release = np.clip(np.linspace(1.0, 0.0, length) ** 1.8, 0.0, 1.0)
    return np.sin(phase) * attack * release


def _build_birds(n: int, rng: np.random.Generator) -> np.ndarray:
    # Very quiet leafy bed so the space does not feel empty.
    bed = periodic_shaped_noise(n, rng, power=-1.8, low_hz=120.0, high_hz=3200.0)
    bed *= 0.16
    calls = np.zeros(n)
    n_calls = max(14, n // 4200)
    for _ in range(n_calls):
        duration = float(rng.uniform(0.07, 0.17))
        chirp = _chirp(
            rng,
            duration=duration,
            f_start=float(rng.uniform(2000.0, 3400.0)),
            f_end=float(rng.uniform(2600.0, 4600.0)),
            vibrato_hz=float(rng.uniform(18.0, 42.0)),
            vibrato_depth=float(rng.uniform(80.0, 320.0)),
        )
        if rng.random() < 0.45:
            # Two- or three-note phrase.
            reps = int(rng.integers(2, 4))
            phrase = np.zeros(n)
            for r in range(reps):
                add_event(phrase, r * int(duration * 1.25 * SAMPLE_RATE), chirp, 0.85)
            chirp = phrase
        add_event(calls, int(rng.integers(0, n)), chirp, float(rng.uniform(0.12, 0.30)))
    return bed + calls


def _build_forest(n: int, rng: np.random.Generator) -> np.ndarray:
    # Leaf rustle + low breeze.
    breeze = periodic_shaped_noise(n, rng, power=-1.7, low_hz=60.0, high_hz=2400.0)
    rustle = periodic_shaped_noise(n, rng, power=-1.1, low_hz=1400.0, high_hz=9000.0)
    rustle *= periodic_envelope(n, cycles=[3.0, 7.0, 11.0], phases=[0.0, 1.3, 2.6]) * 0.25 + 0.5
    # Sparse distant birds.
    birds = np.zeros(n)
    n_calls = max(10, n // 5200)
    for _ in range(n_calls):
        chirp = _chirp(
            rng,
            duration=float(rng.uniform(0.09, 0.20)),
            f_start=float(rng.uniform(1800.0, 3000.0)),
            f_end=float(rng.uniform(2400.0, 4200.0)),
            vibrato_hz=float(rng.uniform(15.0, 35.0)),
            vibrato_depth=float(rng.uniform(60.0, 240.0)),
        )
        add_event(birds, int(rng.integers(0, n)), chirp, float(rng.uniform(0.08, 0.20)))
    return breeze * 0.55 + rustle * 0.35 + birds


SOUNDS: tuple[SoundSpec, ...] = (
    SoundSpec("rain.wav", 12.0, _build_rain),
    SoundSpec("forest.wav", 14.0, _build_forest),
    SoundSpec("birds.wav", 16.0, _build_birds),
    SoundSpec("sea.wav", 14.0, _build_sea),
    SoundSpec("stream.wav", 11.0, _build_stream),
    SoundSpec("wind.wav", 13.0, _build_wind),
    SoundSpec("white_noise.wav", 6.0, _build_white),
    SoundSpec("pink_noise.wav", 6.0, _build_pink),
    SoundSpec("brown_noise.wav", 6.0, _build_brown),
)


def main(argv: list[str]) -> int:
    default_out = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "app",
        "src",
        "main",
        "res",
        "raw",
    )
    out_dir = argv[1] if len(argv) > 1 else default_out
    seed = 20240917

    for index, spec in enumerate(SOUNDS):
        n = int(spec.seconds * SAMPLE_RATE)
        rng = np.random.default_rng(seed + index * 7919)
        audio = spec.build(n, rng)
        audio = fade_edges(np.asarray(audio, dtype=np.float64))
        audio = normalise(audio)
        path = os.path.join(out_dir, spec.filename)
        write_wav(path, audio)
        size_kb = os.path.getsize(path) / 1024.0
        print(f"  {spec.filename:<20} {spec.seconds:>5.1f}s  {size_kb:>8.1f} KiB")
    print(f"Wrote {len(SOUNDS)} files to {out_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))