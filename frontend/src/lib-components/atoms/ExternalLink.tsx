// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'
import { faExternalLink } from 'lib-icons'
import { fontWeights } from '../typography'
import { defaultMargins } from '../white-space'

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
  return (
    <StyledLink
      href={href}
      target={newTab ? '_blank' : undefined}
      rel={newTab ? 'noreferrer' : undefined}
    >
      {typeof text === 'string' ? <span>{text}</span> : text}
      <FontAwesomeIcon icon={faExternalLink} />
    </StyledLink>
  )
})

const StyledLink = styled.a`
  display: inline-block;
  font-weight: ${fontWeights.semibold};
  svg {
    margin-left: ${defaultMargins.xs};
  }
`
