// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { type fi } from './fi'

export const sv: typeof fi = {
  staticSections: {
    basics: {
      title: 'Basuppgifter',
      name: 'Barnets namn',
      dateOfBirth: 'Barnets födelsedatum',
      guardians: 'Vårdnadshavare eller annan laglig företrädare',
      placements: {
        DAYCARE: 'Enhet och grupp inom småbarnspedagogiken',
        PRESCHOOL: 'Enhet och grupp inom förskoleundervisningen'
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
  noRecord: 'Ingen markering',
  events: {
    DAYCARE: 'Händelser gällande barnets plan för småbarnspedagogik',
    PRESCHOOL:
      'Händelser gällande plan för barnets lärande inom förskoleundervisning'
  },
  eventTypes: {
    PUBLISHED: 'Publicerad till vårdnadshavare',
    MOVED_TO_READY: 'Publicerat i Behandlad-läge',
    RETURNED_TO_READY: 'Återställt från Behandlad-läge',
    MOVED_TO_REVIEWED: 'Publicerat i Granskad-läge',
    RETURNED_TO_REVIEWED: 'Återställt från Granskad-läge',
    MOVED_TO_CLOSED: 'Avslutad'
  },
  state: 'Planens läge',
  states: {
    DRAFT: 'Utkast',
    READY: 'Behandlad',
    REVIEWED: 'Granskad',
    CLOSED: 'Avslutad'
  },
  lastModified: 'Senaste redigeringsdatum',
  lastPublished: 'Senaste publicering för vårdnadshavare',
  guardianPermissionGiven: 'Informationsdelning godkänd av'
}
