<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Sandbox

Personal sandboxed development environment for eVaka, backed by a
[Colima](https://github.com/abiosoft/colima) VM (macOS only, uses
Virtualization.framework).

Uses `sandbox.json` configuration and post-create scripts.

## Prerequisites

- [Colima](https://github.com/abiosoft/colima)
- Node.js

## Quick start

```sh
mise sandbox exec
```

On first `exec`, the environment is created and initialized automatically
(mise, frontend dependencies, Playwright).

## Commands

```
mise sandbox create                 Create and provision the VM
mise sandbox exec [cmd...]          Run a command in the VM (default: bash)
mise sandbox stop                   Stop the VM
mise sandbox rm                     Remove the VM and all data
mise sandbox recreate               Remove and recreate the VM
mise sandbox status                 Show VM status
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

- **cpu**, **memory**, **disk** — VM resources. Memory in GiB, disk in GiB.
- **mounts** — Bind mounts into the environment. Formats: `path`, `path:mode`,
  `src:dst`, `src:dst:mode`. Mode is either `ro` (read-only) or `rw`
  (read-write), defaults to `ro`. Paths support `~` expansion.
  **Note:** Only directory mounts are supported (use `copies` for files).
  Remapping mount destinations (`src:dst` where `dst` differs from `src`) is
  not supported, because Colima mounts host directories at their original paths.
- **copies** — Files and directories copied into the environment at creation.
  Formats: `path`, `src:dst`. Paths support `~` expansion.
  Useful for files that need isolation from the host.
- **ports** — Ports to expose to the host via SSH port forwards. Format is
  `port` (same port on both sides) or `host:container`.
- **notify** — Shell command to run on the host when a notification is
  received. A TCP listener on port 39813 is started automatically on `exec`.
  Trigger from inside with `echo notify > /dev/tcp/<host-ip>/39813`.

`sandbox.json` is gitignored — each developer maintains their own.

## Post-create scripts

Post-create scripts run in the project root after the environment is created.
Two scripts are available:

- `post-create.sh` — Shared setup for all developers.
- `post-create.local.sh` — Personal setup (gitignored), runs after the shared
  script. Use this to install your preferred tools, configs, etc.
