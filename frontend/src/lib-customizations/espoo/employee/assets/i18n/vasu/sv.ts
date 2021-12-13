// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { fi } from './fi'

export const sv: typeof fi = {
  staticSections: {
    basics: {
      title: 'Basuppgifter',
      name: 'Barnets namn',
      dateOfBirth: 'Barnets födelsedatum',
      guardians: 'Vårdnadshavare eller annan laglig företrädare',
      placements: 'Enhet inom småbarnspedagogiken och grupp'
    },
    vasuDiscussion: {
      title: 'Samtal om barnets plan för småbarnspedagogik',
      title2: '',
      discussionDate:
        'Datum för samtalet om barnets plan för småbarnspedagogik',
      participants: 'Vårdnadshavare som deltog i samtalet',
      guardianViewsAndCollaboration:
        'Samarbete med vårdnadshavaren/-havarna och synpunkter på innehållet i barnets plan'
    },
    evaluationDiscussion: {
      title: 'Utvärdering av genomförandet',
      title2: '',
      discussionDate: 'Datum för utvärderingssamtalet',
      participants: 'Vårdnadshavare som deltog i utvärderingssamtalet',
      guardianViewsAndCollaboration:
        'Samarbete med vårdnadshavaren/-havarna och synpunkter på innehållet i barnets plan',
      evaluation: 'Utvärdering av hur målen och åtgärderna har genomförts'
    }
  },
  confidential: 'Sekretessbelagd',
  confidentialSectionOfLaw:
    '40 § 3 mom. i lagen om småbarnspedagogik (540/2018)',
  noRecord: 'Ingen märkning'
}
