// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { parseLinks } from './Linkify'

describe('Linkify', () => {
  it('renders text without links as-is', () => {
    expect(parseLinks('foo bar baz')).toEqual([
      { type: 'text', text: 'foo bar baz' }
    ])
  })
  it('renders a sole link', () => {
    expect(parseLinks('https://google.com')).toEqual([
      { type: 'link', href: 'https://google.com/' }
    ])
  })
  it('renders a link in the beginning', () => {
    expect(parseLinks('https://google.com foo bar baz')).toEqual([
      { type: 'link', href: 'https://google.com/' },
      { type: 'text', text: ' foo bar baz' }
    ])
  })
  it('renders a link in the end', () => {
    expect(parseLinks('foo bar baz https://google.com')).toEqual([
      { type: 'text', text: 'foo bar baz ' },
      { type: 'link', href: 'https://google.com/' }
    ])
  })
  it('renders mixed text and links', () => {
    expect(
      parseLinks(
        'foo bar https://google.com baz https://example.com quux xyzzy'
      )
    ).toEqual([
      { type: 'text', text: 'foo bar ' },
      { type: 'link', href: 'https://google.com/' },
      { type: 'text', text: ' baz ' },
      { type: 'link', href: 'https://example.com/' },
      { type: 'text', text: ' quux xyzzy' }
    ])
  })
})
