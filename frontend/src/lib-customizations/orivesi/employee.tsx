// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  daycareAssistanceLevels,
  otherAssistanceMeasureTypes,
  preschoolAssistanceLevels
} from 'lib-common/generated/api-types/assistance'
import type { EmployeeCustomizations } from 'lib-customizations/types'

import OrivesiLogo from './OrivesiLogo.svg'
import featureFlags from './featureFlags'
import sharedCustomizations from './shared'

const customizations: EmployeeCustomizations = {
  appConfig: {},
  translations: {
    fi: {
      titles: {
        placementTool: 'Esiopetussijoitusten tuonti'
      },
      application: {
        serviceNeed: {
          connectedLabel: 'Täydentävä toiminta',
          connectedValue: 'Haen myös täydentävää toimintaa',
          connectedDaycarePreferredStartDateLabel:
            'Täydentävän toivottu aloituspäivä'
        },
        types: {
          PRESCHOOL_DAYCARE: 'Täydentävä varhaiskasvatus'
        },
        decisions: {
          types: {
            PRESCHOOL_DAYCARE: 'Täydentävän varhaiskasvatuksen päätös'
          }
        }
      },
      applications: {
        types: {
          PRESCHOOL_DAYCARE: 'Esiopetus & täydentävä varhaiskasvatus',
          PRESCHOOL_CLUB: 'Esiopetus & esiopetuksen kerho',
          PREPARATORY_DAYCARE: 'Valmistava & täydentävä',
          DAYCARE_ONLY: 'Myöhemmin haettu täydentävä toiminta'
        }
      },
      absences: {
        absenceCategories: {
          NONBILLABLE: 'Esiopetus / kerhotoiminta / koululaisen vuorohoito',
          BILLABLE: 'Varhaiskasvatus / täydentävä varhaiskasvatus'
        },
        absenceTypes: {
          UNKNOWN_ABSENCE: 'Esiopetuksen ilmoittamaton poissaolo',
          PLANNED_ABSENCE: 'Suunniteltu poissaolo',
          FORCE_MAJEURE: 'Hyvityspäivä',
          FREE_ABSENCE: 'Kesäajan maksuttoman jakson poissaolo'
        },
        absenceTypesShort: {
          UNKNOWN_ABSENCE: 'Esiopetuksen ilmoittamaton',
          PLANNED_ABSENCE: 'Suunniteltu',
          FORCE_MAJEURE: 'Hyvitys'
        },
        absenceTypeInfo: {
          NO_ABSENCE: 'Lapsi läsnä varhaiskasvatuksessa/esiopetustoiminnassa.',
          OTHER_ABSENCE:
            'Käytetään tapauksissa, kun ilmoitus lapsen poissaolosta tulee kalenterin lukkiutumisen jälkeen.',
          UNKNOWN_ABSENCE:
            'Käytetään tapauksissa, kun lapsen esiopetuksen poissaolosta ei tule huoltajalta mitään ilmoitusta.',
          PLANNED_ABSENCE: 'Käytetään etukäteen ilmoitetuista poissaoloista.',
          SICKLEAVE: 'Lapsi sairaana.',
          TEMPORARY_RELOCATION:
            'Käytetään tapauksissa, kun lapselle on tehty varasijoitus toiseen yksikköön.',
          PARENTLEAVE:
            'Käytetään tapauksissa, kun lapsi on poissa varhaiskasvatuksesta/esiopetustoiminnasta vanhempainvapaan ajan.',
          FORCE_MAJEURE:
            'Käytetään erityistapauksissa, kun lapsen poissaolosta hyvitetään asiakasmaksua.',
          FREE_ABSENCE:
            'Käytetään kesäaikana ennalta sovitun 6 viikon maksuttoman ajanjakson poissaoloihin.'
        },
        careTypes: {
          PRESCHOOL_DAYCARE: 'Täydentävä varhaiskasvatus'
        },
        modal: {
          absenceTypes: {
            UNKNOWN_ABSENCE: 'Esiopetuksen ilmoittamaton poissaolo',
            PLANNED_ABSENCE: 'Suunniteltu poissaolo',
            FORCE_MAJEURE: 'Hyvityspäivä',
            FREE_ABSENCE: 'Kesäajan maksuttoman jakson poissaolo'
          }
        }
      },
      common: {
        careTypeLabels: {
          'connected-daycare': 'Täydentävä'
        },
        types: {
          PRESCHOOL_DAYCARE: 'Täydentävä varhaiskasvatus',
          PREPARATORY_DAYCARE: 'Täydentävä varhaiskasvatus'
        }
      },
      childInformation: {
        assistance: {
          assistanceFactor: {
            info: () => (
              <ol style={{ margin: '0', padding: '0 1em' }}>
                <li>
                  Kaupungin päiväkodeissa kerroin merkitään aina integroidussa
                  varhaiskasvatusryhmässä oleville tehostettua tai erityistä
                  tukea saaville lapsille ja missä tahansa ryhmässä kotoutumisen
                  tukea saaville lapsille. Lisäksi kerroin voidaan merkitä missä
                  tahansa ryhmässä olevalle tehostettua tai erityistä tukea
                  saavalle lapselle, mikäli ryhmässä ei ole avustajapalvelua.
                  Kertoimen tallentaa varhaiskasvatuksen erityisopettaja.
                </li>
                <li>
                  Mikäli ostopalvelu- tai palvelusetelipäiväkodissa olevalla
                  lapsella on tehostetun tai erityisen tuen tarve, voidaan
                  hänelle määritellä tuen kerroin. Hallintopäätöksen lapsen
                  tuesta tekee palvelupäällikkö. Päätöksen palvelusetelin
                  korotuksesta tekee varhaiskasvatusjohtaja. Molemmat päätökset
                  tehdään varhaiskasvatuksen erityisopettajan esityksen
                  perusteella. Kertoimen tallentaa varhaiskasvatuksen
                  erityisopettaja.
                </li>
              </ol>
            )
          },
          otherAssistanceMeasure: {
            info: {
              TRANSPORT_BENEFIT: () => (
                <>Lapsi on saanut päätöksen kuljetusedusta.</>
              ),
              ACCULTURATION_SUPPORT: () => (
                <>
                  Lapsen ja perheen kotoutumisen tuki voidaan myöntää, kun
                  perheen lapsi tulee ensimmäistä kertaa suomalaiseen
                  päiväkotiin. Jos perheen muita lapsia on tällä hetkellä tai on
                  ollut aiemmin suomalaisessa päiväkodissa, kotoutumisen tukea
                  ei enää myönnetä. Pakolaistaustaisen perheen ollessa kyseessä
                  aika on 6 kk, muiden osalta 3kk. Kotoutumisen tuki alkaa
                  sijoituksen aloituspäivämäärästä.
                </>
              )
            }
          }
        },
        assistanceNeed: {
          fields: {
            capacityFactorInfo: (
              <ol style={{ margin: '0', padding: '0 1em' }}>
                <li>
                  Kaupungin päiväkodeissa kerroin merkitään aina integroidussa
                  varhaiskasvatusryhmässä oleville tehostettua tai erityistä
                  tukea saaville lapsille ja missä tahansa ryhmässä kotoutumisen
                  tukea saaville lapsille. Lisäksi kerroin voidaan merkitä missä
                  tahansa ryhmässä olevalle tehostettua tai erityistä tukea
                  saavalle lapselle, mikäli näin on yksikössä sovittu. Kertoimen
                  tallentaa varhaiskasvatuksen erityisopettaja.
                </li>
                <li>
                  Mikäli ostopalvelu- tai palvelusetelipäiväkodissa olevalla
                  lapsella on tehostetun tai erityisen tuen tarve, voidaan
                  hänelle määritellä tuen kerroin. Hallintopäätöksen lapsen
                  tuesta tekee palvelupäällikkö. Päätöksen palvelusetelin
                  korotuksesta tekee varhaiskasvatusjohtaja. Molemmat päätökset
                  tehdään varhaiskasvatuksen erityisopettajan esityksen
                  perusteella. Kertoimen tallentaa varhaiskasvatuksen
                  erityisopettaja.
                </li>
              </ol>
            ),
            bases: 'Tuen tarve'
          }
        },
        assistanceAction: {
          title: 'Tukitoimet ja tukipalvelut',
          fields: {
            actions: 'Tukitoimet ja tukipalvelut'
          }
        },
        dailyServiceTimes: {
          info: 'Tallenna tähän varhaiskasvatussopimuksella sovittu päivittäinen läsnäoloaika.',
          info2: ''
        },
        application: {
          types: {
            PRESCHOOL_DAYCARE: 'Täydentävä varhaiskasvatus'
          }
        }
      },
      personProfile: {
        application: {
          types: {
            PRESCHOOL_WITH_DAYCARE: 'Esiopetus + täydentävä',
            PRESCHOOL_DAYCARE: 'Täydentävä varhaiskasvatus',
            PREPARATORY_EDUCATION: 'Valmistava opetus',
            PREPARATORY_WITH_DAYCARE: 'Valmistava opetus + täydentävä'
          }
        }
      },
      placementDraft: {
        preschoolDaycare: 'Täydentävä toiminta'
      },
      footer: {
        cityLabel: 'Oriveden kaupunki',
        linkLabel: 'Oriveden varhaiskasvatus',
        linkHref: 'https://www.orivesi.fi/kasvatus-ja-opetus/varhaiskasvatus/'
      },
      unit: {
        placementProposals: {
          rejectReasons: {
            REASON_1: 'Päiväkoti täynnä',
            REASON_2: 'Sisäilma tai muu rakenteellinen syy',
            REASON_3: 'Henkilökuntaa tilapäisesti vähennetty'
          },
          infoTitle: '',
          infoText: ''
        }
      },
      login: {
        loginAD: 'Orivesi AD'
      },
      placement: {
        type: {
          DAYCARE: 'Kokopäiväinen varhaiskasvatus',
          DAYCARE_PART_TIME: 'Osapäiväinen varhaiskasvatus',
          TEMPORARY_DAYCARE: 'Tilapäinen kokopäiväinen varhaiskasvatus',
          PRESCHOOL_DAYCARE:
            'Esiopetus ja esiopetusta täydentävä varhaiskasvatus',
          PRESCHOOL_DAYCARE_ONLY: 'Esiopetusta täydentävä varhaiskasvatus',
          PRESCHOOL_CLUB: 'Esiopetus ja esiopetuksen kerhotoiminta',
          CLUB: 'Kerho',
          SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito',
          PRESCHOOL_WITH_DAYCARE:
            'Esiopetus ja esiopetusta täydentävä varhaiskasvatus',
          PREPARATORY_WITH_DAYCARE:
            'Valmistava opetus ja täydentävä varhaiskasvatus',
          PREPARATORY_DAYCARE:
            'Valmistava opetus ja täydentävä varhaiskasvatus',
          PREPARATORY_DAYCARE_ONLY:
            'Valmistavaa opetusta täydentävä varhaiskasvatus'
        }
      },
      decisionDraft: {
        types: {
          PRESCHOOL_DAYCARE: 'Esiopetusta täydentävä varhaiskasvatus',
          PREPARATORY_DAYCARE: 'Valmistavaa opetusta täydentävä varhaiskasvatus'
        }
      },
      unitEditor: {
        label: {
          costCenter: 'Kohde'
        },
        placeholder: {
          phone: 'esim. +358 40 555 5555',
          email: 'etunimi.sukunimi@orivesi.fi',
          url: 'esim. https://www.orivesi.fi/yhteystiedot/peiponpellon-paivakoti/',
          streetAddress: 'Kadun nimi esim. Rautialantie 60',
          decisionCustomization: {
            name: 'esim. Peiponpellon päiväkoti'
          }
        },
        field: {
          decisionCustomization: {
            handler: ['Palveluohjaus', 'Varhaiskasvatusyksikön johtaja']
          }
        },
        error: {
          costCenter: 'Kohde puuttuu'
        }
      },
      welcomePage: {
        text: 'Olet kirjautunut sisään eVaka Orivesi -palveluun. Käyttäjätunnuksellesi ei ole vielä annettu oikeuksia, jotka mahdollistavat palvelun käytön. Tarvittavat käyttöoikeudet saat omalta esihenkilöltäsi.'
      },
      invoices: {
        buttons: {
          individualSendAlertText: ''
        }
      },
      preferredFirstName: {
        description:
          'Voit määritellä eVakassa käytössä olevan kutsumanimesi. Kutsumanimen tulee olla jokin etunimistäsi.'
      },
      reports: {
        decisions: {
          connectedDaycareOnly: 'Myöhemmin haetun täydentävän päätöksiä',
          preschoolDaycare: 'Esiopetus+täydentävä päätöksiä',
          preparatoryDaycare: 'Valmistava+täydentävä päätöksiä'
        },
        placementSketching: {
          connected: 'Täydentävä'
        },
        units: {
          costCenter: 'Kohde'
        }
      },
      roles: {
        adRoles: {
          DIRECTOR: 'Vaka-päälliköt'
        }
      },
      terms: {
        extendedTermStartInfo:
          'Aika, jolloin varhaiskasvatusmaksu määräytyy täydentävän varhaiskasvatuksen mukaan.'
      },
      placementTool: {
        title: 'Esiopetussijoitusten tuonti',
        description:
          'Voit luoda tuotetuista sijoitusehdotuksista hakemukset eVakaan. Hakemukset luodaan suoraan odottamaan päätöstä.'
      }
    },
    sv: {}
  },
  cityLogo: {
    src: OrivesiLogo,
    alt: 'Orivesi logo'
  },
  featureFlags,
  absenceTypes: [
    'OTHER_ABSENCE',
    'SICKLEAVE',
    'UNKNOWN_ABSENCE',
    'PLANNED_ABSENCE',
    'PARENTLEAVE',
    'FORCE_MAJEURE',
    'FREE_ABSENCE',
    'UNAUTHORIZED_ABSENCE'
  ],
  daycareAssistanceLevels: daycareAssistanceLevels.filter(
    (level) => level !== 'GENERAL_SUPPORT'
  ),
  otherAssistanceMeasureTypes: otherAssistanceMeasureTypes.filter(
    (level) => level !== 'ANOMALOUS_EDUCATION_START'
  ),
  placementTypes: [
    'DAYCARE',
    'TEMPORARY_DAYCARE',
    'PRESCHOOL',
    'PRESCHOOL_DAYCARE',
    'PRESCHOOL_DAYCARE_ONLY',
    'PREPARATORY',
    'PREPARATORY_DAYCARE',
    'PREPARATORY_DAYCARE_ONLY'
  ],
  placementPlanRejectReasons: ['REASON_1', 'REASON_2', 'REASON_3', 'OTHER'],
  preschoolAssistanceLevels: preschoolAssistanceLevels.filter(
    (level) => level !== 'GROUP_SUPPORT'
  ),
  unitProviderTypes: [
    'MUNICIPAL',
    'PURCHASED',
    'PRIVATE',
    'PRIVATE_SERVICE_VOUCHER'
  ],
  voucherValueDecisionTypes: ['NORMAL', 'RELIEF_ACCEPTED', 'RELIEF_REJECTED'],
  additionalStaffAttendanceTypes:
    sharedCustomizations.additionalStaffAttendanceTypes
}

export default customizations
