// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { fontWeights } from 'lib-components/typography'
import colors from 'lib-customizations/common'

export const CheckedRowsInfo = styled.div`
  color: ${colors.grayscale.g70};
  font-style: italic;
  font-weight: ${fontWeights.bold};
  margin: 0 20px;
  display: flex;
  align-items: center;
`
