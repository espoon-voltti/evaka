// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result } from 'lib-common/api'
import { SortDirection } from 'lib-common/generated/api-types/invoicing'
import { ManualDuplicationReportRow } from 'lib-common/generated/api-types/reports'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  Th,
  Tr,
  Td,
  Thead,
  Tbody,
  SortableTh
} from 'lib-components/layout/Table'

import { getManualDuplicationReport } from '../../api/reports'
import { useTranslation } from '../../state/i18n'

import { TableScrollable } from './common'

const StyledRow = styled(Tr)``
const NoWrapTd = styled(Td)`
  white-space: nowrap;
`

export default React.memo(function ManualDuplicationReport() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<ManualDuplicationReportRow[]>>(
    Loading.of()
  )
  const loadData = () => {
    setRows(Loading.of())
    void getManualDuplicationReport().then(setRows)
  }

  useEffect(() => {
    loadData()
  }, [])

  const [sortColumn, setSortColumn] = useState<
    keyof ManualDuplicationReportRow
  >('supplementaryDaycareName')
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')

  const sortBy = useCallback(
    (column: keyof ManualDuplicationReportRow) => {
      if (sortColumn === column) {
        setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
      } else {
        setSortColumn(column)
        setSortDirection('ASC')
      }
    },
    [sortColumn, sortDirection]
  )

  const reportRows = useMemo(
    () =>
      rows
        .map((rs) =>
          orderBy(rs, [sortColumn], [sortDirection === 'ASC' ? 'asc' : 'desc'])
        )
        .getOrElse([]),
    [rows, sortColumn, sortDirection]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.manualDuplication.title}</Title>
        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <TableScrollable>
            <Thead>
              <Tr>
                <Th>{i18n.reports.manualDuplication.childName}</Th>
                <Th>{i18n.reports.manualDuplication.dateOfBirth}</Th>
                <SortableTh
                  sorted={
                    sortColumn === 'supplementaryDaycareName'
                      ? sortDirection
                      : undefined
                  }
                  onClick={() => sortBy('supplementaryDaycareName')}
                >
                  {i18n.reports.manualDuplication.supplementaryDaycare}
                </SortableTh>
                <Th>{i18n.reports.manualDuplication.supplementarySno}</Th>
                <Th>{i18n.reports.manualDuplication.supplementaryStartDate}</Th>
                <Th>{i18n.reports.manualDuplication.supplementaryEndDate}</Th>
                <SortableTh
                  sorted={
                    sortColumn === 'preschoolDaycareName'
                      ? sortDirection
                      : undefined
                  }
                  onClick={() => sortBy('preschoolDaycareName')}
                >
                  {i18n.reports.manualDuplication.preschoolDaycare}
                </SortableTh>
                <Th>{i18n.reports.manualDuplication.preschooldStartDate}</Th>
                <Th>{i18n.reports.manualDuplication.preschoolEndDate}</Th>
              </Tr>
            </Thead>
            <Tbody>
              {reportRows.map((row: ManualDuplicationReportRow) => (
                <StyledRow key={row.childId}>
                  <NoWrapTd>
                    <Link
                      target="_blank"
                      to={`/child-information/${row.childId}`}
                    >
                      {`${row.childLastName}, ${row.childFirstName}`}
                    </Link>
                  </NoWrapTd>
                  <NoWrapTd>{row.dateOfBirth.format()}</NoWrapTd>
                  <NoWrapTd>
                    <Link
                      target="_blank"
                      to={`/units/${row.supplementaryDaycareId}`}
                    >
                      {row.supplementaryDaycareName}
                    </Link>
                  </NoWrapTd>

                  <NoWrapTd>{row.supplementarySnoName}</NoWrapTd>
                  <NoWrapTd>{row.supplementaryStartDate.format()}</NoWrapTd>
                  <NoWrapTd>{row.supplementaryEndDate.format()}</NoWrapTd>
                  <NoWrapTd>
                    <Link
                      target="_blank"
                      to={`/units/${row.preschoolDaycareId}`}
                    >
                      {row.preschoolDaycareName}
                    </Link>
                  </NoWrapTd>
                  <NoWrapTd>{row.preschoolStartDate.format()}</NoWrapTd>
                  <NoWrapTd>{row.preschoolEndDate.format()}</NoWrapTd>
                </StyledRow>
              ))}
            </Tbody>
          </TableScrollable>
        )}
      </ContentArea>
    </Container>
  )
})
