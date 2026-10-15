<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Nokia, Pirkkala ja Kangasala: arkistointi Twebiin (SFTP)

Tässä kuvataan Tampereen seudun Tweb-arkistointiliitäntä, joka on käytössä Nokian, Pirkkalan ja Kangasalan eVakassa. eVaka kirjoittaa jokaisesta asiakirjasta PDF:n ja metatieto-XML:n SFTP-palvelimelle.

Esimerkkitiedostot on tuotettu paikallisessa kehitysympäristössä Nokian asetuksilla tavallisten työntekijätoimintojen kautta ja kerätty paikallisesta SFTP-palvelimesta. Henkilöt ja henkilötunnukset ovat keksittyjä. Kuvaus vastaa koodia lokakuussa 2026, eikä esimerkkejä päivitetä automaattisesti.

## 1. Arkkitehtuuri lyhyesti

- Liitäntä on toteutettu luokassa `RegionalTwebArchivalClient` (`service/src/main/kotlin/evaka/trevaka/archival/tweb/RegionalTwebArchivalClient.kt`). Se toteuttaa coren rajapinnan `ArchivalIntegrationClient`.
- Kukin kunta luo clientin omassa konfiguraatiossaan: `NokiaConfig.kt:150`, `PirkkalaConfig.kt:149` ja `KangasalaConfig.kt:151` (`service/src/main/kotlin/evaka/instance/<kunta>/`). Client luodaan vain, jos `evaka.integration.archival.enabled=true` ja kunnan `archival`-asetukset on annettu. Muuten käyttöön tulee `ArchivalIntegrationClient.FailingClient`, joka heittää poikkeuksen.
- Tiedostot siirretään coren `SftpClient`illa (`service/src/main/kotlin/evaka/core/shared/sftp/SftpClient.kt`, JSch).
- Coren `ArchivalService` (`service/src/main/kotlin/evaka/core/document/archival/ArchivalService.kt`) hakee asiakirjan, sen PDF:n S3:sta ja asiankäsittelyprosessin (case process). Se kutsuu clientia ja merkitsee asiakirjaan `archived_at`-aikaleiman.
- Metatieto on Profiumin "collections"-XML:ää (sähke2-elementit), ja se generoidaan JAXB:llä XSD:stä.

## 2. SFTP-yhteys ja asetukset

Asetusluokat:

- `SftpArchivalProperties(schedule: ArchivalSchedule = ArchivalSchedule(), sftp: SftpProperties)` – `service/src/main/kotlin/evaka/instance/nokia/NokiaProperties.kt`. Pirkkala ja Kangasala käyttävät samaa luokkaa (`PirkkalaProperties.kt`, `KangasalaProperties.kt`, kenttä `archival`).
- `SftpProperties(host, port, hostKeys, username, password?, privateKey?, prefix)` – `service/src/main/kotlin/evaka/trevaka/sftp/SftpProperties.kt`.

Ympäristömuuttujat (Spring relaxed binding, `<KUNTA>` = `NOKIA` / `PIRKKALA` / `KANGASALA`):

| Muuttuja | Merkitys |
|---|---|
| `EVAKA_INTEGRATION_ARCHIVAL_ENABLED` | yleinen arkistoinnin päälle-/poiskytkin (`EvakaEnv.archivalEnabled`) |
| `<KUNTA>_ARCHIVAL_SFTP_HOST`, `_PORT`, `_HOST_KEYS`, `_USERNAME` | SFTP-palvelin, portti, palvelimen julkinen avain (base64, ilman tyyppietuliitettä) ja käyttäjätunnus |
| `<KUNTA>_ARCHIVAL_SFTP_PRIVATE_KEY` tai `_PASSWORD` | tunnistautuminen; tuotannossa käytetään yksityistä avainta |
| `<KUNTA>_ARCHIVAL_SFTP_PREFIX` | hakemistoetuliite, joka liitetään suoraan tiedostonimen eteen (merkkijonojen yhdistäminen, joten lopussa tarvitaan `/`) |
| `<KUNTA>_JOB_PLAN_DOCUMENT_ARCHIVAL_ENABLED` / `_CRON` | ajastetun työn kytkin ja aikataulu |
| `<KUNTA>_ARCHIVAL_SCHEDULE_DAILY_DOCUMENT_LIMIT`, `_DECISION_DELAY_DAYS`, `_FEE_DECISION_DELAY_DAYS`, `_VOUCHER_DECISION_DELAY_DAYS`, `_DOCUMENT_DECISION_DELAY_DAYS`, `_DOCUMENT_PLAN_DELAY_DAYS` | ajastuksen raja-arvot (ks. luku 7) |

