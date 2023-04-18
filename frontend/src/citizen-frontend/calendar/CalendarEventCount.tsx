// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { fontWeights } from 'lib-components/typography'

export const CalendarEventCountContainer = styled.div`
  position: relative;
  font-size: 20px;
  height: fit-content;
`
export const CalendarEventCount = styled.div`
  padding: 2px;
  height: 20px;
  min-width: 20px;
  background-color: ${(p) => p.theme.colors.status.warning};
  color: ${(p) => p.theme.colors.grayscale.g0};
  font-weight: ${fontWeights.bold};
  display: flex;
  justify-content: center;
  align-items: center;
  position: absolute;
  top: 100%;
  left: 100%;
  transform: translate(-60%, -60%);
  font-size: 14px;
  border-radius: 9999px;
`
