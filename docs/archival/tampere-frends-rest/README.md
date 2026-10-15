<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Tampere: arkistointi Frendsin kautta (REST)

Tampere vie asiakirjat arkistoon Frends-integraatioalustan kautta. eVaka lähettää yhden asiakirjan kerrallaan HTTP-pyyntönä, ja vastauksessa palautuu arkistotunnus. Metatiedot noudattavat Profiumin SÄHKE2-pohjaista vastaanottoskeemaa.

Esimerkit on tuotettu ajamalla eVakaa paikallisesti Tampereen asetuksilla. Asiakirjat käsiteltiin tavallisten työntekijätoimintojen kautta, ja eVakan lähettämät pyynnöt tallennettiin sellaisenaan. Henkilöt ovat kuvitteellisia, ja henkilötunnukset ovat keksittyjä. Esimerkit kuvaavat tilannetta lokakuussa 2026, eikä niitä päivitetä automaattisesti koodin muuttuessa.

## Rajapinta

| | |
|---|---|
| Menetelmä ja polku | `POST {baseUrl}/records/add` |
| Sisältötyyppi | `multipart/form-data` |
| Osa `xml` | Metatiedot, `application/xml; charset=utf-8`. Skeema [`profium-collection-sahke2.xsd`](../../../service/src/main/schema/profium-collection-sahke2.xsd), joka viittaa tiedostoon [`sahke2.xsd`](../../../service/src/main/schema/sahke2.xsd) |
| Osa `content` | Asiakirja (PDF). Osan `filename` on sama kuin metatietojen `file/@originalId` |
| Otsakkeet | `X-API-version: 1.0`, `X-API-transactionid: <eVakan jäljitystunnus>`, `Accept: */*` |
| Tunnistautuminen | HTTP Basic -tunnistautuminen Frendsiin (`TAMPERE_FRENDS_USERNAME`, `TAMPERE_FRENDS_PASSWORD`). Integraatioalusta lisää arkistojärjestelmälle menevän `X-API-key`-otsakkeen itse |
| Osoite | `TAMPERE_ARCHIVAL_BASEURL` |
| Aikakatkaisut | Yhteys, luku ja kirjoitus 1 min |

Jokaisessa pyynnössä on yksi asiakirja: yksi `collection` ja yksi tiedosto.

### Vastaus

eVaka tulkitsee vastauksen näin (rakenne: [`frends-response.xsd`](frends-response.xsd)):

- **HTTP 2xx ja `<success>`**: eVaka kirjaa onnistumisen ja vastauksen arkistotunnuksen (`records/record/archiveid`) lokiin ja merkitsee asiakirjan arkistoiduksi. Arkistotunnusta ei tallenneta kantaan.
- **HTTP 2xx, mutta runko ei ole `<success>`**: eVaka kirjaa virheen lokiin, mutta merkitsee asiakirjan silti arkistoiduksi.
- **HTTP 4xx/5xx**: eVaka kirjaa virheen (`<errors>`-rungon sisältö tai raakavastaus), eikä asiakirjaa merkitä arkistoiduksi. Ajastetussa viennissä työtä ei yritetä uudelleen, vaan asiakirja tulee uudelleen ehdolle seuraavalla ajokerralla.

Esimerkit: [`responses/success.xml`](responses/success.xml) ja [`responses/error.xml`](responses/error.xml). Virhe-esimerkin `error_code`- ja `error_summary`-arvot ovat havainnollistavia, eivät toimittajan todellisia koodeja. `frends-response.xsd` on johdettu eVakan lukukoodista (`TampereArchivalSchema.kt`), joten se ei ole toimittajan virallinen skeema.

## Metatiedot

