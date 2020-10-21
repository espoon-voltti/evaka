// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

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
      font-weight: 600;
      width: ${(p) => p.labelWidth || '250px'};
    }
  }
`
