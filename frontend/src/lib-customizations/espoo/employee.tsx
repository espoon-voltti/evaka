// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import {
  daycareAssistanceLevels,
  preschoolAssistanceLevels
} from 'lib-common/generated/api-types/assistance'
import type { EmployeeCustomizations } from 'lib-customizations/types'

import { employeeConfig } from './appConfigs'
import Logo from './assets/EspooLogoPrimary.svg'
import featureFlags from './featureFlags'

const customizations: EmployeeCustomizations = {
  appConfig: employeeConfig,
  translations: {
    fi: {
      common: {
        retroactiveConfirmation: {
          checkboxLabel:
            'Ymmärrän, olen asiasta yhteydessä laskutustiimiin vaka.maksut@espoo.fi *'
        }
      },
      childInformation: {
        pedagogicalDocument: {
          explanation:
            'Pedagogisen dokumentoinnin ominaisuutta käytetään toiminnasta kertovien kuvien ja digitaalisten dokumenttien jakamiseen.',
          explanationInfo:
            'Pedagogisen dokumentoinnin prosessi tapahtuu pääsääntöisesti ennen dokumentin jakamista huoltajille. Tarkastele dokumentteja yhdessä lasten ja kasvattajatiimin kanssa. Nosta yhteisistä havainnoista kehittämiskohteita käytännön pedagogiselle työlle, sen tavoitteille ja sisällöille, jotta pedagoginen toiminta vastaa mahdollisimman hyvin yksittäisen lapsen ja lapsiryhmän tarpeita, vahvuuksia ja mielenkiinnon kohteita. Erityistä huomiota kiinnitetään aikuisen toimintaan. Pedagoginen dokumentointi luo pohjan lapsilähtöisen pedagogiikan toteuttamiselle.',
          documentInfo:
            'Liitetiedoston tallennusmuoto voi olla JPG, PDF, MP3/4',
          descriptionInfo:
            'Kuvaillaan huoltajalle, mikä tilanteessa oli lapselle merkityksellistä oppimisen ja kehityksen näkökulmasta. Voit myös linkittää toiminnan vasun sisältöihin'
        }
      }
    },
    sv: {}
  },
  vasuTranslations: {
    FI: {},
    SV: {}
  },
  cityLogo: <img src={Logo} alt="Espoo Logo" data-qa="footer-city-logo" />,
  featureFlags,
  placementTypes: [
    'PRESCHOOL',
    'PRESCHOOL_DAYCARE',
    'DAYCARE',
    'DAYCARE_PART_TIME',
    'PREPARATORY',
    'PREPARATORY_DAYCARE',
    'CLUB',
    'TEMPORARY_DAYCARE',
    'TEMPORARY_DAYCARE_PART_DAY'
  ],
  absenceTypes: [
    'OTHER_ABSENCE',
    'SICKLEAVE',
    'UNKNOWN_ABSENCE',
    'PLANNED_ABSENCE',
    'PARENTLEAVE',
    'FORCE_MAJEURE'
  ],
  daycareAssistanceLevels: [...daycareAssistanceLevels],
  otherAssistanceMeasureTypes: [
    'TRANSPORT_BENEFIT',
    'ACCULTURATION_SUPPORT',
    'ANOMALOUS_EDUCATION_START'
  ],
  placementPlanRejectReasons: ['REASON_1', 'REASON_2', 'OTHER'],
  preschoolAssistanceLevels: [...preschoolAssistanceLevels],
  unitProviderTypes: [
    'MUNICIPAL',
    'PURCHASED',
    'PRIVATE',
    'MUNICIPAL_SCHOOL',
    'PRIVATE_SERVICE_VOUCHER',
    'EXTERNAL_PURCHASED'
  ],
  voucherValueDecisionTypes: [
    'NORMAL',
    'RELIEF_ACCEPTED',
    'RELIEF_PARTLY_ACCEPTED',
    'RELIEF_REJECTED'
  ]
}

export default customizations
