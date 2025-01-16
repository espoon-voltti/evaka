// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled, { useTheme } from 'styled-components'

import { tabletMin } from 'lib-components/breakpoints'
import { Light } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { fasExclamationTriangle } from 'lib-icons'

export const Grid = styled.div`
  display: grid;
  grid-template-columns: auto 1fr;
  row-gap: ${defaultMargins.s};
  column-gap: ${defaultMargins.L};

  @media (max-width: ${tabletMin}) {
    grid-template-columns: auto;
    row-gap: ${defaultMargins.xxs};

    > *:nth-child(2n) {
      margin-bottom: ${defaultMargins.s};
    }
  }
`

export const FullRow = styled.div`
  grid-column: 1 / span 2;
  @media (max-width: ${tabletMin}) {
    grid-column: unset;
  }
`

export const EditButtonRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  margin-top: -20px;
`

export const MandatoryValueMissingWarning = styled(
  React.memo(function MandatoryValueMissingWarning({
    text,
    className
  }: {
    text: string
    className?: string
  }) {
    const { colors } = useTheme()
    return (
      <Light className={className}>
        {text}
        <FontAwesomeIcon
          icon={fasExclamationTriangle}
          color={colors.status.warning}
        />
      </Light>
    )
  })
)`
  svg {
    margin-left: ${defaultMargins.xs};
  }
`
