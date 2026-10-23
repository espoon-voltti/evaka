<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# 06: Annulled fee decision

Fee decision in the ANNULLED state (replaced by a new decision for the same period).

## Files and paths

Each document produces two files in the same SFTP directory (no subdirectories). The filename is the document UUID in eVaka:

| File | Path on the SFTP server |
|---|---|
| `95b7b41a-152b-4b37-aa1f-0fadd61bb970.pdf` | `<prefix>95b7b41a-152b-4b37-aa1f-0fadd61bb970.pdf` |
| `95b7b41a-152b-4b37-aa1f-0fadd61bb970.xml` | `<prefix>95b7b41a-152b-4b37-aa1f-0fadd61bb970.xml` |

Write order: PDF first, then XML (each using a separate SFTP connection).

## Metadata

| UUID | folder | title | calculationBaseDate | created | initiator | agents |
|---|---|---|---|---|---|---|
| `95b7b41a-152b-4b37-aa1f-0fadd61bb970` | 04.01.00.12 | Maksupäätös;Mitätöity;Sanna Esimerkki;210489-952E | 2027-08-01 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Laskuttaja Lasse |

The people and personal identity codes are made up. The PDF files are in the same directory.
