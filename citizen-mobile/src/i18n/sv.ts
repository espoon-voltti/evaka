// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const sv = {
  common: {
    appName: 'eVaka',
    cancel: 'Avbryt',
    confirm: 'Bekräfta',
    retry: 'Försök igen',
    loading: 'Laddar',
    error: 'Ett fel uppstod',
    networkError: 'Kontrollera din anslutning',
    save: 'Spara',
    send: 'Skicka',
    close: 'Stäng',
    back: 'Tillbaka'
  },
  login: {
    title: 'Logga in',
    username: 'Användarnamn',
    password: 'Lösenord',
    submit: 'Logga in',
    invalidCredentials: 'Felaktigt användarnamn eller lösenord',
    rateLimited: 'För många försök. Försök igen senare.'
  },
  inbox: {
    title: 'Meddelanden',
    empty: 'Inga meddelanden',
    refreshing: 'Uppdaterar...',
    unreadCount: { one: '%{count} oläst', other: '%{count} olästa' }
  },
  thread: {
    notFound: 'Den här konversationen är inte längre tillgänglig',
    replyPlaceholder: 'Skriv ett svar...',
    replySend: 'Skicka',
    replyError: 'Det gick inte att skicka svaret',
    replyForbidden: 'Det går inte längre att svara på den här konversationen'
  },
  settings: {
    title: 'Inställningar',
    language: 'Språk',
    languageFi: 'Finska',
    languageSv: 'Svenska',
    languageEn: 'Engelska',
    logout: 'Logga ut'
  },
  push: {
    permissionDeniedTitle: 'Push-aviseringar inaktiverade',
    permissionDeniedBody:
      'Appen fungerar utan aviseringar, men du blir inte påmind om nya meddelanden.'
  }
}

export default sv
