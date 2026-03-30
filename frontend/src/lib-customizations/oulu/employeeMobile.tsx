{
  /*
SPDX-FileCopyrightText: 2021 City of Oulu

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
      mobile: {
        landerText1: 'Tervetuloa käyttämään eVaka Oulu -mobiilisovellusta!'
      },
      absences: {
        title: 'Poissaolomerkintä',
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
        careTypes: {
          SCHOOL_SHIFT_CARE: 'Koululaisten vuorohoito',
          PRESCHOOL: 'Esiopetus',
          PRESCHOOL_DAYCARE: 'Liittyvä varhaiskasvatus',
          DAYCARE_5YO_FREE: '5-vuotiaiden varhaiskasvatus',
          DAYCARE: 'Varhaiskasvatus',
          CLUB: 'Kerho'
        },
        chooseStartDate: 'Valitse tuleva päivä',
        startBeforeEnd: 'Aloitus oltava ennen päättymispäivää.',
        reason: 'Poissaolon syy',
        fullDayHint: 'Poissaolomerkintä tehdään koko päivälle',
        confirmDelete: 'Haluatko poistaa tämän poissaolon?',
        futureAbsence: 'Tulevat poissaolot'
      }
    },
    sv: {}
  },
  featureFlags,
  additionalStaffAttendanceTypes
}

export default customizations
