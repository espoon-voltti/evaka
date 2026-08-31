// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import ExternalLink from 'lib-components/atoms/ExternalLink'
import UnorderedList from 'lib-components/atoms/UnorderedList'
import { H1, H2, H3, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import type { Translations } from 'lib-customizations/citizen'
import type { DeepPartial } from 'lib-customizations/types'

import featureFlags from './featureFlags'

export const preschoolEnabled = featureFlags.preschool
export const serviceApplicationsEnabled = featureFlags.serviceApplications

const customerContactText = function () {
  return (
    <>
      <a href="mailto:varhaiskasvatus.palveluohjaus@nokiankaupunki.fi">
        varhaiskasvatus.palveluohjaus@nokiankaupunki.fi
      </a>
      , puh 040 483 7145 maanantaisin, keskiviikkoisin ja torstaisin klo 10-12
    </>
  )
}

const fi: DeepPartial<Translations> = {
  personalDetails: {
    familySizeSection: {
      description: (
        <P>
          Samassa taloudessa asuvien aikuisten ja lasten määrä vaikuttaa
          asiakasmaksuihin. Jos perheen tiedoissa on tapahtunut muutos, ole
          yhteydessä asiakasmaksutiimiin.
        </P>
      )
    }
  },
  applications: {
    creation: {
      daycareInfo:
        'Varhaiskasvatushakemuksella haetaan paikkaa kunnallisesta päiväkodista tai perhepäivähoidosta, ostopalvelupäiväkodista tai palvelusetelillä tuetusta päiväkodista.',
      clubInfo:
        'Kerhohakemuksella haetaan paikkaa kunnallisista tai palvelusetelillä tuetuista kerhoista.',
      preschoolLabel: 'Esiopetukseen ilmoittautuminen',
      preschoolInfo:
        'Maksutonta esiopetusta on neljä tuntia päivässä. Tämän lisäksi lapselle voidaan hakea maksullista täydentävää toimintaa. Hakemuksen täydentävään toimintaan voi tehdä esiopetukseen ilmoittautumisen yhteydessä tai erillisenä hakemuksena opetuksen jo alettua. Täydentävän toiminnan paikkaa ei pysty hakemaan tietystä yksiköstä, vaan se määräytyy aina esiopetusyksikön mukaan. Mikäli täydentävän toiminnan paikkaa haetaan myöhemmin, hakemukselle merkitään myös esiopetuksen tarve.',
      preschoolDaycareInfo:
        'Täydentävän toiminnan hakeminen lapsille, jotka ilmoitetaan / on ilmoitettu esiopetukseen',
      applicationInfo: (
        <P>
          Huoltaja voi tehdä muutoksia hakemukseen verkkopalvelussa siihen asti,
          kun hakemus otetaan asiakaspalvelussa käsittelyyn. Tämän jälkeen
          muutokset tai hakemuksen peruminen on mahdollista ottamalla yhteyttä{' '}
          {customerContactText()}.
        </P>
      ),
      duplicateWarning:
        'Lapsella on jo samantyyppinen, keskeneräinen hakemus. Palaa Hakemukset-näkymään ja muokkaa olemassa olevaa hakemusta tai ota yhteyttä Varhaiskasvatuksen asiakaspalveluun.',
      transferApplicationInfo: {
        DAYCARE:
          'Lapsella on jo paikka Nokian varhaiskasvatuksessa. Tällä hakemuksella voit hakea siirtoa toiseen varhaiskasvatusta tarjoavaan yksikköön Nokialla.',
        PRESCHOOL: (
          <span>
            Lapsella on jo esiopetuspaikka. Tällä hakemuksella voit hakea
            esiopetusta täydentävää toimintaa tai siirtoa toiseen esiopetusta
            tarjoavaan yksikköön.
          </span>
        )
      }
    },
    editor: {
      actions: {
        allowOtherGuardianAccess: (
          <span>
            Ymmärrän, että tieto hakemuksesta menee myös lapsen toiselle
            huoltajalle. Jos tieto ei saa mennä toiselle huoltajalle, ole
            yhteydessä {customerContactText()}.
          </span>
        )
      },
      verification: {
        serviceNeed: {
          connectedDaycare: {
            label: 'Esiopetusta täydentävä toiminta',
            withConnectedDaycare: 'Haen esiopetusta täydentävää toimintaa.'
          },
          startDate: {
            title: {
              PRESCHOOL: 'Esiopetuksen aloitus'
            }
          }
        },
        unitPreference: {
          siblingBasis: {
            unit: 'Sisaruksen koulu'
          }
        }
      },
      unitPreference: {
        siblingBasis: {
          checkbox: {
            PRESCHOOL:
              'Toivomme ensisijaisesti esiopetukseen samaan kouluun, jossa lapsen vanhempi sisarus on.'
          },
          info: {
            PRESCHOOL: null
          },
          unit: 'Sisaruksen koulu',
          unitPlaceholder: 'Koulun nimi'
        },
        units: {
          info: {
            PRESCHOOL: (
              <>
                <P>
                  Esiopetuspaikka määräytyy lapsen kotiosoitteen mukaan, tulevan
                  koulupolun mukaisesti. Ilmoittautuessa lapsi ilmoitetaan
                  koulupolun mukaiseen esiopetukseen tai painotettuun
                  esiopetukseen. Mikäli lapsi tarvitsee säännöllisesti ilta- tai
                  vuorohoitoa, hänet ilmoitetaan ilta- tai vuorohoidon
                  esiopetukseen.
                </P>
                <P>
                  Tieto tulevasta esiopetuspaikasta ilmoitetaan huoltajille
                  sähköisesti Suomi.fi-viestit palvelun kautta. Mikäli huoltaja
                  ei ole ottanut palvelua käyttöönsä, tieto lähetetään hänelle
                  kirjeitse.
                </P>
              </>
            )
          },
          serviceVoucherLink:
            'https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatuksen-maksut-tuet-palveluseteli/palveluseteli/'
        }
      },
      heading: {
        info: {
          DAYCARE: (
            <>
              <P>
                Varhaiskasvatuspaikkaa voi hakea ympäri vuoden.
                Varhaiskasvatushakemus tulee jättää viimeistään neljä kuukautta
                ennen hoidon toivottua alkamisajankohtaa. Mikäli
                varhaiskasvatuksen tarve johtuu työllistymisestä, opinnoista tai
                koulutuksesta, eikä hoidon tarpeen ajankohtaa ole pystynyt
                ennakoimaan, on varhaiskasvatuspaikkaa haettava mahdollisimman
                pian - kuitenkin viimeistään kaksi viikkoa ennen kuin lapsi
                tarvitsee hoitopaikan.
              </P>
              <P>
                Kirjallinen päätös varhaiskasvatuspaikasta lähetetään
                Suomi.fi-viestit -palveluun. Mikäli haluatte päätöksen
                sähköisenä tiedoksiantona, teidän tulee ottaa Suomi.fi-viestit
                -palvelu käyttöön. Palvelusta ja sen käyttöönotosta saatte
                lisätietoa{' '}
                <ExternalLink
                  text="https://www.suomi.fi/viestit"
                  href="https://www.suomi.fi/viestit"
                  newTab
                />
                . Mikäli ette ota Suomi.fi-viestit -palvelua käyttöön, päätös
                lähetetään teille postitse.
              </P>
              <P $fitted={true}>* Tähdellä merkityt tiedot ovat pakollisia</P>
            </>
          ),
          PRESCHOOL: (
            <>
              <P>
                Perusopetuslain (26 a §) mukaan lasten on oppivelvollisuuden
                alkamista edeltävänä vuonna osallistuttava vuoden kestävään
                esiopetukseen tai muuhun esiopetuksen tavoitteet saavuttavaan
                toimintaan. Esiopetus on maksutonta.
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
                Kerhopaikkaa voi hakea ympäri vuoden. Kerhohakemuksella voi
                hakea kunnallista tai palvelusetelillä tuettua kerhopaikkaa.
                Kirjallinen ilmoitus kerhopaikasta lähetään Suomi.fi-viestit
                -palveluun. Mikäli haluatte ilmoituksen sähköisenä
                tiedoksiantona, teidän tulee ottaa Suomi.fi-viestit -palvelu
                käyttöön. Palvelusta ja sen käyttöönotosta saatte lisätietoa{' '}
                <ExternalLink
                  text="https://www.suomi.fi/viestit"
                  href="https://www.suomi.fi/viestit"
                  newTab
                />
                . Mikäli ette ota Suomi.fi-viestit -palvelua käyttöön, ilmoitus
                kerhopaikasta lähetetään teille postitse. Paikka myönnetään
                yhdeksi toimintakaudeksi kerrallaan.
              </P>
              <P>
                Kerhohakemus kohdistuu yhdelle kerhon toimintakaudelle. Kyseisen
                kauden päättyessä hakemus poistetaan järjestelmästä.
              </P>
            </>
          )
        }
      },
      serviceNeed: {
        startDate: {
          instructions: {
            DAYCARE: (
              <>
                Toivottua aloituspäivää on mahdollista muuttaa myöhemmäksi niin
                kauan, kun hakemusta ei ole otettu käsittelyyn. Tämän jälkeen
                toivotun aloituspäivän muutokset tehdään ottamalla yhteyttä{' '}
                {customerContactText()}.
              </>
            ),
            PRESCHOOL: null
          },
          info: {
            PRESCHOOL: ['Esiopetusta järjestetään esiopetuskausittain.']
          },
          label: {
            PRESCHOOL: 'Tarve alkaen'
          }
        },
        clubDetails: {
          wasOnDaycareInfo:
            'Jos lapsi on ollut kunnallisessa päiväkodissa tai perhepäivähoidossa ja hän luopuu paikastaan kerhon alkaessa, hänellä on suurempi mahdollisuus saada kerhopaikka.',
          wasOnClubCareInfo:
            'Jos lapsi on ollut kerhossa jo edellisen toimintakauden aikana, hänellä on suurempi mahdollisuus saada paikka kerhosta myös tulevana toimintakautena.'
        },
        urgent: {
          attachmentsMessage: {
            text: (
              <P $fitted={true}>
                Mikäli varhaiskasvatuspaikan tarve johtuu äkillisestä
                työllistymisestä tai opiskelupaikan saamisesta, tulee paikkaa
                hakea viimeistään kaksi viikkoa ennen kuin hoidon tarve alkaa.
                Lisäksi huoltajan tulee ottaa yhteyttä viipymättä{' '}
                {customerContactText()}.
              </P>
            )
          }
        },
        shiftCare: {
          instructions:
            'Mikäli lapsi tarvitsee esiopetuksen lisäksi ilta-/vuorohoitoa, hänet pitää ilmoittaa ilta- tai vuorohoidon esiopetukseen. Lisäksi täydentäväksi toiminnaksi on lapselle valittava täydentävä varhaiskasvatus, yli 5h päivässä. Päiväkodit palvelevat normaalisti arkisin klo 6.00–18.00. Iltahoito on tarkoitettu lapsille, jotka vanhempien työn tai tutkintoon johtavan opiskelun vuoksi tarvitsevat säännöllisesti hoitoa klo 18.00 jälkeen. Iltahoitoa tarjoavat päiväkodit aukeavat tarvittaessa klo 5.30 ja menevät kiinni viimeistään klo 22.30. Osa iltahoitoa antavista päiväkodeista on auki myös viikonloppuisin. Vuorohoito on tarkoitettu niille lapsille, joiden vanhemmat tekevät vuorotyötä ja lapsen hoitoon sisältyy myös öitä.',
          attachmentsMessage: {
            DAYCARE: (
              <P>
                Ilta- ja vuorohoito on tarkoitettu lapsille, jotka vanhempien
                työn tai tutkintoon johtavan opiskelun vuoksi tarvitsevat ilta-
                ja vuorohoitoa. Hakemuksen liitteeksi on toimitettava vanhempien
                osalta työnantajan todistus vuorotyöstä tai opiskelusta
                johtuvasta ilta- tai vuorohoidon tarpeesta.
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
                  Ilta- ja vuorohoito on tarkoitettu lapsille, jotka vanhempien
                  työn tai tutkintoon johtavan opiskelun vuoksi tarvitsevat
                  ilta- ja vuorohoitoa. Hakemuksen liitteeksi on toimitettava
                  vanhempien osalta työnantajan todistus vuorotyöstä tai
                  opiskelusta johtuvasta ilta- tai vuorohoidon tarpeesta.
                </P>
              </>
            )
          }
        },
        assistanceNeedInstructions: {
          DAYCARE:
            'Tehostettua tai erityistä tukea annetaan lapselle heti tarpeen ilmettyä. Mikäli lapsella on olemassa tuen tarpeesta asiantuntijalausunto, tämä tulee ilmoittaa varhaiskasvatushakemuksella. Tukitoimet toteutuvat lapsen arjessa osana varhaiskasvatuksen toimintaa. Nokian varhaiskasvatuksesta otetaan erikseen yhteyttä hakemuksen jättämisen jälkeen, jos lapsella on tuen tarve. Lisätiedot näkyvät varhaiskasvatuksen palveluohjauksessa ja varhaiskasvatuksen erityisopettajalle paikan järjestämiseksi.',
          CLUB: 'Jos lapsella on tuen tarve, Nokian varhaiskasvatuksesta otetaan yhteyttä hakemuksen jättämisen jälkeen.',
          PRESCHOOL:
            'Valitse hakemuksesta tämä kohta, jos lapsi tarvitsee kehitykselleen ja/tai oppimiselleen tukea esiopetusvuonna. Tukea toteutetaan lapsen arjessa osana esiopetuksen toimintaa. Osa esiopetuspaikoista on varattu tukea tarvitseville lapsille. Jos lapsellanne on kehityksen ja/tai oppimisen tuen tarvetta, varhaiskasvatuksen erityisopettaja ottaa teihin yhteyttä, jotta lapsen tarpeet voidaan ottaa huomioon esiopetuspaikkaa osoitettaessa. Lisätiedot näkyvät varhaiskasvatuksen palveluohjauksessa ja varhaiskasvatuksen erityisopettajalle paikan järjestämiseksi.'
        },
        partTime: {
          true: 'Osapäiväinen'
        },
        dailyTime: {
          label: {
            DAYCARE: 'Palveluntarve',
            PRESCHOOL: 'Esiopetusta täydentävä toiminta'
          },
          connectedDaycare: 'Haen esiopetusta täydentävää toimintaa.',
          connectedDaycareInfo: (
            <P>
              Esiopetusaika on neljä tuntia päivässä, pääsääntöisesti klo 9–13.
              Esiopetuksen lisäksi lapsi voi osallistua maksulliseen
              täydentävään toimintaan aamuisin ja iltapäivisin. Täydentävän
              toiminnan vaihtoehtoina ovat päiväkodeissa annettava täydentävä
              varhaiskasvatus ja kouluilla annettava esiopetuksen kerho.
            </P>
          )
        }
      },
      contactInfo: {
        info: (
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
            että huoltajalle. Virallisena osoitetietoa pidetään vasta, kun se on
            päivittynyt väestötietojärjestelmään. Varhaiskasvatus
            {preschoolEnabled ? '-, ja esiopetus' : ''}päätös toimitetaan
            automaattisesti myös eri osoitteessa asuvalle väestötiedoista
            löytyvälle huoltajalle.
          </P>
        ),
        futureAddressInfo: `Nokian varhaiskasvatuksessa ja esiopetuksessa virallisena osoitteena pidetään väestötiedoista saatavaa osoitetta. Osoite väestötiedoissa muuttuu hakijan tehdessä muuttoilmoituksen postiin tai maistraattiin.`
      },
      additionalDetails: {
        dietInfo: (
          <>
            Erityisruokavaliosta huoltaja toimittaa varhaiskasvatus
            {preschoolEnabled ? ' tai esiopetuspaikkaan' : 'paikkaan'} lääkärin
            tai ravitsemusterapeutin täyttämän ja allekirjoittaman{' '}
            <ExternalLink
              href="https://www.nokiankaupunki.fi/kaupunki-ja-hallinto/organisaatio/palvelualueet/ruoka-ja-siivouspalvelut/erityisruokavaliot/"
              text="Selvitys erityisruokavaliosta -lomakkeen"
              newTab
            />
            , joka on määräaikainen.
          </>
        )
      }
    }
  },
  applicationsList: {
    title: `Hakeminen varhaiskasvatukseen${
      preschoolEnabled ? ' ja esiopetukseen' : ''
    }`,
    summary: (
      <P $width="800px">
        Lapsen huoltaja voi tehdä lapselleen hakemuksen varhaiskasvatukseen
        {preschoolEnabled ? ' ja esiopetukseen' : ''}. Huoltajan lasten tiedot
        haetaan tähän näkymään automaattisesti Väestötietojärjestelmästä.
      </P>
    )
  },
  children: {
    pageDescription:
      'Tällä sivulla näet lastesi varhaiskasvatukseen liittyvät yleiset tiedot.'
  },
  footer: {
    cityLabel: '© Nokian kaupunki',
    privacyPolicyLink: (
      <ExternalLink
        href="https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatukseen-hakeminen-ja-irtisanominen/evaka/#5e6b46cb"
        text="Tietosuojaselosteet"
        newTab={true}
        data-qa="footer-policy-link"
      />
    ),
    sendFeedbackLink: (
      <ExternalLink
        href="https://www.nokiankaupunki.fi/kaupunki-ja-hallinto/osallistu-ja-vaikuta/palaute/#5e6b46cb"
        text="Lähetä palautetta"
        newTab={true}
        data-qa="footer-feedback-link"
      />
    )
  },
  components: {
    metadata: {
      organizationName: 'Nokian kaupunki, varhaiskasvatus ja esiopetus'
    }
  },
  loginPage: {
    helpUrl:
      'https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatukseen-hakeminen-ja-irtisanominen/evaka/',
    login: {
      infoBoxText: (
        <>
          <P>
            Mikäli kirjautuminen tästä ei onnistu, katso ohjeet{' '}
            <a
              href="https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatukseen-hakeminen-ja-irtisanominen/evaka/"
              target="_blank"
              rel="noreferrer"
            >
              eVaka | Nokian kaupunki
            </a>
            . Voit kirjautua myös käyttämällä vahvaa tunnistautumista.
          </P>
        </>
      ),
      paragraph: `Huoltajat, joiden lapsi on jo varhaiskasvatuksessa${
        preschoolEnabled ? ' tai esiopetuksessa' : ''
      }: hoida lapsesi päivittäisiä asioita kuten lue viestejä ja ilmoita lapsen läsnäoloajat ja poissaolot.`
    },
    title: `Nokian kaupungin varhaiskasvatus${
      preschoolEnabled ? ' ja esiopetus' : ''
    }`
  },
  map: {
    mainInfo: `Tässä näkymässä voit hakea kartalta kaikki Nokian varhaiskasvatusyksiköt.${
      preschoolEnabled
        ? '  Esiopetusta järjestetään pääsääntöisesti kouluilla.'
        : ''
    }`,
    privateUnitInfo: <></>,
    serviceVoucherLink:
      'https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatuksen-maksut-tuet-palveluseteli/palveluseteli/',
    searchPlaceholder: 'Esim. Peiponpellon päiväkoti'
  },
  decisions: {
    summary: 'Tälle sivulle saapuvat kaikki lasta koskevat päätökset.',
    applicationDecisions: {
      type: {
        PRESCHOOL_DAYCARE: 'Täydentävä varhaiskasvatus'
      },
      summary:
        'Päätöksessä ilmoitettu paikka tulee vastaanottaa tai hylätä viipymättä, viimeistään kahden viikon kuluessa päätöksen tiedoksisaannista.',
      warnings: {
        doubleRejectWarning: {
          text: 'Olet hylkäämässä tarjotun esiopetuspaikan. Täydentävän toiminnan paikka merkitään samalla hylätyksi.'
        }
      },
      response: {
        disabledInfo:
          'HUOM! Pystyt vastaanottamaan / hylkäämään esiopetusta täydentävän toiminnan paikan vasta sen jälkeen, kun olet vastaanottanut esiopetuspaikan.'
      }
    },
    assistanceDecisions: {
      decision: {
        disclaimer:
          'Varhaiskasvatuslain 15 e §:n mukaan tämä päätös voidaan panna täytäntöön muutoksenhausta huolimatta.',
        unitMayChange: '',
        appealInstructionsTitle: 'OIKAISUVAATIMUSOHJE',
        legalInstructionsText: 'Varhaiskasvatuslaki (540/2018), 3 a luku',
        appealInstructions: (
          <>
            <P>
              Tähän päätökseen tyytymätön voi tehdä oikaisuvaatimuksen.
              Päätökseen ei saa hakea muutosta valittamalla.
            </P>
            <H3>Oikaisuvaatimusoikeus</H3>
            <P>
              Oikaisuvaatimuksen saa tehdä se, johon päätös on kohdistettu tai
              jonka oikeuteen, velvollisuuteen tai etuun päätös välittömästi
              vaikuttaa (asianosainen).
            </P>
            <H3>Oikaisuviranomainen</H3>
            <P>
              Oikaisu tehdään Länsi- ja Sisä-Suomen aluehallintovirastolle
              (Vaasan toimipaikka).
            </P>
            <P>
              Länsi- ja Sisä-Suomen aluehallintovirasto
              <br />
              Vaasan toimipaikka
              <br />
              PL 200, 65101 Vaasa
              <br />
              sähköposti:{' '}
              <ExternalLink
                href="mailto:kirjaamo.lansi@avi.fi"
                text="kirjaamo.lansi@avi.fi"
                newTab
              />
            </P>
            <P>
              Sähköinen lomake oikaisuvaatimuksen tekoa varten on saatavilla
              aluehallintoviraston asiointipalvelussa:
              <br />
              <ExternalLink
                href="https://www.suomi.fi/palvelut/oppilas-ja-opiskelija-asioita-seka-varhaiskasvatuksen-tukiasioita-koskeva-oikaisuvaatimus-aluehallintovirasto/ee86d56c-1717-4993-b772-8dde0df57b69"
                text="https://www.suomi.fi/palvelut/oppilas-ja-opiskelija-asioita-seka-varhaiskasvatuksen-tukiasioita-koskeva-oikaisuvaatimus-aluehallintovirasto/ee86d56c-1717-4993-b772-8dde0df57b69"
                newTab
              />
            </P>
            <H3>Oikaisuvaatimusaika</H3>
            <P>
              Oikaisuvaatimus on tehtävä 30 päivän kuluessa päätöksen
              tiedoksisaannista. Oikaisuvaatimus on toimitettava
              oikaisuvaatimusviranomaiselle viimeistään määräajan viimeisenä
              päivänä ennen virka-ajan päättymistä. Oikaisuvaatimuksen
              lähettäminen postitse tai sähköisesti tapahtuu lähettäjän omalla
              vastuulla.
            </P>
            <P>
              Asianosaisen katsotaan saaneen päätöksestä tiedon, jollei muuta
              näytetä, 7 päivän kuluttua kirjeen lähettämisestä, 3 päivän
              kuluttua sähköpostin lähettämisestä, saantitodistuksen osoittamana
              aikana tai tiedoksisaantitodistukseen merkittynä aikana.
              Tiedoksisaantipäivää tai sitä päivää, jona päätös on asetettu
              nähtäväksi, ei lueta määräaikaan. Jos määräajan viimeinen päivä on
              pyhäpäivä tai muu sellainen päivä, jona työt virastoissa on
              keskeytettävä, saa tehtävän toimittaa ensimmäisenä arkipäivänä sen
              jälkeen.
            </P>

            <H3>Oikaisuvaatimuksen muoto ja sisältö</H3>
            <P>
              Oikaisuvaatimus on tehtävä kirjallisesti. Myös sähköinen asiakirja
              täyttää vaatimuksen kirjallisesta muodosta.
            </P>
            <P $noMargin>Oikaisuvaatimuksessa on ilmoitettava:</P>
            <ol>
              <li>
                oikaisuvaatimuksen tekijän nimi, kotikunta, postiosoite ja
                puhelinnumero. Jos oikaisuvaatimuksen tekijän puhevaltaa käyttää
                hänen laillinen edustajansa tai asiamiehensä, tai jos
                oikaisuvaatimuksen laatijana on joku muu henkilö,
                oikaisuvaatimuksessa on ilmoitettava myös tämän nimi ja
                kotikunta.
              </li>
              <li>
                mikäli oikaisuvaatimuspäätös voidaan antaa tiedoksi
                sähköpostilla, yhteystietona pyydetään ilmoittamaan myös
                sähköpostiosoite
              </li>
              <li>päätös, johon haetaan oikaisua</li>
              <li>miten päätöstä halutaan oikaistavaksi</li>
              <li>perusteet, joilla oikaisua vaaditaan.</li>
            </ol>
            <P $noMargin>Oikaisuvaatimukseen on liitettävä:</P>
            <ol>
              <li>asiakirjat, joihin vedotaan</li>
              <li>valtakirja, mikäli käytetään asiamiestä.</li>
            </ol>
            <P>
              Oikaisuvaatimuksen tekijän, laillisen edustajan tai asiamiehen on
              allekirjoitettava valitus. Sähköistä asiakirjaa ei kuitenkaan
              tarvitse täydentää allekirjoituksella, jos asiakirjassa on tiedot
              lähettäjästä eikä asiakirjan alkuperäisyyttä tai eheyttä ole syytä
              epäillä.
            </P>
          </>
        ),
        jurisdictionText: () =>
          'Sivistyslautakunnan toimivallan delegointi viranhaltijoille 1.3.2023 alkaen: Varhaiskasvatuksen palvelupäällikkö päättää varhaiskasvatuslain mukaisesta tuesta ja tukipalveluista yksityisessä varhaiskasvatuksessa (Varhaiskasvatuslaki 3 a luku) ja päiväkodin johtaja päättää varhaiskasvatuslain mukaisesta tuesta ja tukipalveluista kunnallisessa varhaiskasvatuksessa (Varhaiskasvatuslaki 3 a luku)'
      }
    },
    assistancePreschoolDecisions: {
      disclaimer:
        'Perusopetuslain 17 §:n mukaan tämä päätös voidaan panna täytäntöön muutoksenhausta huolimatta.',
      appealInstructionsTitle: 'Oikaisuvaatimus- ja valitusosoitusohje',
      appealInstructions: (
        <>
          <H2>OIKAISUVAATIMUSOHJE ERITYISEN TUEN PÄÄTÖKSESTÄ</H2>
          <P>
            Tähän päätökseen tyytymätön voi tehdä kirjallisen
            oikaisuvaatimuksen. Päätökseen ei saa hakea muutosta valittamalla.
          </P>

          <H3>Oikaisuvaatimusoikeus</H3>
          <P>
            Oikaisuvaatimuksen saa tehdä se, johon päätös on kohdistettu tai
            jonka oikeuteen, velvollisuuteen tai etuun päätös välittömästi
            vaikuttaa (asianosainen). Alle 15-vuotiaan lapsen päätökseen
            oikaisua voi hakea lapsen huoltaja tai muu laillinen edustaja.
          </P>

          <H3>Oikaisuviranomainen</H3>
          <P>
            Oikaisu tehdään Länsi- ja Sisä-Suomen aluehallintovirastolle (Vaasan
            toimipaikka).
          </P>
          <P>
            Länsi- ja Sisä-Suomen aluehallintovirasto
            <br />
            Vaasan toimipaikka
            <br />
            PL 200, 65101 Vaasa
            <br />
            sähköposti:{' '}
            <ExternalLink
              href="mailto:kirjaamo.lansi@avi.fi"
              text="kirjaamo.lansi@avi.fi"
              newTab
            />
          </P>
          <P>
            Sähköinen lomake oikaisuvaatimuksen tekoa varten on saatavilla
            aluehallintoviraston asiointipalvelussa:
            <br />
            <ExternalLink
              href="https://www.suomi.fi/palvelut/oppilas-ja-opiskelija-asioita-seka-varhaiskasvatuksen-tukiasioita-koskeva-oikaisuvaatimus-aluehallintovirasto/ee86d56c-1717-4993-b772-8dde0df57b69"
              text="https://www.suomi.fi/palvelut/oppilas-ja-opiskelija-asioita-seka-varhaiskasvatuksen-tukiasioita-koskeva-oikaisuvaatimus-aluehallintovirasto/ee86d56c-1717-4993-b772-8dde0df57b69"
              newTab
            />
          </P>
          <H3>Oikaisuvaatimusaika</H3>
          <P>
            Oikaisuvaatimus on tehtävä 14 päivän kuluessa päätöksen
            tiedoksisaannista. Oikaisuvaatimus on toimitettava
            oikaisuvaatimusviranomaiselle määräajan viimeisenä päivänä ennen
            virka-ajan päättymistä. Oikaisuvaatimuksen lähettäminen postitse tai
            sähköisesti tapahtuu lähettäjän omalla vastuulla.
          </P>
          <H3>Oikaisuvaatimuksen muoto ja kieli</H3>
          <P $noMargin>Oikaisuvaatimuksessa on ilmoitettava:</P>
          <ol>
            <li>
              Oikaisuvaatimuksen tekijän nimi, kotikunta, postiosoite ja
              puhelinnumero
            </li>
            <li>
              mikäli oikaisuvaatimuspäätös voidaan antaa tiedoksi sähköisenä
              viestinä, yhteystietona pyydetään ilmoittamaan myös
              sähköpostiosoite
            </li>
            <li>päätös, johon haetaan oikaisua</li>
            <li>miten päätöstä halutaan oikaistavaksi</li>
            <li>perusteet, joilla oikaisua vaaditaan</li>
          </ol>

          <P $noMargin>Oikaisuvaatimukseen on liitettävä</P>
          <ol>
            <li>asiakirjat, joihin vedotaan</li>
            <li>valtakirja, mikäli käytetään asiamiestä</li>
          </ol>

          <P>
            Oikaisuvaatimuksen tekijän, laillisen edustajan tai asiamiehen on
            allekirjoitettava oikaisuvaatimus. Sähköistä asiakirjaa ei
            kuitenkaan tarvitse täydentää allekirjoituksella, jos asiakirjassa
            on tiedot lähettäjästä eikä asiakirjan alkuperäisyyttä tai eheyttä
            ole syytä epäillä.
          </P>
        </>
      ),
      jurisdictionText:
        'Sivistyslautakunnan päätös 15.2.2023 13 § (Sivistyslautakunnan toimivallan delegointi 1.3.2023 alkaen), jonka mukaan varhaiskasvatuksen johtaja päättää esiopetuksessa annettavasta tuesta ja tukipalveluista.',
      lawReference:
        'Laki viranomaisen toiminnan julkisuudesta 24 § 1 mom. 30 kohta'
    }
  },
  placement: {
    type: {
      PRESCHOOL_DAYCARE: 'Täydentävä varhaiskasvatus päiväkodissa',
      PRESCHOOL_CLUB: 'Esiopetuksen kerho koululla klo 7-17',
      PRESCHOOL_WITH_DAYCARE: 'Täydentävä varhaiskasvatus päiväkodissa'
    }
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
          <a href="https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatuksen-maksut-tuet-palveluseteli/varhaiskasvatuksen-asiakasmaksut-2/">
            Lisätietoja asiakasmaksuista
          </a>
        </p>
      </>
    ),
    formDescription: (
      <>
        <P data-qa="income-formDescription-p1">
          Tuloselvitys liitteineen palautetaan sen kalenterikuukauden loppuun
          mennessä, jolloin lapsi on aloittanut varhaiskasvatuksessa. Maksu
          voidaan määrätä puutteellisilla tulotiedoilla korkeimpaan maksuun.
          Puutteellisia tulotietoja ei korjata takautuvasti oikaisuvaatimusajan
          jälkeen.
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
            Jos tulosi ylittävät perhekoon mukaisen korkeimman maksun tulorajan,
            hyväksy korkein varhaiskasvatusmaksu. Tällöin sinun ei tarvitse
            selvittää tulojasi lainkaan.
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
              href="https://www.nokiankaupunki.fi/varhaiskasvatus-ja-koulutus/varhaiskasvatuspalvelut/varhaiskasvatuksen-maksut-tuet-palveluseteli/varhaiskasvatuksen-asiakasmaksut-2/"
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
  calendar: {
    absenceModal: {
      absenceTypes: {
        PLANNED_ABSENCE: 'Suunniteltu poissaolo'
      },
      selectChildrenInfo: 'Ilmoita tässä vain koko päivän poissaolot.'
    },
    absences: {
      PLANNED_ABSENCE: 'Suunniteltu poissaolo'
    },
    absentPlanned: 'Suunniteltu poissaolo'
  },
  accessibilityStatement: (
    <>
      <H1>Saavutettavuusseloste</H1>
      <P>
        Tämä saavutettavuusseloste koskee Nokian kaupungin varhaiskasvatuksen
        eVaka-verkkopalvelua osoitteessa{' '}
        <a href="https://evaka.nokiankaupunki.fi">evaka.nokiankaupunki.fi</a>.
        Nokian kaupunki pyrkii takaamaan verkkopalvelun saavutettavuuden,
        parantamaan käyttäjäkokemusta jatkuvasti ja soveltamaan asianmukaisia
        saavutettavuusvaatimuksia.
      </P>
      <H2>Palvelun vaatimustenmukaisuus</H2>
      <P>
        Verkkopalvelu täyttää lain asettamat saavutettavuusvaatimukset (WCAG
        2.1, taso AA) suurimmalta osin, mutta palvelussa on vielä joitakin osia,
        jotka eivät ole vaatimusten mukaisia.
      </P>
      <H2>Toimet saavutettavuuden tukemiseksi</H2>
      <P>
        Varmistamme verkkopalvelun saavutettavuuden muun muassa seuraavilla
        toimenpiteillä:
      </P>
      <ul>
        <li>
          Saavutettavuus huomioidaan jo palvelun suunnitteluvaiheessa, muun
          muassa valitsemalla saavutettavat värit ja kirjasinten koot.
        </li>
        <li>
          Palvelun elementit on määritelty semantiikaltaan johdonmukaisiksi.
        </li>
        <li>Palvelua testataan ruudunlukijalla kehitystyön yhteydessä.</li>
        <li>
          Erilaiset käyttäjät testaavat palvelua ja antavat saavutettavuudesta
          palautetta.
        </li>
        <li>
          Palvelun saavutettavuudesta huolehditaan jatkuvalla valvonnalla
          sisällön ja/tai teknisen toteutuksen muuttuessa.
        </li>
      </ul>
      <P>
        Tätä saavutettavuusselostetta päivitetään palvelun muutosten ja
        saavutettavuuden tarkistusten yhteydessä.
      </P>
      <H2>Tunnetut saavutettavuusongelmat</H2>
      <P>
        Käyttäjät saattavat edelleen kohdata verkkopalvelussa joitakin ongelmia.
        Tunnetut saavutettavuusongelmat on kuvattu alla. Jos huomaat palvelussa
        ongelman, joka ei ole luettelossa, otathan yhteyttä meihin ylläpitäjiin.
      </P>
      <ul>
        <li>
          Kosketusnäyttöä käytettäessä joidenkin toimintojen kosketusalueet
          saattavat olla liian pieniä.
        </li>
      </ul>
      <H2>Kolmannet osapuolet</H2>
      <P>
        Verkkopalvelussa käytetään seuraavia kolmannen osapuolen palveluita,
        joiden saavutettavuutta emme voi taata.
      </P>
      <ul>
        <li>Suomi.fi-tunnistautuminen</li>
        <li>Leaflet-karttapalvelu</li>
      </ul>
      <H2>Vaihtoehtoiset asiointitavat</H2>
      <P>
        <strong>Varhaiskasvatuksen ja esiopetuksen asiakaspalvelu</strong>
        <br />
        Sähköposti:{' '}
        <a href="mailto:varhaiskasvatus.palveluohjaus@nokiankaupunki.fi">
          varhaiskasvatus.palveluohjaus@nokiankaupunki.fi
        </a>
        <br />
        Puhelin: <a href="tel:0404837145">040 483 7145</a>, soittoaika: ma, ke
        ja to klo 9.00–12.00
      </P>
      <H2>Selosteen laatiminen</H2>
      <P>
        Tämä seloste on laadittu 12.4.2022. Palvelun saavutettavuuden on
        arvioinut palvelun kehitystiimi sekä ulkopuolinen asiantuntija-arvioija.
        Viimeisin ulkopuolinen asiantuntija-arvio on tehty keväällä 2024.
        Saavutettavuusseloste on päivitetty viimeksi 30.6.2026.
      </P>
      <H2>Anna palautetta</H2>
      <P>
        Jos huomaat saavutettavuusongelman eVaka-verkkopalvelussa, kerro siitä
        meille ylläpitäjille. Voit kertoa saavutettavuusongelmasta tai antaa
        meille palautetta{' '}
        <ExternalLink
          href="https://www-nokiankaupunki-fi.suomiviestit.fi/suomi.fi/lomake/5f69acac475a6c5531b74150"
          text="tällä verkkolomakkeella"
        />
        . Vastauksen saamisessa voi kestää 14 päivää.
      </P>
      <H2>Täytäntöönpanomenettely</H2>
      <P>
        Jos et ole tyytyväinen ylläpitäjiltä saamaasi vastaukseen, tai et saa
        vastausta 14 päivän aikana, voit tehdä ilmoituksen Traficomille.
        Traficomin sivulla kerrotaan tarkasti, miten ilmoituksen voi tehdä ja
        miten asia käsitellään.
      </P>

      <P>
        <strong>Valvontaviranomaisen yhteystiedot:</strong>
        <br />
        Liikenne- ja viestintävirasto Traficom
        <br />
        Saavutettavuusvalvonta
        <br />
        <a href="mailto:saavutettavuus@traficom.fi">
          saavutettavuus@traficom.fi
        </a>
        <br />
        Vaihde: 029 534 5000
        <br />
        <ExternalLink
          href="https://www.saavutettavuusvaatimukset.fi"
          text="www.saavutettavuusvaatimukset.fi"
        />
      </P>
    </>
  )
}

export default fi
