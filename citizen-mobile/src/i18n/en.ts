// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

const en = {
  common: {
    appName: 'eVaka',
    cancel: 'Cancel',
    confirm: 'Confirm',
    retry: 'Retry',
    loading: 'Loading',
    error: 'An error occurred',
    networkError: 'Check your connection',
    save: 'Save',
    send: 'Send',
    close: 'Close',
    back: 'Back'
  },
  login: {
    title: 'Sign in',
    username: 'Username',
    password: 'Password',
    submit: 'Sign in',
    invalidCredentials: 'Invalid username or password',
    rateLimited: 'Too many attempts. Try again later.'
  },
  inbox: {
    title: 'Messages',
    empty: 'No messages',
    refreshing: 'Refreshing...',
    unreadCount: { one: '%{count} unread', other: '%{count} unread' }
  },
  thread: {
    notFound: 'This conversation is no longer available',
    replyPlaceholder: 'Type a reply...',
    replySend: 'Send',
    replyError: 'Could not send reply',
    replyForbidden: 'You can no longer reply to this conversation'
  },
  settings: {
    title: 'Settings',
    language: 'Language',
    languageFi: 'Finnish',
    languageSv: 'Swedish',
    languageEn: 'English',
    logout: 'Sign out'
  },
  push: {
    permissionDeniedTitle: 'Push notifications disabled',
    permissionDeniedBody:
      'The app works without notifications, but you will not be alerted about new messages.'
  }
}

export default en
