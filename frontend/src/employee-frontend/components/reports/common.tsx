// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { Table } from 'lib-components/layout/Table'
import { fontWeights } from 'lib-components/typography'
import { useTranslation } from '../../state/i18n'
import { FlexRow } from '../common/styled/containers'
import colors from 'lib-customizations/common'
import { Gap } from 'lib-components/white-space'

export const TableScrollableWrapper = styled.div`
  display: block;
  overflow-x: auto;
  overflow-y: auto;
  max-height: 600px;
  width: 100%;

  th {
    position: sticky;
    top: 0;
    background: white;
    z-index: 2;
  }
`

interface TableScrollableProps {
  children: React.ReactNode
  'data-qa'?: string
}

export function TableScrollable({
  children,
  'data-qa': dataQa
}: TableScrollableProps) {
  return (
    <TableScrollableWrapper>
      <Table data-qa={dataQa}>{children}</Table>
    </TableScrollableWrapper>
  )
}

export const TableFooter = styled.tfoot`
  td {
    position: sticky;
    bottom: 0;
    background: ${colors.greyscale.lightest};
    z-index: 2;
  }
`

export const FilterRow = styled.div`
  display: flex;
  width: 500px;
  align-items: center;
  margin-bottom: 6px;
`
export const FilterLabel = styled.label`
  font-weight: ${fontWeights.semibold};
  width: 150px;
  min-width: 150px;
`

export function RowCountInfo({ rowCount }: { rowCount: number }) {
  const { i18n } = useTranslation()

  return (
    <>
      <Gap size="xxs" />
      <FlexRow justifyContent="flex-end">
        <span>
          {i18n.reports.common.total}: {rowCount}
        </span>
      </FlexRow>
    </>
  )
}
