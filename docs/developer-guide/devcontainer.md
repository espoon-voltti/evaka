<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Development Container

eVaka provides a [dev container](https://containers.dev/) configuration for
running the development environment inside a Docker container. This is useful
when you don't want to install tools directly on your host machine, and for a
general security enhancement by sandboxing the environment.

## Prerequisites

- Docker
- [Dev Container CLI](https://github.com/devcontainers/cli) (`mise install`
  sets this up) or and IDE with devcontainers support (like VS Code or IntelliJ IDEA).

## Using the CLI

Start the container:

```sh
devcontainer up
```

To rebuild after changing the Dockerfile or devcontainer.json:

```sh
devcontainer up --remove-existing-container
```

Open a shell inside the container:

```sh
devcontainer exec bash
```

Once inside, start the development environment as usual:

```sh
mise run start
```

## Using VS Code

1. Open the repository in VS Code.
2. When prompted, click **Reopen in Container**, or run the
   **Dev Containers: Reopen in Container** command from the command palette.

VS Code will build the container, run the post-create setup, and connect
automatically.

## Ports

The following ports are forwarded from the container to the host:

| Port | Service |
| ---- | ------- |
| 3000 | API Gateway |
| 9090 | dummy-idp |
| 9099 | Frontend dev server |

Port forwarding works both via the CLI (`appPort` in devcontainer.json) and
VS Code (`forwardPorts`).
