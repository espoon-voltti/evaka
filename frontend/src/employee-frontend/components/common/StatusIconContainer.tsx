// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import colors from 'lib-customizations/common'

interface StatusIconContainerProps {
  color: string
}

export const StatusIconContainer = styled.div<StatusIconContainerProps>`
  background: ${(p) => p.color};
  border-radius: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 30px;
  height: 30px;
  color: ${colors.grayscale.g0};
`
