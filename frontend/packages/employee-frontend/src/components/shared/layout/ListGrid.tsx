// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import {
  defaultMargins,
  SpacingSize
} from '@evaka/lib-components/src/white-space'

interface ListGridProps {
  labelWidth?: string
  rowGap?: SpacingSize
  columnGap?: SpacingSize
}
const ListGrid = styled.div<ListGridProps>`
  display: grid;
  grid-template-columns: ${(p) => p.labelWidth ?? '235px'} auto;
  row-gap: ${(p) => defaultMargins[p.rowGap || 'xs']};
  column-gap: ${(p) => defaultMargins[p.columnGap || 's']};
`

export default ListGrid
