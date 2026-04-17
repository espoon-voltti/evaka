{
  /*
SPDX-FileCopyrightText: 2021 City of Tampere

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import type { EmployeeMobileCustomizations } from 'lib-customizations/types'

import featureFlags from './featureFlags'
import sharedCustomizations from './shared'

const customizations: EmployeeMobileCustomizations = {
  appConfig: {},
  featureFlags,
  translations: {
    fi: {
      mobile: {
        landerText1: 'Tervetuloa käyttämään eVaka Tampere -mobiilisovellusta!'
      },
      absences: {
        absenceTypes: {
          UNKNOWN_ABSENCE: 'Esiopetuksen ilmoittamaton poissaolo',
          PLANNED_ABSENCE: 'Sopimuksen mukainen poissaolo',
          FORCE_MAJEURE: 'Hyvityspäivä',
          FREE_ABSENCE: 'Kesäajan maksuttoman jakson poissaolo'
        },
        careTypes: {
          PRESCHOOL_DAYCARE: 'Täydentävä varhaiskasvatus'
        }
      },
      common: {
        types: {
          PRESCHOOL_DAYCARE: 'Täydentävä varhaiskasvatus',
          PREPARATORY_DAYCARE: 'Täydentävä varhaiskasvatus'
        },
        placement: {
          PRESCHOOL_DAYCARE: 'Täydentävä varhaiskasvatus',
          PRESCHOOL_DAYCARE_ONLY: 'Pelkkä täydentävä',
          PREPARATORY_DAYCARE_ONLY: 'Pelkkä täydentävä'
        }
      }
    },
    sv: {}
  },
  additionalStaffAttendanceTypes:
    sharedCustomizations.additionalStaffAttendanceTypes
}

export default customizations
