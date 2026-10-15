<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Espoo: arkistointi Särmään

Tässä kuvataan, miten eVaka siirtää asiakirjoja Espoon Särmä-arkistoon (Avaintecin X-Archive-rajapinta). Toiminnallinen kuvaus on wikissä: [Särmä-arkisto-integraatio](https://github.com/espoon-voltti/evaka/wiki/S%C3%A4rm%C3%A4-arkisto-integraatio).

Kuvaus perustuu koodiin (lokakuu 2026) ja paikalliseen ajoon, jossa liitäntä ohjattiin vastaanottimeen, joka tallensi pyynnöt sellaisenaan. Hakemistossa `examples/` ovat tallennetut pyynnöt. Henkilöt, henkilötunnukset ja tehtäväluokat (`12.06.01.SL1.RT34` jne.) on keksitty. Työntekijä "Pääkäyttäjä Päivi" on eVakan paikallisen kehitysdatan kuvitteellinen käyttäjä. Esimerkkejä ei päivitetä automaattisesti koodin muuttuessa.

Koodiviittaukset ovat polkuja repon juuresta.
Lyhenteet:

- `espoo/` = `service/src/main/kotlin/evaka/instance/espoo/`
- `core/` = `service/src/main/kotlin/evaka/core/`

## 1. Mitä Espoon liitäntä vie

**Vain lapsen asiakirjoja (`child_document`).** Espoon `ArchivalIntegrationClient`-toteutus
on `SärmäChildDocumentClient` (`espoo/archival/SärmäChildDocumentClient.kt`):

- `uploadChildDocumentToArchive` on toteutettu (rivi 59).
- `uploadDecisionToArchive`, `uploadFeeDecisionToArchive` ja
  `uploadVoucherValueDecisionToArchive` ovat muotoa `TODO("Not yet implemented")` (rivit 38, 47, 56).
  Jos varhaiskasvatus-, maksu- tai arvopäätöksen arkistointi käynnistetään Espoossa
  (`POST /employee/decisions/{id}/archive` tms.), async-job kaatuu `NotImplementedError`iin
  eikä Särmään lähde pyyntöä. Käyttöliittymässä Espoolla on päällä vain
  `archiveIntegration: { childDocuments: true }` (`frontend/src/lib-customizations/espoo/featureFlags.tsx:44`),
  joten päätösten arkistointipainikkeita ei näytetä.

Lapsen asiakirja lähtee arkistoon vain, jos sen asiakirjapohjassa on
`archive_externally = true`. Pohjassa on oltava myös `process_definition_number`
(tehtäväluokka). Muuten `SärmäChildDocumentClient` heittää `IllegalStateException`in
ennen HTTP-kutsua (rivit 68–72).

### Asiakirjatyypit ja niiden metatiedot

Metatiedot muodostetaan tiedostossa `espoo/archival/DocumentMetadataUtils.kt`:
`createDocumentDescription` (rivi 133 alkaen) ja `createRetentionPolicy` (rivi 69).

| `ChildDocumentType` | `documentType` | `documentTypeSpecifier` | Säilytysaika (`retentionPolicy`) | Esimerkki |
|---|---|---|---|---|
| `VASU`, `MIGRATED_VASU` | Suunnitelma | Varhaiskasvatussuunnitelma | pysyvä (`InPerpetuity`, KA/13089/07.01.01.03.01/2018) | 01, 02, 12 |
| `LEOPS`, `MIGRATED_LEOPS` | Suunnitelma | Lapsen esiopetuksen oppimissuunnitelma LEOPS | pysyvä | 03, 13 |
| `HOJKS` | Suunnitelma | Henkilökohtaisen opetuksen järjestämistä koskeva suunnitelma HOJKS | pysyvä | 04 |
| `PEDAGOGICAL_ASSESSMENT` | Arvio | Pedagoginen arvio | 28 v syntymästä (`YearsFromCustomerBirthDate`, Perusopetuslaki 16 a §) | 05, 14 |
| `PEDAGOGICAL_REPORT` | Selvitys | Pedagoginen selvitys | 28 v syntymästä | 06 |
| `OTHER` | (puuttuu) | (puuttuu) | pysyvä | 07 |
| `CITIZEN_BASIC` | (puuttuu) | (puuttuu) | pysyvä | 08 |
| `OTHER_DECISION` | (puuttuu) | (puuttuu) | pysyvä | 09, 10, 11 |
| `MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION`, `MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION` | (puuttuu) | (puuttuu) | pysyvä | – |

Jos tyypille ei ole arvoa, elementit `documentType` ja `documentTypeSpecifier`
jätetään pois (`mainTextElement` ohittaa null-arvot, `espoo/archival/RecordMetadataXml.kt`).

Metatiedoissa ei ole asiakirjan tilaa (valmis/luonnos) eikä päätöksen lopputulosta
(hyväksytty/hylätty/mitätöity). Esimerkkien 09–11 metatiedot eroavat vain
tunnisteiden osalta. Lopputulos näkyy vain PDF:n sisällössä.

## 2. Rajapinta

### Endpoint ja metodi

- `POST {evaka.integration.sarma.url}PUT`. URL rakennetaan kutsulla
  `archiveEnv.url.resolve("PUT")` (`espoo/archival/SärmäHttpClient.kt:63`).
  Asetuksen on siis päätyttävä kauttaviivaan, esimerkiksi `https://<host>/archive-core/`,
  jolloin kutsu menee osoitteeseen `https://<host>/archive-core/PUT`
  (`core/EvakaEnv.kt:758`, kommentti).
- Jokainen asiakirja lähetetään omana pyyntönään (`nr_of_instances=1`).

### Sisältötyyppi ja multipart-osat

`Content-Type: multipart/form-data; boundary=<uuid>` (OkHttp `MultipartBody.FORM`,
`SärmäHttpClient.kt:36–61`). Osat lähetetään tässä järjestyksessä:

| Osa | Arvo | Lähde |
|---|---|---|
| `protocol_version` | `1.0` | vakio |
| `operation_id` | `PUT` | vakio |
| `response_format` | `URL-ENCODED` | vakio |
| `user_id` | integraatiokäyttäjä | `evaka.integration.sarma.user_id` |
| `user_role` | esim. `xa-api-integration` | `evaka.integration.sarma.user_role` |
| `nr_of_instances` | `1` | vakio |
| `instance_1_md_model_id` | `standard` | vakio |
| `instance_1_md_model_version` | `1.0` | vakio |
| `instance_1_md_master_id` | esim. `yleinen` | `evaka.integration.sarma.master_id` |
| `instance_1_md_master_version` | `1.0` | vakio |
| `instance_1_class_id` | pohjan tehtäväluokka | `document_template.process_definition_number` |
| `instance_1_virtual_archive_id` | esim. `YLEINEN` | `evaka.integration.sarma.virtual_archive_id` |
| `instance_1_record_payload_location` | `SELF_CONTAINED` | vakio |
| `instance_1_md_instance` | metatieto-XML, `Content-Type: application/xml; charset=utf-8`, ei tiedostonimeä | `marshalMetadata(...)` |
| `instance_1_record_payload_content_size` | PDF:n koko tavuina | PDF |
| `instance_1_record_payload_data` | PDF, `Content-Type: application/pdf`, `filename="child-documents/child_document_<id>_v<n>.pdf"` | S3, julkaistu versio |

Huomioita:

- Multipart-osan `filename` on S3-avain hakemistoineen. Metatiedon `format/fileName`
  on pelkkä tiedostonimi (`Paths.get(...).fileName`, `SärmäChildDocumentClient.kt:79`).
- Metatieto-XML lähetetään yhdellä rivillä ilman sisennyksiä. Esimerkkien
  `metadata.xml` on muotoiltu luettavaksi (`xmllint --format`), ja `request.http`
  sisältää XML:n täsmälleen sellaisena kuin se lähetettiin.
- XML-määrittelyssä on yksinkertaiset lainausmerkit (`<?xml version='1.0' encoding='UTF-8'?>`).
  Ne tulevat `XMLStreamWriter`istä.
- Lähetettävä PDF on asiakirjan **viimeisin julkaistu versio**
  (`getChildDocumentPublishedVersion`, `core/document/archival/ArchivalService.kt:196`).
  Huoltajan täydentämässä asiakirjassa (esimerkki 08) se on `_v2.pdf`.

### Otsakkeet ja tunnistautuminen

Pyynnössä on vain OkHttp:n oletusotsakkeet: `Content-Type`, `Content-Length`, `Host`,
`Connection: Keep-Alive`, `Accept-Encoding: gzip` ja `User-Agent: okhttp/<versio>`.
**HTTP-tason tunnistautumista ei ole** (ei `Authorization`-otsaketta, ei
asiakasvarmennetta koodissa). Käyttäjä ja rooli välitetään lomakekentissä
`user_id` ja `user_role`. Pääsynhallinta perustuu siis verkkotason rajaukseen
(infra-asetukset eivät ole tässä repossa).

### Aikakatkaisut

OkHttp: `connectTimeout`, `readTimeout` ja `writeTimeout` ovat kukin 1 minuutti
(`SärmäHttpClient.kt:19–21`). Kokonaisaikakatkaisua (`callTimeout`) ei ole asetettu.

### Vastauksen tulkinta

`SärmäChildDocumentClient.kt:97–131`:

1. HTTP-statuskoodi ja runko kirjataan lokiin (info).
2. Runko jäsennetään `application/x-www-form-urlencoded`-muotoisena
   (`URLEncodedUtils.parse`). Siitä luetaan `status_code` (kokonaisluku) ja `instance_ids`.
3. Jos `instance_ids` puuttuu, kirjataan virhe lokiin, mutta tämä ei yksinään estä onnistumista.
4. **Onnistuminen** vaatii, että HTTP-status on `200` **ja** rungon `status_code` on `200`.
   - Onnistuessa `ArchivalService` asettaa `child_document.archived_at = now()`
     (`ArchivalService.kt:221`). Särmän palauttamaa `instance_ids`-arvoa ei tallenneta
     kantaan, se näkyy vain lokissa.
   - Jokin muu yhdistelmä aiheuttaa `RuntimeException("Failed to archive document <id>")`.
     Myös `HTTP 200` + `status_code=400` (esimerkki 14), HTML-runko tai tyhjä runko
     tulkitaan virheeksi. Asiakirjaa ei merkitä arkistoiduksi.
   - Verkkovirhe tai aikakatkaisu (`IOException`) johtaa samaan lopputulokseen.
5. Kaikki arkistointijobit suunnitellaan asetuksella `retryCount = 1`, eli ne yritetään
   **kerran ilman uusintaa**. Tämä koskee sekä manuaalista arkistointia
   (`ChildDocumentController.kt:696`) että ajastettua poimintaa
   (`ChildDocumentArchivalScheduling.kt:50`). Epäonnistunut job jää `async_job`-tauluun
   arvolla `retry_count = 0` eikä enää aja.
   Testissä havaittu: `ArchiveChildDocument|retry_count 0|initial_retry_count 1|completed_at null`.

Onnistuneen vastauksen muoto on otettu `SärmäMockClient`ista
(`espoo/archival/SärmäMockClient.kt`), koska oikean Särmän vastauksia ei ole repossa:

```
status_message=Success.&transaction_id=2872934&protocol_version=1.0&status_code=200&instance_ids=354319&
```

Esimerkkien `response.http`-tiedostot ovat paikallisen vastaanottimen vastauksia, eivät
oikean Särmän. Virhevastauksen sisältö (`status_code=400`, `status_message=...`) on keksitty
eVakan tulkintalogiikan esittelyä varten.

## 3. Metatieto-XML

Juurielementti on `RecordMetadataInstance`. Nimiavaruudet tulevat asetuksista:
`ns0` = `evaka.integration.sarma.metadata_main_namespace` ja
`at0` = `evaka.integration.sarma.metadata_policy_namespace`.
Esimerkeissä käytettiin arvoja `http://www.avaintec.com/2005/x-archive/record-metadata-instance/2.0`
ja `http://www.avaintec.com/2004/records-schedule-fi/1.0`. Ne vastaavat XSD:iden
`targetNamespace`-arvoja. XML-kirjoitin on `espoo/archival/RecordMetadataXml.kt` ja
tietomalli `espoo/archival/model/RecordMetadata.kt`.

| Elementti | Arvo / lähde |
|---|---|
| `metadataMasterVersion/masterName` | `master_id` (sama kuin multipart-kentässä) |
| `metadataMasterVersion/versionNumber` | `1.0` |
| `virtualArchiveId` | `virtual_archive_id` |
| `recordIdentifiers/CaseIdentifier` | `case_process.case_identifier` = `<juokseva nro>/<tehtäväluokka>/<vuosi>` (generoitu sarake, `docs/db/schema.sql`). Puuttuu, jos asiakirjalla ei ole prosessia. |
| `recordIdentifiers/RecordIdentifier` | `child_document.id` (UUID) |
| `documentDescription/title` | asiakirjapohjan nimi (`document_template.name`) |
| `documentType`, `documentTypeSpecifier` | asiakirjatyypin mukaan, ks. taulukko yllä |
| `registerName` | vakio "Varhaiskasvatuksen asiakastietorekisteri" |
| `firstName`, `lastName` | lapsen nimi |
| `socialSecurityNumber` | lapsen hetu (puuttuu, jos lapsella ei ole hetua) |
| `birthDate` | lapsen syntymäaika |
| `personalData` | vakio `containsSensitivePersonalInformation` |
| `personalDataCollectionReason` | vakio "Rekisterinpitäjän lakisääteisten velvoitteiden noudattaminen" |
| `language` | pohjan kieli ISO 639-1 -muodossa (`fi`/`sv`/`en`) |
| `dataManagement` | vakio "Palvelujen tiedonhallinta" |
| `dataSource` | vakio "Varhaiskasvatuksen tietovaranto" |
| `Agents/Agent` | Yksi agentti jokaista eri käyttäjää kohden, joka on kirjannut asiakirjan prosessihistoriaan (`case_process_history.entered_by`). `Role` on aina "Henkilökunta" ja `CorporateName` on `archiveMetadataOrganization` = "Espoon kaupungin esiopetus ja varhaiskasvatus" (`espoo/EspooConfig.kt:233`). Päätösasiakirjoissa mukana on myös järjestelmäkäyttäjä "eVaka", joka kirjaa COMPLETED-tilan Suomi.fi-lähetyksen yhteydessä. Huoltajan täydentämässä asiakirjassa mukana on huoltaja. |
| `format/recordType` | `digital` |
| `format/mimeType` | `application/pdf` |
| `format/fileFormat` | `not applicable` |
| `format/fileName` | PDF:n tiedostonimi ilman polkua |
| `creation/originatingSystem` | vakio "Varhaiskasvatuksen toiminnanohjausjärjestelmä" |
| `creation/created` | **Asian päättymispäivä** (sama arvo kuin `Case_Finished`), ei luontipäivä (`DocumentMetadataUtils.kt:287`). Ks. kohta 7. |
| `policies/retentionPolicy` | ks. asiakirjatyyppitaulukko |
| `policies/disclosurePolicy` | Salassa pidettävä pohja: `InitialTargetState=confidential`, `TimeSpan` = pohjan salassapitoaika (oletus 100), `ActionAnnotation` = pohjan salassapitoperuste. Julkinen pohja: `public`, `TimeSpan=0`, `ActionAnnotation` "JulkL 24 § 1 mom. 32 k" (oletusarvo). `TriggerEvent=YearsFromRecordCreation`, kohdetila `public`. (`DocumentMetadataUtils.kt:26`) |
| `policies/informationSecurityPolicy` | vakio `notSecurityClassified` |
| `policies/protectionPolicy` | vakio `3` |
| `CaseFile/Case_Created` | Prosessihistorian INITIAL-rivin päivä, tai jos sitä ei ole, asiakirjan luontipäivä. |
| `CaseFile/Case_Finished` | Prosessihistorian COMPLETED-rivin päivä. Jos sitä ei ole, pohjan voimassaolon loppupäivä. Jos sitäkään ei ole, `Case_Created`-päivää seuraava 31.7. (`DocumentMetadataUtils.kt:227`) |

Prosessihistorian tilat (`core/caseprocess/CaseProcess.kt:230`, `updateDocumentCaseProcessHistory`):

- Tavallinen asiakirja saa COMPLETED-rivin, kun se merkitään valmiiksi.
- Päätösasiakirja saa COMPLETED-rivin vasta, kun Suomi.fi-viesti on lähetetty
  (`core/document/childdocument/ChildDocumentService.kt:133`).

Prosessi (`case_process`) luodaan asiakirjaa luotaessa vain, jos pohjalla on tehtäväluokka
(`createChildDocument`, `ChildDocumentController.kt:131`). Koska Särmä-siirto vaatii tehtäväluokan, käytännössä
`CaseIdentifier` on aina mukana.

### XSD:t

Särmän XSD:t (metatietomalli `yleinen-1.0.xsd` ja sen sisällyttämät `include-extended.xsd` ja `include-policies.xsd`) ovat toimittajan omaisuutta, eikä niitä ole repossa. Skeemat saa Espoolta tai arkistojärjestelmän toimittajalta. eVaka kirjoittaa XML:n `XMLStreamWriter`illä (`espoo/archival/RecordMetadataXml.kt`). Odotettu XML-muoto on myös tiedostossa `service/src/test/resources/archival/expected-metadata.xml`.

Kaikkien esimerkkien metatiedot validoituivat `yleinen-1.0.xsd`-skeemaa vasten, kun esimerkit tuotettiin.

## 4. Mistä arkistointi käynnistyy

Molemmat reitit suunnittelevat async-jobin `AsyncJob.ArchiveChildDocument`, jonka käsittelee
`ArchivalService.uploadChildDocumentToArchive` (`core/document/archival/ArchivalService.kt:183`).
Jobit ajetaan `archival`-poolissa: oletuksena rinnakkaisuus 4 ja ei kuristusta
(`evaka.archival_pool_concurrency`, `evaka.archival_pool_throttle_interval_seconds`,
`core/EvakaEnv.kt:786`).

### a) Manuaalinen: `POST /employee/child-documents/{documentId}/archive`

`core/document/childdocument/ChildDocumentController.kt:656`. Ehdot:

- `evaka.integration.sarma.enabled` (tai `evaka.integration.archival.enabled`) on päällä
  (`core/EvakaEnv.kt:84`).
- Käyttäjällä on oikeus `Action.ChildDocument.ARCHIVE`.
- Pohjalla on `archive_externally = true`.
- Asiakirjan tila on `COMPLETED`.

Jobi ajetaan heti (`runAt = now`, `retryCount = 1`). Esimerkit 01–11 ja 14 on tuotettu tällä reitillä.

### b) Ajastettu poiminta: `ArchiveEligibleChildDocuments`

- Määritelty coressa (`core/shared/job/ScheduledJobs.kt:317`), oletuksena
  **pois päältä**, aikataulu `nightly`. Se kytketään päälle asetuksella
  `evaka.job.archive_eligible_child_documents.enabled=true`, ja aikataulua voi muuttaa
  asetuksella `...cron`. Espoolla ei ole omaa ajastusta: `EspooScheduledJobs` ei sisällä
  arkistointia. Tässä repossa ei ole Espoon tuotantoasetuksia, joten se, onko ajastus
  päällä tuotannossa, ei selviä täältä.
- Ajastetut tehtävät käynnistyvät vain Spring-profiilissa `production`
  (`core/shared/config/ScheduledJobConfig.kt:32`). Paikallisesti poiminta käynnistettiin
  lisäämällä `async_job`-tauluun rivi `RunScheduledJob {"job": "ArchiveEligibleChildDocuments"}`.
- Poiminta (`core/document/archival/ChildDocumentArchivalScheduling.kt`,
  kysely `getChildDocumentsEligibleForArchival`, `core/document/childdocument/ChildDocumentQueries.kt:796`)
  valitsee lapsen asiakirjat, joille pätee:
  - `document_template.archive_externally = true`,
  - `child_document.archived_at IS NULL`,
  - `upper(document_template.validity) <= tänään - viive`. Viive on
    `evaka.child_document_archival_delay_days`, oletus 30 (`core/EvakaEnv.kt:737`).
  - Järjestys `created_at`. Valinnaisena katkaisuna `evaka.child_document_archival_limit`
    (oletus 0 = ei rajaa).
- Huomioita poimintaehdoista:
  - **Toistaiseksi voimassa olevan pohjan** (voimassaolon loppu `NULL`) asiakirjat eivät
    koskaan täytä ehtoa, koska `upper(...)` on NULL. Ne voi arkistoida vain manuaalisesti.
  - **Asiakirjan tilaa ei tarkisteta.** Ks. kohta 7.
- Jobit suunnitellaan ilman käyttäjää (`user = null`), jolloin `ArchivalService` käyttää
  järjestelmäkäyttäjää. Käyttäjä ei kuitenkaan näy Särmä-pyynnössä.

Esimerkit 12 ja 13 on tuotettu tällä reitillä.

## 5. Esimerkit (`examples/`)

Jokaisessa hakemistossa on:

- `request.http`: koko HTTP-pyyntö. Peitetty `Host` (`<sarma-host>`) ja `user_id`
  (`<käyttäjätunnus>`), ja PDF:n binäärisisältö on korvattu viittauksella.
  `Content-Length` on alkuperäinen.
- `metadata.xml`: `instance_1_md_instance` muotoiltuna.
- `response.http`: paikallisen vastaanottimen vastaus.

| Hakemisto | Tyyppi | Reitti | Mitä havainnollistaa |
|---|---|---|---|
| `01-varhaiskasvatussuunnitelma` | VASU, valmis | manuaalinen | perustapaus, salassa pidettävä, pysyvä säilytys |
| `02-varhaiskasvatussuunnitelma-ruotsinkielinen` | VASU (sv), valmis | manuaalinen | `language=sv`. Pohjan kieli rajaa sen ruotsinkielisiin yksiköihin. |
| `03-esiopetuksen-oppimissuunnitelma` | LEOPS, valmis | manuaalinen | pohjan voimassaolon loppu asetettu. COMPLETED-rivi määrää silti `Case_Finished`-arvon. |
| `04-hojks` | HOJKS, valmis | manuaalinen | |
| `05-pedagoginen-arvio` | PEDAGOGICAL_ASSESSMENT | manuaalinen | säilytys 28 v syntymästä |
| `06-pedagoginen-selvitys` | PEDAGOGICAL_REPORT | manuaalinen | säilytys 28 v syntymästä |
| `07-muu-asiakirja-julkinen` | OTHER, julkinen pohja | manuaalinen | ei `documentType`-elementtejä, `disclosurePolicy` = `public` |
| `08-huoltajan-taydentama-asiakirja` | CITIZEN_BASIC | manuaalinen | huoltaja täydensi, PDF on versio 2, huoltaja mukana `Agents`-listassa |
| `09-paatosasiakirja-hyvaksytty` | OTHER_DECISION, hyväksytty | manuaalinen | järjestelmäkäyttäjä "eVaka" mukana `Agents`-listassa |
| `10-paatosasiakirja-hylatty` | OTHER_DECISION, hylätty | manuaalinen | metatiedot samat kuin hyväksytyssä |
| `11-paatosasiakirja-mitatoity` | OTHER_DECISION, hyväksytty ja mitätöity | manuaalinen | metatiedot samat, mitätöinti ei näy metatiedoissa |
| `12-ajastettu-poiminta-valmis` | VASU, valmis, pohjan voimassaolo päättynyt yli 30 pv sitten | ajastettu | ajastetun reitin pyyntö on samanmuotoinen |
| `13-ajastettu-poiminta-luonnos` | LEOPS, **luonnos** (julkaistu versio), pohja päättynyt | ajastettu | luonnos lähtee arkistoon. `Case_Finished` = `created` = pohjan loppupäivä (2026-07-31), joka on ennen `Case_Created`-päivää (2026-10-15). Ks. kohta 7. |
| `14-virhevastaus` | PEDAGOGICAL_ASSESSMENT | manuaalinen | vastaus `HTTP 200` + `status_code=400`: asiakirjaa ei merkitty arkistoiduksi, jobia ei yritetty uudelleen |

## 6. Mitä esimerkeistä puuttuu ja miksi

- **Varhaiskasvatus-, esiopetus- ja muut hakemuspäätökset sekä maksu- ja arvopäätökset:**
  Espoon liitäntä ei tue niitä (`TODO`, ks. kohta 1).
- **`MIGRATED_VASU`, `MIGRATED_LEOPS` ja `MIGRATED_*_ASSISTANCE_NEED_DECISION`:**
  Nämä syntyvät vain tietojen migraatiossa, eikä niitä voi luoda tavallisen käyttöliittymä-
  tai API-polun kautta. Metatiedot ovat samat kuin vastaavilla tyypeillä (VASU/LEOPS)
  tai päätösasiakirjoilla (ei `documentType`-elementtejä).
- **Asiakirja ilman tehtäväluokkaa tai ilman julkaistua versiota:** HTTP-pyyntöä ei
  lähde lainkaan.
  - Ilman tehtäväluokkaa tulee `IllegalStateException` (`SärmäChildDocumentClient.kt:68–72`).
  - Ilman julkaistua versiota tulee `NotFound` (`ArchivalService.kt:199`). Tämä todettiin
    ajastetussa poiminnassa julkaisemattomalla luonnoksella.
- **Oikean Särmän vastaukset:** Niitä ei ollut käytettävissä. Vastausten muoto perustuu
  `SärmäMockClient`iin ja integraatiotestiin
  (`service/src/integrationTest/kotlin/evaka/core/document/archival/ArchivalServiceIntegrationTest.kt`).

## 7. Huomioita nykytoteutuksesta

1. **Ajastettu poiminta lähettää luonnoksia, ja sama asiakirja voi lähteä joka yö uudelleen.**
   `getChildDocumentsEligibleForArchival` ei suodata tilaa, toisin kuin manuaalinen reitti,
   joka vaatii `COMPLETED`.
   - Jos luonnoksella on julkaistu versio, PDF ja metatiedot lähetetään Särmään, ja Särmä
     vastaa onnistuneesti (esimerkki 13).
   - Tämän jälkeen `markDocumentAsArchived` kaatuu kannan rajoitteeseen
     `archived_documents_must_be_completed` (`CHECK (archived_at IS NULL OR status = 'COMPLETED')`,
     `docs/db/schema.sql`).
   - `archived_at` jää tyhjäksi, joten asiakirja poimitaan uudelleen joka ajossa, ja Särmään
     syntyy kaksoiskappaleita.
   - Julkaisematon luonnos kaatuu joka ajossa `NotFound`-virheeseen.
   - Havaittiin paikallisessa ajossa lokiriveillä "violates check constraint
     archived_documents_must_be_completed" ja "No published version found".
2. **Toistaiseksi voimassa olevien pohjien asiakirjoja ei poimita koskaan ajastetusti**
   (`upper(validity)` on NULL).
3. **`creation/created` on asian päättymispäivä eikä luontipäivä**
   (`creation.copy(created = caseFinishDate)`, `DocumentMetadataUtils.kt:287`). Jos asiakirjalla
   ei ole COMPLETED-riviä, `Case_Finished` (ja `created`) voi olla ennen `Case_Created`-päivää
   (esimerkki 13). Muutos voi olla tarkoituksellinen, jos Särmä laskee säilytyksen
   `created`-kentästä, mutta sitä ei ole kommentoitu.
4. **Huoltaja merkitään `Agents`-listaan roolilla "Henkilökunta"** (esimerkki 08):
   `createAgents` antaa kaikille prosessihistorian kirjaajille saman roolin.
5. **Päätöksen lopputulos ja mitätöinti eivät näy metatiedoissa** (esimerkit 09–11).
6. **Särmän `instance_ids`-arvoa ei tallenneta eVakaan.** Se on vain lokissa, joten
   arkistokappaleen yhdistäminen eVakan asiakirjaan onnistuu vain `RecordIdentifier`-
   kentällä (= `child_document.id`).

## 8. Asetukset

| Asetus (Spring) | Ympäristömuuttuja | Merkitys |
|---|---|---|
| `evaka.integration.sarma.enabled` | `EVAKA_INTEGRATION_SARMA_ENABLED` | Kytkee arkistoinnin päälle (`EvakaEnv.archivalEnabled`, vaihtoehtona `evaka.integration.archival.enabled`). |
| `evaka.integration.sarma.use_mock_client` | `..._USE_MOCK_CLIENT` | `true` = `SärmäMockClient`, oletus `false`. Paikallisessa profiilissa `true`. |
| `evaka.integration.sarma.url` | `..._URL` | Perus-URL, joka päättyy `/`-merkkiin. |
| `evaka.integration.sarma.user_id` / `user_role` | `..._USER_ID` / `..._USER_ROLE` | Lomakekentät `user_id` / `user_role`. |
| `evaka.integration.sarma.master_id` / `virtual_archive_id` | `..._MASTER_ID` / `..._VIRTUAL_ARCHIVE_ID` | Metatietomalli ja virtuaaliarkisto. |
| `evaka.integration.sarma.metadata_main_namespace` / `metadata_policy_namespace` | `..._METADATA_MAIN_NAMESPACE` / `..._METADATA_POLICY_NAMESPACE` | XML-nimiavaruudet. |
| `evaka.job.archive_eligible_child_documents.enabled` / `.cron` | `EVAKA_JOB_ARCHIVE_ELIGIBLE_CHILD_DOCUMENTS_ENABLED` / `_CRON` | Ajastettu poiminta. |
| `evaka.child_document_archival_delay_days` / `_limit` | | Poiminnan viive (30) ja enimmäismäärä (0 = ei rajaa). |
| `evaka.archival_pool_concurrency` / `_throttle_interval_seconds` | | Async-poolin rinnakkaisuus (4) ja kuristus (0 s). |

Beanit: `EspooConfig.särmäClient` (`espoo/EspooConfig.kt:323`) valitsee mock- tai
HTTP-asiakkaan, ja `archivalIntegrationClient` (rivi 331) valitsee `SärmäChildDocumentClient`in.
Jos arkistointi ei ole päällä, käytössä on `FailingClient`.

## 9. Miten esimerkit tuotettiin

- Paikallinen eVaka (Espoo-kustomoinnit, profiili `local`).
- Ympäristömuuttujat: `EVAKA_INTEGRATION_SARMA_USE_MOCK_CLIENT=false` ja
  `EVAKA_INTEGRATION_SARMA_URL=http://127.0.0.1:<portti>/archive-core/`.
- Vastaanotin tallensi pyynnöt ja palautti `SärmäMockClient`in mukaisen vastauksen.
- Henkilöt, sijoitukset ja asiakirjapohjat luotiin dev-API:lla. Asiakirjat kulkivat
  oikeiden työntekijä- ja huoltajarajapintojen kautta (luonti, sisältö, tilasiirrot,
  päätösehdotus, hyväksyntä/hylkäys/mitätöinti, huoltajan täydennys).
- Arkistointi käynnistettiin `POST /employee/child-documents/{id}/archive`
  -kutsulla tai ajastetun poiminnan async-jobilla.
- Ajastettua poimintaa varten pohjien voimassaolo siirrettiin SQL:llä menneisyyteen
  (2025-08-01–2026-07-31).