Tuotanto- ja testiympäristöjen arvot (palvelin, tunnukset, hakemistoetuliite, päiväraja) ovat kuntien infra-asetuksissa, eivät tässä repossa.

Paikallinen kehitysympäristö: `application-<kunta>_evaka.yaml` asettaa `evaka.integration.archival.enabled: true` local-profiilissa, mutta `archival.sftp`-asetuksia ei ole. Siksi paikallisesti käytössä on oletuksena `FailingClient`. Esimerkkiajossa liitäntä ohjattiin compose-SFTP-konttiin (`compose/docker-compose.yml`, palvelu `sftp`, käyttäjä `foo`, hakemisto `upload`) ympäristömuuttujilla, jotka lisättiin `mise.local.toml`iin:

```
NOKIA_ARCHIVAL_SFTP_HOST=localhost
NOKIA_ARCHIVAL_SFTP_PORT=2229            # EVAKA_SFTP_PORT = 2222 + instanssi
NOKIA_ARCHIVAL_SFTP_HOSTKEYS=<compose/sftp/ssh_host_ed25519_key.pub:n base64-osa>
NOKIA_ARCHIVAL_SFTP_USERNAME=foo
NOKIA_ARCHIVAL_SFTP_PASSWORD=pass
NOKIA_ARCHIVAL_SFTP_PREFIX=upload/
```

Yhteyden yksityiskohdat (`SftpClient.kt`):

- Palvelimen avain tarkistetaan `hostKeys`-listaa vasten (`ReadOnlyHostKeyRepository`).
- Yhteys- ja lukuaikakatkaisu on 60 s.
- **Jokainen `put` avaa oman SSH-istunnon**, eli yksi asiakirja tarkoittaa kahta yhteyttä.
- Tiedosto kirjoitetaan suoraan lopulliseen nimeen (`ChannelSftp.put`, oletustila OVERWRITE). Väliaikaistiedostoa tai uudelleennimeämistä ei ole.

## 3. Hakemistorakenne ja tiedostonimet

`RegionalTwebArchivalClient.postRecord` (`RegionalTwebArchivalClient.kt:109–131`):

1. `<prefix><uuid>.<pääte>` – asiakirjan sisältö. Pääte päätellään sisällön MIME-tyypistä Tikalla (`MimeTypes.forName(contentType).extension`). Käytännössä tiedosto on aina `.pdf`.
2. `<prefix><uuid>.xml` – metatieto.

- `<uuid>` on eVakan asiakirjan id: `decision.id`, `fee_decision.id`, `voucher_value_decision.id` tai `child_document.id`. Sama arvo on XML:ssä `file/@originalId`. Tiedostonimi on `file/@name`.
- Kaikki tiedostot menevät samaan, litteään hakemistoon. Kunta- tai tyyppikohtaisia alihakemistoja ei ole. Asiakirjatyypin voi päätellä vain XML:n `folder`- ja `title`-kentistä.
- Kirjoitusjärjestys on ensin PDF ja sitten XML. Jos XML:n kirjoitus epäonnistuu, palvelimelle jää orpo PDF.
- Uusi vienti samalla id:llä (esim. käsin käynnistetty uudelleenarkistointi) ylikirjoittaa molemmat tiedostot. Tämä todettiin ajossa: tiedostojen aikaleima ja `archived_at` päivittyivät.

