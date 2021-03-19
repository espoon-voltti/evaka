// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { fasInfo, faTimes } from 'lib-icons'
import React, { ReactNode, useState } from 'react'
import styled from 'styled-components'
import colors from '../colors'
import RoundIcon from '../atoms/RoundIcon'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { defaultMargins } from '../white-space'
import { tabletMin } from '../breakpoints'
import IconButton from '../atoms/buttons/IconButton'

const InfoBoxContainer = styled(Container)`
  @keyframes open {
    from {
      max-height: 0;
    }
    to {
      max-height: 100px;
    }
  }

  background-color: ${colors.blues.lighter};
  overflow: hidden;
  margin: ${defaultMargins.s} -${defaultMargins.L} ${defaultMargins.xs};

  @media (min-width: ${tabletMin}) {
    animation-name: open;
    animation-duration: 0.2s;
    animation-timing-function: ease-out;
  }
`

const InfoBoxContentArea = styled(ContentArea)`
  display: flex;
`

const InfoContainer = styled.div`
  flex-grow: 1;
  color: ${colors.blues.dark};
  padding: 0 ${defaultMargins.s};
`

const RoundIconWithMargin = styled(RoundIcon)`
  margin-top: ${defaultMargins.xs};
`

type ExpandingInfoProps = {
  children: React.ReactNode
  info: ReactNode
  ariaLabel: string
}

export default function ExpandingInfo({
  children,
  info,
  ariaLabel
}: ExpandingInfoProps) {
  const [expanded, setExpanded] = useState<boolean>(false)

  return (
    <span aria-live="polite">
      <FixedSpaceRow spacing="xs">
        <div>{children}</div>
        <RoundIconWithMargin
          content={fasInfo}
          color={colors.brandEspoo.espooTurquoise}
          size="s"
          onClick={() => setExpanded(!expanded)}
          tabindex={0}
          role="button"
          aria-label={ariaLabel}
        />
      </FixedSpaceRow>
      {expanded && (
        <InfoBoxContainer>
          <InfoBoxContentArea opaque={false}>
            <RoundIcon
              content={fasInfo}
              color={colors.brandEspoo.espooTurquoise}
              size="s"
            />

            <InfoContainer>{info}</InfoContainer>

            <IconButton
              onClick={() => setExpanded(false)}
              icon={faTimes}
              gray
            />
          </InfoBoxContentArea>
        </InfoBoxContainer>
      )}
    </span>
  )
}
