// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { uri } from './uri'

describe('uri template', () => {
  it('accepts an empty template', () => {
    expect(uri``.value).toBe('')
  })
  it('accepts a template with no parameters', () => {
    expect(uri`/no-params`.value).toBe('/no-params')
  })
  it('accepts a template that starts with a parameter', () => {
    expect(uri`${1}/bar`.value).toBe('1/bar')
  })
  it('accepts a template that ends with a parameter', () => {
    expect(uri`foo/${2}`.value).toBe('foo/2')
  })
  it('accepts a template that has a parameter in the middle', () => {
    expect(uri`foo/${42}/bar`.value).toBe('foo/42/bar')
  })
  it('escapes parameters', () => {
    expect(uri`foo/${'/'}/${'ö'}/${' '}`.value).toBe('foo/%2F/%C3%B6/%20')
  })
  describe('appendQuery', () => {
    it('doesnt add a question mark if the params are empty', () => {
      const params = new URLSearchParams()
      expect(uri`/something`.appendQuery(params).value).toBe('/something')
    })
    it('adds and escapes query parameters correctly', () => {
      const params = new URLSearchParams()
      params.set('first', 'value')
      params.set('second', 'with space')
      params.set('third', '?!"=&')
      expect(uri`/something`.appendQuery(params).value).toBe(
        '/something?first=value&second=with+space&third=%3F%21%22%3D%26'
      )
    })
    it('adds multiple values of the same parameter correctly', () => {
      const params = new URLSearchParams()
      params.append('multi', 'value1')
      params.append('multi', 'value2')
      params.append('multi', 'value3')
      expect(uri`/something`.appendQuery(params).value).toBe(
        '/something?multi=value1&multi=value2&multi=value3'
      )
    })
  })
})