## 4. Metatietomuoto ja skeema

- Skeemat ovat repossa: [`profium-collection-sahke2.xsd`](../../../service/src/main/schema/profium-collection-sahke2.xsd) (nimiavaruus `http://www.profium.com/reception/2022/03`, juurielementti `collections`) ja [`sahke2.xsd`](../../../service/src/main/schema/sahke2.xsd) (nimiavaruus `http://www.profium.com/sahke2`). Niistä generoidaan JAXB-luokat Gradle-tehtävällä `xjcGenerate` (`service/build.gradle.kts`, `registerXjcTask("xjcGenerate", "src/main/schema")`).
- JAXB tuottaa muotoillun XML:n: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>`, oletusnimiavaruus reception/2022/03 ja sähke2-elementit etuliitteellä `ns2:`.
- **Validointi:** kaikki 20 tuotettua metatietotiedostoa validoituvat skeemaa vasten (`xmllint --noout --schema service/src/main/schema/profium-collection-sahke2.xsd *.xml` → "validates").
- XSD vaatii `created`-elementin (minOccurs=1). Koodi asettaa sen vain, kun lähdearvo ei ole null (`sentDate`, `approvedAt`, `publishedAt`). Arkistoitavissa tiloissa nämä on käytännössä aina asetettu, mutta muuten XML ei olisi skeeman mukainen.
- XSD:n valinnaisia kenttiä `description`, `file/@format`, `file/@size` ja `collections/@notifyemail` ei käytetä. Myöskään `agent/corporateName` ei ole käytössä (koodissa kommentti "agent corporateName left empty").

Rakenne:

```
collections
  initiator
    source_system   "eVaka" (vakio)
    initiator_id    viennin käynnistäjän evaka_user.id
    initiator_name  viennin käynnistäjän nimi
  collection        (aina täsmälleen yksi)
    type            "record" (vakio)
    folder          tehtäväluokka (processDefinitionNumber)
    metadata
      ns2:title                tyyppi;tila;etunimi sukunimi;henkilötunnus
      ns2:calculationBaseDate  säilytysajan laskentaperuste
      ns2:created              päivämäärä
      ns2:agent*               { agentRole, agentName }
    content
      file @name="<uuid>.pdf" @originalId="<uuid>"   (aina täsmälleen yksi)
