// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

export const FullHeightContainer = styled(Container)<{ spaced?: boolean }>`
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: ${colors.grayscale.g0};
  padding: ${defaultMargins.s};
  ${(p) => (p.spaced ? 'justify-content: space-between;' : '')}
`

export const WideLinkButton = styled(Link)<{ $primary?: boolean }>`
  min-height: ${defaultMargins.XL};
  outline: none;
  cursor: pointer;
  font-family: 'Open Sans', sans-serif;
  font-size: 14px;
  line-height: 16px;
  font-weight: ${fontWeights.semibold};
  white-space: nowrap;
  letter-spacing: 0.2px;
  width: 100%;

  display: flex;
  justify-content: center;
  align-items: center;
  background: ${(p) => (p.$primary ? colors.main.m2 : colors.grayscale.g0)};
  color: ${(p) => (p.$primary ? colors.grayscale.g0 : colors.main.m2)};
`

export const BackButton = styled(IconOnlyButton)`
  color: ${colors.main.m1};
  position: absolute;
`

export const TallContentArea = styled(ContentArea)<{ spaced?: boolean }>`
  min-height: 100%;
  display: flex;
  flex-direction: column;
  flex: 1;
  ${(p) => (p.spaced ? 'justify-content: space-between;' : '')}
`
