// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import { Td } from 'lib-components/layout/Table'
import { defaultMargins } from 'lib-components/white-space'

export const NameTd = styled(Td)`
  width: 30%;
`
export const DateTd = styled(Td)`
  width: 12%;
`
export const ButtonsTd = styled(Td)`
  width: 13%;
`
export const StatusTd = styled(Td)`
  width: 13%;
`
export const HeaderRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
`
export const InfoLabelContainer = styled.div`
  display: flex;

  > div:not(:last-child) {
    margin-right: ${defaultMargins.xs};
  }
`
