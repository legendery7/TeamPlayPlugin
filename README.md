# TeamPlayPlugin

Domain-oriented game mechanics module for team-based multiplayer sessions on the Paper/Spigot server runtime.

## Overview

TeamPlayPlugin implements team lifecycle and progression features used in a custom multiplayer game mode: teams, upgrades, PvP rules, chat, resurrection/spectator flow, and related managers.

This repository is a **software module** for a real-time multiplayer platform. The Minecraft server API is the runtime — the engineering focus is modular game logic, state management, and clear package boundaries.

## Architecture

Package-based modular design under `org.legendery.teamPlayPlugin`:

```
commands/     → player/admin command handlers
team/         → team aggregate & related flows
upgrades/     → progression / unlock managers
manager/      → cross-cutting gameplay services
constructor/  → upgrade model construction
zapret/       → gated unlock upgrades (crafting, enchanting, effects, …)
prepire/      → preparation-phase craft restrictions
```

Typical flow:

```
Command / Event  →  Manager  →  Team / Upgrade state  →  Persistence (DatabaseManager)
```

## Features

- Team create / join / info flows
- Team chat
- Upgrade tree (protection, magnet blocks, bonus upgrades, gated crafts)
- PvP management helpers
- Death / spectator related handlers
- Enemy compass utility
- Preparation-phase craft restrictions

## Tech stack

- Java
- Paper / Spigot API
- Maven
- YAML configuration (`config.yml`, `plugin.yml`)

## Project structure

```
TeamPlayPlugin/
├── pom.xml
├── README.md
└── src/main/
    ├── java/org/legendery/teamPlayPlugin/
    └── resources/
        ├── config.yml
        └── plugin.yml
```

## Getting started

Requirements: JDK compatible with the project POM, Maven, a Paper/Spigot server for runtime testing.

```bash
mvn clean package
```

Copy the built JAR from `target/` into the server `plugins/` directory and restart.

## Roadmap

- [ ] Rename unclear packages (`prepire`, `zapret`) to English domain names
- [ ] Extract repository interfaces over `DatabaseManager`
- [ ] Document commands and permissions in `/docs`
- [ ] Add unit tests for upgrade unlock rules
- [ ] Remove IDE metadata from version control

## License

All rights reserved unless otherwise stated by the author.
