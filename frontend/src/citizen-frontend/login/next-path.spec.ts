// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from 'vitest'

import { validatedNextPath } from './next-path'

describe('validatedNextPath', () => {
  const origin = 'https://example.com'
  const base = { origin }
  it('returns the front page if the input is missing', () => {
    expect(validatedNextPath(null, base)).toEqual('/')
  })
  it('keeps the path, query and hash of a relative path', () => {
    expect(validatedNextPath('/test?query=qvalue#hash', base)).toEqual(
      '/test?query=qvalue#hash'
    )
  })
  it('removes the origin if it is correct', () => {
    expect(validatedNextPath(`${origin}/valid`, base)).toEqual('/valid')
  })
  it('returns the front page if the origin is wrong', () => {
    expect(validatedNextPath('https://other.example.com/x', base)).toEqual('/')
  })
  it('returns the front page if the path would become a protocol-relative URL', () => {
    expect(validatedNextPath('/..//evil.example.com', base)).toEqual('/')
    expect(validatedNextPath('/.//evil.example.com', base)).toEqual('/')
    expect(validatedNextPath(`${origin}//evil.example.com`, base)).toEqual('/')
  })
})
