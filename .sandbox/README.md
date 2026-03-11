<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Sandbox

Personal sandboxed development environment for eVaka. Two backends are
available:

- **sandbox** — Docker container with Docker-in-Docker
- **vmbox** — Colima VM (macOS only, uses Virtualization.framework)

Both use the same `sandbox.json` configuration and post-create scripts.

## Prerequisites

- **sandbox**: Docker, Node.js
- **vmbox**: [Colima](https://github.com/abiosoft/colima), Node.js

## Quick start

```sh
# Docker container
mise sandbox exec

# Colima VM
mise vmbox exec
```

On first `exec`, the environment is created and initialized automatically
(mise, frontend dependencies, Playwright).

## Commands

### sandbox (Docker)

```
mise sandbox build [--no-cache]     Build the container image
mise sandbox exec [cmd...]          Run a command in the container (default: bash)
mise sandbox stop                   Stop the container
mise sandbox rm                     Remove the container
mise sandbox recreate [--no-cache]  Remove, rebuild and recreate the container
```

### vmbox (Colima VM)

```
mise vmbox create                   Create and provision the VM
mise vmbox exec [cmd...]            Run a command in the VM (default: bash)
mise vmbox stop                     Stop the VM
mise vmbox rm                       Remove the VM and all data
mise vmbox recreate                 Remove and recreate the VM
mise vmbox status                   Show VM status
```

## Configuration

Copy `sandbox.example.json` to `sandbox.json` and customize:

```json
{
  "cpu": 8,
  "memory": 12,
  "disk": 160,
  "mounts": ["~/.gitconfig"],
  "copies": [],
  "ports": ["3000", "9090", "9099"]
}
```

- **cpu**, **memory**, **disk** — VM resources (vmbox only). Memory in GiB,
  disk in GiB.
- **mounts** — Bind mounts into the environment. Formats: `path`, `path:mode`,
  `src:dst`, `src:dst:mode`. Mode is either `ro` (read-only) or `rw`
  (read-write), defaults to `ro`. Paths support `~` expansion.
  **Note:** vmbox only supports directory mounts (use `copies` for files) and
  does not support remapping mount destinations (`src:dst` where `dst` differs
  from `src`), because Colima mounts host directories at their original paths.
- **copies** — Files and directories copied into the environment at creation.
  Formats: `path`, `src:dst`. Paths support `~` expansion.
  Useful for files that need isolation from the host.
- **ports** — Ports to expose to the host. Format is `port` (same port on
  both sides) or `host:container`. For sandbox, these are Docker port
  mappings. For vmbox, these are SSH port forwards.
- **notify** — Shell command to run on the host when a notification is
  received. A TCP listener on port 39813 is started automatically on `exec`.
  Trigger from inside with `echo notify > /dev/tcp/<host>/39813`
  (use `host.docker.internal` in sandbox, or the host IP in vmbox).

`sandbox.json` is gitignored — each developer maintains their own.

## Post-create scripts

Post-create scripts run in the project root after the environment is created.
Two scripts are available:

- `post-create.sh` — Shared setup for all developers.
- `post-create.local.sh` — Personal setup (gitignored), runs after the shared
  script. Use this to install your preferred tools, configs, etc.
