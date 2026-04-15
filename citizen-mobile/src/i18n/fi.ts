// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const fi = {
  common: {
    appName: 'eVaka',
    cancel: 'Peruuta',
    confirm: 'Vahvista',
    retry: 'Yritä uudelleen',
    loading: 'Ladataan',
    error: 'Tapahtui virhe',
    networkError: 'Tarkista yhteys',
    save: 'Tallenna',
    send: 'Lähetä',
    close: 'Sulje',
    back: 'Takaisin'
  },
  login: {
    title: 'Kirjaudu sisään',
    username: 'Käyttäjätunnus',
    password: 'Salasana',
    submit: 'Kirjaudu',
    invalidCredentials: 'Virheellinen käyttäjätunnus tai salasana',
    rateLimited: 'Liian monta yritystä. Yritä myöhemmin uudelleen.'
  },
  inbox: {
    title: 'Viestit',
    empty: 'Ei viestejä',
    refreshing: 'Päivitetään...',
    unreadCount: { one: '%{count} lukematon', other: '%{count} lukematonta' }
  },
  thread: {
    notFound: 'Tätä keskustelua ei ole enää saatavilla',
    replyPlaceholder: 'Kirjoita vastaus...',
    replySend: 'Lähetä',
    replyError: 'Vastauksen lähetys epäonnistui',
    replyForbidden: 'Tähän keskusteluun ei voi enää vastata'
  },
  settings: {
    title: 'Asetukset',
    language: 'Kieli',
    languageFi: 'Suomi',
    languageSv: 'Ruotsi',
    languageEn: 'Englanti',
    logout: 'Kirjaudu ulos'
  },
  push: {
    permissionDeniedTitle: 'Push-ilmoitukset eivät käytössä',
    permissionDeniedBody:
      'Sovellus toimii ilman ilmoituksia, mutta et saa hälytystä uusista viesteistä.'
  }
}

export default fi
