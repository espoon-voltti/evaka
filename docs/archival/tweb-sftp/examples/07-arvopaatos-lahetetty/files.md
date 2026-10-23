<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# 07: Sent voucher value decision

Service voucher value decision in the SENT state.

## Files and paths

Each document produces two files in the same SFTP directory (no subdirectories). The filename is the document UUID in eVaka:

| File | Path on the SFTP server |
|---|---|
| `6a0cf122-ad19-44c4-af3e-afa376c4b5ce.pdf` | `<prefix>6a0cf122-ad19-44c4-af3e-afa376c4b5ce.pdf` |
| `6a0cf122-ad19-44c4-af3e-afa376c4b5ce.xml` | `<prefix>6a0cf122-ad19-44c4-af3e-afa376c4b5ce.xml` |
| `83383876-260f-4a4a-bd74-0512a4889c9a.pdf` | `<prefix>83383876-260f-4a4a-bd74-0512a4889c9a.pdf` |
| `83383876-260f-4a4a-bd74-0512a4889c9a.xml` | `<prefix>83383876-260f-4a4a-bd74-0512a4889c9a.xml` |

Write order: PDF first, then XML (each using a separate SFTP connection).

## Metadata

| UUID | folder | title | calculationBaseDate | created | initiator | agents |
|---|---|---|---|---|---|---|
| `6a0cf122-ad19-44c4-af3e-afa376c4b5ce` | 04.01.02.59 | Arvopäätös;Lähetetty;Sanna Esimerkki;210489-952E | 2026-10-15 | 2026-10-15 | eVaka | Päättäjä Laskuttaja Lasse |
| `83383876-260f-4a4a-bd74-0512a4889c9a` | 04.01.02.59 | Arvopäätös;Lähetetty;Sanna Esimerkki;210489-952E | 2027-08-01 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Laskuttaja Lasse |

## Notes

- `6a0cf122-ad19-44c4-af3e-afa376c4b5ce`: Exported by the scheduled job (system user as initiator).

The people and personal identity codes are made up. The PDF files are in the same directory.