| Kenttä | Sisältö |
|---|---|
| `initiator/source_system` | Aina `eVaka` |
| `initiator/initiator_id`, `initiator_name` | Viennin käynnistäjä. Ajastetussa viennissä järjestelmäkäyttäjä, käsin käynnistetyssä viennissä työntekijä (muodossa Sukunimi Etunimi) |
| `collection/type` | Aina `record` |
| `collection/folder` | Tehtäväluokka (arkistonmuodostussuunnitelman tunnus) asianhallintaprosessista (`case_process.process_definition_number`) |
| `metadata/title` | `<Asiakirjan nimi>, <Tila>, <Etunimi Sukunimi>, <syntymäaika pp.kk.vvvv>`. Henkilö on lapsi, paitsi maksu- ja arvopäätöksissä päämies |
| `metadata/calculationBaseDate`, `created` | Päätöksissä lähetyspäivä, maksu- ja arvopäätöksissä hyväksymispäivä, lapsen asiakirjoissa julkaisupäivä |
| `metadata/agent` | Toimijat roolilla `Laatija` tai `Päättäjä`, ks. taulukko alla |
| `content/file/@name` | `<Asiakirjan nimi>.pdf` |
| `content/file/@originalId` | Asiakirjan eVaka-tunniste (UUID) |

Henkilötunnusta ei lähetetä metatiedoissa. Se näkyy vain PDF:ssä.

## Asiakirjatyypit

| Esimerkki | Asiakirja | Tehtäväluokka | Tilat otsikossa | Toimijat |
|---|---|---|---|---|
| [01](examples/01-varhaiskasvatuspaatos-hyvaksytty), [02](examples/02-varhaiskasvatuspaatos-hylatty) | Varhaiskasvatuspäätös (myös osa-aikainen) | 12.06.01.17 | Hyväksytty, Hylätty | – |
| [03](examples/03-esiopetuspaatos-hyvaksytty) | Esiopetuspäätös | 12.06.01.17 | Hyväksytty, Hylätty | – |
| [04](examples/04-taydentavan-varhaiskasvatuksen-paatos-hyvaksytty) | Täydentävän varhaiskasvatuksen päätös | 12.06.01.17 | Hyväksytty, Hylätty | – |
| [05](examples/05-maksupaatos-lahetetty), [06](examples/06-maksupaatos-mitatoity) | Maksupäätös | 12.06.01.23 | Lähetetty, Mitätöity | Päättäjä = päätöksen käsittelijä |
| [07](examples/07-arvopaatos-lahetetty), [08](examples/08-arvopaatos-mitatoity) | Arvopäätös (palveluseteli) | 12.06.01.23 | Lähetetty, Mitätöity | Päättäjä = päätöksen käsittelijä |
| [09](examples/09-varhaiskasvatussuunnitelma-valmis) | Varhaiskasvatussuunnitelma | asiakirjapohjan mukaan* | Valmis | Laatija |
| [10](examples/10-esiopetuksen-oppimissuunnitelma-valmis) | Esiopetuksen oppimissuunnitelma | asiakirjapohjan mukaan* | Valmis | Laatija |
| [11](examples/11-pedagoginen-arvio-valmis) | Pedagoginen arvio | asiakirjapohjan mukaan* | Valmis | Laatija |
| [12](examples/12-paatosasiakirja-tuki-hyvaksytty), [13](examples/13-paatosasiakirja-tuki-hylatty) | Päätösasiakirja (esim. päätös tuesta) | asiakirjapohjan mukaan* | Hyväksytty, Hylätty, Mitätöity | Laatija, Päättäjä |

Päätösten sekä maksu- ja arvopäätösten tehtäväluokat tulevat Tampereen konfiguraatiosta (`TampereConfig.archiveMetadataConfigs`). Paikkapäätös saa hakemuksensa tehtäväluokan.

