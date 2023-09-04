// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { toASCII } from 'punycode'

import LinkifyIt from 'linkify-it'
import React, { useMemo } from 'react'

import ExternalLink from './ExternalLink'

export interface Props {
  text: string
}

export default React.memo(function Linkify({ text }: Props) {
  const children = useMemo(
    () =>
      parseLinks(text).map((part, i): React.ReactNode => {
        switch (part.type) {
          case 'text':
            return part.text
          case 'link':
            return (
              <ExternalLink key={i} newTab href={part.href} text={part.href} />
            )
        }
      }),
    [text]
  )
  return <>{children}</>
})

type Part = { type: 'text'; text: string } | { type: 'link'; href: string }

export function parseLinks(text: string): Part[] {
  if (!linkify.test(text)) return [{ type: 'text', text }]

  const matches = linkify.match(text)
  if (matches === null) return [{ type: 'text', text }]

  const parts: Part[] = []
  let lastIndex = 0
  for (const match of matches) {
    const { index, lastIndex: nextIndex, raw: href } = match
    if (index !== lastIndex) {
      parts.push({ type: 'text', text: text.substring(lastIndex, index) })
    }

    const parsedLink = new URL(href)
    // prevents IDN homograph attacks; the URL API already
    // converts the hostname to punycode, but not on all browsers
    parsedLink.host = toASCII(parsedLink.host)
    const link = parsedLink.toString()

    parts.push({ type: 'link', href: link })
    lastIndex = nextIndex
  }

  if (lastIndex !== text.length) {
    parts.push({ type: 'text', text: text.substring(lastIndex) })
  }

  return parts
}

const linkify = new LinkifyIt({
  fuzzyLink: false
})
  .add('ftp:', null)
  .add('mailto:', null)
  .add('//', null)
