// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import { buildLoginRedirectPath } from './loginRedirect'

describe('buildLoginRedirectPath', () => {
  it('builds a /login?next= URL for a plain path with no query', () => {
    expect(buildLoginRedirectPath('/messages/abc', '')).toBe(
      '/login?next=' + encodeURIComponent('/messages/abc')
    )
  })

  it('preserves non-fromNotification query params in next', () => {
    expect(
      buildLoginRedirectPath('/messages/abc', 'focus=reply&scrollTo=latest')
    ).toBe(
      '/login?next=' +
        encodeURIComponent('/messages/abc?focus=reply&scrollTo=latest')
    )
  })

  it('strips fromNotification=1 from the preserved next URL', () => {
    expect(
      buildLoginRedirectPath(
        '/messages/abc',
        'fromNotification=1&scrollTo=latest'
      )
    ).toBe(
      '/login?next=' +
        encodeURIComponent('/messages/abc?scrollTo=latest') +
        '&reason=session-expired-open-thread'
    )
  })

  it('appends reason=session-expired-open-thread when fromNotification=1 is present even with no other params', () => {
    expect(buildLoginRedirectPath('/messages/abc', 'fromNotification=1')).toBe(
      '/login?next=' +
        encodeURIComponent('/messages/abc') +
        '&reason=session-expired-open-thread'
    )
  })

  it('does not append reason when fromNotification is absent', () => {
    expect(buildLoginRedirectPath('/messages/abc', 'focus=reply')).toBe(
      '/login?next=' + encodeURIComponent('/messages/abc?focus=reply')
    )
  })

  it('ignores fromNotification values other than "1"', () => {
    expect(buildLoginRedirectPath('/messages/abc', 'fromNotification=0')).toBe(
      '/login?next=' + encodeURIComponent('/messages/abc?fromNotification=0')
    )
  })

  it('handles an empty search string without producing a trailing ?', () => {
    expect(buildLoginRedirectPath('/', '')).toBe(
      '/login?next=' + encodeURIComponent('/')
    )
  })
})