\* Lapsen asiakirjojen tehtäväluokka, säilytysaika ja asiakirjan nimi tulevat kunnan pääkäyttäjän ylläpitämästä asiakirjapohjasta. Esimerkeissä käytetyt pohjat (12.06.01.11 suunnitelmille ja 12.06.01.26 päätöksille) ovat havainnollistavia. Vain pohjat, joissa on valittu "arkistoidaan ulkoisesti", viedään. Lapsen asiakirjojen toimijat muodostetaan käsittelyhistoriasta: jokainen asiakirjaa käsitellyt työntekijä on `Laatija`, ja päätöksen tehnyt on `Päättäjä`.

Esimerkeistä puuttuu kolme tapausta:

- **Kerhopäätös** ja **esiopetuksen kerhon päätös**: rakenne on sama kuin päätöksissä 01–04 (otsikot "Kerhopäätös" ja "Esiopetuksen kerhon päätös"). Kerhopäätökset eivät kuulu ajastettuun vientiin, mutta ne voi viedä käsin.
- **Mitätöity päätösasiakirja**: rakenne on sama kuin esimerkissä 12, otsikon tila on "Mitätöity".
- **Valmistava opetus**: valmistavan opetuksen päätöksiä ei tueta.

## Milloin asiakirjat viedään

Ajastettu vienti valitsee päivittäin enintään päivärajan verran asiakirjoja. Kiintiö jaetaan asiakirjaryhmien kesken niiden määrien suhteessa. Oletusviiveet (asetukset `tampere.archival.schedule.*`):

| Asiakirja | Ehdolle |
|---|---|
| Varhaiskasvatus- ja esiopetuspäätökset (hyväksytty/hylätty, ei kerho) | 180 pv päätöksen ratkaisusta |
| Maksu- ja arvopäätökset (lähetetty/mitätöity) | 180 pv hyväksymisestä |
| Suunnitelmat ja muut lapsen asiakirjat (valmis) | 180 pv asiakirjapohjan voimassaolon päättymisestä |
| Lapsen päätösasiakirjat | 30 pv siitä, kun lapsen viimeinen sijoitus on päättynyt |

Ajastettu ajo (`archiveEligibleDocuments`) on oletuksena pois päältä, ja käyttöön otettuna se ajetaan päivittäin klo 20. Myös päivärajan oletus on 0, jolloin ajo ei valitse yhtään asiakirjaa. Lisäksi työntekijä voi käynnistää viennin käsin yksittäisen asiakirjan kohdalta.

Kun asiakirja on merkitty arkistoiduksi, sitä ei viedä uudelleen ajastetusti. Jos jo arkistoitu maksu- tai arvopäätös mitätöidään myöhemmin, arkistoon jää tila "Lähetetty". Päätöspäivää verrataan aikaleimaan, joten asiakirja tulee ehdolle vasta viiveen jälkeisenä päivänä.

## Esimerkkien rakenne

```
examples/<nn-tyyppi>/
  metadata.xml    multipart-osa "xml" sellaisenaan
  request.http    koko HTTP-pyyntö (tunnistautuminen peitetty, PDF korvattu viittauksella)
responses/        esimerkkivastaukset
frends-response.xsd
```

Metatiedot voi validoida komennolla:

```sh
xmllint --noout --schema service/src/main/schema/profium-collection-sahke2.xsd docs/archival/tampere-frends-rest/examples/*/metadata.xml
```

## Lähdekoodi

- Viestin muodostus: `service/src/main/kotlin/evaka/instance/tampere/archival/` (`TampereArchivalClient.kt` ja asiakirjatyypeittäiset `TampereArchival*.kt`)
- Otsikoiden tilat: `service/src/main/kotlin/evaka/trevaka/archival/DocumentStatus.kt`
- Ajastus ja viiveet: `service/src/main/kotlin/evaka/trevaka/archival/TrevakaArchivalScheduling.kt`
- Asetukset: `service/src/main/kotlin/evaka/instance/tampere/TampereConfig.kt` ja `TampereProperties.kt`
- Integraatiotestit: `service/src/integrationTest/kotlin/evaka/instance/tampere/archival/TampereArchivalClientTest.kt`
