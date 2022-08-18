// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { toASCII } from 'punycode'

import * as Sentry from '@sentry/browser'
import LinkifyIt from 'linkify-it'
import React from 'react'
import ReactLinkify from 'react-linkify'

import ExternalLink from './ExternalLink'

const linkify = new LinkifyIt({
  fuzzyLink: false
})
  .add('ftp:', null)
  .add('mailto:', null)
  .add('//', null)

export default React.memo(function Linkify({
  children
}: {
  children: React.ReactNode
}) {
  return (
    <ReactLinkify
      componentDecorator={(href, text, key) => {
        if (href !== text) {
          Sentry.captureMessage(
            'Linkified message should only match links where the href and text equal',
            'error'
          )
          return text
        }

        const parsedLink = new URL(href)
        // prevents IDN homograph attacks; the URL API already
        // converts the hostname to punycode, but not on all browsers
        parsedLink.host = toASCII(parsedLink.host)

        const link = parsedLink.toString()

        return <ExternalLink href={link} key={key} newTab text={link} />
      }}
      matchDecorator={(text) => linkify.match(text)}
    >
      {children}
    </ReactLinkify>
  )
})
