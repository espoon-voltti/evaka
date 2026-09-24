// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

const TextOnlyChip = styled.div<{
  $backgroundColor: string
  $textColor: string
}>`
  padding: 0 10px;
  border-radius: 1000px;
  background-color: ${(props) => props.$backgroundColor};
  color: ${(props) => props.$textColor};
  font-size: 14px;
  line-height: 24px;
  font-weight: 600;
  white-space: nowrap;
`

export default TextOnlyChip
