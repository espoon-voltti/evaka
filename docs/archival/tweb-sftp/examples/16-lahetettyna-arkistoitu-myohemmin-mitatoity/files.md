<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# 16: Archived as sent, later annulled

A fee decision and a voucher value decision archived in the SENT state ("Lähetetty" in the title). They were later annulled in eVaka when the income statement changed, but were not exported again: archived_at was already set, so the archive retains "Lähetetty" (sent).

## Files and paths

Each document produces two files in the same SFTP directory (no subdirectories). The filename is the document UUID in eVaka:

| File | Path on the SFTP server |
|---|---|
| `413eed98-c522-41cf-b500-6199b9e8c1e0.pdf` | `<prefix>413eed98-c522-41cf-b500-6199b9e8c1e0.pdf` |
| `413eed98-c522-41cf-b500-6199b9e8c1e0.xml` | `<prefix>413eed98-c522-41cf-b500-6199b9e8c1e0.xml` |
| `b1270fba-0280-4d09-b685-1f931fe0e355.pdf` | `<prefix>b1270fba-0280-4d09-b685-1f931fe0e355.pdf` |
| `b1270fba-0280-4d09-b685-1f931fe0e355.xml` | `<prefix>b1270fba-0280-4d09-b685-1f931fe0e355.xml` |

Write order: PDF first, then XML (each using a separate SFTP connection).

## Metadata

| UUID | folder | title | calculationBaseDate | created | initiator | agents |
|---|---|---|---|---|---|---|
| `413eed98-c522-41cf-b500-6199b9e8c1e0` | 04.01.02.59 | Arvopäätös;Lähetetty;Sanna Esimerkki;210489-952E | 2026-10-15 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Laskuttaja Lasse |
| `b1270fba-0280-4d09-b685-1f931fe0e355` | 04.01.00.12 | Maksupäätös;Lähetetty;Sanna Esimerkki;210489-952E | 2026-10-15 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Laskuttaja Lasse |

The people and personal identity codes are made up. The PDF files are in the same directory.
