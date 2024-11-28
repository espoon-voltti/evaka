// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { P } from 'lib-components/typography'
import type { CitizenCustomizations } from 'lib-customizations/types'

import { citizenConfig } from './appConfigs'
import EspooLogo from './assets/EspooLogoPrimary.svg'
import featureFlags from './featureFlags'
import mapConfig from './mapConfig'

const MultiLineCheckboxLabel = styled(FixedSpaceColumn).attrs({
  spacing: 'zero'
})`
  margin-top: -6px;
`

const customizations: CitizenCustomizations = {
  appConfig: citizenConfig,
  langs: ['fi', 'sv', 'en'],
  translations: {
    fi: {
      applications: {
        editor: {
          heading: {
            info: {
              DAYCARE: (
                <>
                  <P>
                    Varhaiskasvatuspaikkaa voi hakea ympäri vuoden. Jätä hakemus
                    viimeistään neljä kuukautta ennen kuin tarvitset paikan.
                    Mikäli tarvitset lapselle varhaiskasvatusta kiireellisesti
                    työn tai opiskelujen vuoksi, hae paikkaa viimeistään kaksi
                    viikkoa etukäteen.
                  </P>
                  <P>
                    Saat kirjallisen päätöksen varhaiskasvatuspaikasta{' '}
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
                  <P>
                    Kunnallisen varhaiskasvatuksen asiakasmaksu ja
                    palvelusetelin omavastuuosuus määräytyvät prosenttiosuutena
                    perheen bruttotuloista. Tulojen lisäksi maksuihin vaikuttaa
                    perheen koko ja sovittu varhaiskasvatusaika.
                  </P>
                  <P>
                    Palvelusetelipäiväkodit voivat periä lisämaksun,{' '}
                    <a
                      href="https://www.espoo.fi/kasvatus-ja-opetus/varhaiskasvatus/yksityiseen-varhaiskasvatukseen-hakeminen"
                      target="_blank"
                      rel="noreferrer"
                    >
                      tieto mahdollisesta lisämaksusta löytyy täältä
                    </a>
                    . Perheen tulee toimittaa tuloselvitys bruttotuloistaan
                    viimeistään kaksi viikkoa siitä, kun lapsi on aloittanut
                    varhaiskasvatuksessa.
                  </P>
                  <P>
                    <a
                      href="https://www.espoo.fi/kasvatus-ja-opetus/varhaiskasvatus/varhaiskasvatuksen-asiakasmaksut"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Lisätietoa kunnallisen ja ostopalveluvarhaiskasvatuksen
                      asiakasmaksuista ja tuloselvityksen toimittamisesta löydät
                      täältä.
                    </a>
                  </P>
                  <P>
                    <a
                      href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/maksut-palvelusetelipaivakodeissa"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Lisätietoa palvelusetelivarhaiskasvatuksen maksuista sekä
                      tuloselvityksen tekemisestä löydät täältä.
                    </a>
                  </P>
                  <P fitted={true}>
                    * Tähdellä merkityt tiedot ovat pakollisia
                  </P>
                </>
              ),
              PRESCHOOL: (
                <>
                  <P>
                    Esiopetukseen osallistutaan oppivelvollisuuden alkamista
                    edeltävänä vuonna. Esiopetus on maksutonta. Lukuvuoden
                    2025–2026 esiopetukseen ilmoittaudutaan 8.–20.1.2025.
                    Suomen- ja ruotsinkielinen esiopetus alkaa 7.8.2025.
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
                  <P fitted>* Tähdellä merkityt tiedot ovat pakollisia</P>
                </>
              )
            }
          },
          serviceNeed: {
            startDate: {
              info: {
                DAYCARE: [
                  'Huomaa, että sinun tulee varata noin 1-2 viikkoa aikaa lapsen tutustumiselle ja harjoittelulle varhaiskasvatusyksikössä. Tutustumisjaksolla lapsi tutustuu varhaiskasvatuspaikkaan yhdessä huoltajan kanssa ennen varhaiskasvatuksen varsinaista aloituspäivää. Varhaiskasvatusyksikkö ottaa sinuun yhteyttä tutustumiseen ja aloittamiseen liittyvien asioiden sopimiseksi. Tutustumisajalta ei peritä varhaiskasvatusmaksua.',
                  'Valitse hakemukseen aloituspäiväksi päivämäärä, jolloin lapsi jää ensimmäistä kertaa varhaiskasvatukseen tutustumisen jälkeen ilman huoltajaa. Varhaiskasvatuksen maksu alkaa vasta virallisesta huoltajan vahvistamasta aloituspäivästä.',
                  <a
                    key="link"
                    href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/paikan-vastaanottaminen-ja-varhaiskasvatuksen-aloitus"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Lue lisää lapsen aloittamisesta varhaiskasvatuksesta täältä.
                  </a>
                ],
                PRESCHOOL: [
                  'Suomen- ja ruotsinkielinen esiopetus alkaa 7.8.2025. Jos tarvitset varhaiskasvatusta elokuun alusta ennen esiopetuksen alkua, hae sitä tällä hakemuksella valitsemalla ”Haen myös esiopetukseen liittyvää varhaiskasvatusta”.'
                ]
              },
              instructions: {
                DAYCARE: (
                  <>
                    Voit muuttaa toivottua aloituspäivää myöhemmäksi niin kauan
                    kuin hakemusta ei ole otettu käsittelyyn. Jos haluat tämän
                    jälkeen muuttaa aloituspäivää, sinun tulee ottaa yhteyttä
                    varhaiskasvatuksen palveluohjaukseen (puh. 09 816 31000).
                  </>
                ),
                PRESCHOOL: (
                  <>
                    Voit muuttaa toivottua aloituspäivää myöhemmäksi niin kauan
                    kuin hakemusta ei ole otettu käsittelyyn. Jos haluat tämän
                    jälkeen muuttaa aloituspäivää, sinun tulee ottaa yhteyttä
                    varhaiskasvatuksen palveluohjaukseen (puh. 09 816 31000).
                  </>
                )
              }
            },
            urgent: {
              attachmentsMessage: {
                text: (
                  <>
                    <P fitted>
                      Jos tarvitset varhaiskasvatuspaikkaa äkillisen
                      työllistymisen tai opiskelun vuoksi, hae paikkaa
                      viimeistään kaksi viikkoa ennen tarpeen alkamista. Liitä
                      hakemukseen työ- tai opiskelutodistus molemmilta samassa
                      taloudessa asuvilta huoltajilta. Suosittelemme
                      toimittamaan liitteet sähköisesti, sillä voimme ottaa
                      hakemuksen käsittelyyn vasta sitten, kun tarvittavat
                      liitteet ovat saapuneet perille.
                    </P>
                    <P>
                      Jos et voi lisätä liitteitä hakemukselle sähköisesti,
                      lähetä ne postilla osoitteeseen Varhaiskasvatuksen
                      palveluohjaus, PL 3125, 02070 Espoon kaupunki.
                    </P>
                  </>
                )
              }
            },
            partTime: {
              true: 'Osapäiväinen (max 5h/pv ja max 25h/vko)',
              false: 'Kokopäiväinen'
            },
            dailyTime: {
              connectedDaycareInfo: (
                <>
                  <P>
                    Hae lapselle tarvittaessa esiopetukseen liittyvää
                    maksullista varhaiskasvatusta. Liittyvää varhaiskasvatusta
                    järjestetään aamuisin ennen esiopetusta ja esiopetuksen
                    jälkeen esiopetusyksikön aukioloaikojen mukaisesti.
                    (Esiopetusaika yksiköissä on pääsääntöisesti klo 9-13).
                  </P>
                  <P>
                    Jos tarvitset varhaiskasvatusta elokuun alusta ennen
                    esiopetuksen alkua, huomioi tämä toivotun aloituspäivämäärän
                    valinnassa.
                  </P>
                  <P>
                    Liittyvää varhaiskasvatusta haetaan samasta yksiköstä
                    (kunnallinen, palveluseteli) josta haet esiopetusta.
                  </P>
                  <P>
                    Yksityisiin esiopetusyksiköihin haettaessa, liittyvä
                    varhaiskasvatus haetaan suoraan yksiköstä (pois lukien
                    palveluseteliyksiköt), yksiköt informoivat asiakkaita
                    hakutavasta. Jos esiopetushakemuksessa on haettu liityvää
                    varhaiskasvatusta yksityisestä yksiköstä, palveluohjaus
                    muuttaa hakemuksen vain esiopetushakemukseksi.
                  </P>
                  <P>
                    Saat varhaiskasvatuspaikasta erillisen kirjallisen
                    päätöksen. Päätös tulee{' '}
                    <a
                      href="https://www.suomi.fi/viestit"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Suomi.fi-viestit
                    </a>{' '}
                    -palveluun tai postitse, mikäli et ole ottanut
                    Suomi.fi-viestit -palvelua käyttöön.
                  </P>
                </>
              ),
              instructions: {
                DAYCARE:
                  'Ilmoita lapsen yleisimmin tarvitsema varhaiskasvatusaika. Aika tarkennetaan palvelusopimuksessa varhaiskasvatuksen alkaessa.'
              }
            },
            shiftCare: {
              label: 'Vuorohoito',
              instructions:
                'Vuorohoito on varhaiskasvatusta, jota järjestetään päivällä tapahtuvan varhaiskasvatuksen lisäksi iltaisin, öisin, viikonloppuisin sekä arki- ja juhlapyhinä vuorohoitopäiväkodeissa.',
              attachmentsMessage: {
                DAYCARE: (
                  <>
                    <P>
                      Vuorohoito on tarkoitettu lapsille, joiden molemmat
                      huoltajat ovat vuorotyössä tai opiskelevat pääsääntöisesti
                      iltaisin tai viikonloppuisin. Oikeus vuorohoitoon päättyy,
                      mikäli lapsi ei enää tarvitse vuorohoitoa huoltajan
                      työssäkäynnin tai opiskelun vuoksi, tällöin huoltajan
                      tulee hakea lapselleen uutta varhaiskasvatuspaikkaa.
                    </P>
                    <P>
                      Liitä hakemukseen työnantajan todistus säännöllisestä
                      vuorotyöstä tai oppilaitoksen edustajan todistus
                      päätoimisesta ilta- tai viikonloppuopiskelusta. Toimita
                      todistus molemmilta samassa taloudessa asuvilta
                      huoltajilta.
                    </P>
                    <P>
                      Jos et voi lisätä liitteitä hakemukselle sähköisesti,
                      lähetä ne postilla osoitteeseen Varhaiskasvatuksen
                      palveluohjaus, PL 3125, 02070 Espoon kaupunki.
                    </P>
                  </>
                ),
                PRESCHOOL: (
                  <>
                    <P>
                      Vuorohoito on tarkoitettu lapsille, joiden molemmat
                      huoltajat ovat vuorotyössä tai opiskelevat pääsääntöisesti
                      iltaisin tai viikonloppuisin. Oikeus vuorohoitoon päättyy,
                      mikäli lapsi ei enää tarvitse vuorohoitoa huoltajan
                      työssäkäynnin tai opiskelun vuoksi.
                    </P>
                    <P>
                      Vuorohoidossa esiopetusta annetaan klo 8.00–16.00 välillä
                      noin neljä tuntia kerrallaan. Suunnittelemme tarkemman
                      esiopetuksen ajankohdan lapsen läsnäoloaikojen pohjalta.
                      Suunnittelussa huomioidaan, ettei lapsi joudu viettämään
                      päiväkodissa liian pitkää aikaa.
                    </P>
                    <P>
                      Liitä hakemukseen työnantajan todistus säännöllisestä
                      vuorotyöstä tai oppilaitoksen edustajan todistus
                      päätoimisesta ilta- tai viikonloppuopiskelusta. Toimita
                      todistus molemmilta samassa taloudessa asuvilta
                      huoltajilta.
                    </P>
                    <P>
                      Jos et voi lisätä liitteitä hakemukselle sähköisesti,
                      lähetä ne postilla osoitteeseen Varhaiskasvatuksen
                      palveluohjaus, PL 3125, 02070 Espoon kaupunki.{' '}
                    </P>
                  </>
                )
              },
              attachmentsSubtitle:
                'Lisää tähän molemmilta huoltajilta todistus vuorotyöstä tai todistus opiskelusta iltaisin/viikonloppuisin'
            },
            preparatory:
              'Lapsi tarvitsee tukea suomen kielen oppimisessa ja lapselle on suositeltu valmistavaa esiopetusta nykyisestä päiväkodista. Haen myös perusopetukseen valmistavaan opetukseen. Ei koske ruotsinkielistä esiopetusta.',
            preparatoryInfo: null,
            preparatoryExtraInstructions: (
              <>
                <P>
                  Esiopetuksessa toteutettavaan perusopetukseen valmistavaan
                  opetukseen voivat hakeutua lapset, joilla ei ole vielä suomen
                  kielen taitoa tai jotka osaavat jo jonkin verran suomea.
                  Esiopetusikäisten perusopetukseen valmistavaa opetusta
                  järjestetään kunnallisissa suomenkielisissä esiopetusryhmissä.
                </P>
                <P>
                  Pidennetyn oppivelvollisuuden piirissä olevilla lapsilla ei
                  ole oikeutta perusopetukseen valmistavaan opetukseen. Mikäli
                  lapsella on tai hänelle myöhemmin myönnetään pidennetyn
                  oppivelvollisuuden päätös, lapsen sijoitus muutetaan
                  esiopetussijoitukseksi
                </P>
              </>
            ),
            assistanceNeeded: {
              DAYCARE:
                'Lapsella on kehitykseen tai oppimiseen liittyvä tuen tarve',
              PRESCHOOL:
                'Valitse tämä kohta, jos lapsi tarvitsee kasvulleen ja/tai oppimiselleen tukea esiopetusvuonna.',
              CLUB: 'Lapsella on kehitykseen tai oppimiseen liittyvä tuen tarve'
            },
            assistanceNeedInstructions: {
              DAYCARE:
                'Valitse tämä kohta, jos lapsesi tarvitsee tukea kehitykseen, oppimiseen tai hyvinvointiin. Tukea annetaan lapsen arjessa osana varhaiskasvatusta. Jos lapsella on tuen tarvetta, varhaiskasvatuksen erityisopettaja ottaa hakijaan yhteyttä. Näin voimme huomioida lapsen tarpeet ja osoittaa hänelle sopivan varhaiskasvatuspaikan.',
              PRESCHOOL: null
            },
            assistanceNeedExtraInstructions: {
              PRESCHOOL: (
                <P>
                  Tukea annetaan lapsen arjessa osana esiopetusta ja
                  varhaiskasvatusta. Valitse tämä kohta myös, jos lapsella on
                  muu erityinen syy, jolla on suoranaista vaikutusta
                  esiopetuksen järjestämiseen ja siihen, missä yksikössä lapsen
                  esiopetus tulee järjestää. Jos lapsella on kasvun ja/tai
                  oppimisen tuen tarvetta, varhaiskasvatuksen erityisopettaja
                  ottaa hakijaan yhteyttä. Näin voimme huomioida lapsen tarpeet
                  ja osoittaa hänelle sopivan esiopetuspaikan.
                </P>
              )
            }
          },
          unitPreference: {
            siblingBasis: {
              title: 'Haku sisarusperusteella',
              info: {
                DAYCARE: (
                  <>
                    <P fitted>
                      Lapsella on sisarusperuste varhaiskasvatuspaikkaan, jossa
                      hänen sisaruksensa on silloin, kun lapsen varhaiskasvatus
                      alkaa.
                    </P>
                    <P>
                      Sisaruksiksi katsotaan kaikki samassa osoitteessa asuvat
                      lapset. Pyrimme sijoittamaan sisarukset samaan
                      varhaiskasvatuspaikkaan, jos perhe niin toivoo. Jos haet
                      paikkaa sisaruksille, jotka eivät vielä ole
                      varhaiskasvatuksessa, kirjoita tieto lisätietokenttään.
                    </P>
                    <P>
                      Täytä nämä tiedot vain, jos käytät sisarusperustetta.
                      Valitse lisäksi alla olevasta valikosta ensisijaiseksi
                      hakutoiveeksi sama varhaiskasvatusyksikkö, jossa lapsen
                      sisarus on.
                    </P>
                  </>
                ),
                PRESCHOOL: (
                  <>
                    <P>Esioppilaalla on sisarusperuste:</P>
                    <ol type="a">
                      <li>
                        Oman palvelualueen päiväkotiin, jossa esioppilaalla on
                        sisarus, jolla on päätöksentekohetkellä ja tulevana
                        esiopetusvuonna paikka esiopetuspäiväkodissa.
                      </li>
                      <li>
                        Kunnan osoittamaan lähikouluun, jota esioppilaan sisarus
                        käy tulevana lukuvuonna.
                      </li>
                    </ol>
                    <P>
                      Valitse käytätkö sisarusperustetta kohdan a vai b
                      mukaisesti, jos esioppilaalla on sisarusperuste molempien
                      kohtien mukaan.
                    </P>
                    <P>
                      Täytä nämä tiedot vain, jos käytät sisarusperustetta.
                      Valitse lisäksi alla olevasta valikosta ensisijaiseksi
                      hakutoiveeksi sama yksikkö, jossa lapsen sisarus on.
                    </P>
                  </>
                )
              }
            },
            units: {
              info: {
                DAYCARE: (
                  <>
                    <P fitted>
                      Voit hakea 1-3 paikkaa toivomassasi järjestyksessä.
                      Hakutoiveet eivät takaa paikkaa toivotussa yksikössä,
                      mutta mahdollisuus toivotun paikan saamiseen kasvaa, kun
                      annat useamman vaihtoehdon.
                    </P>
                    <P>
                      Hae palveluseteliä valitsemalla hakutoiveeksi se
                      palveluseteliyksikkö, johon haluat hakea. Kun haet
                      palveluseteliyksikköön, myös yksikön johtaja saa tiedon
                      hakemuksestasi.
                    </P>
                    <P>
                      Näet eri varhaiskasvatusyksiköiden sijainnin valitsemalla
                      ‘Yksiköt kartalla’.
                    </P>
                  </>
                ),
                PRESCHOOL: (
                  <>
                    <P>
                      Voit hakea 1-3 eri yksikköön toivomassasi järjestyksessä.
                      Hakutoiveet eivät takaa paikkaa toivotusta yksiköstä,
                      mutta mahdollisuus toivotun paikan saamiseen kasvaa, kun
                      annat useamman vaihtoehdon.
                    </P>
                    <P>
                      Hae palveluseteliä valitsemalla hakutoiveeksi se
                      palveluseteliyksikkö, johon haluat hakea. Kun haet
                      palveluseteliyksikköön, myös yksikön johtaja saa tiedon
                      hakemuksestasi.
                    </P>
                    <P>
                      Näet eri varhaiskasvatusyksiköiden sijainnin valitsemalla
                      ‘Yksiköt kartalla’.
                    </P>
                  </>
                )
              },
              select: {
                label: (maxUnits: number): string =>
                  maxUnits === 1 ? 'Hae yksikköä' : 'Hae yksiköitä'
              },
              preferences: {
                info: (maxUnits: number) =>
                  maxUnits === 1
                    ? 'Valitse yksi varhaiskasvatusyksikkö'
                    : `Valitse 1-${maxUnits} varhaiskasvatusyksikköä toivomassasi järjestyksessä. Voit muuttaa järjestystä nuolien avulla.`
              }
            }
          },
          contactInfo: {
            info: (
              <P>
                Henkilötiedot tulevat väestötiedoista, eikä niitä voi muuttaa
                tällä hakemuksella. Jos henkilötiedoissa on virheitä,
                päivitäthän tiedot{' '}
                <a
                  href="https://dvv.fi/henkiloasiakkaat"
                  target="_blank"
                  rel="noreferrer"
                >
                  Digi- ja Väestötietoviraston sivuilla
                </a>
                . Jos osoitteesi on muuttumassa, voit lisätä tulevan osoitteen
                erilliseen kohtaan hakemuksella. Lisää tuleva osoite sekä
                lapselle että huoltajalle. Pidämme osoitetietoa virallisena
                vasta, kun se on päivittynyt väestötietojärjestelmään.
                Toimitamme päätökset esiopetus- ja varhaiskasvatuspaikoista
                automaattisesti myös väestötiedoista löytyvälle huoltajalle,
                joka asuu eri osoitteessa.
              </P>
            ),
            futureAddressInfo:
              'Käytämme virallisena osoitteena väestötiedoista saatavaa osoitetta. Osoitteesi väestötiedoissa muuttuu, kun teet muuttoilmoituksen postiin tai digi- ja väestötietovirastoon.',
            emailInfoText:
              'Sähköpostiosoitteen avulla saat ilmoituksen uusista viesteistä, jotka saapuvat sinulle varhaiskasvatuksen asiointipalvelu eVakaan. Esitäytetty sähköpostiosoite on haettu eVakan asiakastiedoista. Mikäli muokkaat sitä, päivitetään vanha sähköpostiosoite, kun lähetät hakemuksen.',
            secondGuardianNotFound:
              'Väestötietojärjestelmästä saamamme tiedon mukaan lapsella ei ole toista huoltajaa.',
            otherChildrenInfo:
              'Samassa taloudessa asuvat alle 18-vuotiaat lapset vaikuttavat varhaiskasvatuksen asiakasmaksuun.',
            areExtraChildren:
              'Samassa taloudessa asuu muita alle 18-vuotiaita lapsia (esim. aviopuolison / avopuolison lapset)'
          },
          additionalDetails: {
            dietInfo:
              'Osaan erityisruokavalioista tarvitset erikseen lääkärintodistuksen, joka sinun tulee toimittaa varhaiskasvatuspaikkaan. Poikkeuksena ovat vähälaktoosinen tai laktoositon ruokavalio, uskonnollisiin syihin perustuva ruokavalio tai kasvisruokavalio (lakto-ovo).'
          },
          verification: {
            serviceNeed: {
              assistanceNeed: {
                assistanceNeed: 'Kehitykseen tai oppimiseen liittyvä tuen tarve'
              }
            }
          }
        }
      },
      income: {
        assure: (
          <MultiLineCheckboxLabel>
            <span>
              Vakuutan antamani tiedot oikeiksi ja olen tutustunut
              asiakasmaksuperusteisiin:
            </span>
            <a
              href="https://www.espoo.fi/fi/kasvatus-ja-opetus/varhaiskasvatus/varhaiskasvatuksen-asiakasmaksut#section-59617"
              target="_blank"
              rel="noreferrer"
            >
              Lasten varhaiskasvatuksesta perittävät maksut | Espoon kaupunki
            </a>
          </MultiLineCheckboxLabel>
        ),
        incomeType: {
          description: (
            <>
              Jos olet yrittäjä, mutta sinulla on myös muita tuloja, valitse
              sekä <strong>Yrittäjän tulotiedot</strong>, että{' '}
              <strong>Asiakasmaksun määritteleminen tulojen mukaan</strong>.
            </>
          ),
          grossIncome: 'Maksun määritteleminen tulojen mukaan'
        },
        grossIncome: {
          title: 'Tulotietojen täyttäminen',
          estimate: 'Arvio palkkatuloistani (ennen veroja)'
        }
      }
    },
    sv: {
      applications: {
        editor: {
          heading: {
            info: {
              DAYCARE: (
                <>
                  <P>
                    Plats inom småbarnspedagogiken kan ansökas året om. Lämna in
                    ansökan senast fyra månader före barnet behöver en plats. Om
                    barnet behöver en plats i brådskande ordning på grund av
                    arbete eller studier, ska du ansöka om plats senast två
                    veckor i förväg.
                  </P>
                  <P>
                    Du får ett skriftligt beslut om plats inom
                    småbarnspedagogiken till tjänsten{' '}
                    <a
                      href="https://www.suomi.fi/meddelanden"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Suomi.fi-meddelanden
                    </a>{' '}
                    eller per post, om du inte använder Suomi.fi-tjänsten.
                  </P>
                  <P>
                    Klientavgiften för kommunal småbarnspedagogik och
                    servicesedelns självriskandel beräknas som en procentuell
                    andel av familjens bruttoinkomster. Utöver inkomsterna
                    påverkas avgiften också av familjens storlek och den
                    överenskomna tiden i småbarnspedagogik.
                  </P>
                  <P>
                    Servicesedel daghem kan ta ut en extra avgift.{' '}
                    <a
                      href="https://www.espoo.fi/sv/fostran-och-utbildning/smabarnspedagogik/ansokan-till-privat-smabarnspedagogik"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Uppgift om en eventuell extra avgift finns här.
                    </a>{' '}
                    Familjen ska lämna in en inkomstutredning senast två veckor
                    efter att barnet har börjat i småbarnspedagogiken.
                  </P>
                  <P>
                    <a
                      href="https://www.espoo.fi/sv/fostran-och-utbildning/smabarnspedagogik/klientavgifterna-smabarnspedagogik"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Här hittar du mer information om klientavgifterna inom den
                      kommunala småbarnspedagogiken och småbarnspedagogiken som
                      köpt tjänst och om inlämnande av inkomstutredning.
                    </a>
                  </P>
                  <P>
                    <a
                      href="https://www.espoo.fi/sv/fostran-och-utbildning/smabarnspedagogik/avgifter-i-servicesedeldaghem"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Här hittar du mer information om klientavgifterna för
                      småbarnspedagogik vid servicesedeldaghem och om
                      upprättande av inkomstutredningen.
                    </a>
                  </P>
                  <P fitted={true}>
                    * Informationen markerad med en stjärna är obligatorisk
                  </P>
                </>
              ),
              PRESCHOOL: (
                <>
                  <P>
                    Barn deltar i förskoleundervisning året innan läroplikten
                    inleds. Förskoleundervisningen är kostnadsfri. Anmälan till
                    förskoleundervisning under läsåret 2025–2026 pågår 8–20
                    januari 2025. Den finsk- och den svenskspråkiga
                    förskoleundervisningen börjar den 7 augusti 2025.
                  </P>
                  <P>
                    Du får ett skriftligt beslut om plats inom
                    småbarnspedagogiken till tjänsten{' '}
                    <a
                      href="https://www.suomi.fi/meddelanden"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Suomi.fi-meddelanden
                    </a>{' '}
                    eller per post, om du inte använder Suomi.fi-tjänsten.
                  </P>
                  <P fitted={true}>
                    * Informationen markerad med en stjärna är obligatorisk
                  </P>
                </>
              )
            }
          },
          serviceNeed: {
            startDate: {
              header: {
                DAYCARE: 'Inledande av småbarnspedagogik'
              },
              info: {
                DAYCARE: [
                  'Observera att du ska reservera ungefär 1–2 veckor för barnets invänjning och träning vid enheten för småbarnspedagogik. Under tillvänjningen lär barnet känna enheten för småbarnspedagogik tillsammans med vårdnadshavaren före den egentliga första dagen i småbarnspedagogik. Enheten för småbarnspedagogik kontaktar dig för att komma överens om invänjningen och annat som rör starten i småbarnspedagogiken. För invänjningstiden tas ingen avgift för småbarnspedagogik ut.',
                  'Ange som startdatum i ansökan det datum då barnet för första gången stannar kvar i småbarnspedagogiken utan sin vårdnadshavare efter invänjningen.  Avgiften för småbarnspedagogik räknas först från det officiella startdatumet som vårdnadshavaren har bekräftat.',
                  <a
                    key="link"
                    href="https://www.espoo.fi/sv/fostran-och-utbildning/smabarnspedagogik/mottagande-av-plats-och-inledande-av-smabarnspedagogik"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Läs mer om barnets start i småbarnspedagogiken här
                  </a>
                ],
                PRESCHOOL: [
                  'Den finsk- och den svenskspråkiga förskoleundervisningen börjar den 7 augusti 2025. Om du behöver småbarnspedagogik i början av augusti före förskoleundervisningen börjar, ska du ansöka om det med denna ansökan genom att kryssa för ”Jag ansöker också om småbarnspedagogik i samband med förskolan”.'
                ]
              },
              label: {
                PRESCHOOL: 'Önskat startdatum'
              },
              instructions: {
                DAYCARE: (
                  <>
                    Du kan senarelägga det önskade startdatumet så länge
                    behandlingen av ansökan ännu inte har börjat. Om du vill
                    ändra startdatumet efter detta ska du kontakta
                    småbarnspedagogikens servicehandledning (tfn 09 816 27600).
                  </>
                ),
                PRESCHOOL: (
                  <>
                    Du kan senarelägga det önskade startdatumet så länge
                    behandlingen av ansökan ännu inte har börjat. Om du vill
                    ändra startdatumet efter detta ska du kontakta
                    småbarnspedagogikens servicehandledning (tfn 09 816 27600).
                  </>
                )
              }
            },
            urgent: {
              attachmentsMessage: {
                text: (
                  <>
                    <P fitted>
                      Om du behöver en plats inom småbarnspedagogiken för att du
                      hittat ett jobb eller fått en studieplats med kort varsel,
                      ska du ansöka om platsen senast två veckor innan den
                      behövs. Bifoga i ansökan ett arbets- eller studieintyg för
                      båda föräldrarna som bor i samma hushåll. Vi rekommenderar
                      att man skickar in bilagorna elektroniskt, eftersom vi kan
                      börja behandla ansökan först när de nödvändiga bilagorna
                      har kommit fram.
                    </P>
                    <P>
                      Om du inte kan lägga till bilagorna till ansökan
                      elektroniskt, posta de till Småbarnspedagogikens
                      servicehandledning, PB 32, 02070 Esbo stad.
                    </P>
                  </>
                ),
                subtitle:
                  'Bifoga här ett arbets- eller studieintyg för båda föräldrarna.'
              }
            },
            partTime: {
              true: 'Deltid (högst 5 h/dag och högst 25 h/vecka)',
              false: 'Heldag'
            },
            dailyTime: {
              connectedDaycareInfo: (
                <>
                  <P>
                    Om det behövs, ska du ansöka om avgiftsbelagd
                    småbarnspedagogik i samband med förskolan för barnet. Denna
                    småbarnspedagogik ordnas på morgonen före förskolan och
                    efter förskolan enligt öppettiderna för enheten för
                    förskoleundervisning. (Tiden för förskoleundervisning vid
                    enheterna är i regel kl. 9–13). Om du behöver
                    småbarnspedagogik i början av augusti innan förskolan
                    börjar, beakta detta när du väljer önskat startdatum.
                  </P>
                  <P>
                    Småbarnspedagogik i samband med förskolan ansöks vid samma
                    enhet där du ansöker om förskoleundervisning.
                  </P>
                  <P>
                    När du söker till privata enheter för förskoleundervisning
                    ansöker du om småbarnspedagogik i samband med förskolan
                    direkt hos enheten (exklusive servicesedelenheter).
                    Enheterna informerar kunderna om ansökningssättet. Om det i
                    ansökan om förskoleplats har ansökts om småbarnspedagogik i
                    en privat enhet, ändrar servicehanledningen ansökan till
                    bara ansökan om förskoleplats.
                  </P>
                  <P>
                    Du får ett separat skriftligt beslut om platsen inom
                    småbarnspedagogiken. Beslutet kommer till tjänsten
                    Suomi.fi-meddelanden eller per post om du inte har tagit
                    tjänsten Suomi.fi-meddelanden i bruk.
                  </P>
                </>
              ),
              instructions: {
                DAYCARE:
                  'Ange vilka tider barnet oftast behöver småbarnspedagogik. Tiden preciseras i serviceavtalet när småbarnspedagogiken börjar.'
              }
            },
            shiftCare: {
              label: 'Skiftvård',
              instructions:
                'Skiftvård är småbarnspedagogik som ordnas utöver småbarnspedagogiken på dagtid på kvällar, nätter och veckoslut samt på söckenhelger och helgdagar i skiftdaghem.',
              attachmentsMessage: {
                DAYCARE: (
                  <>
                    <P fitted>
                      Skiftvården är avsedd för barn vars båda föräldrar arbetar
                      skift eller studerar på kvällstid eller under veckoslut
                      som sin huvudsyssla. Rätten till skiftvård upphör om
                      barnet inte längre behöver skiftvård på grund av
                      vårdnadshavarens arbete eller studier. Då ska
                      vårdnadshavaren ansöka om en ny plats inom
                      småbarnspedagogik för barnet.{' '}
                    </P>
                    <P>
                      Bifoga till ansökan arbetsgivarens intyg om regelbundet
                      skiftarbete eller ett intyg från läroanstalten om studier
                      på kvällstid eller under veckoslut, som är en huvudsyssla.
                      Lämna in ett intyg för båda vårdnadshavarna som bor i
                      samma hushåll.{' '}
                    </P>
                    <P>
                      Om du inte kan lägga till bilagorna till ansökan
                      elektroniskt, posta de till Småbarnspedagogikens
                      servicehandledning, PB 32, 02070 Esbo stad.
                    </P>
                  </>
                ),
                PRESCHOOL: (
                  <>
                    <P>
                      Skiftvården är avsedd för barn vars båda föräldrar arbetar
                      skift eller studerar på kvällstid eller under veckoslut
                      som sin huvudsyssla. Rätten till skiftvård upphör om
                      barnet inte längre behöver skiftvård på grund av
                      vårdnadshavarens arbete eller studier.
                    </P>
                    <P>
                      I skiftvård ges förskoleundervisning ungefär fyra timmar i
                      taget mellan kl. 8.00 och 16.00. Vi planerar den närmare
                      tidpunkten för förskoleundervisningen utifrån barnets
                      tider i daghemmet. I planeringen tar man hänsyn till att
                      barnet inte behöver tillbringa alltför långa tider i
                      daghemmet.
                    </P>
                    <P>
                      Bifoga till ansökan arbetsgivarens intyg om regelbundet
                      skiftarbete eller ett intyg från läroanstalten om studier
                      på kvällstid eller under veckoslut, som är en huvudsyssla.
                      Lämna in ett intyg för båda vårdnadshavarna som bor i
                      samma hushåll.
                    </P>
                    <P>
                      Om du inte kan lägga till bilagorna till ansökan
                      elektroniskt, posta de till Småbarnspedagogikens
                      servicehandledning, PB 32, 02070 Esbo stad.
                    </P>
                  </>
                )
              },
              attachmentsSubtitle:
                'Bifoga intyg för båda vårdnadshavarna om skiftarbete eller studier på kvällstid eller under veckoslut'
            },
            assistanceNeed: 'Stödbehov',
            assistanceNeeded: {
              DAYCARE: 'Barnet behöver stöd för utvecklingen eller lärandet',
              PRESCHOOL: 'Barnet behöver stöd för utvecklingen eller lärandet',
              CLUB: 'Barnet behöver stöd för utvecklingen eller lärandet'
            },
            assistanceNeedInstructions: {
              DAYCARE:
                'Kryssa för denna punkt om ditt barn behöver stöd för sin utveckling, sitt lärande eller sitt välbefinnande. Stöd ges i barnets vardag som en del av småbarnspedagogiken. Om barnet har stödbehov, kontaktar specialläraren inom småbarnspedagogiken den sökande. På så sätt kan vi ta hänsyn till barnets behov och anvisa barnet en lämplig plats inom småbarnspedagogiken.',
              PRESCHOOL: null
            },
            assistanceNeedExtraInstructions: {
              PRESCHOOL: (
                <P>
                  Kryssa för denna punkt om barnet behöver stöd för sin uppväxt
                  och/eller sitt lärande under förskoleåret. Stöd ges i barnets
                  vardag som en del av förskoleundervisningen och
                  småbarnspedagogiken. Kryssa för denna punkt också om det finns
                  någon annan särskild anledning som direkt påverkar ordnandet
                  av förskoleundervisningen och vid vilken enhet
                  förskoleundervisningen bör ordnas för barnet. Om barnet
                  behöver stöd för utvecklingen eller lärandet kontaktar
                  specialläraren inom småbarnspedagogiken den sökande. På så
                  sätt kan vi ta hänsyn till barnets behov och anvisa barnet en
                  lämplig plats inom förskoleundervisningen.
                </P>
              )
            },
            preparatory:
              'Barnet behöver stöd med att lära sig finska och ja barnets nuvarande daghem har rekommenderat att barnet deltar i undervisning som förbereder för grundläggande utbildning. Jag ansöker också till undervisning som förbereder för grundläggande utbildning. Gäller inte svenskspråkig förskoleundervisning.',
            preparatoryInfo: null,
            preparatoryExtraInstructions: (
              <>
                <P>
                  Plats inom undervisning som förbereder för grundläggande
                  utbildning och som genomförs inom förskoleundervisningen kan
                  sökas för barn som ännu inte kan finska eller som kan lite
                  finska. Undervisning som förbereder för grundläggande
                  utbildning för barn i förskoleåldern ordnas i kommunala,
                  finskspråkiga förskolegrupper.
                </P>
                <P>
                  Barn med förlängd läroplikt har inte rätt att delta i
                  undervisning som förbereder för grundläggande utbildning. Om
                  barnet har ett beslut om förlängd läroplikt eller får ett
                  sådant beslut senare ändras barnets placering till en
                  placering inom förskoleundervisning.
                </P>
              </>
            )
          },
          unitPreference: {
            title: 'Önskemål i ansökan',
            siblingBasis: {
              title: 'Ansökan med syskongrund',
              info: {
                DAYCARE: (
                  <>
                    <P fitted>
                      Barnet har en syskongrund till den enhet för
                      småbarnspedagogik där barnets syskon går vid den tidpunkt
                      då barnet ska börja i småbarnspedagogiken.
                    </P>
                    <P>
                      Som syskon räknas alla barn som bor på samma adress. Vi
                      strävar efter att placera syskon vid samma enhet för
                      småbarnspedagogik, om detta är familjens önskemål. Om du
                      ansöker om en plats för syskon som ännu inte deltar i
                      småbarnspedagogik, ange detta i fältet för
                      tilläggsinformation.
                    </P>
                    <P>
                      Fyll i dessa uppgifter endast om syskongrunden ska
                      användas. Välj också i menyn endan den enhet för
                      småbarnspedagogik där barnets syskon har en plats som
                      primärt önskemål.
                    </P>
                  </>
                ),
                PRESCHOOL: (
                  <>
                    <P>Förskoleeleven ansöker med syskongrund:</P>
                    <ol type="a">
                      <li>
                        Till ett daghem i det egna serviceområdet som ger
                        förskoleundervisning, där ett syskon till förskoleeleven
                        har en plats vid tidpunkten för beslutet och under det
                        kommande förskoleåret.
                      </li>
                      <li>
                        Till en närskola som anvisats av kommunen, där ett
                        syskon till förskoleeleven kommer att gå under det
                        kommande läsåret.
                      </li>
                    </ol>
                    <P>
                      Välj om syskongrunden ska användas enligt punkt a eller b,
                      om båda syskongrunderna existerar. Fyll i dessa uppgifter
                      endast om syskongrunden ska användas. Utöver detta, välj i
                      menyn endan den enhet där barnets syskon har en plats som
                      primärt önskemål.
                    </P>
                  </>
                )
              },
              checkbox: {
                PRESCHOOL:
                  'Jag ansöker i första hand till samma enhet där barnets syskon har en plats.'
              }
            },
            units: {
              info: {
                DAYCARE: (
                  <>
                    <P fitted>
                      Du kan ansöka till 1–3 enheter i önskad ordning.
                      Önskemålen i ansökan garanterar inte en plats vid den
                      önskade enheten, men chansen att få den önskade platsen är
                      desto större ju fler alternativ du anger.
                    </P>
                    <P>
                      Ansök om servicesedel genom att ange den önskade
                      servicesedelsenheten som önskemål i ansökan. Kontrollera
                      före ansökan vilken enhet ger småbarnspedagogik på
                      svenska.När du ansöker om en plats vid en
                      servicesedelsenhet får också ledaren för enheten besked om
                      din ansökan
                    </P>
                    <P>
                      Du ser läget för samtliga enheter för småbarnspedagogik
                      genom att klicka på ‘Enheterna på kartan’.
                    </P>
                  </>
                ),
                PRESCHOOL: (
                  <>
                    <P>
                      Du kan söka till 1–3 olika enheter i valfri ordning.
                      Önskemålen i ansökan garanterar inte en plats vid den
                      önskade enheten, men chansen att få den önskade platsen är
                      desto större ju fler alternativ du anger.
                    </P>
                    <P>
                      Ansök om servicesedel genom att ange den önskade
                      servicesedelsenheten som önskemål i ansökan. När du
                      ansöker om en plats vid en servicesedelsenhet får också
                      ledaren för enheten besked om din ansökan. Svenskspråkig
                      förskola ordnas inte i servicesedelenheter.
                    </P>
                  </>
                )
              },
              mapLink: 'Enheterna på kartan',
              select: {
                label: (_maxUnits: number) => 'Sök enheter'
              },
              preferences: {
                info: (maxUnits: number) =>
                  maxUnits === 1
                    ? 'Välj en enhet'
                    : `Välj 1–${maxUnits} enheter för småbarnspedagogik i valfri ordning. Du kan ändra ordningen med pilarna.`
              }
            }
          },
          contactInfo: {
            info: (
              <P>
                Personuppgifterna hämtas från befolkningsdatassystemet och kan
                inte ändras i ansökan. Om personuppgifterna innehåller fel, ska
                du uppdatera uppgifterna på{' '}
                <a
                  href="https://dvv.fi/sv/privatpersoner"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Myndigheten för digitalisering och befolkningsdatas
                </a>{' '}
                webbplats. Om din adress kommer att ändras inom kort, kan du
                lägga till din kommande adress i ett separat fält i ansökan.
                Lägg till den kommande adressen både för barnet och för
                vårdnadshavaren. I vårt system är en adressuppgift officiell
                först när den har uppdaterats i befolkningsdatasystemet. Vi
                skickar besluten om platser inom förskoleundervisningen och
                småbarnspedagogiken automatiskt också till den vårdnadshavare
                som finns i barnets uppgifter i befolkningsdatasystemet och som
                bor på en annan adress.
              </P>
            ),
            futureAddressInfo:
              'Vi använder den adress som finns i befolkningsdatasystemet som officiell adress. Din adress i befolkningsdatasystemet ändras när du gör en flyttanmälan till posten eller Myndigheten för digitalisering och befolkningsdata.',
            emailInfoText:
              'När du har angett en e-postadress får du notiser om nya meddelanden som du har fått i småbarnspedagogikens ärendehanteringstjänst eVaka. Den förhandsifyllda e-postadressen har hämtats från klientuppgifterna i eVaka. Om du ändrar den uppdateras adressen när du skickar in ansökan.',
            secondGuardianInfoTitle: 'Den andra vårdnadshavarens uppgifter',
            secondGuardianNotFound:
              'Enligt uppgift som vi har fått från befolkningsdatasystemet har barnet ingen andra vårdnadshavare.',
            otherChildrenInfo:
              'Barn under 18 år som bor i samma hushåll påverkar klientavgifterna inom småbarnspedagogiken.',
            areExtraChildren:
              'I samma hushåll bor andra barn som är under 18 år (t.ex. makens/makans/sambos barn)'
          },
          additionalDetails: {
            dietLabel: 'Specialkost',
            dietInfo:
              'För vissa specialdieter behövs ett läkarintyg som ska lämnas till enheten för småbarnspedagogik. Undantag är låglaktoskost eller laktosfri kost, kost anpassad efter religiös övertygelse och vegetarisk kost (lakto-ovo).'
          },
          verification: {
            serviceNeed: {
              assistanceNeed: {
                assistanceNeed:
                  'Behov av stöd i anslutning till utveckling eller lärande'
              }
            }
          }
        }
      },
      income: {
        assure: (
          <MultiLineCheckboxLabel>
            <span>
              Jag försäkrar att de uppgifter jag lämnat in är riktiga och jag
              har bekantat mig med avgifter för småbarnspedagogik:{' '}
            </span>
            <a
              href="https://www.espoo.fi/sv/fostran-och-utbildning/smabarnspedagogik/klientavgifterna-smabarnspedagogik#section-59617"
              target="_blank"
              rel="noreferrer"
            >
              Avgifter för småbarnspedagogik | Esbo stad (espoo.fi)
            </a>
          </MultiLineCheckboxLabel>
        ),
        incomeType: {
          description: (
            <>
              Om du är företagare men har också andra inkomster, välj både{' '}
              <strong>Företagarens inkomstuppgifter</strong>, och{' '}
              <strong>Fastställande av klientavgiften enligt inkomster</strong>.
            </>
          ),
          grossIncome: 'Fastställande av avgiften enligt inkomster'
        },
        grossIncome: {
          title: 'Att fylla i uppgifterna om inkomster',
          estimate: 'Uppskattning av mina bruttolön (före skatt)'
        }
      }
    },
    en: {
      applications: {
        editor: {
          heading: {
            title: {
              DAYCARE:
                'Early childhood education and service voucher application'
            },
            info: {
              DAYCARE: (
                <>
                  <P>
                    You can apply for a place in early childhood education all
                    year round. Submit your application no later than four
                    months before your child needs the place. If your child
                    urgently needs early childhood education due to your work or
                    studies, you must apply for a place no later than two weeks
                    before needing the place.
                  </P>
                  <P>
                    You will receive a written decision on the early childhood
                    education place through the{' '}
                    <a
                      href="https://www.suomi.fi/messages"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Suomi.fi Messages
                    </a>{' '}
                    service or by post if you have not activated the Suomi.fi
                    service.
                  </P>
                  <P>
                    The client fee for municipal early childhood education and
                    the service voucher client charge are calculated as a
                    percentage of your family’s gross income. In addition to
                    your income, the fee depends on your family’s size and the
                    agreed hours of early childhood education.
                  </P>
                  <P>
                    Service voucher day care centres may charge an additional
                    fee.{' '}
                    <a
                      href="https://www.espoo.fi/en/childcare-and-education/early-childhood-education/applying-private-early-childhood-education"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Here you can find information about the additional fees.
                    </a>{' '}
                    You must provide information about your family’s gross
                    income no later than two weeks after your child’s early
                    childhood education has started.
                  </P>
                  <P>
                    <a
                      href="https://www.espoo.fi/en/childcare-and-education/early-childhood-education/early-childhood-education-fees"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Here you can find more information about the fees for
                      municipal and outsourced early childhood education and how
                      to submit an income statement.
                    </a>
                  </P>
                  <P>
                    <a
                      href="https://www.espoo.fi/en/childcare-and-education/early-childhood-education/fees-service-voucher-day-care-centres"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Here you can find more information about the fees charged
                      at service voucher day care centres and how to submit an
                      income statement.
                    </a>
                  </P>
                  <P fitted={true}>
                    * Information marked with a star is required
                  </P>
                </>
              ),
              PRESCHOOL: (
                <>
                  <P>
                    Children attend pre-primary education during the year before
                    the start of compulsory education. Pre-primary education is
                    free of charge. Enrolment for pre-primary education for the
                    school year 2025–2026 takes place between 8 and 20 January
                    2025. Finnish and Swedish pre-primary education starts on 7
                    August 2025.
                  </P>
                  <P>
                    The decisions will be sent to the{' '}
                    <a
                      href="https://www.suomi.fi/messages"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Suomi.fi Messages
                    </a>{' '}
                    service or by post, if the applicant does not use the{' '}
                    <a
                      href="https://www.suomi.fi/messages"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Suomi.fi Messages
                    </a>{' '}
                    service.
                  </P>
                  <P fitted={true}>
                    * Information marked with a star is required
                  </P>
                </>
              )
            }
          },
          serviceNeed: {
            title: 'Need for services',
            startDate: {
              header: {
                DAYCARE: 'Start of early childhood education'
              },
              info: {
                DAYCARE: [
                  'Please note that you have to reserve about 1–2 weeks for familiarisation and orientation at the day care centre. During the familiarisation period, children familiarise themselves with the day care centre together with their guardians before early childhood education officially starts. The day care centre staff will contact you to discuss the familiarisation period and the start of early childhood education. No fee is charged for the duration of the familiarisation period.',
                  'Your desired start date should be the date when your child stays at the day care centre without a guardian for the first time after the familiarisation period. Early childhood education fees will not be charged until the official start date confirmed by the guardian.',
                  <a
                    key="link"
                    href="https://www.espoo.fi/en/childcare-and-education/early-childhood-education/accepting-place-and-starting-early-childhood-education"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Read more about the start of early childhood education here.
                  </a>
                ],
                PRESCHOOL: [
                  'Finnish and Swedish pre-primary education starts on 7 August 2025. If your child needs early childhood education at the beginning of August before pre-primary education starts, you can apply for it on this application by selecting  I also apply for early childhood education related to pre-primary education.'
                ]
              },
              instructions: {
                DAYCARE: (
                  <>
                    You can postpone your desired start date until the
                    processing of your application starts. If you want to make
                    changes to your start date later, you need to contact early
                    childhood education service guidance (tel. 09 8163 1000).
                  </>
                ),
                PRESCHOOL: (
                  <>
                    You can postpone your desired start date until the
                    processing of your application starts. If you want to make
                    changes to your start date later, you need to contact early
                    childhood education service guidance (tel. 09 8163 1000).
                  </>
                )
              }
            },
            urgent: {
              label:
                'Urgent application (not applicable to transfer applications)',
              attachmentsMessage: {
                text: (
                  <>
                    <P fitted>
                      If your child needs a place in early childhood education
                      due to your sudden employment or studies, you must apply
                      for a place no later than two weeks before your child
                      needs it. Your application must include documents
                      concerning the employment or student status of both
                      guardians living in the same household. We recommend that
                      you send the documents online, as we will not process your
                      application until we have received the required documents.
                    </P>
                    <P>
                      If you are unable to add attachments to your online
                      application, you can send the documents by post to Early
                      Childhood Education Service Guidance, P.O. Box 3125, 02070
                      City of Espoo.
                    </P>
                  </>
                )
              }
            },
            partTime: {
              true: 'Part-time (max. 5 hours/day and 25 hours/week)',
              false: 'Full-day'
            },
            dailyTime: {
              label: {
                PRESCHOOL:
                  'Need for early childhood education connected to pre-primary education'
              },
              connectedDaycareInfo: (
                <>
                  <P>
                    If necessary, apply for early childhood education connected
                    to pre-primary education for your child (fees apply). Early
                    childhood education is provided in the morning before
                    pre-primary education and in the afternoon after pre-primary
                    education during the opening hours of the pre-primary
                    education unit. (As a rule, pre-primary education is
                    provided from 9:00 to 13:00.)
                  </P>
                  <P>
                    If your child needs early childhood education at the
                    beginning of August before pre-primary education starts,
                    take this into account when choosing your desired start
                    date.
                  </P>
                  <P>
                    You can apply for connected early childhood education from
                    the same unit (municipal or service voucher) from where you
                    apply for pre-primary education.
                  </P>
                  <P>
                    When applying for private pre-primary education units, apply
                    for related early childhood education directly from the unit
                    (with the exception of service voucher units); the units
                    inform customers about the application procedure. If you
                    have applied for related early childhood education from a
                    private unit on the pre-primary education application, the
                    service guidance will change your application to a
                    pre-primary education application only.
                  </P>
                  <P>
                    A separate written decision will be issued on the early
                    childhood education place. The decision will be sent to the{' '}
                    <a
                      href="https://www.suomi.fi/messages"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Suomi.fi Messages
                    </a>{' '}
                    service or by post if you do not use the{' '}
                    <a
                      href="https://www.suomi.fi/messages"
                      target="_blank"
                      rel="noreferrer"
                    >
                      Suomi.fi messages
                    </a>{' '}
                    service.
                  </P>
                </>
              ),
              instructions: {
                DAYCARE:
                  'Indicate the hours of early childhood education usually needed by your child. The hours will be specified in the service agreement when early childhood education starts.'
              },
              usualArrivalAndDeparture: {
                DAYCARE: 'Start and end time of early childhood education'
              }
            },
            shiftCare: {
              label: 'Shift care',
              instructions:
                'Shift care refers to early childhood education that is provided, in addition to daytime early childhood education, in the evenings, at night, at weekends and on holidays at shift day care centres.',
              attachmentsMessage: {
                DAYCARE: (
                  <>
                    <P fitted>
                      Shift care is intended for children whose both guardians
                      do shift work or mainly study in the evening and/or at
                      weekends. The right to shift care ends if the child no
                      longer needs shift care due to the guardian’s work or
                      studies, in which case the guardian must apply for a new
                      early childhood education place for the child.
                    </P>
                    <P>
                      Your application must include a document issued by your
                      employer concerning your shift work or a document issued
                      by a representative of your educational institution
                      concerning your full-time evening or weekend studies. Both
                      guardians living in the same household must provide the
                      required document.
                    </P>
                    <P>
                      If you are unable to add attachments to your online
                      application, you can send the documents by post to Early
                      Childhood Education Service Guidance, P.O. Box 3125, 02070
                      City of Espoo.
                    </P>
                  </>
                ),
                PRESCHOOL: (
                  <>
                    <P>
                      Shift care is intended for children whose both guardians
                      do shift work or mainly study in the evening and/or at
                      weekends. The right to shift care ends if the child no
                      longer needs shift care due to their guardian’s work or
                      studies.
                    </P>
                    <P>
                      In shift care, pre-primary education is provided between
                      8:00 and 16:00 for approximately four hours at a time. The
                      exact hours of pre-primary education are planned based on
                      the child’s hours of attendance. We always ensure that
                      children do not have to spend too much time at the day
                      care centre.
                    </P>
                    <P>
                      Your application must include a document issued by your
                      employer concerning your shift work or a document issued
                      by a representative of your educational institution
                      concerning your full-time evening or weekend studies. Both
                      guardians living in the same household must provide the
                      required document.
                    </P>
                    <P>
                      If you are unable to add attachments to your online
                      application, you can send the documents by post to Early
                      Childhood Education Service Guidance, P.O. Box 3125, 02070
                      City of Espoo.
                    </P>
                  </>
                )
              },
              attachmentsSubtitle:
                'Add here both guardians’ documents concerning shift work or evening/weekend studies.'
            },
            assistanceNeed: 'Need for support',
            assistanceNeeded: {
              DAYCARE: 'My child needs support for development or learning',
              PRESCHOOL: 'My child needs support for development or learning',
              CLUB: 'My child needs support for development or learning'
            },
            assistanceNeedInstructions: {
              DAYCARE:
                'Tick this box if your child needs support for their development, learning or wellbeing. Support is provided as part of early childhood education. If your child needs support, a special needs teacher will contact you. This will ensure that your child’s needs are taken into account and your child is given a place in a suitable early childhood education unit.',
              PRESCHOOL: null
            },
            assistanceNeedExtraInstructions: {
              PRESCHOOL: (
                <P>
                  Tick this box if your child needs support for their
                  development and/or learning during the pre-primary education
                  year. Support is provided as part of pre-primary education and
                  early childhood education. Also tick this box if your child
                  has another special reason that directly affects the provision
                  of pre-primary education or the unit in which their
                  pre-primary education should be provided. If your child needs
                  support for development and/or learning, a special needs
                  teacher will contact you. This will ensure that your child’s
                  needs are taken into account and your child is given a place
                  in a suitable pre-primary education unit.
                </P>
              )
            },
            preparatory:
              'My child needs support in learning Finnish and the child’s current day care centre has recommended that the child attend preparatory pre-primary education. I am also applying for preparatory education. This does not apply to Swedish pre-primary education.',
            preparatoryInfo: null,
            preparatoryExtraInstructions: (
              <>
                <P>
                  Preparatory education is offered in connection with
                  pre-primary education to children who do not yet have Finnish
                  language skills or who know some Finnish. Preparatory
                  education is offered to children of pre-primary education age
                  in Finnish-speaking municipal pre-primary education groups.
                </P>
                <P>
                  Children attending extended compulsory education are not
                  entitled to preparatory education. If the child has received
                  or later receives a decision on extended compulsory education,
                  the child’s placement will be changed to a pre-primary
                  education placement.
                </P>
              </>
            )
          },
          unitPreference: {
            siblingBasis: {
              title: 'Application based on the sibling principle',
              info: {
                DAYCARE: (
                  <>
                    <P fitted>
                      The sibling principle applies to an early childhood
                      education unit in which the child’s sibling has a place
                      when the child’s early childhood education starts.
                    </P>
                    <P>
                      All children living at the same address are considered
                      siblings. Our aim is to place siblings in the same early
                      childhood education unit if the family so wishes. If you
                      are applying for places for siblings who do not yet attend
                      early childhood education, enter this information in the
                      additional information field.
                    </P>
                    <P>
                      Only add this information if you are using the sibling
                      principle. Also, select the sibling’s unit as your first
                      choice from the menu below
                    </P>
                  </>
                ),
                PRESCHOOL: (
                  <>
                    <P>
                      A child enrolling for pre-primary education has a
                      sibling-basis right to a
                    </P>
                    <ol type="a">
                      <li>
                        early childhood education in their own service area,
                        where they have a sibling who has, at the moment of
                        making the decision and in the coming pre-primary
                        education year, a place at the pre-primary education
                        unit
                      </li>
                      <li>
                        local school determined by the city, which will be
                        attended by their sibling in the coming school year.
                      </li>
                    </ol>
                    <P>
                      Select whether you wish to use the sibling principle based
                      on section a or b if they both apply to your child.
                    </P>
                    <P>
                      Only add this information if you are using the sibling
                      principle. Also, select the sibling’s unit as your first
                      choice from the menu below.
                    </P>
                  </>
                )
              },
              checkbox: {
                PRESCHOOL:
                  'I am primarily applying for a place in the same unit in which the child’s sibling is.'
              }
            },
            units: {
              info: {
                DAYCARE: (
                  <>
                    <P fitted>
                      You can apply for a place in 1–3 units in order of
                      preference. We cannot guarantee a place in the desired
                      unit, but your chances increase if you select several
                      options.{' '}
                    </P>
                    <P>
                      Apply for a service voucher by selecting the service
                      voucher unit to which you want to apply. When applying to
                      a service voucher unit, the unit’s director will also be
                      informed of your application.
                    </P>
                    <P>
                      You can see the locations of all early childhood education
                      units by clicking on ‘Units on the map’.
                    </P>
                  </>
                )
              },
              mapLink: 'Units on the map',
              select: {
                label: (maxUnits: number) =>
                  maxUnits === 1 ? 'Search for a unit' : 'Search for units'
              },
              preferences: {
                info: (maxUnits: number) =>
                  maxUnits === 1
                    ? 'Select a unit'
                    : `Select 1–${maxUnits} units in order of preference. You can change the order with the arrows.`
              }
            }
          },
          contactInfo: {
            familyInfo: (
              <P>
                Your application must include all adults and children living in
                the same household.
              </P>
            ),
            info: (
              <P>
                Personal information is retrieved from the Population
                Information System and cannot be changed on this application. If
                your personal information is incorrect, please update the
                information on the{' '}
                <a
                  href="https://dvv.fi/en/individuals"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  website of the Digital and Population Data Services Agency
                </a>
                . If your address is about to change, you can add the new
                address in a separate field on the application. Add a new
                address for both the child and guardian. Your address will not
                be considered official until it has been updated in the
                Population Information System. We send decisions on early
                childhood and pre-primary education places automatically to the
                child’s other guardian who lives at a different address based on
                the Population Information System.
              </P>
            ),
            hasFutureAddress:
              'The address registered in the Population Information System has changed or is about to change',
            futureAddressInfo:
              'We use the address retrieved from the Population Information System as your official address. Your address in the Population Information System will change after you have submitted a notification of change of address through Posti or the Digital and Population Data Services Agency.',
            emailInfoText:
              'Your email address is used for notifying you about new messages sent to you through eVaka, the online system for early childhood education. The pre-filled email address has been retrieved from the eVaka customer records. If you change your email address, the information will be updated once the application is sent.',
            secondGuardianInfoTitle: 'Other guardian’s information',
            secondGuardianNotFound:
              'According to the information obtained from the Population Information System, your child does not have another guardian.',
            otherChildrenTitle:
              'Under 18-year-old children living in the same household',
            otherChildrenInfo:
              'Under 18-year-old children living in the same household affect the early childhood education fee.',
            areExtraChildren:
              'Other under 18-year-old children live in the same household (e.g. spouse’s/partner’s children)'
          },
          additionalDetails: {
            dietLabel: 'Special dietary needs',
            dietInfo:
              'Some special diets require a separate medical certificate, which you must deliver to the early childhood education unit. Exceptions include a low-lactose and lactose-free diet, a diet based on religious beliefs and a vegetarian diet (lacto-ovo).'
          },
          verification: {
            serviceNeed: {
              assistanceNeed: {
                assistanceNeed: 'Support for development or learning'
              }
            }
          }
        }
      },
      income: {
        assure: (
          <MultiLineCheckboxLabel>
            <span>
              I confirm that the information I have provided is accurate, and I
              have reviewed the customer fee information:
            </span>
            <a
              href="https://www.espoo.fi/en/childcare-and-education/early-childhood-education/early-childhood-education-fees#section-59617"
              target="_blank"
              rel="noreferrer"
            >
              Early education fees | City of Espoo
            </a>
          </MultiLineCheckboxLabel>
        ),
        incomeType: {
          description: (
            <>
              If you are an entrepreneur but also have other income, choose both{' '}
              <strong>Entrepreneur&apos;s income information</strong>, and{' '}
              <strong>Determination of the client fee by income</strong>.
            </>
          ),
          grossIncome: 'Determination of the client fee by income'
        },
        grossIncome: {
          title: 'Filling in income data',
          estimate: 'Estimate of my gross salary (before taxes)'
        }
      }
    }
  },
  cityLogo: {
    src: EspooLogo,
    alt: 'Espoo Logo'
  },
  routeLinkRootUrl: 'https://reittiopas.hsl.fi/reitti/',
  mapConfig,
  featureFlags,
  getMaxPreferredUnits() {
    return 3
  }
}

export default customizations
