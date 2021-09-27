// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import colors from 'lib-customizations/common'
import { fontWeights } from 'lib-components/typography'

export const CheckedRowsInfo = styled.div`
  color: ${colors.greyscale.dark};
  font-style: italic;
  font-weight: ${fontWeights.bold};
  margin: 0 20px;
  display: flex;
  align-items: center;
`
