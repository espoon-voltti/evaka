<!--
SPDX-FileCopyrightText: 2017-2021 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Fonts

Open source fonts used by eVaka. Licenses included as `*.license` files next to
the originals.

As Google Fonts officially only provides the `.ttf` variants as downloads,
these were downloaded from the excellent <https://google-webfonts-helper.herokuapp.com>.

In order to avoid users downloading fonts they don't need, all font files
have been split by character set (defined in `fonts.css` as `unicode-range`s),
which means the fonts are downloaded if those characters are drawn.

## Replacing or updating fonts

1. Download all required font weights and styles of your font(s), ideally split by character set (latin, cyrillic, ...)
    - E.g. from <https://google-webfonts-helper.herokuapp.com>:
        1. Find your font
        1. For each character set, select one at a time
        1. Select all styles/weights
        1. Select "Modern Browsers"
            - I.e. download only WOFF and WOFF2 variants
            - All browsers that support TLS 1.2 (requierd for eVaka), support at least WOFF
        1. Copy the `@font-face` rules (and adjust accordingly)
        1. Download and unzip into this directory
1. Add relevant license files (see <https://fonts.google.com/attribution> for Google Font attributions) for all fonts
1. Update `fonts.css` with the new `@font-face` directives
1. Update all eVaka styles referencing the font(s) you're replacing
