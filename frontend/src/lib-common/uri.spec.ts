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
})
