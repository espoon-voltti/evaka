{
  /*
SPDX-FileCopyrightText: 2021 City of Turku

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import type { EmployeeMobileCustomizations } from 'lib-customizations/types'

import featureFlags from './featureFlags'
import { additionalStaffAttendanceTypes } from './shared'

const customizations: EmployeeMobileCustomizations = {
  appConfig: {},
  translations: {
    fi: {
      absences: {
        title: 'Poissaolomerkintä',
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
        }
      },
      mobile: {
        landerText1: 'Tervetuloa käyttämään eVaka Turku -mobiilisovellusta!'
      }
    },
    sv: {}
  },
  featureFlags,
  additionalStaffAttendanceTypes
}

export default customizations
