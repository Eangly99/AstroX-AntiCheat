# AstroX AntiCheat

AstroX AntiCheat is a Geyser extension that performs packet-level anti-cheat analysis for Minecraft: Bedrock Edition clients before their traffic is translated to Java. It targets common Bedrock cheat behaviors (movement, reach, and packet abuse) while prioritizing performance and low latency on high-load servers.

## Features
- Packet interception for upstream (Bedrock) and downstream (Java) traffic
- Modular check system with per-check configuration
- Async, striped execution model to avoid blocking Netty I/O
- Bedrock-aware movement prediction and combat reach heuristics
- Statistical buffer (z-score) to reduce false positives under jitter

## Requirements
- Java 17+
- Geyser API 2.9.2-SNAPSHOT (extension API)

## Build
```bash
gradle build
```
The output JAR will be in `build/libs/`.

## Install
1. Copy the built JAR to your Geyser `extensions/` folder.
2. Start/reload Geyser.
3. Edit `config.yml` in the extension data folder to tune checks.

## Configuration
See `src/main/resources/config.yml` for default options. Each check can be enabled/disabled and tuned (thresholds, buffers, and per-check parameters).

## Project Structure
- `dev.naruto.astrox.AstroX` — extension entry point
- `dev.naruto.astrox.packet` — packet interception + listeners
- `dev.naruto.astrox.check` — checks and sample models
- `dev.naruto.astrox.player` — per-player state and violation tracking
- `dev.naruto.astrox.engine` — movement prediction engine

## Notes
- This extension relies on internal Geyser classes for packet access; keep Geyser core and API versions aligned.
- Tuning is essential for your server’s latency profile and player base.

## License
MIT — see `LICENSE`.