```

## 5. Metatietokenttien merkitys ja arvojen lähteet

| Kenttä | Lähde | Viite |
|---|---|---|
| `initiator_id` / `initiator_name` | `EvakaUser` (id ja nimi), joka käynnisti viennin. Ajastetussa ajossa käyttäjä on järjestelmäkäyttäjä `00000000-0000-0000-0000-000000000000` / `eVaka`. Käsin käynnistetyssä viennissä se on ADMIN-työntekijä, nimi muodossa "Sukunimi Etunimi". | `RegionalTwebArchivalClient.kt:99–107`, `ArchivalService.getUser` |
| `folder` | **Varhaiskasvatus-, esiopetus- ja kerhopäätökset:** hakemuksen asiankäsittelyprosessin `processDefinitionNumber`. Poikkeus: `PRESCHOOL_DAYCARE`-päätöksille luetaan aina kunnan `APPLICATION_DAYCARE`-asetus vuodelta 2026 (kovakoodattu vuosi, koska vuosikohtaista konfiguraatiota ei käytetä). **Maksu- ja arvopäätökset:** prosessin numero. **Lapsen asiakirjat:** asiakirjapohjan `processDefinitionNumber`. | `RegionalArchivalDecision.kt:29–41`, `RegionalArchivalFeeDecision.kt:23`, `RegionalArchivalVoucherValueDecision.kt:23`, `RegionalArchivalChildDocument.kt:25` |
| `title` | `;`-erotettu merkkijono: asiakirjatyyppi; tila; nimi ("Etunimi Sukunimi"); henkilötunnus. Päätöksissä ja lapsen asiakirjoissa henkilö on lapsi, maksu- ja arvopäätöksissä päämies. Jos henkilötunnus puuttuu, vienti kaatuu (`error("No owner ssn available")`). | `hofTitle`/`childTitle`, `RegionalTwebArchivalClient.kt:169–185` |
| `calculationBaseDate` | Päätökset: `decision.endDate + 1`. Maksupäätös: `validDuring.end + 1`. Arvopäätös: `validTo + 1`. **Lapsen asiakirjat: asiakirjapohjan voimassaolon alkupäivä** (`template.validity.start`), ei asiakirjan oma päivämäärä. | `RegionalArchivalDecision.kt:45`, `RegionalArchivalFeeDecision.kt:27`, `RegionalArchivalVoucherValueDecision.kt:32`, `RegionalArchivalChildDocument.kt:34` |
| `created` | Päätökset: `sentDate`. Maksu- ja arvopäätökset: `approvedAt` (päivämääränä). Lapsen asiakirjat: `publishedAt` (päivämääränä). | samat tiedostot |
| `agent` | Asiankäsittelyprosessin historiasta otetaan rivit, joiden `enteredBy` on tyyppiä EMPLOYEE, ja ne ryhmitellään työntekijöittäin. Rooli on **"Päättäjä"**, jos työntekijällä on rivi tilassa `DECIDING`, muuten **"Laatija"**. Nimi on `EvakaUser.name` ("Sukunimi Etunimi"). SYSTEM- ja CITIZEN-rivit ohitetaan. Varhaiskasvatus- ja esiopetuspäätöksissä agentit ovat valinnaisia (migroidut päätökset → ei agent-elementtejä, fixture `decision-metadata-no-agents.xml`). Muissa tyypeissä vähintään yksi työntekijä vaaditaan, muuten vienti kaatuu. | `RegionalTwebArchivalClient.kt:146–167` |
| `file/@name`, `@originalId` | asiakirjan UUID (+ pääte) | `RegionalArchival*.kt` |

Mitä metatiedoissa **ei** välitetä, vaikka eVakassa tieto on olemassa:

- asiatunnus (`case_process.case_identifier`, esim. `1/04.01.00.11/2026`)
- organisaatio (`archiveMetadataOrganization`) ja Y-tunnus (`metadataBusinessId`)
- säilytysaika (`archiveDurationMonths`)
- julkisuusluokka tai salassapitoperuste
- yksikkö ja päätöksen voimassaoloaika

Säilytysajan laskenta jää siis vastaanottavan järjestelmän vastuulle (`folder` + `calculationBaseDate`).

## 6. Tuetut asiakirjatyypit ja tila-arvot

Tila-arvot: `service/src/main/kotlin/evaka/trevaka/archival/DocumentStatus.kt`. Tyyppinimet: `RegionalArchivalDecision.kt:59–73`.

| Asiakirja | `title`-tyyppi | Arkistoitavat tilat → `title`-tila | Esimerkki |
|---|---|---|---|
| Päätös DAYCARE / DAYCARE_PART_TIME | Varhaiskasvatuspäätös | ACCEPTED → Hyväksytty, REJECTED → Hylätty | 01, 02 |
| Päätös PRESCHOOL | Esiopetuspäätös | sama | 03 |
| Päätös PRESCHOOL_DAYCARE | Täydentävän varhaiskasvatuksen päätös | sama | 04 |
| Päätös PRESCHOOL_CLUB | Esiopetuksen kerhon päätös | sama | – |
| Päätös CLUB | Kerhopäätös | sama (ajastettu ajo ohittaa, ks. luku 7) | – |
| Päätös PREPARATORY_EDUCATION | – | `UnsupportedOperationException` | – |
| Maksupäätös | Maksupäätös | SENT → Lähetetty, ANNULLED → Mitätöity | 05, 06, 16 |
| Arvopäätös | Arvopäätös | SENT → Lähetetty, ANNULLED → Mitätöity | 07, 08, 16 |
| Lapsen asiakirja ilman päätöstä (VASU, LEOPS, HOJKS, PEDAGOGICAL_ASSESSMENT, PEDAGOGICAL_REPORT, OTHER, CITIZEN_BASIC, MIGRATED_*) | asiakirjapohjan nimi | COMPLETED → Valmis | 09–12 |
| Lapsen päätösasiakirja (OTHER_DECISION) | asiakirjapohjan nimi | päätös ACCEPTED → Hyväksytty, REJECTED → Hylätty, ANNULLED → Mitätöity | 13–15 |

Muita tilarajoituksia:

- Lapsen asiakirjasta viedään vain, jos pohjassa `archive_externally = true` ja asiakirjan tila on `COMPLETED` (`ChildDocumentController.planArchiveChildDocument`). Lisäksi asiakirjalle on oltava asiankäsittelyprosessi, muuten `error("No child document case process available")`. Prosessi syntyy vain, jos pohjalla on `processDefinitionNumber`.
- Päätöksillä on oltava hakemuksen asiankäsittelyprosessi (`ArchivalService.uploadDecisionToArchive` → `NotFound`). Prosessi syntyy vain, jos kunnan `archiveMetadataConfigs` palauttaa hakemustyypille asetuksen (`CaseProcess.kt:33`).

## 7. Ajastus ja viiveet

- Ajastettu työ `PlanDocumentArchival` on kaikissa kolmessa kunnassa (`<Kunta>ScheduledJobs.kt:33`). Oletuksena se ajetaan päivittäin klo 20.00 (Helsingin aikaa), ja se on oletuksena pois päältä. Ympäristöissä työ kytketään päälle infra-asetuksilla. Työ kutsuu `planDocumentArchival` (`service/src/main/kotlin/evaka/trevaka/archival/TrevakaArchivalScheduling.kt`).
- Oletusarvot ovat luokassa `ArchivalSchedule` (`service/src/main/kotlin/evaka/instance/tampere/TampereProperties.kt:50`): `dailyDocumentLimit=0` (0 = ei ajeta lainkaan), päätökset 180 vrk, maksupäätökset 180, arvopäätökset 180, lapsen päätösasiakirjat 30 ja lapsen suunnitelmat 180.
- Poimintaehdot (`TrevakaArchivalScheduling.kt:72–135`):
  - **Päätökset:** status ACCEPTED/REJECTED, `type <> 'CLUB'`, ei arkistoitu, `resolved <= tänään − decisionDelayDays` ja PDF olemassa.
  - **Maksu- ja arvopäätökset:** status SENT/ANNULLED, ei arkistoitu, `approved_at <= tänään − delay` ja PDF olemassa.
  - **Lapsen suunnitelma-asiakirjat** (`type <> 'OTHER_DECISION'`): COMPLETED, julkaistulla versiolla PDF, `archive_externally`, ja **asiakirjapohjan voimassaolon päättymispäivä <= tänään − documentPlanDelayDays**. Pohjat, joilla on avoin voimassaolo (`validity.end = null`), eivät koskaan täytä ehtoa, joten niiden asiakirjoja ei arkistoida ajastetusti.
  - **Lapsen päätösasiakirjat** (`OTHER_DECISION`): edellisten lisäksi lapsella ei saa olla yhtään sijoitusta, jonka `end_date > tänään − documentDecisionDelayDays`. Päätösasiakirjat viedään siis vasta, kun lapsen varhaiskasvatus on kokonaan päättynyt ja viive kulunut.
- Päivittäinen kiintiö jaetaan luokkien kesken suhteessa poimittavien määrään (`distributeProportionally`, kokonaislukujako alaspäin). Kunkin luokan sisällä vanhin viedään ensin (`ORDER BY created / resolved / approved_at`, `DocumentArchivalQueries.kt`).
- Asynkroniset työt (`ArchiveDecision`, `ArchiveFeeDecision`, `ArchiveVoucherValueDecision`, `ArchiveChildDocument`) suunnitellaan `retryCount = 1`, eli **yksi yritys ilman uusintaa** (`TrevakaArchivalScheduling.kt:59`). Ajastetussa ajossa `user = null`, jolloin initiatorina on järjestelmäkäyttäjä.
- Käsin käynnistys: `POST /employee/decisions/{id}/archive`, `/employee/fee-decisions/{id}/archive`, `/employee/value-decisions/{id}/archive` ja `/employee/child-documents/{id}/archive`. Ne vaativat ADMIN-roolin (`Action.kt`, `ARCHIVE(HasGlobalRole(ADMIN))`), ja `retryCount = 1`. Endpointit eivät tarkista `archived_at`ia, joten jo arkistoidun asiakirjan voi viedä uudelleen (tiedostot ylikirjoittuvat).

## 8. Onnistumisen ja virheen käsittely – kuittausta ei ole

- `postRecord` palauttaa aina vakiomerkkijonon **`"static-sftp-response"`** (`RegionalTwebArchivalClient.kt:131`). Arvoa käytetään vain lokirivissä "Successfully archived … to instance static-sftp-response" (`ArchivalService`), eikä sitä tallenneta.
- **eVaka ei saa vastaanottavalta järjestelmältä minkäänlaista kuittausta.** Onnistuminen tarkoittaa vain, että kaksi SFTP-kirjoitusta meni läpi ilman poikkeusta. eVaka ei tiedä, käsittelikö Tweb/Frends tiedostot tai hyväksyikö se metatiedon. Paluukanavaa (kuittaustiedosto, sähköposti tai `notifyemail`) ei ole toteutettu.
- Onnistuneen kirjoituksen jälkeen `ArchivalService` merkitsee `archived_at = now()` erillisessä transaktiossa. Jos tämä merkintä epäonnistuisi, tiedostot olisivat jo palvelimella ja seuraava ajo veisi ne uudelleen samoilla nimillä.
- Virhetilanne (todettu ajossa: SFTP-kontti pysäytetty, käsin käynnistetty vienti): JSch heittää `JSchException: java.net.ConnectException: Connection refused`. Async-työ merkitään "permanently failed (remainingAttempts=0)", ja `archived_at` pysyy ennallaan. Mitään ei merkitä asiakirjaan, eikä käyttäjälle näy virhettä (työ ajetaan taustalla).
- Ajastetussa käytössä epäonnistunut asiakirja jää arkistoimattomaksi, ja seuraavan päivän ajo poimii sen uudelleen. Koska poiminta alkaa vanhimmista, pysyvästi kaatuvat asiakirjat (esim. henkilötunnus puuttuu tai agentteja ei ole) poimitaan joka päivä ensimmäisinä ja kuluttavat kiintiötä.

## 9. Esimerkit (`examples/`)

| Hakemisto | Sisältö |
|---|---|
| 01-varhaiskasvatuspaatos-hyvaksytty | kunnallinen yksikkö ja palveluseteliyksikkö (sama tyyppi) |
| 02-varhaiskasvatuspaatos-hylatty | huoltaja hylkäsi paikan |
| 03-esiopetuspaatos-hyvaksytty | |
| 04-taydentavan-varhaiskasvatuksen-paatos-hyvaksytty | esiopetushakemukselta; `folder` = varhaiskasvatuksen numero |
| 05-maksupaatos-lahetetty | yksi käsin ja yksi ajastetulla ajolla viety (initiator `eVaka`) |
| 06-maksupaatos-mitatoity | |
| 07-arvopaatos-lahetetty | yksi käsin ja yksi ajastetulla ajolla viety |
| 08-arvopaatos-mitatoity | |
| 09-varhaiskasvatussuunnitelma-valmis | VASU |
| 10-pedagoginen-arvio-valmis | PEDAGOGICAL_ASSESSMENT |
| 11-esiopetuksen-oppimissuunnitelma-valmis | LEOPS |
| 12-pedagoginen-selvitys-valmis | PEDAGOGICAL_REPORT, ajastetulla ajolla viety |
| 13–15-lapsen-paatosasiakirja-* | OTHER_DECISION: hyväksytty, hylätty, mitätöity |
| 16-lahetettyna-arkistoitu-myohemmin-mitatoity | ks. luku 11, kohta 1 |

Kussakin hakemistossa on XML-tiedosto(t) sellaisinaan alkuperäisillä tiedostonimillä vastaavat PDF:t sekä `files.md`, jossa on tiedostonimet ja metatietojen yhteenveto.

Tuotantotapa: dev-api:lla luotiin henkilöt (myös VTJ-mockiin) ja asiakirjapohjat. Työntekijän rajapinnoilla tehtiin paperihakemus → sijoitusehdotus → päätösten lähetys → hyväksyntä/hylkäys, maksu- ja arvopäätösten vahvistus ja lähetys sekä mitätöinti tulotiedon muutoksella. Lapsen asiakirjat kulkivat tilasiirtymien läpi, ja päätösasiakirjat ehdotettiin päättäjälle, joka hyväksyi, hylkäsi tai mitätöi ne. Vienti käynnistettiin ADMIN-käyttäjän arkistointiendpointeilla. Lisäksi ajettiin `PlanDocumentArchival` lisäämällä async-jonoon `RunScheduledJob`-työ, sillä db-scheduler käynnistyy vain `production`-profiilissa (`ScheduledJobConfig.kt`). Tämä on sama työ, jonka tuotannon ajastin lisää. Ajastetussa ajossa viiveet asetettiin nollaan tai negatiivisiksi, jotta saman päivän asiakirjat kelpasivat.

Tehtäväluokkien arvot ovat Nokian konfiguraatiosta: 04.01.00.11 varhaiskasvatushakemus, 04.01.03.41 esiopetushakemus, 04.01.00.12 maksupäätös ja 04.01.02.59 arvopäätös. Lapsen asiakirjojen numerot (04.01.00.20–23, 04.01.03.42) ovat esimerkkipohjiin keksittyjä, koska ne tulevat pohjasta eivätkä kunnan konfiguraatiosta.

## 10. Mitä esimerkeistä puuttuu ja miksi

- **Kerhopäätös (CLUB):** ajastettu ajo ohittaa CLUB-päätökset (`type <> 'CLUB'`). Nokialla ja Kangasalalla ei myöskään ole `APPLICATION_CLUB`-asetusta, joten kerhohakemuksille ei synny asiankäsittelyprosessia eikä vienti onnistu edes käsin. Pirkkalalla asetus on (4801.23), joten käsin vienti olisi mahdollinen, mutta sitä ei ajettu.
- **PRESCHOOL_CLUB, DAYCARE_PART_TIME:** muoto on sama kuin muilla päätöksillä, ja vain `title`-tyyppi vaihtelee (ks. luku 6). Ei tuotettu.
- **PREPARATORY_EDUCATION:** ei tuettu (poikkeus).
- **HOJKS, OTHER, CITIZEN_BASIC, MIGRATED_*:** muoto on sama kuin esimerkeissä 09–12 (pohjan nimi; Valmis). Ei tuotettu erikseen.
- **Migroitu päätös ilman agentteja:** ei tuotettu. Integraatiotestin fixture `service/src/integrationTest/resources/tweb-archival-client/decision-metadata-no-agents.xml` näyttää muodon.
- **Pirkkala ja Kangasala:** ajoa ei tehty, koska koodi on täsmälleen sama ja vain tehtäväluokat ja kuntanimet eroavat (luku 12).
- **SFTP-virhe:** testattiin (luku 8), mutta siitä ei synny tiedostoa.

## 11. Huomioita nykytoteutuksesta

1. **Lähetettynä arkistoitua päätöstä ei viedä uudelleen mitätöitynä.** Jos maksu- tai arvopäätös on jo arkistoitu tilassa SENT ja se myöhemmin mitätöidään (korvaava päätös), `archived_at` on jo asetettu eikä uutta vientiä tehdä. Arkistoon jää otsikko "Lähetetty" (esimerkki 16). Oletusviive 180 vrk pienentää riskiä, mutta ei poista sitä.
2. **Lapsen asiakirjan `calculationBaseDate` on asiakirjapohjan voimassaolon alkupäivä**, eli sama kaikille saman pohjan asiakirjoille. Esimerkeissä se on 2026-08-01 myös 2026-10-15 tehdyille päätöksille.
3. **Avoimen voimassaolon pohjien suunnitelma-asiakirjoja ei arkistoida ajastetusti koskaan** (`upper(validity)` on null → ehto ei toteudu).
4. **Päivämäärä- ja aikaleimavertailu:** `resolved` ja `approved_at` ovat `timestamptz`-sarakkeita, ja niitä verrataan `LocalDate`en (= keskiyö). Asiakirja kelpaa siis vasta päivänä *päätöspäivä + viive + 1*. Ajossa viive 0 ei poiminut saman päivän maksupäätöstä, viive −1 poimi.
5. **Täydentävän varhaiskasvatuksen päätöksen `folder`** luetaan varhaiskasvatushakemuksen asetuksesta kovakoodatulla vuodella 2026 (`RegionalArchivalDecision.kt:36`), ei päätöksen omasta prosessista.
6. **`created` puuttuu XML:stä**, jos lähdearvo on null. Se rikkoisi XSD:n (minOccurs=1), mutta arkistoitavissa tiloissa arvo on käytännössä aina olemassa.
7. **Henkilötunnus on selväkielisenä `title`-kentässä.** Ilman henkilötunnusta vienti kaatuu ja jää toistumaan päivittäin (luku 8).
8. **Agentit:** maksu- ja arvopäätöksissä "Päättäjä" on päätökset vahvistanut talouskäyttäjä. Suunnitelma-asiakirjoissa kaikki työntekijät ovat "Laatija", myös asiakirjan valmiiksi merkinnyt. Nimet ovat muodossa "Sukunimi Etunimi", mutta `title`-kentässä henkilön nimi on muodossa "Etunimi Sukunimi".
9. **Asiatunnusta ei välitetä**, vaikka eVaka muodostaa sen (luku 5).
10. **Kaksi erillistä SFTP-istuntoa** asiakirjaa kohden, ei atomista kirjoitusta, eikä kuittausta (luvut 2, 3 ja 8).

## 12. Kuntakohtaiset erot

Koodi (`RegionalTwebArchivalClient` ja `PlanDocumentArchival`) on kolmella kunnalla identtinen. Erot ovat `FeatureConfig`issa (`<Kunta>Config.kt`, `archiveMetadataConfigs`) ja infran ympäristömuuttujissa.

| | Nokia | Pirkkala | Kangasala |
|---|---|---|---|
| `archiveMetadataOrganization` (ei viedä) | Nokian kaupunki, varhaiskasvatus ja esiopetus | Pirkkalan kunta, varhaiskasvatus | Kangasalan kaupunki, varhaiskasvatus |
| APPLICATION_DAYCARE → `folder` (myös PRESCHOOL_DAYCARE) | 04.01.00.11 | 4801.101 | 46.00.63 |
| APPLICATION_PRESCHOOL | 04.01.03.41 | **ei asetusta** → esiopetushakemuksen päätöksillä ei ole prosessia eikä niitä voi arkistoida | 41.01.05 |
| APPLICATION_CLUB | ei | 4801.23 (ajastettu ajo kuitenkin ohittaa CLUB-päätökset) | ei |
| FEE_DECISION | 04.01.00.12 | 4801.102 | 46.00.64 |
| VOUCHER_VALUE_DECISION | 04.01.02.59 | 4803.03 | 46.02.07 |
| archiveDurationMonths (ei viedä) | hakemukset 120, talouspäätökset 180 | 144 / 180 | hakemukset 1440 (120 v), talouspäätökset 120 |
| Ympäristömuuttujien etuliite | `NOKIA_` | `PIRKKALA_` | `KANGASALA_` |

Lapsen asiakirjojen `folder` tulee asiakirjapohjasta, joten se on kunnan pääkäyttäjän ylläpitämä eikä koodissa.
