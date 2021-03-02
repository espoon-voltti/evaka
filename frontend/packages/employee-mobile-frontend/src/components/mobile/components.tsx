// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Link } from 'react-router-dom'
import styled from 'styled-components'

import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'
import colors from '@evaka/lib-components/src/colors'
import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import { defaultMargins } from '@evaka/lib-components/src/white-space'

export const FullHeightContainer = styled(Container)<{ spaced?: boolean }>`
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: ${colors.greyscale.white};
  padding: ${defaultMargins.s};
  ${(p) => (p.spaced ? 'justify-content: space-between;' : '')}
`

export const WideLinkButton = styled(Link)`
  min-height: 45px;
  outline: none;
  cursor: pointer;
  font-family: 'Open Sans', sans-serif;
  font-size: 14px;
  line-height: 16px;
  font-weight: 600;
  text-transform: uppercase;
  white-space: nowrap;
  letter-spacing: 0.2px;
  width: 100%;
  color: ${colors.greyscale.white};
  background: ${colors.blues.primary};
  display: flex;
  justify-content: center;
  align-items: center;
`

export const BackButton = styled(IconButton)`
  color: ${colors.blues.dark};
  position: absolute;
`

export const TallContentArea = styled(ContentArea)<{ spaced?: boolean }>`
  height: 100%;
  display: flex;
  flex-direction: column;
  flex: 1;
  ${(p) => (p.spaced ? 'justify-content: space-between;' : '')}
`

export const ContentAreaWithShadow = styled(ContentArea)<{ active?: boolean }>`
  box-shadow: 0px 4px 4px 0px #d9d9d9;
  ${(p) =>
    p.active
      ? `background-color: ${colors.brandEspoo.espooTurquoiseLight}`
      : ''}
`
