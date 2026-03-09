{
  /*
SPDX-FileCopyrightText: 2021 City of Turku

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React from 'react'

import ExternalLink from 'lib-components/atoms/ExternalLink'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { H1, H2, H3, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import type { Translations } from 'lib-customizations/citizen'
import type { DeepPartial } from 'lib-customizations/types'

const customerContactText = function () {
  return (
    <>
      {' '}
      varhaiskasvatuksen palveluohjaukseen puh.{' '}
      <a href="tel:+35822625610">02 2625610</a>.
    </>
  )
}

const fi: DeepPartial<Translations> = {
  calendar: {
    absenceModal: {
      absenceTypes: {
        PLANNED_ABSENCE: 'Sopimuspoissaolo'
      },
      selectChildrenInfo: 'Ilmoita tässä vain koko päivän poissaolot.'
    }
  },
  children: {
    serviceApplication: {
      startDateInfo:
        'Valitse päivä, jolloin haluaisit uuden palveluntarpeen alkavan. Palveluntarvetta voi vaihtaa vain kuukauden alusta ja sen tulee olla voimassa vähintään 4 kuukautta.'
    }
  },
  applications: {
    creation: {
      daycareInfo:
        'Varhaiskasvatushakemuksella haetaan varhaiskasvatuspaikkaa kunnalliseen päiväkotiin tai perhepäivähoitoon. Samalla hakemuksella voi hakea myös varhaiskasvatuksen palveluseteliä yksityiseen varhaiskasvatukseen valitsemalla Hakutoiveet-kohtaan palveluseteliyksikkö, johon halutaan hakea.',
      preschoolLabel: 'Ilmoittautuminen esiopetukseen ',
      preschoolInfo:
        'Maksutonta esiopetusta järjestetään neljä (4) tuntia päivässä. Lukuvuosi noudattaa pääosin koulujen loma- ja työaikoja. Tämän lisäksi lapselle voidaan hakea esiopetuksen täydentävään varhaiskasvatusta, jota tarjotaan esiopetuspaikoissa aamulla ennen esiopetuksen alkua ja iltapäivisin esiopetuksen jälkeen.',
      preschoolDaycareInfo: '',
      clubLabel: 'Avoimen varhaiskasvatuksen hakemus',
      clubInfo:
        'Avoimen varhaiskasvatuksen hakemuksella haetaan avoimen varhaiskasvatuksen kerhoihin ja leikkipuistotoimintaan.',
      applicationInfo: (
        <P>
          Huoltaja voi tehdä muutoksia hakemukseen verkkopalvelussa siihen asti,
          kun hakemus otetaan asiakaspalvelussa käsittelyyn. Tämän jälkeen
          muutokset tai hakemuksen peruminen on mahdollista ottamalla yhteyttä
          {customerContactText()}
        </P>
      ),
      duplicateWarning:
        'Lapsella on jo samantyyppinen, keskeneräinen hakemus. Palaa Hakemukset-näkymään ja muokkaa olemassa olevaa hakemusta tai ota yhteyttä Varhaiskasvatuksen asiakaspalveluun.',
      transferApplicationInfo: {
        DAYCARE:
          'Lapsella on jo paikka Turun varhaiskasvatuksessa. Tällä hakemuksella voit hakea siirtoa toiseen varhaiskasvatusta tarjoavaan yksikköön Turussa.'
      }
    },
    editor: {
      heading: {
        title: {
          DAYCARE: 'Varhaiskasvatus- ja palvelusetelihakemus',
          PRESCHOOL: 'Ilmoittautuminen esiopetukseen',
          CLUB: 'Hakemus avoimeen varhaiskasvatukseen'
        },
        info: {
          DAYCARE: (
            <>
              <P>
                Varhaiskasvatusta voi hakea ympäri vuoden. Hakemus on jätettävä
                neljä kuukautta ennen kuin tarvitsette paikan. Mikäli
                tarvitsette varhaiskasvatusta kiireellisesti työn tai
                opiskelujen vuoksi, tulee paikkaa hakea viimeistään kaksi
                viikkoa ennen.
              </P>
              <p>
                Saatte kirjallisen päätöksen varhaiskasvatuspaikasta{' '}
                <ExternalLink
                  text="Suomi.fi-viestit -palveluun"
                  href="https://www.suomi.fi/viestit"
                  newTab
                />{' '}
                tai postitse, mikäli et ole ottanut Suomi.fi-palvelua käyttöön.
                Päätös on nähtävillä myös eVaka-palvelussa kohdassa Hakeminen -
                Päätökset.
              </p>
              <P $fitted={true}>* Tähdellä merkityt tiedot ovat pakollisia</P>
            </>
          ),
          PRESCHOOL: (
            <>
              <P>
                Esiopetukseen osallistutaan vuosi ennen oppivelvollisuuden
                Esiopetus on maksutonta. Lukuvuoden 2026–2027 esiopetukseen
                ilmoittaudutaan 1.1-15.1.2026. Esiopetus alkaa 11.8.2026.
                Ilmoittautumisen yhteydessä perustele muut lisätiedot kohtaan
                hakuvaihtoehtosi.
              </P>
              <P>
                Päätökset tulevat{' '}
                <a
                  href="https://www.suomi.fi/viestit"
                  target="_blank"
                  rel="noreferrer"
                >
                  Suomi.fi-viestit
                </a>{' '}
                -palveluun tai postitse, mikäli et ole ottanut Suomi.fi
                -palvelua käyttöön.
              </P>
              <P $fitted={true}>* Tähdellä merkityt tiedot ovat pakollisia</P>
            </>
          ),
          CLUB: (
            <>
              <P>
                Avoimeen varhaiskasvatukseen voi hakea ympäri vuoden ja paikka
                myönnetään siihen saakka, kunnes paikka irtisanotaan tai lapsi
                siirtyy varhaiskasvatukseen tai esiopetukseen. Päätös avoimesta
                varhaiskasvatuspaikasta tulee Suomi.fi-palveluun tai postitse,
                mikäli ette ole ottanut palvelua käyttöön. Päätös on nähtävillä
                myös eVaka-palvelussa kohdassa Hakeminen - Päätökset.
              </P>
              <P>
                Turun kaupungin järjestämä avoin varhaiskasvatustoiminta on
                maksutonta (kerho ja leikkipuistotoiminta).
              </P>
              <P>
                Lisätietoa avoimesta varhaiskasvatuksesta Turun kaupungin
                verkkosivuilta:{' '}
                <ExternalLink
                  text="Kerhot, leikkipuistotoiminta ja avoimet päiväkodit."
                  href="https://www.turku.fi/varhaiskasvatus-ja-esiopetus/avoin-varhaiskasvatustoiminta"
                  newTab
                />
              </P>
              <P>* Tähdellä merkityt tiedot ovat pakollisia.</P>
            </>
          )
        }
      },
      serviceNeed: {
        preparatory: 'Lapsi tarvitsee tukea suomen kielen oppimisessa.',
        preparatoryInfo:
          'Jokaiselle lapselle, jonka äidinkieli ei ole suomi, ruotsi tai saame. Turun varhaiskasvatuksessa arvioidaan lapsen valmistavan opetuksen tarve.',
        startDate: {
          header: {
            DAYCARE: 'Varhaiskasvatuksen aloitus',
            PRESCHOOL: 'Esiopetuksen alkaminen',
            CLUB: 'Avoimen varhaiskasvatuksen alkaminen'
          },
          clubTerm: 'Avoimen varhaiskasvatuksen toimintakausi',
          clubTerms: 'Avoimen varhaiskasvatuksen toimintakaudet',
          label: {
            DAYCARE: 'Toivottu aloituspäivä',
            PRESCHOOL: 'Toivottu aloituspäivä',
            CLUB: 'Avoimen varhaiskasvatuksen toivottu aloituspäivä'
          },
          info: {
            DAYCARE: [],
            PRESCHOOL: [
              'Esiopetukseen osallistutaan vuosi ennen oppivelvollisuuden alkamista. Esiopetus on maksutonta. Lukuvuoden 2026–2027 esiopetukseen ilmoittaudutaan 1.1-15.1.2026. Esiopetus alkaa 11.8.2026. Ilmoittautumisen yhteydessä perustele muut lisätiedot kohtaan hakuvaihtoehtosi.'
            ],
            CLUB: [
              'Avoimen varhaiskasvatuksen kerhot ja leikkipuistotoiminta noudattavat pääsääntöisesti esiopetuksen työ- ja loma-aikoja. Lapsi voi osallistua yhteen avoimen varhaiskasvatuspalveluun kerralla, poissulkien perhekerhot.'
            ]
          },
          instructions: {
            DAYCARE: (
              <>
                Toivottua aloituspäivää on mahdollista muuttaa myöhemmäksi
                siihen saakka, kunnes palveluohjaus on ottanut hakemuksen
                käsittelyyn. Tämän jälkeen toivotun aloituspäivän muutokset
                tehdään ottamalla yhteyttä
                {customerContactText()}
              </>
            ),
            PRESCHOOL: (
              <>
                Toivottua aloituspäivää on mahdollista muuttaa myöhemmäksi
                siihen saakka, kunnes palveluohjaus on ottanut hakemuksen
                käsittelyyn. Tämän jälkeen toivotun aloituspäivän muutokset
                tehdään ottamalla yhteyttä
                {customerContactText()}
              </>
            ),
            CLUB: null
          }
        },
        clubDetails: {
          wasOnDaycare:
            'Lapsella on varhaiskasvatuspaikka, josta hän luopuu avoimen varhaiskasvatuspaikan saadessaan.',
          wasOnDaycareInfo: '',
          wasOnClubCare:
            'Lapsi on ollut avoimessa varhaiskasvatuksessa edellisen toimintakauden aikana.',
          wasOnClubCareInfo: ''
        },
        urgent: {
          attachmentsMessage: {
            text: (
              <P $fitted={true}>
                Mikäli varhaiskasvatuspaikan tarve johtuu äkillisestä
                työllistymisestä tai opiskelusta, tulee paikkaa hakea
                viimeistään kaksi viikkoa ennen kuin tarve alkaa. Hakemuksen
                liitteenä tulee olla työ- tai opiskelutodistus molemmilta
                samassa taloudessa asuvilta huoltajilta. Kahden viikon
                käsittelyaika alkaa siitä, kun olemme vastaanottaneet hakemuksen
                tarvittavine liitteineen. Jos et voi lisätä liitteitä
                hakemukselle sähköisesti, ole yhteydessä puhelimitse
                {customerContactText()} Voit myös lähettää liitteet postitse
                osoitteeseen Varhaiskasvatuksen palveluohjaus PL 355, 20101
                Turun kaupunki tai toimittamalla Kauppatorin Monitoriin,
                Varhaiskasvatuksen palveluohjaus, Aurakatu 8.
              </P>
            )
          }
        },
        shiftCare: {
          instructions:
            'Ilta- ja vuorohoidolla tarkoitetaan pääasiassa klo 6.00–18.00 ulkopuolella ja viikonloppuisin sekä ympärivuorokautisesti tapahtuvaa varhaiskasvatusta. Mikäli tarvitset ilta- tai vuorohoitoa, täsmennä tarvetta hakemuksen Muut lisätiedot -kohdassa.',
          attachmentsMessage: {
            DAYCARE: (
              <P>
                Ilta-, ja vuorohoito on tarkoitettu lapsille, joiden molemmat
                vanhemmat ovat vuorotyössä tai opiskelevat pääsääntöisesti
                iltaisin ja/tai viikonloppuisin. Hakemuksen liitteeksi
                toimitetaan molempien vanhempien osalta työnantajan todistus
                vuorotyöstä tai opiskelusta johtuvasta ilta- tai vuorohoidon
                tarpeesta. Jos et voi lisätä liitteitä hakemukselle sähköisesti,
                ole yhteydessä puhelimitse Varhaiskasvatuksen palveluohjaukseen
                02 2625610. Voit myös lähettää liitteet postitse osoitteeseen
                Varhaiskasvatuksen palveluohjaus PL 355, 20101 Turun kaupunki
                tai toimittamalla Kauppatorin Monitoriin, Varhaiskasvatuksen
                palveluohjaus, Aurakatu 8.
              </P>
            ),
            PRESCHOOL: (
              <>
                <P>
                  Esiopetushakemukselle pyydämme liittämään samassa taloudessa
                  asuvien huoltajien osalta todistukset työnantajalta
                  säännöllisestä vuorotyöstä tai oppilaitoksen edustajalta
                  päätoimisesta iltaopiskelusta. Dokumenttien tulee olla
                  kirjattu sinä vuonna, kun hakemus esiopetukseen tehdään.
                </P>
                <P>
                  Ilta-, ja vuorohoito on tarkoitettu lapsille, joiden molemmat
                  vanhemmat ovat vuorotyössä tai opiskelevat pääsääntöisesti
                  iltaisin ja/tai viikonloppuisin. Hakemuksen liitteeksi
                  toimitetaan molempien vanhempien osalta työnantajan todistus
                  vuorotyöstä tai opiskelusta johtuvasta ilta- tai vuorohoidon
                  tarpeesta. Jos et voi lisätä liitteitä hakemukselle
                  sähköisesti, ole yhteydessä puhelimitse Varhaiskasvatuksen
                  palveluohjaukseen 02 2625610. Voit myös lähettää liitteet
                  postitse osoitteeseen Varhaiskasvatuksen palveluohjaus PL 355,
                  20101 Turun kaupunki tai toimittamalla Kauppatorin Monitoriin,
                  Varhaiskasvatuksen palveluohjaus, Aurakatu 8.
                </P>
              </>
            )
          },
          attachmentsSubtitle:
            'Lisää tähän molemmilta vanhemmilta joko työnantajan todistus vuorotyöstä tai todistus opiskelusta iltaisin/viikonloppuisin.'
        },
        assistanceNeedInstructions: {
          DAYCARE:
            'Lapsen tuen tarpeella tarkoitetaan sellaisten tukitoimien tarvetta, jotka on osoitettu asiantuntijalausunnoin. Tukitoimet toteutuvat lapsen arjessa osana varhaiskasvatuksen muuta toimintaa. Varhaiskasvatuksen erityisopettaja ottaa hakijaan yhteyttä, jotta lapsen tarpeet voidaan ottaa huomioon varhaiskasvatuspaikkaa myönnettäessä.',
          CLUB: 'Lapsen tuen tarpeella tarkoitetaan sellaisten tukitoimien tarvetta, jotka on osoitettu asiantuntijalausunnoin. Tukitoimet toteutuvat lapsen arjessa osana varhaiskasvatuksen muuta toimintaa. Varhaiskasvatuksen erityisopettaja ottaa hakijaan yhteyttä, jotta lapsen tarpeet voidaan ottaa huomioon varhaiskasvatuspaikkaa myönnettäessä.',
          PRESCHOOL:
            'Valitse hakemuksesta tämä kohta, jos lapsi tarvitsee kehitykselleen ja/tai oppimiselleen tukea esiopetusvuonna. Tukea toteutetaan lapsen arjessa osana esiopetuksen ja varhaiskasvatuksen muuta toimintaa. Varhaiskasvatuksen erityisopettaja ottaa hakijaan yhteyttä, jotta lapsen tuen tarve voidaan ottaa huomioon esiopetuspaikkaa osoitettaessa.'
        },
        partTime: {
          true: 'Osapäiväinen (max 20h/vko, max 84h/kk)',
          false: 'Kokopäiväinen'
        },
        dailyTime: {
          label: {
            DAYCARE: 'Palveluntarve',
            PRESCHOOL:
              ' Esiopetusta tarjotaan päiväkodeissa ja kouluissa neljä tuntia päivässä. Ilmoita lapsen tarvitsema varhaiskasvatusaika siten, että̈ se sisältää myös esiopetusajan (esim. 7.00–17.00). Aika tarkennetaan varhaiskasvatuksen alkaessa. Päivittäisen varhaiskasvatusajan vaihdellessa päivittäin tai viikoittain (esim. vuorohoidossa), ilmoita tarve tarkemmin hakemuksen Muut lisätiedot -kohdassa.'
          },
          connectedDaycare:
            'Haen myös esiopetuksen täydentävää varhaiskasvatusta.',
          connectedDaycareInfo: (
            <>
              <P>
                Voit hakea lapselle tarvittaessa esiopetukseen täydentävää
                maksullista varhaiskasvatusta. Jos haluat aloittaa
                varhaiskasvatuksen myöhemmin kuin esiopetus alkaa, kirjoita
                haluttu aloituspäivämäärä hakemuksen “Muut lisätiedot” -kohtaan.
                Palveluseteliä yksityiseen päiväkotiin haetaan valitsemalla
                hakutoiveeksi se palveluseteliyksikkö, johon halutaan hakea.
              </P>
              <P>
                Saat varhaiskasvatuspaikasta kirjallisen päätöksen{' '}
                <a
                  href="https://www.suomi.fi/viestit"
                  target="_blank"
                  rel="noreferrer"
                >
                  Suomi.fi-viestit
                </a>{' '}
                -palveluun tai postitse, jos et ole ottanut Suomi.fi-viestit
                -palvelua käyttöön. Päätös on nähtävillä myös eVaka-palvelussa
                kohdassa Hakeminen - Päätökset.
              </P>
            </>
          )
        }
      },
      verification: {
        title: {
          CLUB: 'Avoimen varhaiskasvatushakemuksen tarkistaminen'
        },
        serviceNeed: {
          wasOnClubCareYes:
            'Lapsi on ollut avoimessa varhaiskasvatuksessa edellisen toimintakauden aikana.',
          connectedDaycare: {
            label: 'Täydentävä varhaiskasvatus',
            withConnectedDaycare:
              'Haen myös esiopetuksen täydentävää varhaiskasvatusta.',
            withoutConnectedDaycare: 'Ei'
          }
        }
      },
      unitPreference: {
        title: 'Hakutoive',
        siblingBasis: {
          title: 'Haku sisarperusteella',
          info: {
            DAYCARE: (
              <>
                <P>
                  Lapsella on sisarusperuste samaan varhaiskasvatuspaikkaan,
                  jossa hänen sisaruksensa on. Tavoitteena on järjestää
                  sisarukset samaan varhaiskasvatuspaikkaan perheen niin
                  toivoessa. Jos haet paikkaa sisaruksille, jotka{' '}
                  <b>eivät vielä ole</b> varhaiskasvatuksessa, kirjoita tieto
                  hakemuksen Muut lisätiedot -kohtaan.
                </P>
                <P>
                  Täytä nämä tiedot vain, jos käytät sisarusperustetta, sekä
                  valitse alla olevissa hakutoiveissa ensisijaiseksi toiveeksi
                  sama varhaiskasvatusyksikkö, jossa lapsen sisarus on.
                </P>
              </>
            ),
            PRESCHOOL: (
              <>
                <P>Haku sisarperusteella ei ole käytössä Turussa.</P>
              </>
            ),
            CLUB: (
              <>
                <P>
                  Tavoitteena on sijoittaa sisarukset samaan avoimeen
                  varhaisvatukseen perheen niin toivoessa.
                </P>
                <P>
                  Täytä nämä tiedot vain, jos käytät sisarusperustetta, sekä
                  valitse alla olevassa Hakutoiveet-kohdassa ensisijaiseksi
                  toiveeksi sama avoimen varhaiskasvatuksen yksikkö, jossa
                  lapsen sisarus on.
                </P>
              </>
            )
          },
          checkbox: {
            DAYCARE:
              'Haen ensisijaisesti samaan paikkaan, jossa lapsen sisarus on jo varhaiskasvatuksessa.',
            PRESCHOOL:
              'Haen ensisijaisesti samaan paikkaan, jossa lapsen sisarus on.',
            CLUB: 'Haen ensisijaisesti samaan avoimeen varhaiskasvatukseen, jossa lapsen sisarus jo on.'
          },
          radioLabel: {
            DAYCARE:
              'Valitse sisarus, jonka kanssa haet samaan varhaiskasvatuspaikkaan',
            PRESCHOOL: 'Valitse sisarus, jonka kanssa haet samaan paikkaan',
            CLUB: 'Valitse sisarus, jonka kanssa haet samaan avoimeen varhaiskasvatuspaikkaan'
          },
          otherSibling: 'Muu sisarus',
          names: 'Sisaruksen etunimet ja sukunimi',
          namesPlaceholder: 'Etunimet ja sukunimi',
          ssn: 'Sisaruksen henkilötunnus',
          ssnPlaceholder: 'Henkilötunnus'
        },
        units: {
          title: (maxUnits: number): string =>
            maxUnits === 1 ? 'Hakutoive' : 'Hakutoiveet',
          startDateMissing:
            'Päästäksesi valitsemaan hakutoiveet valitse ensin toivottu aloituspäivä "Palvelun tarve" -osiosta',
          info: {
            DAYCARE: (
              <>
                <P>
                  Voit hakea 1-3 paikkaa toivomassasi järjestyksessä.
                  Hakutoiveet eivät takaa paikkaa toivotussa yksikössä, mutta
                  mahdollisuus toivotun paikan saamiseen kasvaa antamalla
                  useamman vaihtoehdon.
                </P>
                <P>
                  Näet eri varhaiskasvatusyksiköiden sijainnin valitsemalla
                  ‘Yksiköt kartalla’.
                </P>
                <P>
                  Palveluseteliä haetaan valitsemalla hakutoiveeksi se
                  palveluseteliyksikkö, johon halutaan hakea. Jos ensisijainen
                  valintasi on palveluseteliyksikkö, ota yhteyttä kyseiseen
                  yksikköön.{' '}
                  <i>
                    Palveluseteliyksikköön haettaessa myös yksikön esimies saa
                    tiedon hakemuksesta.
                  </i>
                </P>
              </>
            ),
            PRESCHOOL: (
              <>
                <P>
                  Voit hakea 1-3 paikka paikkaa toivomassasi järjestyksessä.
                  Hakutoiveet eivät takaa paikkaa toivotussa yksikössä, mutta
                  mahdollisuus toivotun paikan saamiseen kasvaa antamalla
                  useamman vaihtoehdon.
                </P>
                <P>
                  Näet eri yksiköiden sijainnin valitsemalla ‘Yksiköt kartalla’.
                </P>
                <P>
                  Palveluseteliä haetaan valitsemalla hakutoiveeksi se
                  palveluseteliyksikkö, johon halutaan hakea.
                  Palveluseteliyksikköön haettaessa myös yksikön esimies saa
                  tiedon hakemuksesta.
                </P>
              </>
            ),
            CLUB: (
              <>
                <P>
                  Voit hakea 1–3 paikkaa toivomassasi järjestyksessä.
                  Hakutoiveet eivät takaa paikkaa toivotussa avoimessa
                  varhaiskasvatuksessa, mutta mahdollisuus toivotun paikan
                  saamiseen kasvaa antamalla useamman vaihtoehdon.
                </P>
                <P>
                  Näet eri avoimien varhaiskasvatuspaikkojen sijainnin
                  valitsemalla ’Yksiköt kartalla’.
                </P>
                <P>
                  Yksiköt kartalla -linkki avaa kartan Turun näkymään. Yksikön
                  kieli -jätetään toistaiseksi ennalleen.
                </P>
              </>
            )
          },
          mapLink: 'Yksiköt kartalla',
          serviceVoucherLink:
            'https://www.turku.fi/varhaiskasvatus-ja-esiopetus/maksut-tuet-ja-palveluseteli',
          languageFilter: {
            label: 'Yksikön kieli',
            fi: 'suomi',
            sv: 'ruotsi'
          },
          select: {
            label: (maxUnits: number): string =>
              maxUnits === 1 ? 'Valitse hakutoive' : 'Valitse hakutoiveet',
            placeholder: 'Hae yksiköitä',
            maxSelected: 'Maksimimäärä yksiköitä valittu',
            noOptions: 'Ei hakuehtoja vastaavia yksiköitä'
          },
          preferences: {
            label: (maxUnits: number): string =>
              maxUnits === 1
                ? 'Valitsemasi hakutoive'
                : 'Valitsemasi hakutoiveet',
            noSelections: 'Ei valintoja',
            info: (maxUnits: number) =>
              maxUnits === 1
                ? 'Valitse yksi varhaiskasvatusyksikkö'
                : `Valitse 1-${maxUnits} varhaiskasvatusyksikköä ja järjestä ne toivomaasi järjestykseen. Voit muuttaa järjestystä nuolien avulla.`,
            fi: 'suomenkielinen',
            sv: 'ruotsinkielinen',
            en: 'englanninkielinen',
            moveUp: 'Siirrä ylöspäin',
            moveDown: 'Siirrä alaspäin',
            remove: 'Poista hakutoive'
          }
        }
      },
      contactInfo: {
        secondGuardianInfoPreschoolSeparated:
          'Toisen huoltajan tiedot haetaan automaattisesti väestötietojärjestelmästä. Tietojemme mukaan lapsen toinen huoltaja asuu eri osoitteessa. Esiopetukseen ilmoittautumisesta tulee sopia yhdessä toisen huoltajan kanssa.',
        info: (
          <>
            <P>
              Ilmoita hakemuksella kaikki samassa taloudessa asuvat aikuiset ja
              lapset.
            </P>
            <P data-qa="contact-info-text">
              Henkilötiedot on haettu väestötiedoista, eikä niitä voi muuttaa
              tällä hakemuksella. Jos henkilötiedoissa on virheitä, päivitäthän
              tiedot{' '}
              <ExternalLink
                text="Digi- ja Väestötietoviraston sivuilla"
                href="https://dvv.fi/henkiloasiakkaat"
                newTab
              />
              . Mikäli osoitteenne on muuttumassa, voit lisätä tulevan osoitteen
              erilliseen kohtaan hakemuksella; lisää tuleva osoite sekä lapselle
              että huoltajalle. Virallisena osoitetietoa pidetään vasta, kun se
              on päivittynyt väestötietojärjestelmään. Päätökset esiopetus- ja
              varhaiskasvatuspaikoista toimitetaan automaattisesti myös eri
              osoitteessa asuvalle väestötiedoista löytyvälle huoltajalle.
            </P>
          </>
        ),
        futureAddressInfo:
          'Turun varhaiskasvatuksen virallisena osoitteena pidetään väestötiedoista saatavaa osoitetta. Osoite väestötiedoissa muuttuu hakijan tehdessä muutosilmoituksen postiin tai Digi- ja väestötietovirastoon.'
      },
      fee: {
        info: {
          DAYCARE: (
            <P>
              Kunnallisen varhaiskasvatuksen asiakasmaksut ja palvelusetelin
              omavastuuosuus määräytyvät prosenttiosuutena perheen
              bruttotuloista. Maksut vaihtelevat perheen koon ja tulojen sekä
              varhaiskasvatusajan mukaan. Mikäli varhaiskasvatuspaikan hinta
              yksityisellä on enemmän kuin palvelusetelin arvo, erotuksen maksaa
              perhe. Perhe toimittaa tuloselvityksen bruttotuloistaan
              tuloselvityslomakkeella mahdollisimman pian siitä, kun lapsi on
              aloittanut varhaiskasvatuksessa.
            </P>
          ),
          PRESCHOOL: (
            <P>
              Esiopetus on maksutonta, mutta täydentävä varhaiskasvatus on
              maksullista. Jos lapsi osallistuu esiopetukseen täydentävään
              varhaiskasvatukseen, perhe toimittaa tuloselvityksen
              bruttotuloistaan tulonselvityslomakkeella mahdollisimman pian
              siitä, kun lapsi on aloittanut varhaiskasvatuksessa.
            </P>
          )
        },
        links: (
          <>
            <P>
              Tuloselvityslomake löytyy eVakassa Käyttäjä-valikosta kohdasta
              Tulotiedot.
            </P>
            <P>
              Lisätietoa asiakasmaksuista löydät Turun kaupungin nettisivuilta:{' '}
              <ExternalLink
                href="https://www.turku.fi/varhaiskasvatus-ja-esiopetus/maksut-tuet-ja-palveluseteli"
                text="Varhaiskasvatuksen asiakasmaksut."
                newTab
              />
            </P>
          </>
        )
      },
      sentInfo: {
        title: 'Hakemus on lähetetty',
        text: 'Hakemukseen voi tehdä muutoksia siihen saakka, kunnes varhaiskasvatuksen palveluohjaus on ottanut sen käsittelyyn.',
        ok: 'Selvä!'
      },
      actions: {
        allowOtherGuardianAccess: (
          <span>
            Ymmärrän, että tieto hakemuksesta menee myös lapsen toiselle
            huoltajalle. Jos tieto ei saa mennä toiselle huoltajalle, ole
            yhteydessä palveluohjaukseen..
          </span>
        )
      }
    }
  },
  applicationsList: {
    title: 'Hakeminen varhaiskasvatukseen',
    summary: (
      <>
        <P $width="800px">
          Lapsen huoltaja voi tehdä lapselle hakemuksen varhaiskasvatukseen ja
          avoimeen varhaiskasvatukseen tai ilmoittaa lapsen esiopetukseen.
          Samalla hakemuksella voi hakea myös varhaiskasvatuksen palveluseteliä,
          hakemalla varhaiskasvatuspaikkaa palveluseteliyksiköstä. Huoltajan
          lasten tiedot haetaan tähän näkymään automaattisesti
          Väestötietojärjestelmästä.
        </P>
        <P $width="800px">
          Jos lapsella on jo paikka Turun varhaiskasvatuksessa ja halutaan hakea
          siirtoa toiseen yksikköön, tehdään lapselle uusi hakemus.
        </P>
      </>
    )
  },
  footer: {
    cityLabel: '© Turun kaupunki',
    privacyPolicyLink: (
      <ExternalLink
        href="https://www.turku.fi/tietosuoja"
        text="Tietosuojaselosteet"
        newTab={true}
        data-qa="footer-policy-link"
      />
    ),
    sendFeedbackLink: (
      <ExternalLink
        href="https://opaskartta.turku.fi/efeedback"
        text="Lähetä palautetta"
        newTab={true}
        data-qa="footer-feedback-link"
      />
    )
  },
  loginPage: {
    title: 'Turun kaupungin varhaiskasvatus',
    login: {
      title: 'Kirjaudu käyttäjätunnuksella',
      paragraph:
        'Huoltajat, joiden lapsi on jo varhaiskasvatuksessa tai esiopetuksessa: hoida lapsesi päivittäisiä varhaiskasvatusasioita kuten lue viestejä ja ilmoita lapsen läsnäoloajat ja poissaolot.',
      link: 'Kirjaudu sisään',
      infoBoxText: (
        <>
          <P>
            Voit luoda eVaka käyttäjätunnuksen kirjautumalla vahvasti
            tunnistautuen.
          </P>
        </>
      )
    }
  },
  map: {
    mainInfo: `Tässä näkymässä voit hakea kartalta Turun varhaiskasvatus-, esiopetus- ja avoimia varhaiskasvatuspaikkoja. Tietoa yksityisistä päiväkodeista löydät Turun varhaiskasvatuksen kotisivuilta.`,
    privateUnitInfo: <></>,
    serviceVoucherLink:
      'https://www.turku.fi/varhaiskasvatus-ja-esiopetus/maksut-tuet-ja-palveluseteli',
    searchPlaceholder: 'Esim. Arkeologinkadun päiväkoti.',
    careTypes: {
      CLUB: 'Avoin varhaiskasvatus',
      DAYCARE: 'Varhaiskasvatus',
      PRESCHOOL: 'Esiopetus'
    }
  },
  decisions: {
    applicationDecisions: {
      decision: 'Päätös',
      type: {
        CLUB: 'Kerho',
        DAYCARE: 'Varhaiskasvatus',
        DAYCARE_PART_TIME: 'Osa-aikainen varhaiskasvatus',
        PRESCHOOL: 'Esiopetus',
        PRESCHOOL_DAYCARE: 'Täydentävä varhaiskasvatus',
        PRESCHOOL_CLUB: 'Esiopetuksen kerho',
        PREPARATORY_EDUCATION: 'Valmistava opetus'
      }
    },
    assistanceDecisions: {
      title: 'Päätös tuesta varhaiskasvatuksessa',
      decision: {
        jurisdiction: 'Toimivalta',
        jurisdictionText: (): React.ReactNode =>
          'Kasvatuksen ja opetuksen palvelukokonaisuuden hallinnon järjestämispäätös 3 luku 11 §',
        unitMayChange: 'Loma-aikoina tuen järjestämispaikka saattaa muuttua.',
        appealInstructionsTitle: 'Oikaisuvaatimusohjeet',
        appealInstructions: (
          <>
            <H3>Oikaisuvaatimusohjeet</H3>
            <P>
              Edellä mainittuun päätökseen saa hakea oikaisua Lounais-Suomen
              aluehallintovirastolta 30 päivän kuluessa päätöksen
              tiedoksisaannista. Päätökseen ei saa hakea muutosta valittamalla
              tuomioistuimeen.
            </P>
            <P>Oikaisuvaatimuksen saa tehdä</P>
            <ul>
              <li>se, johon päätös on kohdistettu </li>
              <li>
                {' '}
                tai jonka oikeuteen, velvollisuuteen tai etuun päätös
                välittömästi vaikuttaa.
              </li>
            </ul>
            <h3>Tiedoksisaanti</h3>
            <P>
              Mikäli päätös annetaan tiedoksi kirjeellä, asianosaisen (lapsen
              huoltajan) katsotaan saaneen päätöksestä tiedon, jollei muuta
              näytetä, 7 päivän kuluttua kirjeen lähettämisestä.
            </P>
            <P>
              Käytettäessä tavallista sähköistä tiedoksiantoa katsotaan
              asianosaisen saaneen tiedon päätöksestä kolmantena päivänä viestin
              lähettämisestä.
            </P>
            <P>
              Mikäli päätös annetaan tiedoksi henkilökohtaisesti, asianosaisen
              (lapsen huoltajan) katsotaan saaneen päätöksestä tiedon sinä
              päivänä, jona päätös on luovutettu asianosaiselle tai hänen
              lailliselle edustajalleen.
            </P>
            <P>
              Postitse saantitodistusta vastaan lähetetystä asiakirjasta
              katsotaan asianosaisen saaneen tiedonsaantitodistuksen osoittamana
              aikana.
            </P>
            <P>
              Tiedoksisaantipäivää ei lueta oikaisuvaatimusaikaan. Jos
              oikaisuvaatimusajan viimeinen päivä on pyhäpäivä,
              itsenäisyyspäivä, vapunpäivä, joulu- tai juhannusaatto tai
              arkilauantai, saa oikaisuvaatimuksen tehdä ensimmäisenä
              arkipäivänä sen jälkeen.
            </P>
            <H3>Oikaisuvaatimuksen sisältö</H3>
            <P>
              Oikaisuvaatimus on tehtävä kirjallisesti. Myös sähköinen asiakirja
              täyttää vaatimuksen kirjallisesta muodosta.
            </P>
            <P>Oikaisuvaatimuksessa on ilmoitettava</P>
            <ul>
              <li>päätös, johon haetaan oikaisua</li>
              <li>
                miltä kohdin päätökseen haetaan oikaisua ja millaista oikaisua
                siihen vaaditaan tehtäväksi
              </li>
              <li>perusteet, joilla oikaisua vaaditaan</li>
            </ul>
            <P>
              Oikaisuvaatimuksessa on ilmoitettava tekijän nimi ja kotikunta.
              Jos oikaisuvaatimuksen tekijän puhevaltaa käyttää hänen laillinen
              edustajansa tai asiamiehensä tai jos oikaisuvaatimuksen laatijana
              on joku muu henkilö, oikaisuvaatimuksessa on ilmoitettava myös
              tämän nimi ja kotikunta.
            </P>
            <P>
              Oikaisuvaatimuksessa on lisäksi ilmoitettava postiosoite,
              puhelinnumero ja muut tarvittavat yhteystiedot. Jos
              oikaisuvaatimusviranomaisen päätös voidaan antaa tiedoksi
              sähköisenä viestinä, yhteystietona pyydetään ilmoittamaan myös
              sähköpostiosoite.
            </P>
            <P>
              Oikaisuvaatimuksen tekijän, laillisen edustajan tai asiamiehen on
              allekirjoitettava oikaisuvaatimus. Sähköistä asiakirjaa ei
              kuitenkaan tarvitse täydentää allekirjoituksella, jos asiakirjassa
              on tiedot lähettäjästä eikä asiakirjan alkuperäisyyttä tai eheyttä
              ole syytä epäillä.
            </P>
            <P>Oikaisuvaatimuksessa on liitettävä</P>
            <ul>
              <li>
                päätös, johon haetaan oikaisua, alkuperäisenä tai jäljennöksenä
              </li>
              <li>
                todistus siitä, minä päivänä päätös on annettu tiedoksi, tai muu
                selvitys oikaisuvaatimusajan alkamisesta
              </li>
              <li>
                asiakirjat, joihin oikaisuvaatimuksen tekijä vetoaa, jollei
                niitä ole aikaisemmin toimitettu viranomaiselle.
              </li>
            </ul>
            <H3>Oikaisuvaatimuksen toimittaminen</H3>
            <P>
              Oikaisuvaatimus on toimitettava oikaisuvaatimusajan kuluessa
              Lounais-Suomen aluehallintovirastolle osoitteella:
            </P>
            <P>
              Lounais-Suomen aluehallintovirasto
              <br />
              PL 4, 13035 AVI
              <br />
              Itsenäisyydenaukio 2, 20800 Turku
              <br />
              Sähköposti: kirjaamo.lounais@avi.fi
              <br />
              Puh: 0295 018 000
              <br />
              Kirjaamon aukioloaika: 8.00–16.15
              <br />
              Fax (02) 2511 820
            </P>
            <P>
              Oikaisuvaatimuksen aluehallintovirastolle voi tehdä myös
              sähköisessä asiointipalvelussa https://www.avi.fi -{'>'}{' '}
              henkilöasiakas -{'>'} oikaisuvaatimukset -{'>'} sähköinen
              asiointi.
            </P>
            <P>
              Omalla vastuulla oikaisuvaatimuksen voi lähettää postitse tai
              lähetin välityksellä. Postiin oikaisuvaatimusasiakirjat on
              jätettävä niin ajoissa, että ne ehtivät perille viimeistään
              oikaisuvaatimusajan viimeisenä päivänä ennen viraston aukioloajan
              päättymistä.
            </P>
            <P>
              Omalla vastuulla oikaisuvaatimuksen voi toimittaa ennen
              oikaisuvaatimusajan päättymistä myös telekopiona/faxina tai
              sähköpostilla. Määräajassa toimitettava asiakirja on lähetettävä
              ennen määräajan päättymistä siten, että asiakirja on viranomaisen
              käytettävissä vastaanottolaitteessa tai tietojärjestelmässä.
            </P>
          </>
        )
      }
    },
    assistancePreschoolDecisions: {
      jurisdiction: 'Toimivalta',
      jurisdictionText:
        'Kasvatuksen ja opetuksen palvelukokonaisuuden hallinnon järjestämispäätös 3 luku 11 §',
      appealInstructions: (
        <>
          <H3>Oikaisuvaatimusohjeet</H3>
          <P>
            Edellä mainittuun päätökseen saa hakea oikaisua Lounais-Suomen
            aluehallintovirastolta 14 päivän kuluessa päätöksen
            tiedoksisaannista. Päätökseen ei saa hakea muutosta valittamalla
            tuomioistuimeen.
          </P>
          <P>Oikaisuvaatimuksen saa tehdä</P>
          <ul>
            <li>se, johon päätös on kohdistettu </li>
            <li>
              {' '}
              tai jonka oikeuteen, velvollisuuteen tai etuun päätös välittömästi
              vaikuttaa.
            </li>
          </ul>
          <h3>Tiedoksisaanti</h3>
          <P>
            Mikäli päätös annetaan tiedoksi kirjeellä, asianosaisen (lapsen
            huoltajan) katsotaan saaneen päätöksestä tiedon, jollei muuta
            näytetä, 7 päivän kuluttua kirjeen lähettämisestä.
          </P>
          <P>
            Käytettäessä tavallista sähköistä tiedoksiantoa katsotaan
            asianosaisen saaneen tiedon päätöksestä kolmantena päivänä viestin
            lähettämisestä.
          </P>
          <P>
            Mikäli päätös annetaan tiedoksi henkilökohtaisesti, asianosaisen
            (lapsen huoltajan) katsotaan saaneen päätöksestä tiedon sinä
            päivänä, jona päätös on luovutettu asianosaiselle tai hänen
            lailliselle edustajalleen.
          </P>
          <P>
            Postitse saantitodistusta vastaan lähetetystä asiakirjasta katsotaan
            asianosaisen saaneen tiedonsaantitodistuksen osoittamana aikana.
          </P>
          <P>
            Tiedoksisaantipäivää ei lueta oikaisuvaatimusaikaan. Jos
            oikaisuvaatimusajan viimeinen päivä on pyhäpäivä, itsenäisyyspäivä,
            vapunpäivä, joulu- tai juhannusaatto tai arkilauantai, saa
            oikaisuvaatimuksen tehdä ensimmäisenä arkipäivänä sen jälkeen.
          </P>
          <H3>Oikaisuvaatimuksen sisältö</H3>
          <P>
            Oikaisuvaatimus on tehtävä kirjallisesti. Myös sähköinen asiakirja
            täyttää vaatimuksen kirjallisesta muodosta.
          </P>
          <P>Oikaisuvaatimuksessa on ilmoitettava</P>
          <ul>
            <li>päätös, johon haetaan oikaisua</li>
            <li>
              miltä kohdin päätökseen haetaan oikaisua ja millaista oikaisua
              siihen vaaditaan tehtäväksi
            </li>
            <li>perusteet, joilla oikaisua vaaditaan</li>
          </ul>
          <P>
            Oikaisuvaatimuksessa on ilmoitettava tekijän nimi ja kotikunta. Jos
            oikaisuvaatimuksen tekijän puhevaltaa käyttää hänen laillinen
            edustajansa tai asiamiehensä tai jos oikaisuvaatimuksen laatijana on
            joku muu henkilö, oikaisuvaatimuksessa on ilmoitettava myös tämän
            nimi ja kotikunta.
          </P>
          <P>
            Oikaisuvaatimuksessa on lisäksi ilmoitettava postiosoite,
            puhelinnumero ja muut tarvittavat yhteystiedot. Jos
            oikaisuvaatimusviranomaisen päätös voidaan antaa tiedoksi sähköisenä
            viestinä, yhteystietona pyydetään ilmoittamaan myös
            sähköpostiosoite.
          </P>
          <P>
            Oikaisuvaatimuksen tekijän, laillisen edustajan tai asiamiehen on
            allekirjoitettava oikaisuvaatimus. Sähköistä asiakirjaa ei
            kuitenkaan tarvitse täydentää allekirjoituksella, jos asiakirjassa
            on tiedot lähettäjästä eikä asiakirjan alkuperäisyyttä tai eheyttä
            ole syytä epäillä.
          </P>
          <P>Oikaisuvaatimuksessa on liitettävä</P>
          <ul>
            <li>
              päätös, johon haetaan oikaisua, alkuperäisenä tai jäljennöksenä
            </li>
            <li>
              todistus siitä, minä päivänä päätös on annettu tiedoksi, tai muu
              selvitys oikaisuvaatimusajan alkamisesta
            </li>
            <li>
              asiakirjat, joihin oikaisuvaatimuksen tekijä vetoaa, jollei niitä
              ole aikaisemmin toimitettu viranomaiselle.
            </li>
          </ul>
          <H3>Oikaisuvaatimuksen toimittaminen</H3>
          <P>
            Oikaisuvaatimus on toimitettava oikaisuvaatimusajan kuluessa
            Lounais-Suomen aluehallintovirastolle osoitteella:
          </P>
          <P>
            Lounais-Suomen aluehallintovirasto
            <br />
            PL 4, 13035 AVI
            <br />
            Itsenäisyydenaukio 2, 20800 Turku
            <br />
            Sähköposti: kirjaamo.lounais@avi.fi
            <br />
            Puh: 0295 018 000
            <br />
            Kirjaamon aukioloaika: 8.00–16.15
            <br />
            Fax (02) 2511 820
          </P>
          <P>
            Omalla vastuulla oikaisuvaatimuksen voi lähettää postitse tai
            lähetin välityksellä. Postiin oikaisuvaatimusasiakirjat on jätettävä
            niin ajoissa, että ne ehtivät perille viimeistään
            oikaisuvaatimusajan viimeisenä päivänä ennen viraston aukioloajan
            päättymistä.
          </P>
          <P>
            Omalla vastuulla oikaisuvaatimuksen voi toimittaa ennen
            oikaisuvaatimusajan päättymistä myös telekopiona/faxina tai
            sähköpostilla. Määräajassa toimitettava asiakirja on lähetettävä
            ennen määräajan päättymistä siten, että asiakirja on viranomaisen
            käytettävissä vastaanottolaitteessa tai tietojärjestelmässä.
          </P>
        </>
      ),
      disclaimer:
        'Perusopetuslain 17 §:n mukaan tämä päätös voidaan panna täytäntöön muutoksenhausta huolimatta.'
    },
    summary: 'Tälle sivulle saapuvat kaikki lapsen päätökset.'
  },
  personalDetails: {
    description: (
      <P>
        Täällä voit tarkistaa ja täydentää omat henkilö- ja yhteystietosi.
        Nimesi ja osoitteesi haetaan väestötietojärjestelmästä, ja mikäli ne
        muuttuvat, sinun tulee tehdä ilmoitus väestötietojärjestelmään.
      </P>
    )
  },
  income: {
    description: (
      <>
        <p data-qa="income-description-p1">
          Tällä sivulla voit lähettää selvitykset varhaiskasvatusmaksuun
          vaikuttavista tuloistasi. Voit myös tarkastella palauttamiasi
          tuloselvityksiä ja muokata tai poistaa niitä kunnes viranomainen on
          käsitellyt tiedot. Lomakkeen käsittelyn jälkeen voit päivittää
          tulotietojasi toimittamalla uuden lomakkeen.
        </p>
        <p data-qa="income-description-p2">
          <strong>
            Molempien samassa taloudessa asuvien aikuisten tulee toimittaa omat
            erilliset tuloselvitykset.
          </strong>
        </p>
        <p data-qa="income-description-p3">
          Kunnallisen varhaiskasvatuksen asiakasmaksut määräytyvät
          prosenttiosuutena perheen bruttotuloista. Maksut vaihtelevat perheen
          koon ja tulojen sekä varhaiskasvatusajan mukaan.
        </p>
        <p data-qa="income-description-p4">
          <a href="https://www.turku.fi/varhaiskasvatus-ja-esiopetus/maksut-tuet-ja-palveluseteli">
            Lisätietoja asiakasmaksuista
          </a>
        </p>
      </>
    ),
    incomeType: {
      startDate: 'Voimassa alkaen',
      endDate: 'Voimassaolo päättyy',
      title: 'Asiakasmaksun perusteet',
      agreeToHighestFee: 'Suostun korkeimpaan varhaiskasvatusmaksuun',
      highestFeeInfo:
        'Palveluntarpeen mukainen korkein maksu on voimassa toistaiseksi siihen saakka, kunnes toisin ilmoitan tai kunnes lapseni varhaiskasvatus päättyy. (Tulotietoja ei tarvitse toimittaa)',
      grossIncome: 'Maksun määritteleminen bruttotulojen mukaan'
    },
    grossIncome: {
      title: 'Bruttotulotietojen täyttäminen',
      description: (
        <>
          <P> </P>
        </>
      ),
      incomeSource: 'Tulotietojen toimitus',
      incomesRegisterConsent:
        'Hyväksyn, että tuloihini liittyviä tietoja tarkastellaan tulorekisteristä, ja toimitan mahdolliset etuustiedot liitteinä.',
      provideAttachments: 'Toimitan tulotietoni liitteenä',
      estimate: 'Arvio bruttotuloistani',
      estimatedMonthlyIncome: 'Keskimääräiset tulot sisältäen lomarahat, €/kk',
      otherIncome: 'Muut tulot',
      otherIncomeDescription:
        'Jos sinulla on muita tuloja, on niistä toimitettavana tositteet liitteinä. Listan tarvittavista liitteistä löydät lomakkeen alaosasta kohdasta: Tuloihin ja varhaiskasvatusmaksuihin liittyvät liitteet.',
      choosePlaceholder: 'Valitse',
      otherIncomeTypes: {
        PENSION: 'Eläke',
        ADULT_EDUCATION_ALLOWANCE: 'Aikuiskoulutustuki',
        SICKNESS_ALLOWANCE: 'Sairauspäiväraha',
        PARENTAL_ALLOWANCE: 'Äitiys- ja vanhempainraha',
        HOME_CARE_ALLOWANCE: 'Lasten kotihoidontuki',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE:
          'Joustava tai osittainen hoitoraha',
        ALIMONY: 'Elatusapu tai -tuki',
        INTEREST_AND_INVESTMENT_INCOME: 'Korko- ja osinkotulot',
        RENTAL_INCOME: 'Vuokratulot',
        UNEMPLOYMENT_ALLOWANCE: 'Työttömyyspäiväraha',
        LABOUR_MARKET_SUBSIDY: 'Työmarkkinatuki',
        ADJUSTED_DAILY_ALLOWANCE: 'Soviteltu päiväraha',
        JOB_ALTERNATION_COMPENSATION: 'Vuorotteluvapaakorvaus',
        REWARD_OR_BONUS: 'Palkkio tai bonus',
        RELATIVE_CARE_SUPPORT: 'Omaishoidontuki',
        BASIC_INCOME: 'Perustulo',
        FOREST_INCOME: 'Metsätulo',
        FAMILY_CARE_COMPENSATION: 'Perhehoidon palkkiot',
        REHABILITATION: 'Kuntoutustuki tai kuntoutusraha',
        EDUCATION_ALLOWANCE: 'Koulutuspäiväraha',
        GRANT: 'Apuraha',
        APPRENTICESHIP_SALARY: 'Palkkatulo oppisopimuskoulutuksesta',
        ACCIDENT_INSURANCE_COMPENSATION: 'Korvaus tapaturmavakuutuksesta',
        OTHER_INCOME: 'Muut tulot'
      },
      otherIncomeInfoLabel: 'Arviot muista tuloista',
      otherIncomeInfoDescription:
        'Kirjoita tähän arviot muiden tulojen määristä €/kk, esim. "Vuokratulot 150, lasten kotihoidontuki 300"'
    },
    entrepreneurIncome: {
      title: 'Yrittäjän tulotietojen täyttäminen',
      description:
        'Tällä lomakkeella voit tarvittaessa täyttää tiedot myös useammalle yritykselle valitsemalla kaikkia yrityksiäsi koskevat kohdat.',
      startOfEntrepreneurship: 'Yrittäjyys alkanut',
      spouseWorksInCompany: 'Työskenteleekö puoliso yrityksessä?',
      yes: 'Kyllä',
      no: 'Ei',
      startupGrantLabel: 'Saako yrityksesi starttirahaa?',
      startupGrant:
        'Yritykseni saa starttirahaa. Toimitan starttirahapäätöksen liitteenä.',
      checkupLabel: 'Tietojen tarkastus',
      checkupConsent:
        'Hyväksyn, että tuloihini liittyviä tietoja tarkastellaan tarvittaessa tulorekisteristä sekä Kelasta.',
      companyInfo: 'Yrityksen tiedot',
      companyForm: 'Yrityksen toimintamuoto',
      selfEmployed: 'Toiminimi',
      limitedCompany: 'Osakeyhtiö',
      partnership: 'Avoin yhtiö tai kommandiittiyhtiö',
      lightEntrepreneur: 'Kevytyrittäjyys',
      lightEntrepreneurInfo:
        'Maksutositteet palkoista ja työkorvauksista tulee toimittaa liitteinä.',
      partnershipInfo: ''
    },
    moreInfo: {
      title: 'Muita maksuun liittyviä tietoja',
      studentLabel: 'Oletko opiskelija?',
      student: 'Olen opiskelija.',
      studentInfo:
        'Opiskelijat toimittavat oppilaitoksesta opiskelutodistuksen ja päätöksen opintoetuudesta.',
      deductions: 'Vähennykset',
      alimony:
        'Maksan elatusmaksuja. Toimitan kopion maksutositteesta liitteenä.',
      otherInfoLabel: 'Lisätietoja tulotietoihin liittyen'
    },
    attachments: {
      title: 'Tuloihin ja varhaiskasvatusmaksuihin liittyvät liitteet',
      description:
        'Toimita tässä tuloihin tai varhaiskasvatusmaksuihin liittyvät liitteet. Liitteitä ei tarvita, jos perheenne on suostunut korkeimpaan maksuun.',
      required: {
        title: 'Tarvittavat liitteet'
      },
      attachmentNames: {
        PENSION: 'Päätös eläkkeestä',
        ADULT_EDUCATION_ALLOWANCE: 'Päätös aikuiskoulutustuesta',
        SICKNESS_ALLOWANCE: 'Päätös sairauspäivärahasta',
        PARENTAL_ALLOWANCE: 'Päätös äitiys- tai vanhempainrahasta',
        HOME_CARE_ALLOWANCE: 'Päätös kotihoidontuesta',
        FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE: 'Päätös hoitorahasta',
        ALIMONY: 'Elatussopimus tai päätös elatustuesta',
        UNEMPLOYMENT_ALLOWANCE: 'Päätös työttömyyspäivärahasta',
        LABOUR_MARKET_SUBSIDY: 'Päätös työmarkkinatuesta',
        ADJUSTED_DAILY_ALLOWANCE: 'Päätös päivärahasta',
        JOB_ALTERNATION_COMPENSATION: 'Tosite vuorotteluvapaakorvaus',
        REWARD_OR_BONUS: 'Palkkatosite bonuksesta tai/ja palkkiosta',
        RELATIVE_CARE_SUPPORT: 'Päätös omaishoidontuesta',
        BASIC_INCOME: 'Päätös perustulosta',
        FOREST_INCOME: 'Tosite metsätulosta',
        FAMILY_CARE_COMPENSATION: 'Tositteet perhehoidon palkkioista',
        REHABILITATION: 'Päätös kuntoutustuesta tai kuntoutusrahasta',
        EDUCATION_ALLOWANCE: 'Päätös koulutuspäivärahasta',
        GRANT: 'Tosite apurahasta',
        APPRENTICESHIP_SALARY: 'Tosite oppisopimuskoulutuksen palkkatuloista',
        ACCIDENT_INSURANCE_COMPENSATION:
          'Tosite tapaturmavakuutuksen korvauksesta',
        OTHER_INCOME: 'Liitteet muista tuloista',
        ALIMONY_PAYOUT: 'Maksutosite elatusmaksuista',
        INTEREST_AND_INVESTMENT_INCOME: 'Tositteet korko- ja osinkotuloista',
        RENTAL_INCOME: 'Tositteet vuokratuloista ja vastikkeesta',
        PAYSLIP_GROSS: 'Viimeisin palkkalaskelma',
        PAYSLIP_LLC: 'Viimeisin palkkalaskelma',
        STARTUP_GRANT: 'Starttirahapäätös',
        ACCOUNTANT_REPORT_PARTNERSHIP:
          'Kirjanpitäjän selvitys palkasta ja luontoiseduista',
        ACCOUNTANT_REPORT_LLC:
          'Kirjanpitäjän selvitys luontoiseduista ja osingoista',
        PROFIT_AND_LOSS_STATEMENT_SELF_EMPLOYED:
          'Tulos- ja taselaskelma tai veropäätös',
        PROFIT_AND_LOSS_STATEMENT_PARTNERSHIP: 'Tulos- ja taselaskelma',
        SALARY: 'Maksutositteet palkoista ja työkorvauksista',
        PROOF_OF_STUDIES:
          'Opiskelutodistus tai päätös työttömyyskassan opintoetuudesta / työllisyysrahaston koulutustuesta',
        CHILD_INCOME: 'Tositteet lapsen tuloista'
      }
    },
    selfEmployed: {
      info: '',
      attachments:
        'Toimitan liitteinä yrityksen viimeisimmän tulos- ja taselaskelman tai veropäätöksen.',
      estimatedIncome:
        'Olen uusi yrittäjä. Täytän arvion keskimääräisistä kuukausituloistani. Toimitan tuloslaskelman ja taseen mahdollisimman pian.',
      estimatedMonthlyIncome: 'Keskimääräiset tulot €/kk',
      timeRange: 'Aikavälillä'
    },
    formDescription: (
      <>
        <P data-qa="income-formDescription-p1">
          Tuloselvitys liitteineen palautetaan varhaiskasvatuksen
          aloituskuukauden aikana. Maksu voidaan määrätä puutteellisilla
          tulotiedoilla korkeimpaan maksuun. Puutteellisia tulotietoja ei
          korjata takautuvasti oikaisuvaatimusajan jälkeen.
        </P>
        <P>
          Asiakasmaksu peritään päätöksen mukaisesta varhaiskasvatuksen
          alkamispäivästä lähtien.
        </P>
        <P>
          Asiakkaan on viipymättä ilmoitettava tulojen ja perhekoon muutoksista
          varhaiskasvutuksen asiakasmaksuihin. Viranomainen on tarvittaessa
          oikeutettu perimään varhaiskasvatusmaksuja myös takautuvasti.
        </P>
        <P>
          <strong>Huomioitavaa:</strong>
        </P>
        <Gap $size="xs" />
        <UnorderedList data-qa="income-formDescription-ul">
          <li>
            Jos tulosi ylittävät perhekoon mukaisen tulorajan, hyväksy korkein
            varhaiskasvatusmaksu. Tällöin sinun ei tarvitse selvittää tulojasi
            lainkaan.
          </li>
          <li>
            Jos perheeseesi kuuluu toinen aikuinen, myös hänen on toimitettava
            tuloselvitys tunnistautumalla eVakaan omilla henkilötiedoillaan ja
            täyttämällä tämä lomake.
          </li>
          <li>
            Katso voimassaolevat tulorajat{' '}
            <a
              target="_blank"
              rel="noreferrer"
              href="https://www.turku.fi/varhaiskasvatus-ja-esiopetus/maksut-tuet-ja-palveluseteli#anchor-tarkista-maksusi-suuruus-laskurilla"
            >
              tästä
            </a>
            .
          </li>
        </UnorderedList>
        <P>* Tähdellä merkityt tiedot ovat pakollisia</P>
      </>
    )
  },
  accessibilityStatement: (
    <>
      <H1>Saavutettavuusseloste</H1>
      <P>
        Tämä saavutettavuusseloste koskee Turun kaupungin varhaiskasvatuksen
        eVaka-verkkopalvelua osoitteessa{' '}
        <a href="https://evaka.turku.fi">evaka.turku.fi</a>. Turun kaupunki
        pyrkii takaamaan verkkopalvelun saavutettavuuden, parantamaan
        käyttäjäkokemusta jatkuvasti ja soveltamaan asianmukaisia
        saavutettavuusstandardeja.
      </P>
      <P>
        Palvelun saavutettavuuden on arvioinut palvelun kehitystiimi, ja seloste
        on laadittu 12.4.2022.
      </P>
      <H2>Palvelun vaatimustenmukaisuus</H2>
      <P>
        Verkkopalvelu täyttää lain asettamat kriittiset
        saavutettavuusvaatimukset WCAG v2.1 -tason AA mukaisesti. Palvelu ei ole
        vielä kaikilta osin vaatimusten mukainen.
      </P>
      <H2>Toimet saavutettavuuden tukemiseksi</H2>
      <P>
        Verkkopalvelun saavutettavuus varmistetaan muun muassa seuraavilla
        toimenpiteillä:
      </P>
      <ul>
        <li>
          Saavutettavuus huomioidaan alusta lähtien suunnitteluvaiheessa, mm.
          valitsemalla palvelun värit ja kirjaisinten koot saavutettavasti.
        </li>
        <li>
          Palvelun elementit on määritelty semantiikaltaan johdonmukaisesti.
        </li>
        <li>Palvelua testataan jatkuvasti ruudunlukijalla.</li>
        <li>
          Erilaiset käyttäjät testaavat palvelua ja antavat saavutettavuudesta
          palautetta.
        </li>
        <li>
          Sivuston saavutettavuudesta huolehditaan jatkuvalla valvonnalla
          tekniikan tai sisällön muuttuessa.
        </li>
      </ul>
      <P>
        Tätä selostetta päivitetään sivuston muutosten ja saavutettavuuden
        tarkistusten yhteydessä.
      </P>
      <H2>Tunnetut saavutettavuusongelmat</H2>
      <P>
        Käyttäjät saattavat edelleen kohdata sivustolla joitakin ongelmia.
        Seuraavassa on kuvaus tunnetuista saavutettavuusongelmista. Jos huomaat
        sivustolla ongelman, joka ei ole luettelossa, otathan meihin yhteyttä.
      </P>
      <ul>
        <li>
          Palvelun päivämäärävalitsinta ja monivalintojen alasvetovalikkoa ei
          ole optimoitu käytettäväksi ruudunlukijalla.
        </li>
        <li>
          Palvelun yksikkökartassa ei pysty liikkumaan
          näppäimistöllä/ruudunlukijalla , mutta yksikköjä voi selata samassa
          näkymässä olevalta listalta. Palvelussa käytetty kartta on kolmannen
          osapuolen tuottama
        </li>
      </ul>
      <H2>Kolmannet osapuolet</H2>
      <P>
        Verkkopalvelussa käytetään seuraavia kolmannen osapuolen palveluita,
        joiden saavutettavuudesta emme voi vastata.
      </P>
      <ul>
        <li>Suomi.fi-tunnistautuminen</li>
        <li>Leaflet-karttapalvelu</li>
      </ul>
      <H2>Vaihtoehtoiset asiointitavat</H2>
      <P>
        <ExternalLink
          href="https://www.turku.fi/varhaiskasvatus-ja-esiopetus"
          text="Turun kaupungin asiointipisteistä"
        />{' '}
        saa apua sähköiseen asiointiin. Asiointipisteiden palveluneuvojat
        auttavat käyttäjiä, joille digipalvelut eivät ole saavutettavissa.
      </P>
      <H2>Anna palautetta</H2>
      <P>
        Jos huomaat saavutettavuuspuutteen verkkopalvelussamme, kerro siitä
        meille. Voit antaa palautetta{' '}
        <ExternalLink
          href="https://opaskartta.turku.fi/efeedback"
          text="verkkolomakkeella"
        />{' '}
        tai sähköpostitse{' '}
        <a href="mailto:varhaiskasvatus@turku.fi">varhaiskasvatus@turku.fi</a>.
      </P>
      <H2>Valvontaviranomainen</H2>
      <P>
        Jos huomaat sivustolla saavutettavuusongelmia, anna ensin palautetta
        meille sivuston ylläpitäjille. Vastauksessa voi mennä 14 päivää. Jos ole
        tyytyväinen saamaasi vastaukseen, tai et saa vastausta lainkaan kahden
        viikon aikana, voit antaa palautteen Liikenne- ja viestintävirasto
        Traficomiin. Liikenne- ja viestintävirasto Traficomin sivulla kerrotaan
        tarkasti, miten valituksen voi tehdä, ja miten asia käsitellään.
      </P>
      <P>
        <strong>Valvontaviranomaisen yhteystiedot </strong>
        <br />
        Liikenne- ja viestintävirasto Traficom
        <br />
        Digitaalisen esteettömyyden ja saavutettavuuden valvontayksikkö
        <br />
        <ExternalLink
          href="https://www.saavutettavuusvaatimukset.fi/fi"
          text="Saavutettavuusvaatimukset"
        />
        <br />
        <a href="saavutettavuus@traficom.fi">saavutettavuus@traficom.fi</a>
        <br />
        puhelinnumero vaihde 029 534 5000
      </P>
    </>
  )
}

export default fi
