// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { PreschoolApplicationReportRow } from 'lib-common/generated/api-types/reports'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  SortableTh,
  SortDirection,
  Tbody,
  Td,
  Thead,
  Tr
} from 'lib-components/layout/Table'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { TableScrollable } from './common'
import { preschoolApplicationReportQuery } from './queries'

export default React.memo(function PreschoolApplicationReport() {
  const { i18n } = useTranslation()
  const result = useQueryResult(preschoolApplicationReportQuery())

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.preschoolApplications.title}</Title>
        {renderResult(result, (rows) => (
          <PreschoolApplicationReportTable rows={rows} />
        ))}
      </ContentArea>
    </Container>
  )
})

type SortColumn = keyof PreschoolApplicationReportRow

const PreschoolApplicationReportTable = ({
  rows
}: {
  rows: PreschoolApplicationReportRow[]
}) => {
  const { i18n } = useTranslation()
  const [sort, setSort] = useState<{
    column: SortColumn
    direction: SortDirection
  }>({ column: 'applicationUnitName', direction: 'ASC' })
  const sortedRows = useMemo(
    () =>
      orderBy(
        rows,
        [sort.column, 'childLastName', 'childFirstName', 'childDateOfBirth'],
        [sort.direction === 'ASC' ? 'asc' : 'desc']
      ),
    [rows, sort.column, sort.direction]
  )
  const sortBy = useCallback(
    (column: SortColumn) => () =>
      setSort({
        column,
        direction:
          sort.column === column && sort.direction === 'ASC' ? 'DESC' : 'ASC'
      }),
    [sort.column, sort.direction]
  )
  const sorted = useCallback(
    (column: SortColumn) =>
      sort.column === column ? sort.direction : undefined,
    [sort.column, sort.direction]
  )

  return (
    <TableScrollable>
      <Thead>
        <Tr>
          <SortableTh
            onClick={sortBy('applicationUnitName')}
            sorted={sorted('applicationUnitName')}
          >
            {i18n.reports.preschoolApplications.columns.applicationUnitName}
          </SortableTh>
          <SortableTh
            onClick={sortBy('childLastName')}
            sorted={sorted('childLastName')}
          >
            {i18n.reports.preschoolApplications.columns.childLastName}
          </SortableTh>
          <SortableTh
            onClick={sortBy('childFirstName')}
            sorted={sorted('childFirstName')}
          >
            {i18n.reports.preschoolApplications.columns.childFirstName}
          </SortableTh>
          <SortableTh
            onClick={sortBy('childDateOfBirth')}
            sorted={sorted('childDateOfBirth')}
          >
            {i18n.reports.preschoolApplications.columns.childDateOfBirth}
          </SortableTh>
          <SortableTh
            onClick={sortBy('childStreetAddress')}
            sorted={sorted('childStreetAddress')}
          >
            {i18n.reports.preschoolApplications.columns.childStreetAddress}
          </SortableTh>
          <SortableTh
            onClick={sortBy('childPostalCode')}
            sorted={sorted('childPostalCode')}
          >
            {i18n.reports.preschoolApplications.columns.childPostalCode}
          </SortableTh>
          <SortableTh
            onClick={sortBy('currentUnitName')}
            sorted={sorted('currentUnitName')}
          >
            {i18n.reports.preschoolApplications.columns.currentUnitName}
          </SortableTh>
          <SortableTh
            onClick={sortBy('isDaycareAssistanceNeed')}
            sorted={sorted('isDaycareAssistanceNeed')}
          >
            {i18n.reports.preschoolApplications.columns.isDaycareAssistanceNeed}
          </SortableTh>
        </Tr>
      </Thead>
      <Tbody>
        {sortedRows.length === 0 ? (
          <Tr>
            <Td colSpan={8} align="center" data-qa="no-results">
              {i18n.common.noResults}
            </Td>
          </Tr>
        ) : (
          sortedRows.map((row) => {
            return (
              <Tr key={row.applicationId} data-qa="row">
                <Td>
                  <Link to={`/units/${row.applicationUnitId}`}>
                    {row.applicationUnitName}
                  </Link>
                </Td>
                <Td>{row.childLastName}</Td>
                <Td>
                  <Link to={`/child-information/${row.childId}`}>
                    {row.childFirstName}
                  </Link>
                </Td>
                <Td>{row.childDateOfBirth.format()}</Td>
                <Td>{row.childStreetAddress}</Td>
                <Td>{row.childPostalCode}</Td>
                <Td>
                  {row.currentUnitId !== null &&
                    row.currentUnitName !== null && (
                      <Link to={`/units/${row.currentUnitId}`}>
                        {row.currentUnitName}
                      </Link>
                    )}
                </Td>
                <Td>
                  {row.isDaycareAssistanceNeed
                    ? i18n.common.yes
                    : i18n.common.no}
                </Td>
              </Tr>
            )
          })
        )}
      </Tbody>
    </TableScrollable>
  )
}
