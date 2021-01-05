// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export default {
  header: {
    nav: {
      map: 'Kartta',
      applications: 'Hakemukset',
      decisions: 'Päätökset',
      newDecisions: 'Uusi Päätökset'
    },
    lang: {
      fi: 'Suomeksi',
      sv: 'På svenska',
      en: 'In English'
    },
    logout: 'Kirjaudu ulos'
  },
  decisions: {
    title: 'Päätökset',
    summary:
      'Tälle sivulle saapuvat lapsen varhaiskasvatus-, esiopetus- ja kerhohakemuksiin liittyvät päätökset. Uuden päätöksen saapuessa <strong>sinun tulee kahden viikon sisällä vastata</strong>, hyväksytkö vai hylkäätkö lapselle tarjotun paikan.',
    unconfimedDecisions: (n: number) =>
      `${n} ${n === 1 ? 'päätös' : 'päätöstä'} odottaa vahvistusta`,
    applicationDecisions: {
      decision: 'Päätös',
      type: {
        CLUB: 'kerhosta',
        DAYCARE: 'varhaiskasvatuksesta',
        DAYCARE_PART_TIME: 'osa-aikaisesta varhaiskasvatuksesta',
        PRESCHOOL: 'esiopetuksesta',
        PRESCHOOL_DAYCARE: 'liittyvästä varhaiskasvatuksesta',
        PREPARATORY_EDUCATION: 'valmistavasta opetuksesta'
      },
      sentDate: 'Päätös saapunut',
      resolved: 'Vahvistettu',
      statusLabel: 'Tila',
      status: {
        PENDING: 'Vahvistettavana huoltajalla',
        ACCEPTED: 'Hyväksytty',
        REJECTED: 'Hylätty'
      },
      openPdf: 'Näytä päätös'
    }
  }
}
