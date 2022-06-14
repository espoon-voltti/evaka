// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { useTheme } from 'styled-components'

import { faArrowDownToLine } from 'lib-icons'

import { tabletMin } from '../../breakpoints'
import { defaultMargins } from '../../white-space'

import InlineButton from './InlineButton'

interface WrapperProps {
  margin?: string
}
export const DownloadButtonWrapper = styled.div<WrapperProps>`
  margin: ${(p) => p.margin ?? defaultMargins.xs} 0;

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
  onClick?: () => void
}

export default React.memo(function DownloadButton({
  label,
  'data-qa': dataQa,
  onClick,
  margin
}: Props & WrapperProps) {
  const { colors } = useTheme()
  const defaultBehaviour = () => window.print()
  return (
    <DownloadButtonWrapper margin={margin}>
      <InlineButton
        icon={faArrowDownToLine}
        text={label}
        onClick={onClick ?? defaultBehaviour}
        data-qa={dataQa}
        color={colors.main.m1}
      />
    </DownloadButtonWrapper>
  )
})
