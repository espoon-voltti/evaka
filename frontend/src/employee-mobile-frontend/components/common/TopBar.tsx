// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faTimes } from 'lib-icons'
import React, { PropsWithChildren } from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { topBarHeight, zIndex } from '../constants'
import { LoggedInUser } from './LoggedInUser'

const StickyTopBar = styled.section`
  position: sticky;
  top: 0;
  height: ${topBarHeight};
  z-index: ${zIndex.topBar};

  display: flex;
  justify-content: space-between;
  align-items: center;

  background: ${colors.blues.primary};
  color: ${colors.greyscale.white};

  & > :last-child {
    margin-right: ${defaultMargins.xs};
  }
`

const Title = styled.div`
  flex-grow: 1;
  margin: 0 ${defaultMargins.m};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  ${Label} {
    color: ${colors.greyscale.white};
  }
`

interface Props {
  title: string
}

function TopBarWrapper({ children, title }: PropsWithChildren<Props>) {
  return (
    <StickyTopBar>
      <Title>
        <Label data-qa="top-bar-title">{title}</Label>
      </Title>
      {children}
    </StickyTopBar>
  )
}

export const TopBar = React.memo(function TopBar({ title }: Props) {
  return (
    <TopBarWrapper title={title}>
      <LoggedInUser />
    </TopBarWrapper>
  )
})

interface CloseableTopBarProps extends Props {
  onClose: () => void
}

export const CloseableTopBar = React.memo(function TopBar({
  onClose,
  title
}: CloseableTopBarProps) {
  const { i18n } = useTranslation()
  return (
    <TopBarWrapper title={title}>
      <IconButton
        icon={faTimes}
        white
        onClick={onClose}
        altText={i18n.common.close}
      />
    </TopBarWrapper>
  )
})
