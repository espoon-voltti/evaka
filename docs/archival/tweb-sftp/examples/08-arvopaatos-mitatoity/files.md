<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# 08: Annulled voucher value decision

Service voucher value decision in the ANNULLED state.

## Files and paths

Each document produces two files in the same SFTP directory (no subdirectories). The filename is the document UUID in eVaka:

| File | Path on the SFTP server |
|---|---|
| `70699eae-4194-409a-9169-2c1d615af83e.pdf` | `<prefix>70699eae-4194-409a-9169-2c1d615af83e.pdf` |
| `70699eae-4194-409a-9169-2c1d615af83e.xml` | `<prefix>70699eae-4194-409a-9169-2c1d615af83e.xml` |

Write order: PDF first, then XML (each using a separate SFTP connection).

## Metadata

| UUID | folder | title | calculationBaseDate | created | initiator | agents |
|---|---|---|---|---|---|---|
| `70699eae-4194-409a-9169-2c1d615af83e` | 04.01.02.59 | Arvopäätös;Mitätöity;Sanna Esimerkki;210489-952E | 2027-08-01 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Laskuttaja Lasse |

The people and personal identity codes are made up. The PDF files are in the same directory.
