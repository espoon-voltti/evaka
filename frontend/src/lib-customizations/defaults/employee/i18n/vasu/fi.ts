// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const fi = {
  staticSections: {
    basics: {
      title: 'Perustiedot',
      name: 'Lapsen nimi',
      dateOfBirth: 'Lapsen syntymäaika',
      guardians: 'Huoltaja(t) tai muu laillinen edustaja',
      placements: {
        DAYCARE: 'Varhaiskasvatusyksikkö ja ryhmä',
        PRESCHOOL: 'Esiopetusyksikkö ja ryhmä'
      },
      childLanguage: {
        label: 'Lapsen puhumat kielet',
        info: 'Täytä nämä vain, jos lapsen kotona puhutaan muita kieliä kuin suomea.',
        nativeLanguage: 'Lapsen äidinkieli',
        languageSpokenAtHome: 'Kotona puhuttavat kielet'
      }
    }
  },
  confidential: 'Salassapidettävä',
  law: {
    DAYCARE: 'Varhaiskasvatuslaki (540/2018) 40§:n 3 mom.',
    PRESCHOOL: 'JulkL 24.1 §:n kohdat 25 ja 30'
  },
  noRecord: 'Ei merkintää',
  events: {
    DAYCARE: 'Varhaiskasvatussuunnitelman tapahtumat',
    PRESCHOOL: 'Lapsen esiopetuksen oppimissuunnitelman tapahtumat'
  },
  eventTypes: {
    PUBLISHED: 'Julkaistu huoltajalle',
    MOVED_TO_READY: 'Julkaistu Laadittu-tilaan',
    RETURNED_TO_READY: 'Palautettu Laadittu-tilaan',
    MOVED_TO_REVIEWED: 'Julkaistu Arvioitu-tilaan',
    RETURNED_TO_REVIEWED: 'Palautettu Arvioitu-tilaan',
    MOVED_TO_CLOSED: 'Päättynyt'
  },
  state: 'Suunnitelman tila',
  states: {
    DRAFT: 'Luonnos',
    READY: 'Laadittu',
    REVIEWED: 'Arvioitu',
    CLOSED: 'Päättynyt'
  },
  lastModified: 'Viimeisin muokkauspäivämäärä',
  lastPublished: 'Viimeksi julkaistu huoltajalle',
  guardianPermissionGiven: 'Tiedonsaajatahot hyväksynyt'
}
