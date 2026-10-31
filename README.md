# Custom Sound Overrides — Fabric 26.3

Install `custom-sounds-fabric-26.3-0.1.0.jar` on the client. No server installation or bundled audio is needed.

The first launch creates `config/customsounds/`. Add your own Ogg Vorbis files named after registered sound event IDs:

- `entity.creeper.primed.ogg` replaces `minecraft:entity.creeper.primed`.
- `block.chest.open.ogg` replaces `minecraft:block.chest.open`.
- `modid__registered.event.ogg` replaces `modid:registered.event` when that mod actually registers the event.

Reload with F3+T after adding, changing or removing files. Removing a replacement restores the original sound. Unknown events, invalid identifiers and invalid Vorbis files are skipped and logged. Other Ogg codecs such as Opus are not supported.

A generated overlay lives under `.generated` inside the configuration folder. The mod never modifies Minecraft's installed assets or your source audio. This directory is rebuilt automatically; keep your audio files directly in `config/customsounds/`.

Build using Java 25 and `./gradlew build` (Windows: `gradlew.bat build`) (Gradle 9.7.1). The sound overlay's file validation, pack metadata, replacement and removal were checked without a graphical client. Live playback and F3+T still need a graphical client playtest.
