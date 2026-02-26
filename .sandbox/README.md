<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Sandbox

Personal containerized development environment for eVaka. Runs a sandboxed
Ubuntu container with Docker-in-Docker, matching your host user's UID/GID.

## Prerequisites

- Docker
- Node.js (for running the management script)

## Quick start

```sh
mise sandbox build
mise sandbox exec
```

On first `exec`, the image is built automatically if missing, and the container
is created and initialized (mise, frontend dependencies, Playwright).

## Commands

```
mise run sandbox build [--no-cache]     Build the container image
mise run sandbox exec [cmd...]          Run a command in the container (default: bash)
mise run sandbox stop                   Stop the container
mise run sandbox rm                     Remove the container
mise run sandbox recreate [--no-cache]  Remove, rebuild and recreate the container
```

## Configuration

Copy `sandbox.example.json` to `sandbox.json` and customize:

```json
{
  "mounts": ["~/.gitconfig"],
  "copies": [],
  "ports": ["3000", "9090", "9099"]
}
```

- **mounts** — Bind mounts into the container. Formats: `path`, `path:mode`,
  `src:dst`, `src:dst:mode`. Mode is either `ro` (read-only) or `rw`
  (read-write), defaults to `ro`. Paths support `~` expansion on both sides.
- **copies** — Files and directories copied into the container at creation.
  Formats: `path`, `src:dst`. Paths support `~` expansion on both sides.
  Useful for files and directories that need isolation from the host.
- **ports** — Ports to expose from the container to host. Format is
  `port` (same port on both sides), `host:container`.

`sandbox.json` is gitignored — each developer maintains their own.

## Post-create scripts

Post-create scripts are run in the container after it's started the first
time. Two scripts are available:

- `post-create.sh` — Shared setup for all developers.
- `post-create.local.sh` — Personal setup (gitignored), runs after the shared
  script. Use this to install your preferred tools, configs, etc.
