# Hilal Thursday alarm audio — origin and distribution notes

The three bundled audio files are short, 23-second musical excerpts, prepared from MP3 recordings the app maintainer supplied for this project. The recordings were generated in Google's Gemini music tool using original user-directed prompts. The files were converted to Ogg Vorbis for efficient offline Android playback; they are **not** the previously rejected procedurally synthesized notification sounds.

| In-app label | Android resource | Source preview |
|---|---|---|
| Hilal Noor | `app/src/main/res/raw/hilal_noor.ogg` | `Hilal_Noor_Preview_23s.mp3` |
| Sukoon | `app/src/main/res/raw/sukoon.ogg` | `Sukoon_Preview_23s.mp3` |
| Hilal Nida | `app/src/main/res/raw/hilal_nida.ogg` | `Hilal_Nida_Preview_23s.mp3` |

All three are approx. 23 seconds, stereo, 44.1 kHz, Ogg Vorbis. The source MP3s are intentionally *not* committed to avoid distributing redundant large files. No religious recitations or vocals were deliberately included in the prompts.

**Rights and reuse:** The repository's MIT software license is not intended to represent an independently verified copyright or sublicensing grant for these externally generated audio assets. Google's terms say it does not claim ownership in original content generated using its services, and Gemini allows track downloads/sharing; that does not establish exclusive rights or guarantee non-infringement. The maintainer supplied and authorized their inclusion in this open-source application. Any third-party reuse or commercialization should independently assess applicable rights and terms.

**Usage:** The default is Hilal Noor. An in-app picker offers Sukoon, Hilal Nida and device system alarm tones. Sound preview and ringing alarm share the source-resolution logic. Changing sound selection never changes Android's system-wide alarm tone or the app's reminder schedule. The existing two-minute ringing limit and Snooze controls remain unchanged.
