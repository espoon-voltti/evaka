<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# espoo-customizations

This directory should contain all Espoo-specific customizations, files etc.
that cannot be licensed with the eVaka license (such as the city of Espoo
logo) and must be loaded from private storage during release builds.

The originals are placed in this directory and symbolic links are created
in all the places that use those files. This allows scripts to replace the
files with customized versions in CI easily.

## Usage

`./bin/fetch-espoo-build-customizations.sh` is used by CI to replace all the
files in this directory.
