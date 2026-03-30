{
  /*
SPDX-FileCopyrightText: 2021 City of Oulu

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import {
  daycareAssistanceLevels,
  preschoolAssistanceLevels
} from 'lib-common/generated/api-types/assistance'
import type { EmployeeCustomizations } from 'lib-customizations/types'

import OuluLogo from './city-logo.svg'
import featureFlags from './featureFlags'
import { additionalStaffAttendanceTypes } from './shared'

const customizations: EmployeeCustomizations = {
  appConfig: {},
  translations: {
    fi: {
      application: {
        serviceNeed: {
          connectedLabel: 'Täydentävä varhaiskasvatus',
          connectedValue: 'Haen myös täydentävää varhaiskasvatusta',
          connectedDaycarePreferredStartDateLabel:
            'Täydentävän varhaiskasvatuksen toivottu aloituspäivä'
        },
        decisions: {
          types: {
            CLUB: 'Kerhopäätös',
            DAYCARE: 'Varhaiskasvatuspäätös',
            DAYCARE_PART_TIME: 'Varhaiskasvatuspäätös (osapäiväinen)',
            PRESCHOOL: 'Esiopetuspäätös',
            PRESCHOOL_DAYCARE: 'Täydentävä varhaiskasvatuspäätös',
            PRESCHOOL_CLUB: 'Esiopetuksen kerho',
            PREPARATORY_EDUCATION: 'Valmistavan opetuksen päätös'
          }
        }
      },
      placementDraft: {
        preschoolDaycare: 'Täydentävä varhaiskasvatus'
      },
      decisionDraft: {
        types: {
          CLUB: 'Kerho',
          DAYCARE: 'Varhaiskasvatus',
          DAYCARE_PART_TIME: 'Osapäiväinen varhaiskasvatus',
          PRESCHOOL_DAYCARE: 'Esiopetusta täydentävä varhaiskasvatus',
          PRESCHOOL_CLUB: 'Esiopetuksen kerho',
          PRESCHOOL: 'Esiopetus',
          PREPARATORY: 'Valmistava opetus',
          PREPARATORY_EDUCATION: 'Valmistava opetus',
          PREPARATORY_DAYCARE:
            'Valmistavaan opetusta täydentävä varhaiskasvatus'
        }
      },
      preferredFirstName: {
        popupLink: 'Kutsumanimi',
        title: 'Kutsumanimi',
        description:
          'Voit määritellä eVakassa käytössä olevan kutsumanimesi. Kutsumanimen tulee olla jokin etunimistäsi. Jos nimesi on vaihtunut ja sinulla on tarve päivittää eVakaan uusi nimesi, ole yhteydessä Oulun HelpDeskiin.',
        select: 'Valitse kutsumanimi',
        confirm: 'Vahvista'
      },
      invoices: {
        buttons: {
          individualSendAlertText: ''
        }
      },
      // override translations here
      footer: {
        cityLabel: 'Oulun kaupunki',
        linkLabel: 'Oulun varhaiskasvatus',
        linkHref: 'https://www.ouka.fi/oulu/paivahoito-ja-esiopetus'
      },
      childInformation: {
        assistanceNeed: {
          title: 'Lapsen tuki',
          fields: {
            dateRange: 'Tuen tarve ajalle',
            capacityFactor: 'Lapsen paikkaluku',
            capacityFactorInfo:
              'Täytetään, kun lapsen paikkaluku/tuen kerroin on muu kuin 1. Yksityisen palvelusetelin korotus määräytyy tuen kertoimen mukaan. Täytetään, kun päätös tehty.',
            bases: 'Perusteet'
          }
        },
        assistanceAction: {
          title: 'Tukitoimet',
          fields: {
            dateRange: 'Tukitoimien voimassaoloaika',
            actions: 'Tukitoimet',
            actionTypes: {
              OTHER: 'Muu tukitoimi'
            },
            otherActionPlaceholder:
              'Voit kirjoittaa tähän lisätietoa muista tukitoimista.'
          }
        },
        assistanceNeedVoucherCoefficient: {
          sectionTitle: 'Palvelusetelikerroin',
          voucherCoefficient: 'Palvelusetelikerroin',
          create: 'Aseta uusi palvelusetelikerroin',
          form: {
            title: 'Aseta uusi palvelusetelikerroin',
            editTitle: 'Muokkaa palvelusetelikerrointa',
            titleInfo:
              'Valitse palvelusetelikertoimen voimassaolopäivämäärät korotetun palvelusetelipäätöksen mukaisesti.',
            coefficient: 'Palvelusetelikerroin (luku)',
            validityPeriod: 'Palvelusetelikerroin voimassa',
            errors: {
              previousOverlap:
                'Aiempi päällekkäinen palvelusetelikerroin katkaistaan automaattisesti.',
              upcomingOverlap:
                'Tuleva päällekkäinen palvelusetelikerroin siirretään alkamaan myöhemmin automaattisesti.',
              fullOverlap:
                'Edellinen päällekkäinen palvelusetelikerroin poistetaan automaattisesti.',
              coefficientRange: 'Kerroin tulee olla välillä 1-10'
            }
          },
          deleteModal: {
            title: 'Poistetaanko palvelusetelikerroin?',
            description:
              'Haluatko varmasti poistaa palvelusetelikertoimen? Asiakkaalle ei luoda uutta arvopäätöstä, vaikka kertoimen poistaisi, vaan sinun tulee tehdä uusi takautuva arvopäätös.',
            delete: 'Poista kerroin'
          }
        },
        dailyServiceTimes: {
          info: 'Tallenna tähän varhaiskasvatussopimuksella sovittu päivittäinen läsnäoloaika.',
          info2: ''
        }
      },
      unit: {
        placementProposals: {
          rejectReasons: {
            REASON_1:
              'Tilarajoite, sovittu varhaiskasvatuksen aluepäällikön kanssa',
            REASON_2:
              'Yksikön kokonaistilanne, sovittu varhaiskasvatuksen aluepäällikön kanssa'
          },
          infoTitle: '',
          infoText: ''
        }
      },
      login: {
        loginAD: 'Oulu AD'
      },
      placement: {
        type: {
          DAYCARE: 'Varhaiskasvatus',
          DAYCARE_PART_TIME: 'Osapäiväinen varhaiskasvatus',
          TEMPORARY_DAYCARE: 'Tilapäinen kokopäiväinen varhaiskasvatus',
          PRESCHOOL_DAYCARE: 'Esiopetusta täydentävä varhaiskasvatus',
          CLUB: 'Kerho'
        }
      },
      unitEditor: {
        placeholder: {
          phone: 'esim. +358 40 555 5555',
          email: 'etunimi.sukunimi@ouka.fi',
          url: 'esim. https://www.ouka.fi/oulu/ainolan-paivakoti/etusivu',
          streetAddress: 'Kadun nimi esim. Radiomastontie 12',
          decisionCustomization: {
            name: 'esim. Ainolan päiväkoti'
          }
        },
        field: {
          decisionCustomization: {
            handler: ['Palveluohjaus', 'Varhaiskasvatusyksikön johtaja']
          }
        }
      },
      welcomePage: {
        text: 'Olet kirjautunut sisään eVaka Oulu -palveluun. Käyttäjätunnuksellesi ei ole vielä annettu oikeuksia, jotka mahdollistavat palvelun käytön. Tarvittavat käyttöoikeudet saat omalta esimieheltäsi.'
      },
      absences: {
        title: 'Poissaolot',
        absenceTypes: {
          OTHER_ABSENCE: 'Poissaolo',
          SICKLEAVE: 'Sairaus',
          UNKNOWN_ABSENCE: 'Ilmoittamaton poissaolo',
          PLANNED_ABSENCE: 'Sopimuspoissaolo',
          TEMPORARY_RELOCATION: 'Lapsi varasijoitettuna muualla',
          PARENTLEAVE: 'Vanhempainvapaa',
          FORCE_MAJEURE: 'Hyvityspäivä',
          FREE_ABSENCE: 'Maksuton poissaolo',
          NO_ABSENCE: 'Ei poissaoloa'
        },
        absenceTypesShort: {
          OTHER_ABSENCE: 'Poissaolo',
          SICKLEAVE: 'Sairaus',
          UNKNOWN_ABSENCE: 'Ilmoittamaton',
          PLANNED_ABSENCE: 'Sopimus',
          TEMPORARY_RELOCATION: 'Varasijoitus',
          PARENTLEAVE: 'Vanh.vap.',
          FORCE_MAJEURE: 'Hyvitys',
          FREE_ABSENCE: 'Maksuton',
          NO_ABSENCE: 'Ei poissa'
        },
        absenceTypeInfo: {
          OTHER_ABSENCE:
            'Käytetään aina, kun huoltaja on ilmoittanut poissaolosta mukaan lukien säännölliset vapaat ja loma-ajat. Käytetään myös vuoroyksiköissä lasten lomamerkinnöissä tai muissa poissaoloissa, jotka ovat suunniteltujen läsnäolovarausten ulkopuolella.',
          SICKLEAVE:
            'Merkitään, kun lapsi on sairaana tai kuntoutus- / tutkimusjaksolla.',
          UNKNOWN_ABSENCE:
            'Käytetään silloin, kun huoltaja ei ole ilmoittanut poissaolosta. Koodi muutetaan vain, jos kyseessä on sairauspoissaolo.',
          PLANNED_ABSENCE:
            'Palveluntarvesopimuksen (10 tai 13 pv/kk) mukaiset etukäteen ilmoitetut poissaolot.',
          TEMPORARY_RELOCATION:
            'Lapselle on tehty varasijoitus toiseen yksikköön. Poissa omasta, läsnä muualla.',
          PARENTLEAVE:
            'Poissaolo merkitään sille lapselle, josta maksetaan vanhempainrahaa.',
          FORCE_MAJEURE:
            'Käytetään vain erikoistilanteissa hallinnon ohjeiden mukaan. Yksittäisiä päiviä, joista luvattu maksuhyvitys.',
          FREE_ABSENCE: 'Kesäajan maksuton poissaolo',
          NO_ABSENCE: 'Jos lapsi on paikalla, älä merkitse mitään.'
        },
        modal: {
          absenceSectionLabel: 'Poissaolon syy',
          placementSectionLabel: 'Toimintamuoto, jota poissaolo koskee',
          saveButton: 'Tallenna',
          cancelButton: 'Peruuta',
          absenceTypes: {
            OTHER_ABSENCE: 'Poissaolo',
            SICKLEAVE: 'Sairaus',
            UNKNOWN_ABSENCE: 'Ilmoittamaton poissaolo',
            PLANNED_ABSENCE: 'Sopimuspoissaolo',
            TEMPORARY_RELOCATION: 'Lapsi varasijoitettuna muualla',
            PARENTLEAVE: 'Vanhempainvapaa',
            FORCE_MAJEURE: 'Hyvityspäivä',
            FREE_ABSENCE: 'Maksuton poissaolo',
            NO_ABSENCE: 'Ei poissaoloa'
          },
          free: 'Maksuton',
          paid: 'Maksullinen',
          absenceSummaryTitle: 'Lapsen poissaolokooste'
        }
      },
      reports: {
        invoices: {
          title: 'Laskujen täsmäytys',
          description:
            'Laskujen täsmäytysraportti Monetra-järjestelmän vertailua varten',
          areaCode: 'Alue',
          amountOfInvoices: 'Laskuja',
          totalSumCents: 'Summa',
          amountWithoutSSN: 'Hetuttomia',
          amountWithoutAddress: 'Osoitteettomia',
          amountWithZeroPrice: 'Nollalaskuja'
        }
      }
    },
    sv: {}
  },
  cityLogo: {
    src: OuluLogo,
    alt: 'Oulu logo'
  },
  featureFlags,
  placementTypes: [
    'CLUB',
    'DAYCARE',
    'DAYCARE_PART_TIME',
    'TEMPORARY_DAYCARE',
    'TEMPORARY_DAYCARE_PART_DAY',
    'PRESCHOOL',
    'PRESCHOOL_DAYCARE',
    'PREPARATORY',
    'PREPARATORY_DAYCARE'
  ],
  absenceTypes: [
    'OTHER_ABSENCE',
    'UNKNOWN_ABSENCE',
    'PLANNED_ABSENCE',
    'SICKLEAVE',
    'PARENTLEAVE',
    'FORCE_MAJEURE',
    'FREE_ABSENCE'
  ],
  voucherValueDecisionTypes: [
    'NORMAL',
    'RELIEF_ACCEPTED',
    'RELIEF_PARTLY_ACCEPTED',
    'RELIEF_REJECTED'
  ],
  daycareAssistanceLevels: [...daycareAssistanceLevels],
  otherAssistanceMeasureTypes: [
    'TRANSPORT_BENEFIT',
    'ANOMALOUS_EDUCATION_START',
    'CHILD_DISCUSSION_OFFERED',
    'CHILD_DISCUSSION_HELD',
    'CHILD_DISCUSSION_COUNSELING'
  ],
  placementPlanRejectReasons: ['REASON_1', 'REASON_2', 'OTHER'],
  preschoolAssistanceLevels: [...preschoolAssistanceLevels],
  unitProviderTypes: [
    'MUNICIPAL',
    'PRIVATE',
    'PRIVATE_SERVICE_VOUCHER',
    'PURCHASED'
  ],
  additionalStaffAttendanceTypes
}

export default customizations
