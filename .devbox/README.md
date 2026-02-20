<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Devbox

Personal containerized development environment for eVaka. Runs a sandboxed
Ubuntu container with Docker-in-Docker, matching your host user's UID/GID.

## Prerequisites

- Docker
- Node.js (for running the management script)

## Quick start

```sh
mise devbox build
mise devbox exec
```

On first `exec`, the image is built automatically if missing, and the container
is created and initialized (mise, frontend dependencies, Playwright).

## Commands

```
mise run devbox build [--no-cache]     Build the container image
mise run devbox exec [cmd...]          Run a command in the container (default: bash)
mise run devbox recreate [--no-cache]  Remove, rebuild and recreate the container
```

## Configuration

Copy `devbox.example.json` to `devbox.json` and customize:

```json
{
  "mounts": ["~/.gitconfig"],
  "copies": [],
  "ports": ["3000", "9090", "9099"]
}
```

- **mounts** — Bind mounts into the container. Formats: `path`, `path:mode`,
  `src:dst`, `src:dst:mode`. Mode is either `ro` or `rw`, defaults to `ro`.
  Paths support `~` expansion on both sides.
- **copies** — Files copied into the container at creation. Formats: `path`,
  `src:dst`. Useful for credential files that need isolation from the host.
- **ports** — Ports to expose from the container to host. Format is
  `host:container`. Plain `3000` maps to `3000:3000`.

`devbox.json` is gitignored — each developer maintains their own.

## Post-create scripts

- `post-create.sh` — Shared setup, runs on every `create`/`recreate`.
- `post-create.local.sh` — Personal setup (gitignored), runs after the shared
  script. Use this for your editor, tools, etc.
