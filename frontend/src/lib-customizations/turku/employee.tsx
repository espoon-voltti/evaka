{
  /*
SPDX-FileCopyrightText: 2021 City of Turku

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import { daycareAssistanceLevels } from 'lib-common/generated/api-types/assistance'
import type { EmployeeCustomizations } from 'lib-customizations/types'

import TurkuLogo from './city-logo-citizen.png'
import featureFlags from './featureFlags'
import { additionalStaffAttendanceTypes } from './shared'

const customizations: EmployeeCustomizations = {
  appConfig: {},
  translations: {
    fi: {
      common: {
        careTypeLabels: {
          'connected-daycare': 'Täydentävä'
        }
      },
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
      incomeStatement: {
        incomesRegister:
          'Toimintan tulotietoni liitteenä ja tarvittaessa tuloni saa tarkistaa myös tulorekisteristä'
      },
      personProfile: {
        income: {
          details: {
            incomeCoefficients: {
              MONTHLY_WITH_HOLIDAY_BONUS: 'kuukausi + lomaraha',
              MONTHLY_NO_HOLIDAY_BONUS: 'kuukausi ilman lomarahaa',
              BI_WEEKLY_WITH_HOLIDAY_BONUS: '2 viikkoa + lomaraha',
              BI_WEEKLY_NO_HOLIDAY_BONUS: '2 viikkoa ilman lomarahaa',
              DAILY_ALLOWANCE_21_5: 'Päiväraha x 21,5',
              DAILY_ALLOWANCE_25: 'Päiväraha x 25',
              YEARLY: 'Vuosi'
            }
          }
        }
      },
      invoices: {
        buttons: {
          individualSendAlertText: ''
        }
      },
      preferredFirstName: {
        popupLink: 'Kutsumanimi',
        title: 'Kutsumanimi',
        description:
          'Voit määritellä eVakassa käytössä olevan kutsumanimesi. Kutsumanimen tulee olla jokin etunimistäsi.',
        select: 'Valitse kutsumanimi',
        confirm: 'Vahvista'
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
          FORCE_MAJEURE: 'Päiväkohtainen alennus',
          FREE_ABSENCE: 'Maksuton kesäpoissaolo',
          UNAUTHORIZED_ABSENCE: 'Ilmoittamaton päivystyksen poissaolo',
          NO_ABSENCE: 'Ei poissaoloa'
        },
        absenceTypesShort: {
          OTHER_ABSENCE: 'Poissaolo',
          SICKLEAVE: 'Sairaus',
          UNKNOWN_ABSENCE: 'Ilmoittamaton',
          PLANNED_ABSENCE: 'Sopimus',
          TEMPORARY_RELOCATION: 'Varasijoitus',
          PARENTLEAVE: 'Vanh.vap.',
          FORCE_MAJEURE: 'Päiväkohtainen alennus',
          FREE_ABSENCE: 'Maksuton kesäpoissaolo',
          UNAUTHORIZED_ABSENCE: 'Sakko',
          NO_ABSENCE: 'Ei poissa'
        },
        absenceTypeInfo: {
          OTHER_ABSENCE:
            'Käytetään kuluvan päivän poissaolossa. Edellisenä päivänä tai sitä aiemmin ilmoitetut poissaolot tulee muuttaa sopimuspoissaoloiksi.',
          SICKLEAVE: 'Merkitään kun lapsi on sairaana.',
          UNKNOWN_ABSENCE:
            'Käytetään silloin, kun huoltaja ei ole ilmoittanut poissaolosta, vaikuttaa heinäkuussa myös laskutukseen. Koodi muutetaan vain, jos kyseessä on sairauspoissaolo, jonka jatkumisesta huoltaja ilmoittaa seuraavana päivänä.',
          PLANNED_ABSENCE:
            'Ennalta ilmoitetut poissaolot. Kaikki edeltävänä päivänä tai sitä aiemmin ilmoitetut poissaolot ovat sopimuspoissaoloja.',
          TEMPORARY_RELOCATION:
            'Lapselle on tehty varasijoitus toiseen yksikköön. Poissa omasta, läsnä muualla.',
          PARENTLEAVE:
            'Poissaolo merkitään sille lapselle, josta maksetaan Kelan vanhenpainrahaa.',
          FORCE_MAJEURE:
            'Käytetään vain erikoistilanteissa hallinnon ohjeiden mukaan. Yksittäisiä päiviä, joista on luvattu maksuhyvitys',
          FREE_ABSENCE: 'Kesäajan maksuton poissaolo',
          UNAUTHORIZED_ABSENCE: 'Ilmoittamaton päivystyksen poissaolo',
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
            FORCE_MAJEURE: 'Päiväkohtainen alennus',
            FREE_ABSENCE: 'Maksuton kesäpoissaolo',
            UNAUTHORIZED_ABSENCE: 'Ilmoittamaton päivystyksen poissaolo',
            NO_ABSENCE: 'Ei poissaoloa'
          },
          free: 'Maksuton',
          paid: 'Maksullinen',
          absenceSummaryTitle: 'Lapsen poissaolokooste'
        }
      },
      footer: {
        cityLabel: 'Turun kaupunki',
        linkLabel: 'Turun varhaiskasvatus',
        linkHref: 'https://www.turku.fi/paivahoito-ja-koulutus/varhaiskasvatus'
      },
      childInformation: {
        assistance: {
          types: {
            preschoolAssistanceLevel: {
              SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1:
                'Erityinen tuki ja pidennetty oppivelvollisuus - muu kuin vaikeimmin kehitysvammainen (Koskeen)',
              SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2:
                'Erityinen tuki ja pidennetty oppivelvollisuus - vaikeimmin kehitysvammainen (Koskeen)'
            },
            otherAssistanceMeasureType: {
              TRANSPORT_BENEFIT: 'Kuljetusetu (esioppilailla Koski-tieto)',
              ACCULTURATION_SUPPORT: 'Lapsen kotoutumisen tuki (ELY)'
            }
          }
        },
        assistanceNeed: {
          fields: {
            capacityFactor: 'Lapsen paikkatarve',
            capacityFactorInfo:
              'Rakenteellinen tuki määräytyy yleensä lapsen iän ja palvelun tarpeen mukaan. Mikäli lapsella on sellainen tuen tarve, joka lisää rakenteellisen tuen tarvetta, lisätään tuen tarpeen kerroin tähän. Tuen tarpeen ja kertoimen lisää varhaiskasvatuksen erityisopettaja. Päiväkodinjohtaja lisää kertoimen integroidun ryhmän tai erityisryhmän osalta',
            bases: 'Perusteet'
          }
        },
        assistanceAction: {
          title: 'Tukitoimet ja tukipalvelut',
          fields: {
            actions: 'Rakenteelliset tukitoimet'
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
            REASON_1: 'Vapaat paikat vain esiopetuksen ryhmässä',
            REASON_2: 'Palvelusetelituottajan paikat täynnä',
            OTHER: 'Muu syy - perustelut'
          },
          infoTitle: '',
          infoText: ''
        }
      },
      login: {
        loginAD: 'Kirjaudu edu.turku.fi-tunnuksella'
      },
      placement: {
        type: {
          DAYCARE: 'Varhaiskasvatus',
          DAYCARE_PART_TIME: 'Osapäiväinen varhaiskasvatus',
          PRESCHOOL: 'Esiopetus',
          PRESCHOOL_DAYCARE: 'Esiopetus ja täydentävä varhaiskasvatus',
          PREPARATORY: 'Valmistava opetus',
          PREPARATORY_DAYCARE: 'Valmistava ja täydentävä varhaiskasvatus',
          DAYCARE_FIVE_YEAR_OLDS:
            '5-vuotiaiden maksuton ja täydentävä varhaiskasvatus',
          DAYCARE_PART_TIME_FIVE_YEAR_OLDS:
            '5-vuotiaiden maksuton varhaiskasvatus',
          CLUB: 'Kerho ja puisto',
          TEMPORARY_DAYCARE: 'Tilapäinen varhaiskasvatus',
          SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito'
        }
      },
      unitEditor: {
        placeholder: {
          phone: 'esim. +358 40 555 5555',
          email: 'etunimi.sukunimi@turku.fi',
          url: 'esim. https://www.turku.fi/toimipaikat/peppiina-paaskyvuori-kaenkuja-3',
          streetAddress: 'Kadun nimi esim. Käenkuja 3',
          decisionCustomization: {
            name: 'esim. Ankkalammen Peppiina'
          }
        },
        field: {
          decisionCustomization: {
            handler: ['Palveluohjaus', 'Varhaiskasvatusyksikön johtaja']
          }
        }
      },
      welcomePage: {
        text: 'Olet kirjautunut eVaka-Turku palveluun. Käyttäjätunnuksellesi ei ole vielä annettu oikeuksia, jotka mahdollistavat palvelun käytön. Päiväkodin henkilökunnan käyttäjäoikeudet saat yksikön lähijohtajalta. Muut käyttöoikeudet saa eVakan pääkäyttäjältä ilmoittamalla kirjautumisesta varkas.tietojarjestelmat@turku.fi'
      }
    },
    sv: {}
  },
  cityLogo: {
    src: TurkuLogo,
    alt: 'Turku logo'
  },
  featureFlags,
  placementTypes: [
    'DAYCARE',
    'DAYCARE_PART_TIME',
    'PRESCHOOL',
    'PRESCHOOL_DAYCARE',
    'PREPARATORY',
    'PREPARATORY_DAYCARE',
    'CLUB',
    'TEMPORARY_DAYCARE'
  ],
  absenceTypes: [
    'OTHER_ABSENCE',
    'UNKNOWN_ABSENCE',
    'PLANNED_ABSENCE',
    'SICKLEAVE',
    'FORCE_MAJEURE',
    'PARENTLEAVE',
    'FREE_ABSENCE'
  ],
  voucherValueDecisionTypes: [
    'NORMAL',
    'RELIEF_ACCEPTED',
    'RELIEF_PARTLY_ACCEPTED',
    'RELIEF_REJECTED'
  ],
  daycareAssistanceLevels: daycareAssistanceLevels.filter(
    (level) => level !== 'GENERAL_SUPPORT'
  ),
  otherAssistanceMeasureTypes: [
    'TRANSPORT_BENEFIT',
    'ACCULTURATION_SUPPORT',
    'ANOMALOUS_EDUCATION_START'
  ],
  placementPlanRejectReasons: ['REASON_1', 'REASON_2', 'OTHER'],
  preschoolAssistanceLevels: [
    'INTENSIFIED_SUPPORT',
    'SPECIAL_SUPPORT',
    'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1',
    'SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2',
    'CHILD_SUPPORT',
    'CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION',
    'CHILD_SUPPORT_AND_OLD_EXTENDED_COMPULSORY_EDUCATION',
    'CHILD_SUPPORT_2_AND_OLD_EXTENDED_COMPULSORY_EDUCATION'
  ],
  unitProviderTypes: [
    'MUNICIPAL',
    'PURCHASED',
    'PRIVATE',
    'PRIVATE_SERVICE_VOUCHER'
  ],
  additionalStaffAttendanceTypes
}

export default customizations
