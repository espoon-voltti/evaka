// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { defaultMargins } from 'lib-components/white-space'
import styled from 'styled-components'
import { topBarHeight } from '../../constants'

export const TopBarIconContainer = styled.div`
  height: 100%;
  flex: 1 0 ${topBarHeight};
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 ${defaultMargins.xs};
`
