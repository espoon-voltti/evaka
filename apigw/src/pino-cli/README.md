<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# pino-cli

Transform [Pino](https://github.com/pinojs/pino) log messages to the a JSON log format.

Check the [test fixtures](test-utils/fixtures/log-messages.ts) and [tests](__tests____/cli/bin.ts) for what kind of log messages
`pino-cli` expects and how it transforms them.

## Usage

1. Build `apigw`
1. Run `apigw` and pass its output through `pino-cli`:

    ```sh
    node dist/index.js | node dist/pino-cli/cli/bin.js
    ```

## Development

### Testing

Running the unit tests requires building `pino-cli` first, this is handled
automatically with `apigw` project's [`pretest` npm script](../../package.json).
