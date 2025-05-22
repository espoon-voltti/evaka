// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { fontWeights } from 'lib-components/typography'
import { colors } from 'lib-customizations/common'

export const Status = styled.span`
  text-transform: uppercase;
  color: ${colors.grayscale.g100};
  font-size: 16px;
  font-weight: ${fontWeights.normal};
`
