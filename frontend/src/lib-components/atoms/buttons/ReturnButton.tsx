// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useHistory } from 'react-router-dom'
import styled, { useTheme } from 'styled-components'
import { faAngleLeft } from 'lib-icons'

import { tabletMin } from '../../breakpoints'
import { defaultMargins } from '../../white-space'
import InlineButton from './InlineButton'

export const ReturnButtonWrapper = styled.div`
  margin: 8px 0;
  display: inline-block;

  button {
    padding-left: 0;
    margin-left: 0;
    justify-content: flex-start;
  }

  @media (max-width: ${tabletMin}) {
    margin-left: ${defaultMargins.m};
  }

  @media print {
    display: none;
  }
`

type Props = {
  label: string
  'data-qa'?: string
}

export default React.memo(function ReturnButton({
  label,
  'data-qa': dataQa
}: Props) {
  const { colors } = useTheme()
  const history = useHistory()
  return (
    <ReturnButtonWrapper>
      <InlineButton
        icon={faAngleLeft}
        text={label}
        onClick={() => history.goBack()}
        data-qa={dataQa}
        disabled={history.length <= 1}
        color={colors.main.m1}
      />
    </ReturnButtonWrapper>
  )
})
