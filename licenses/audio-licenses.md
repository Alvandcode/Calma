# Audio Asset Licenses

All ambient sound files shipped with Calma are **procedurally generated** by the script `tools/generate_audio.py` using NumPy. No third-party recordings or samples are used.

## Generation Method

Each sound is synthesized entirely in software using digital signal processing techniques:

- **White / Pink / Brown noise**: Spectral shaping of Gaussian white noise via inverse FFT.
- **Rain / Stream / Wind / Sea**: Filtered noise beds with event-based transients (droplets, bubbles, gusts, waves) placed with circular wrapping for seamless looping.
- **Forest / Birds**: Low-amplitude noise bed + sparse FM-synthesized bird chirps with vibrato, wrapped at loop boundaries.
- **All loops**: Constructed in the frequency domain (rFFT → spectral envelope → irFFT) to guarantee perfect periodicity; discrete events are added with modulo indexing so they wrap seamlessly.

## License

All generated audio files are released under the **same license as the source code** (Apache-2.0). You are free to use, modify, and redistribute them under the terms of that license.

## File Listing

| File | Duration | Description |
|------|----------|-------------|
| `rain.wav` | 12 s | Gentle rain with droplet transients |
| `forest.wav` | 14 s | Forest ambience with distant birds |
| `birds.wav` | 16 s | Sparse bird chirps |
| `sea.wav` | 14 s | Ocean waves with surf |
| `stream.wav` | 11 s | Flowing stream with bubbles |
| `wind.wav` | 13 s | Wind with gusts |
| `white_noise.wav` | 6 s | Flat-spectrum noise |
| `pink_noise.wav` | 6 s | 1/f noise |
| `brown_noise.wav` | 6 s | 1/f² noise |

## Technical Details

- Format: WAV, 16-bit PCM, mono, 22 050 Hz
- Loop points: Perfectly seamless (zero-crossing at boundary by construction)
- Peak normalization: −1.5 dBFS (safe for mixing up to 4 layers)
- Generated with: Python 3.12 + NumPy 2.5.3

## Regeneration

To regenerate the audio assets:

```bash
python tools/generate_audio.py
```

The script writes directly to `app/src/main/res/raw/`. Commit the updated `.wav` files along with any script changes.