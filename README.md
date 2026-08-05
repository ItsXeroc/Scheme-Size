# !Scheme Size

Custom port of [Scheme Size](https://github.com/xzxADIxzx/Scheme-Size) by **Mithra**.

Original author: **xzxADIxzx**. Unofficial port - they are not involved.

## What it does

QoL and admin tooling on top of a larger schematic limit: building helpers, renderer options, AI, keybinds, CLaJ, console, rule setter.

- **Client only** - put the jar in `mods/`. Works on normal servers for QoL (PC and mobile).
- **With server mod** - same jar in `config/mods/` on the server. Admin tools sync in multiplayer when you are not hosting. Needs admin rank.
- Mindustry **v158+**. Vanilla recommended. Mobile is supported (use the release jar, not Desktop-only).

Repo: https://github.com/ItsXeroc/Scheme-Size

## Build

```bash
./build-all.sh
# or one target:
./gradlew deploy -PmindustryVersion=v158.1 -PgameBuild=158
```

Jars: `dist/Scheme-Size-v156.jar`, `Scheme-Size-v158.jar`, `Scheme-Size-v159.jar`
