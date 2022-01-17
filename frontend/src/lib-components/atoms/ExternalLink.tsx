// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import { faExternalLink } from 'lib-icons'

type ExternalLinkProps = {
  text: string | JSX.Element
  href: string
  newTab?: boolean
}

export default React.memo(function ExternalLink({
  text,
  href,
  newTab
}: ExternalLinkProps) {
  const targetProps = newTab
    ? { target: '_blank', rel: 'noreferrer' }
    : undefined
  return (
    <a href={href} {...targetProps}>
      {text} <FontAwesomeIcon icon={faExternalLink} />
    </a>
  )
})
