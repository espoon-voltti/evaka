// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'
import { fontWeights } from 'lib-components/typography'

interface DataListProps {
  labelWidth?: string
  marginBottom?: string
}

export const DataList = styled.div<DataListProps>`
  display: flex;
  flex-direction: column;
  margin-bottom: ${(p) => p.marginBottom || '0'};

  > div {
    display: flex;
    flex-direction: row;
    line-height: 2em;

    label {
      font-weight: ${fontWeights.semibold};
      width: ${(p) => p.labelWidth || '250px'};
    }
  }
`
