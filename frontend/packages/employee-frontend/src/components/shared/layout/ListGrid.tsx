// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import {
  DefaultMargins,
  SpacingSize
} from 'components/shared/layout/white-space'

interface ListGridProps {
  labelWidth?: string
  rowGap?: SpacingSize
}
const ListGrid = styled.div<ListGridProps>`
  display: grid;
  grid-template-columns: ${(p) => p.labelWidth ?? '235px'} auto;
  row-gap: ${(p) => DefaultMargins[p.rowGap || 'xs']};
  column-gap: ${DefaultMargins.s};
`

export default ListGrid
