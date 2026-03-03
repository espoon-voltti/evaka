// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from '@jest/globals'

import {
  matchPath,
  parseDisabledEndpoint,
  isEndpointDisabled
} from '../middleware/endpoint-disabling.ts'

describe('matchPath', () => {
  describe('exact matches', () => {
    it('matches identical paths', () => {
      expect(matchPath('/citizen/absences', '/citizen/absences')).toBe(true)
    })
    it('does not match different paths', () => {
      expect(matchPath('/citizen/absences', '/citizen/other')).toBe(false)
    })
    it('does not match when path has extra segments', () => {
      expect(matchPath('/citizen/absences', '/citizen/absences/123')).toBe(
        false
      )
    })
    it('does not match when pattern has extra segments', () => {
      expect(matchPath('/citizen/absences/123', '/citizen/absences')).toBe(
        false
      )
    })
    it('matches root path', () => {
      expect(matchPath('/', '/')).toBe(true)
    })
  })

  describe('single wildcard *', () => {
    it('matches one segment', () => {
      expect(matchPath('/citizen/absences/*', '/citizen/absences/123')).toBe(
        true
      )
    })
    it('does not match zero segments', () => {
      expect(matchPath('/citizen/absences/*', '/citizen/absences')).toBe(false)
    })
    it('does not match multiple segments', () => {
      expect(
        matchPath('/citizen/absences/*', '/citizen/absences/123/foo')
      ).toBe(false)
    })
    it('works in the middle of a pattern', () => {
      expect(
        matchPath('/citizen/*/decisions', '/citizen/abc/decisions')
      ).toBe(true)
    })
    it('does not match empty segment in the middle', () => {
      expect(matchPath('/citizen/*/decisions', '/citizen//decisions')).toBe(
        false
      )
    })
    it('works with multiple wildcards', () => {
      expect(
        matchPath('/citizen/*/absences/*', '/citizen/abc/absences/123')
      ).toBe(true)
    })
  })

  describe('double wildcard **', () => {
    it('matches zero segments at end', () => {
      expect(matchPath('/citizen/**', '/citizen')).toBe(true)
    })
    it('matches one segment at end', () => {
      expect(matchPath('/citizen/**', '/citizen/absences')).toBe(true)
    })
    it('matches many segments at end', () => {
      expect(matchPath('/citizen/**', '/citizen/absences/123/foo')).toBe(true)
    })
    it('works in the middle of a pattern', () => {
      expect(
        matchPath('/citizen/**/decisions', '/citizen/abc/def/decisions')
      ).toBe(true)
    })
    it('matches zero segments in the middle', () => {
      expect(matchPath('/citizen/**/decisions', '/citizen/decisions')).toBe(
        true
      )
    })
    it('works with * and ** combined', () => {
      expect(matchPath('/citizen/**/foo/*', '/citizen/a/b/foo/x')).toBe(true)
    })
    it('does not match when trailing segment is wrong', () => {
      expect(matchPath('/citizen/**/decisions', '/citizen/abc/other')).toBe(
        false
      )
    })
  })

  describe('edge cases', () => {
    it('empty pattern matches empty path', () => {
      expect(matchPath('', '')).toBe(true)
    })
    it('empty pattern does not match non-empty path', () => {
      expect(matchPath('', '/foo')).toBe(false)
    })
    it('** alone matches everything', () => {
      expect(matchPath('**', '/any/path/at/all')).toBe(true)
    })
    it('** alone matches empty path', () => {
      expect(matchPath('**', '')).toBe(true)
    })
  })
})

describe('parseDisabledEndpoint', () => {
  it('parses method and path', () => {
    expect(parseDisabledEndpoint('POST /citizen/absences/*')).toEqual({
      method: 'POST',
      pathPattern: '/citizen/absences/*'
    })
  })
  it('handles wildcard method', () => {
    expect(parseDisabledEndpoint('* /employee/**')).toEqual({
      method: '*',
      pathPattern: '/employee/**'
    })
  })
  it('returns null for invalid format (no space)', () => {
    expect(parseDisabledEndpoint('/citizen/absences')).toBeNull()
  })
  it('returns null for empty string', () => {
    expect(parseDisabledEndpoint('')).toBeNull()
  })
})

describe('isEndpointDisabled', () => {
  it('matches when method and path match', () => {
    const entries = ['POST /citizen/absences/*']
    expect(isEndpointDisabled(entries, 'POST', '/citizen/absences/123')).toBe(
      true
    )
  })
  it('does not match when method differs', () => {
    const entries = ['POST /citizen/absences/*']
    expect(isEndpointDisabled(entries, 'GET', '/citizen/absences/123')).toBe(
      false
    )
  })
  it('matches wildcard method', () => {
    const entries = ['* /citizen/absences/*']
    expect(isEndpointDisabled(entries, 'GET', '/citizen/absences/123')).toBe(
      true
    )
  })
  it('returns false for empty list', () => {
    expect(isEndpointDisabled([], 'GET', '/citizen/absences/123')).toBe(false)
  })
  it('skips invalid entries', () => {
    const entries = ['invalid', 'POST /citizen/absences/*']
    expect(isEndpointDisabled(entries, 'POST', '/citizen/absences/123')).toBe(
      true
    )
  })
})
