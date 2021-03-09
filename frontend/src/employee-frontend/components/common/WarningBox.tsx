// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import colors from '@evaka/lib-components/colors'

export const WarningBox = styled.div`
  border-radius: 2px;
  border: 1px solid ${colors.accents.orange};
  width: 818px;
  padding-top: 15px;
  padding-bottom: 15px;
  margin-top: 35px;
  display: flex;
  align-items: center;

  svg {
    color: ${colors.accents.orange};
    margin-left: 15px;
  }
`
