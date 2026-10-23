<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# 05: Sent fee decision

Fee decision in the SENT state.

## Files and paths

Each document produces two files in the same SFTP directory (no subdirectories). The filename is the document UUID in eVaka:

| File | Path on the SFTP server |
|---|---|
| `ec95a645-d8b5-4027-8536-746281a9dd4e.pdf` | `<prefix>ec95a645-d8b5-4027-8536-746281a9dd4e.pdf` |
| `ec95a645-d8b5-4027-8536-746281a9dd4e.xml` | `<prefix>ec95a645-d8b5-4027-8536-746281a9dd4e.xml` |
| `ffdcc13f-1f4f-4692-8e76-3219e26f00b8.pdf` | `<prefix>ffdcc13f-1f4f-4692-8e76-3219e26f00b8.pdf` |
| `ffdcc13f-1f4f-4692-8e76-3219e26f00b8.xml` | `<prefix>ffdcc13f-1f4f-4692-8e76-3219e26f00b8.xml` |

Write order: PDF first, then XML (each using a separate SFTP connection).

## Metadata

| UUID | folder | title | calculationBaseDate | created | initiator | agents |
|---|---|---|---|---|---|---|
| `ec95a645-d8b5-4027-8536-746281a9dd4e` | 04.01.00.12 | Maksupäätös;Lähetetty;Sanna Esimerkki;210489-952E | 2026-10-15 | 2026-10-15 | eVaka | Päättäjä Laskuttaja Lasse |
| `ffdcc13f-1f4f-4692-8e76-3219e26f00b8` | 04.01.00.12 | Maksupäätös;Lähetetty;Sanna Esimerkki;210489-952E | 2027-08-01 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Laskuttaja Lasse |

## Notes

- `ec95a645-d8b5-4027-8536-746281a9dd4e`: Exported by the scheduled PlanDocumentArchival job: the initiator is the system user (00000000-…, "eVaka"). Most production exports use this path; for manual exports (an ADMIN user's archive button), the initiator is the employee who triggered the export.

The people and personal identity codes are made up. The PDF files are in the same directory.
