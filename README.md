# !Scheme Size

Custom port by **Mithra**, based on [Scheme Size](https://github.com/xzxADIxzx/Scheme-Size) (`xzxADIxzx`). Unofficial - the original author is not involved.

You already know what Scheme Size is. This page is about **what this port changes**.

## What’s different here

### Server-side admin tools

In the original ports, most admin actions (fill, items, units, teleport, rules, …) only worked if you were the local host. Remote clients on a dedicated server were stuck.

This build ships **server handlers in the same jar**. Put it in `config/mods` on the server and on the client. Admins can use **Scheme Net** over the network without hosting locally. Actions require real admin rank (`player.admin`).

Without the server jar, it still works as a normal client QoL mod.

### Headless / dedicated server

Older Scheme Size jars crash when loaded on a dedicated server (`schematics` null, `netClient` null, `main.js` touching a null loader). This port is safe on headless: server-only init path, no client UI/scripts on the server.

### One jar, modern builds

Single `Scheme-Size.jar` for Mindustry **156–159** (desktop + mobile). API differences (file chooser, labels, …) go through a small compat layer instead of shipping three separate builds

## Install

1. Download `Scheme-Size.jar` from [Releases](https://github.com/ItsXeroc/Scheme-Size/releases)
2. Client: `mods/`
3. For networked admin tools: same jar in server `config/mods/`
4. Enable Admin Tools → Auto or Scheme Net (admin only)



## Build

```bash
./build-all.sh
```

Output: `dist/Scheme-Size.jar`

## Credit

- Original mod: [xzxADIxzx/Scheme-Size](https://github.com/xzxADIxzx/Scheme-Size)
- This port: Mithra ([ItsXeroc/Scheme-Size](https://github.com/ItsXeroc/Scheme-Size))

