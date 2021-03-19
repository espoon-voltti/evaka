// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import classNames from 'classnames'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faMeh } from 'lib-icons'
import { greyscale } from '../../colors'
import { defaultMargins, Gap } from '../../white-space'

const StyledSegment = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: ${greyscale.lightest};
  color: ${greyscale.medium};

  div {
    display: flex;
    justify-content: center;
    align-items: center;
  }

  svg {
    font-size: 1.5em;
    margin-right: ${defaultMargins.s};
  }

  padding: ${defaultMargins.XL} 0;
  &.compact {
    padding: ${defaultMargins.m} 0;
  }
`

interface ErrorSegmentProps {
  title?: string
  info?: string
  compact?: boolean
}

function ErrorSegment({
  title = 'Tietojen hakeminen ei onnistunut',
  info,
  compact
}: ErrorSegmentProps) {
  return (
    <StyledSegment className={classNames({ compact })}>
      <div>
        <FontAwesomeIcon icon={faMeh} />
        <span>{title}</span>
      </div>
      {info && (
        <>
          <Gap size="s" />
          <p>{info}</p>
        </>
      )}
    </StyledSegment>
  )
}

export default ErrorSegment
