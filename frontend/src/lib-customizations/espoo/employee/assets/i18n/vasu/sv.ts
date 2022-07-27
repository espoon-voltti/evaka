// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { fi } from './fi'

export const sv: typeof fi = {
  staticSections: {
    basics: {
      title: 'Basuppgifter',
      name: 'Barnets namn',
      dateOfBirth: 'Barnets födelsedatum',
      guardians: 'Vårdnadshavare eller annan laglig företrädare',
      placements: {
        DAYCARE: 'Enhet inom småbarnspedagogiken och grupp',
        PRESCHOOL: 'Enhet inom förskoleundervisningen och grupp'
      },
      // TODO: Leops in Swedish
      childLanguage: {
        label: '',
        info: '',
        nativeLanguage: '',
        languageSpokenAtHome: ''
      }
    }
  },
  confidential: 'Sekretessbelagd',
  law: {
    DAYCARE: '40 § 3 mom. i lagen om småbarnspedagogik (540/2018)',
    PRESCHOOL: 'OffentlighetsL 24.1 §§ punkt 25 och 30'
  },
  noRecord: 'Ingen märkning'
}
