// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, it } from '@jest/globals'

import { parseUrlWithOrigin } from '../parse-url-with-origin.js'

describe('parseUrlWithOrigin', () => {
  const origin = 'https://example.com'
  const base = { origin }
  it('returns a parsed URL if the input URL is empty', () => {
    const url = parseUrlWithOrigin(base, '')
    expect(url?.toString()).toEqual(`${origin}/`)
  })
  it('returns a parsed URL if the input URL is /', () => {
    const url = parseUrlWithOrigin(base, '/')
    expect(url?.toString()).toEqual(`${origin}/`)
  })
  it('returns a parsed URL if the input URL is a relative path', () => {
    const url = parseUrlWithOrigin(base, '/test')
    expect(url?.toString()).toEqual(`${origin}/test`)
  })
  it('returns a parsed URL if the input URL has the correct origin', () => {
    const url = parseUrlWithOrigin(base, `${origin}/valid`)
    expect(url?.toString()).toEqual(`${origin}/valid`)
  })
  it('retains the query and hash, if present', () => {
    const url = parseUrlWithOrigin(base, '/test?query=qvalue#hash')
    expect(url?.toString()).toEqual(`${origin}/test?query=qvalue#hash`)
    expect(url?.search).toEqual('?query=qvalue')
    expect(url?.hash).toEqual('#hash')
  })
  it('returns undefined if the input URL is not relative and has the wrong origin', () => {
    const url = parseUrlWithOrigin(base, 'https://other.example.com')
    expect(url).toBeUndefined()
  })
  it('returns undefined if the input URL has a protocol-relative URL (two slashes)', () => {
    const url = parseUrlWithOrigin(base, '//something')
    expect(url).toBeUndefined()
  })
  it('returns undefined if the input URL has an unusual protocol + value combination', () => {
    const url = parseUrlWithOrigin(base, 'x:https://example.com')
    expect(url).toBeUndefined()
  })
})
