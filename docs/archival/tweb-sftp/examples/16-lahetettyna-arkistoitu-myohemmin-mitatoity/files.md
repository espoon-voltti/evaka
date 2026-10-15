<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# 16-lahetettyna-arkistoitu-myohemmin-mitatoity

Maksupäätös ja arvopäätös, jotka arkistoitiin tilassa SENT (otsikossa "Lähetetty"). Myöhemmin ne mitätöitiin eVakassa, kun tuloselvitys muuttui, mutta niitä ei viety uudelleen: archived_at oli jo asetettu, joten arkistoon jää tieto "Lähetetty".

## Tiedostot ja polut

Jokaisesta asiakirjasta syntyy kaksi tiedostoa samaan SFTP-hakemistoon (ei alihakemistoja). Tiedostonimi on eVakan asiakirjan UUID:

| Tiedosto | Polku SFTP-palvelimella |
|---|---|
| `413eed98-c522-41cf-b500-6199b9e8c1e0.pdf` | `<etuliite>413eed98-c522-41cf-b500-6199b9e8c1e0.pdf` |
| `413eed98-c522-41cf-b500-6199b9e8c1e0.xml` | `<etuliite>413eed98-c522-41cf-b500-6199b9e8c1e0.xml` |
| `b1270fba-0280-4d09-b685-1f931fe0e355.pdf` | `<etuliite>b1270fba-0280-4d09-b685-1f931fe0e355.pdf` |
| `b1270fba-0280-4d09-b685-1f931fe0e355.xml` | `<etuliite>b1270fba-0280-4d09-b685-1f931fe0e355.xml` |

Kirjoitusjärjestys: ensin PDF, sitten XML (kumpikin omalla SFTP-yhteydellään).

## Metatiedot

| UUID | folder | title | calculationBaseDate | created | initiator | agentit |
|---|---|
| `413eed98-c522-41cf-b500-6199b9e8c1e0` | 04.01.02.59 | Arvopäätös;Lähetetty;Sanna Esimerkki;210489-952E | 2026-10-15 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Laskuttaja Lasse |
| `b1270fba-0280-4d09-b685-1f931fe0e355` | 04.01.00.12 | Maksupäätös;Lähetetty;Sanna Esimerkki;210489-952E | 2026-10-15 | 2026-10-15 | Pääkäyttäjä Päivi | Päättäjä Laskuttaja Lasse |

Henkilöt ja henkilötunnukset ovat keksittyjä. PDF-tiedostot eivät ole mukana repossa.
