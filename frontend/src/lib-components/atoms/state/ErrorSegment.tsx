// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IconDefinition } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { faExclamationTriangle } from 'lib-icons'

import { fontWeights, H3, P } from '../../typography'
import { defaultMargins, Gap } from '../../white-space'

const Card = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;

  border: 1px solid ${(p) => p.theme.colors.grayscale.g15};
  border-radius: 4px;
  background: ${(p) => p.theme.colors.grayscale.g0};

  padding: ${defaultMargins.m} ${defaultMargins.L};
`

const IconContainer = styled.div<{ $color?: string }>`
  color: ${(p) => p.$color ?? p.theme.colors.grayscale.g70};
  font-size: 40px;
  line-height: 1;
`

const Title = styled(H3)`
  font-weight: ${fontWeights.semibold};
`

interface ErrorSegmentProps {
  title?: string
  info?: string
  icon?: IconDefinition
  iconColor?: string
}

export default React.memo(function ErrorSegment({
  title = 'Tietojen hakeminen ei onnistunut',
  info,
  icon = faExclamationTriangle,
  iconColor
}: ErrorSegmentProps) {
  return (
    <Card role="alert">
      <IconContainer $color={iconColor}>
        <FontAwesomeIcon icon={icon} />
      </IconContainer>
      <Gap $size="s" />
      <Title $noMargin>{title}</Title>
      {!!info && (
        <>
          <Gap $size="xs" />
          <P $noMargin>{info}</P>
        </>
      )}
    </Card>
  )
})
