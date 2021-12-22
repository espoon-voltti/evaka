// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import colors from 'lib-customizations/common'

const CircleIcon = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  min-width: 34px;
  min-height: 34px;
  font-size: 24px;
  border-radius: 100%;
  color: ${colors.greyscale.white};
`

export const CircleIconGreen = styled(CircleIcon)`
  background-color: ${colors.accents.green};
`
export const CircleIconRed = styled(CircleIcon)`
  background-color: ${colors.accents.red};
`

export const CircleIconSmallOrange = styled(CircleIcon)`
  min-width: 16px;
  min-height: 16px;
  width: 16px;
  height: 16px;
  font-size: 16px;
  background-color: ${colors.accents.orange};
`
