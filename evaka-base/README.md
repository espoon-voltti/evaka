<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# evaka-base

Shared Docker base image for eVaka services based on Ubuntu 20.04.

Includes `s3download` binary in the `PATH` for downloading files from AWS S3
without the need for AWS SDK in the application.

**TODO:** Distribute `s3download` as source code.

## Usage

Build: `./build.sh`

Extend: `FROM evaka-base`
