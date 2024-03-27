// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { UUID } from 'lib-common/types'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faArrowLeft, faTimes } from 'lib-icons'

import { topBarHeight, zIndex } from '../constants'

import { useTranslation } from './i18n'
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
  flex-shrink: 0;

  background: ${(p) =>
    p.invertedColors ? p.theme.colors.grayscale.g4 : p.theme.colors.main.m2};
  color: ${(p) =>
    p.invertedColors ? p.theme.colors.main.m1 : p.theme.colors.grayscale.g0};

  button {
    color: ${(p) =>
      p.invertedColors ? p.theme.colors.main.m1 : p.theme.colors.grayscale.g0};
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
  unitId: UUID | undefined
  title: string
  onBack?: () => void
  onClose?: () => void
  closeDisabled?: boolean
  invertedColors?: boolean
}

export default React.memo(function TopBar({
  unitId,
  title,
  onBack,
  onClose,
  closeDisabled = false,
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
            aria-label={i18n.common.back}
            data-qa="go-back"
          />
        </TopBarIconContainer>
      )}
      <Title>
        <Label
          data-qa="top-bar-title"
          primary={invertedColors}
          white={!invertedColors}
        >
          {title}
        </Label>
      </Title>
      {onClose ? (
        <TopBarIconContainer>
          <IconButton
            icon={faTimes}
            white
            disabled={closeDisabled}
            onClick={onClose}
            aria-label={i18n.common.close}
          />
        </TopBarIconContainer>
      ) : (
        <LoggedInUser unitId={unitId} />
      )}
    </StickyTopBar>
  )
})
