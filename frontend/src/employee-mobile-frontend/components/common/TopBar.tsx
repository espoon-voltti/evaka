// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faArrowLeft, faTimes } from 'lib-icons'
import React from 'react'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { topBarHeight, zIndex } from '../constants'
import { LoggedInUser } from './top-bar/LoggedInUser'
import { TopBarIconContainer } from './top-bar/TopBarIconContainer'

const StickyTopBar = styled.section<{ invertedColors?: boolean }>`
  position: sticky;
  top: 0;
  width: 100%;
  height: ${topBarHeight};
  padding: 0 ${defaultMargins.s};
  z-index: ${zIndex.topBar};

  display: flex;
  justify-content: space-between;
  align-items: center;

  background: ${({ theme, invertedColors }) =>
    invertedColors
      ? theme.colors.greyscale.lightest
      : theme.colors.main.primary};
  color: ${({ theme, invertedColors }) =>
    invertedColors ? theme.colors.main.dark : theme.colors.greyscale.white};

  button {
    color: ${({ theme, invertedColors }) =>
      invertedColors ? theme.colors.main.dark : theme.colors.greyscale.white};
  }
`

const Title = styled.div`
  flex-grow: 6; // occupy majority of the space, leaving rest for the user menu
  margin: 0 ${defaultMargins.s};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`

interface Props {
  title: string
  onBack?: () => void
  onClose?: () => void
  invertedColors?: boolean
}

export default React.memo(function TopBar({
  title,
  onBack,
  onClose,
  invertedColors
}: Props) {
  const { i18n } = useTranslation()

  return (
    <StickyTopBar invertedColors={invertedColors}>
      {onBack && (
        <TopBarIconContainer>
          <IconButton
            icon={faArrowLeft}
            onClick={onBack}
            altText={i18n.common.back}
            data-qa="go-back"
          />
        </TopBarIconContainer>
      )}
      <Title>
        <Label data-qa="top-bar-title" white={!invertedColors}>
          {title}
        </Label>
      </Title>
      {onClose ? (
        <TopBarIconContainer>
          <IconButton
            icon={faTimes}
            white
            onClick={onClose}
            altText={i18n.common.close}
          />
        </TopBarIconContainer>
      ) : (
        <LoggedInUser />
      )}
    </StickyTopBar>
  )
})
