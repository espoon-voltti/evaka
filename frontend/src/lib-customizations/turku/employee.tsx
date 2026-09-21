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
            'Käytetään silloin, kun huoltaja ei ole ilmoittanut poissaolosta, vaikuttaa kesä-elokuussa myös laskutukseen. Koodi muutetaan vain, jos kyseessä on sairauspoissaolo, jonka jatkumisesta huoltaja ilmoittaa seuraavana päivänä.',
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
        loginAD: 'Kirjaudu sähköpostitunnuksella'
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
      },
      components: {
        metadata: {
          organizationName: 'Turun kaupungin varhaiskasvatus'
        }
      }
    },
    sv: {
      common: {
        careTypeLabels: {
          'connected-daycare': 'Kompletterande'
        }
      },
      application: {
        serviceNeed: {
          connectedLabel: 'Kompletterande småbarnspedagogik',
          connectedValue:
            'Jag ansöker också om kompletterande småbarnspedagogik',
          connectedDaycarePreferredStartDateLabel:
            'Önskat startdatum för kompletterande småbarnspedagogik'
        },
        decisions: {
          types: {
            PRESCHOOL_DAYCARE: 'Beslut om kompletterande småbarnspedagogik'
          }
        }
      },
      decisionDraft: {
        types: {
          PRESCHOOL_DAYCARE:
            'Småbarnspedagogik som kompletterar förskoleundervisningen',
          PREPARATORY_DAYCARE:
            'Småbarnspedagogik som kompletterar den förberedande undervisningen'
        }
      },
      incomeStatement: {
        incomesRegister:
          'Jag lämnar mina inkomstuppgifter som bilaga och vid behov får mina inkomster kontrolleras även i inkomstregistret'
      },
      personProfile: {
        income: {
          details: {
            incomeCoefficients: {
              MONTHLY_WITH_HOLIDAY_BONUS: 'månad + semesterpenning',
              MONTHLY_NO_HOLIDAY_BONUS: 'månad utan semesterpenning',
              BI_WEEKLY_WITH_HOLIDAY_BONUS: '2 veckor + semesterpenning'
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
        description:
          'Du kan definiera ditt tilltalsnamn som används i eVaka. Tilltalsnamnet ska vara ett av dina förnamn.'
      },
      absences: {
        absenceTypes: {
          PLANNED_ABSENCE: 'Avtalsfrånvaro',
          FORCE_MAJEURE: 'Dagsspecifik nedsättning',
          FREE_ABSENCE: 'Avgiftsfri sommarfrånvaro'
        },
        absenceTypesShort: {
          PLANNED_ABSENCE: 'Avtal',
          FORCE_MAJEURE: 'Dagsspecifik nedsättning',
          FREE_ABSENCE: 'Avgiftsfri sommarfrånvaro'
        },
        absenceTypeInfo: {
          OTHER_ABSENCE:
            'Används vid frånvaro samma dag. Frånvaro som anmälts dagen före eller tidigare ska ändras till avtalsfrånvaro.',
          SICKLEAVE: 'Antecknas när barnet är sjukt.',
          UNKNOWN_ABSENCE:
            'Används då vårdnadshavaren inte har anmält frånvaron, påverkar även debiteringen i juni–augusti. Koden ändras endast om det gäller sjukfrånvaro, vars fortsättning vårdnadshavaren meddelar följande dag.',
          PLANNED_ABSENCE:
            'Förhandsanmälda frånvaron. Alla frånvaron som anmälts senast dagen före betraktas som avtalsfrånvaro.',
          TEMPORARY_RELOCATION:
            'Barnet har reservplacerats i en annan enhet. Frånvarande från den egna enheten, närvarande på annan plats.',
          PARENTLEAVE:
            'Frånvaron antecknas för det barn för vilket FPA:s föräldrapenning betalas.',
          FORCE_MAJEURE:
            'Används endast i specialsituationer enligt förvaltningens anvisningar. Enskilda dagar för vilka avgiftsgottgörelse har utlovats',
          FREE_ABSENCE: 'Avgiftsfri frånvaro under sommartid'
        },
        modal: {
          absenceTypes: {
            PLANNED_ABSENCE: 'Avtalsfrånvaro',
            TEMPORARY_RELOCATION: 'Barnet reservplacerat på annan plats',
            FORCE_MAJEURE: 'Dagsspecifik nedsättning',
            FREE_ABSENCE: 'Avgiftsfri sommarfrånvaro'
          }
        }
      },
      footer: {
        cityLabel: 'Åbo stad',
        linkLabel: 'Åbo småbarnspedagogik',
        linkHref:
          'https://www.turku.fi/sv/smabarnspedagogik-och-forskoleundervisning'
      },
      childInformation: {
        assistance: {
          types: {
            preschoolAssistanceLevel: {
              SPECIAL_SUPPORT_WITH_DECISION_LEVEL_1:
                'Särskilt stöd och förlängd läroplikt - annan än gravt utvecklingsstörd (till Koski)',
              SPECIAL_SUPPORT_WITH_DECISION_LEVEL_2:
                'Särskilt stöd och förlängd läroplikt - gravt utvecklingsstörd (till Koski)'
            }
          }
        },
        assistanceNeed: {
          fields: {
            capacityFactor: 'Barnets platsbehov',
            capacityFactorInfo:
              'Det strukturella stödet bestäms vanligtvis utifrån barnets ålder och servicebehov. Om barnet har ett sådant stödbehov som ökar behovet av strukturellt stöd, läggs koefficienten för stödbehovet till här. Stödbehovet och koefficienten läggs till av specialläraren inom småbarnspedagogik. Daghemsföreståndaren lägger till koefficienten för en integrerad grupp eller en specialgrupp'
          }
        },
        assistanceAction: {
          title: 'Stödåtgärder och stödtjänster',
          fields: {
            actions: 'Strukturella stödåtgärder'
          }
        },
        dailyServiceTimes: {
          info: 'Spara här den dagliga närvarotid som avtalats i avtalet om småbarnspedagogik.',
          info2: ''
        }
      },
      unit: {
        placementProposals: {
          rejectReasons: {
            REASON_1: 'Lediga platser endast i förskolegruppen',
            REASON_2: 'Servicesedelproducentens platser är fulla',
            OTHER: 'Annan orsak - motivering'
          },
          infoTitle: '',
          infoText: ''
        }
      },
      login: {
        loginAD: 'Logga in med edu.turku.fi-konto'
      },
      placement: {
        type: {
          DAYCARE_FIVE_YEAR_OLDS:
            'Avgiftsfri och kompletterande småbarnspedagogik för 5-åringar',
          DAYCARE_PART_TIME_FIVE_YEAR_OLDS:
            'Avgiftsfri småbarnspedagogik för 5-åringar',
          CLUB: 'Klubbverksamhet och park',
          TEMPORARY_DAYCARE: 'Tillfällig småbarnspedagogik'
        }
      },
      unitEditor: {
        placeholder: {
          email: 'fornamn.efternamn@turku.fi',
          url: 't.ex. https://www.turku.fi/toimipaikat/peppiina-paaskyvuori-kaenkuja-3',
          streetAddress: 'Gatunamn t.ex. Käenkuja 3',
          decisionCustomization: {
            name: 't.ex. Ankkalammen Peppiina'
          }
        },
        field: {
          decisionCustomization: {
            // Saved verbatim to daycare.decision_handler and matched by exact
            // string in the unit editor, so these must not be translated.
            handler: ['Palveluohjaus', 'Varhaiskasvatusyksikön johtaja']
          }
        }
      },
      welcomePage: {
        text: 'Du har loggat in i tjänsten eVaka-Åbo. Ditt användarkonto har ännu inte beviljats rättigheter som möjliggör användning av tjänsten. Daghemspersonalens användarrättigheter får du av enhetens närmaste chef. Övriga användarrättigheter fås av eVakas huvudanvändare genom att anmäla inloggningen till varkas.tietojarjestelmat@turku.fi'
      },
      components: {
        metadata: {
          organizationName: 'Åbo stads småbarnspedagogik'
        }
      }
    }
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
  absenceTypesNotSelectableInWeekCalendar: [
    'FREE_ABSENCE',
    'PARENTLEAVE',
    'FORCE_MAJEURE'
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
