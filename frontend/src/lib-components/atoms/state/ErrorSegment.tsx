// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled from 'styled-components'

import { faMeh } from 'lib-icons'

import { defaultMargins, Gap } from '../../white-space'

const StyledSegment = styled.div<{ $compact?: boolean }>`
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: ${(p) => p.theme.colors.grayscale.g4};
  color: ${(p) => p.theme.colors.grayscale.g35};

  div {
    display: flex;
    justify-content: center;
    align-items: center;
  }

  svg {
    font-size: 1.5em;
    margin-right: ${defaultMargins.s};
  }

  padding: ${(p) => (p.$compact ? defaultMargins.m : defaultMargins.XL)} 0;
`

interface ErrorSegmentProps {
  title?: string
  info?: string
  compact?: boolean
}

export default React.memo(function ErrorSegment({
  title = 'Tietojen hakeminen ei onnistunut',
  info,
  compact
}: ErrorSegmentProps) {
  return (
    <StyledSegment $compact={compact}>
      <div>
        <FontAwesomeIcon icon={faMeh} />
        <span>{title}</span>
      </div>
      {!!info && (
        <>
          <Gap size="s" />
          <p>{info}</p>
        </>
      )}
    </StyledSegment>
  )
})
