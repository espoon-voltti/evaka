// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { SortDirection } from 'lib-common/generated/api-types/invoicing'
import { NonSsnChildrenReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { SortableTh, Tbody, Td, Thead, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import ReportDownload from './ReportDownload'
import { RowCountInfo, TableScrollable } from './common'
import { nonSsnChildrenReportQuery } from './queries'

type ReportColumnKey = keyof NonSsnChildrenReportRow
export default React.memo(function NonSsnChildren() {
  const { i18n } = useTranslation()
  const reportRows = useQueryResult(nonSsnChildrenReportQuery())

  const [sortColumns, setSortColumns] = useState<ReportColumnKey[]>([
    'lastName',
    'firstName'
  ])
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')

  const sortBy = useCallback(
    (columns: ReportColumnKey[]) => {
      if (isEqual(sortColumns, columns)) {
        setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
      } else {
        setSortColumns(columns)
        setSortDirection('ASC')
      }
    },
    [sortColumns, sortDirection]
  )

  const sortedReportRows = useMemo(() => {
    const sortDirectionInput = sortDirection === 'ASC' ? 'asc' : 'desc'
    return reportRows.map((rows) => {
      const sorted = orderBy(
        rows,
        sortColumns,
        sortColumns.length > 2
          ? [sortDirectionInput]
          : sortColumns.map(() => sortDirectionInput)
      )
      return sorted
    })
  }, [reportRows, sortColumns, sortDirection])

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.nonSsnChildren.title}</Title>

        {renderResult(sortedReportRows, (rows) => (
          <>
            <ReportDownload
              data={rows.map((row) => ({
                ...row,
                dateOfBirth: row.dateOfBirth.format()
              }))}
              headers={[
                {
                  label: i18n.reports.common.lastName,
                  key: 'lastName'
                },
                {
                  label: i18n.reports.common.firstName,
                  key: 'firstName'
                },
                {
                  label: i18n.reports.nonSsnChildren.dateOfBirth,
                  key: 'dateOfBirth'
                },
                {
                  label: i18n.reports.nonSsnChildren.personOid,
                  key: 'existingPersonOid'
                },
                {
                  label: i18n.reports.nonSsnChildren.vardaOid,
                  key: 'vardaOid'
                }
              ]}
              filename={`Hetuttomat lapset ${LocalDate.todayInHelsinkiTz().formatIso()}.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <SortableTh
                    data-qa="child-name-header"
                    sorted={
                      isEqual(sortColumns, ['lastName', 'firstName'])
                        ? sortDirection
                        : undefined
                    }
                    onClick={() => sortBy(['lastName', 'firstName'])}
                  >
                    {i18n.reports.common.childName}
                  </SortableTh>
                  <SortableTh
                    sorted={
                      isEqual(sortColumns, [
                        'dateOfBirth',
                        'lastName',
                        'firstName'
                      ])
                        ? sortDirection
                        : undefined
                    }
                    onClick={() =>
                      sortBy(['dateOfBirth', 'lastName', 'firstName'])
                    }
                  >
                    {i18n.childInformation.personDetails.birthday}
                  </SortableTh>
                  <SortableTh
                    sorted={
                      isEqual(sortColumns, [
                        'existingPersonOid',
                        'lastName',
                        'firstName'
                      ])
                        ? sortDirection
                        : undefined
                    }
                    onClick={() =>
                      sortBy(['existingPersonOid', 'lastName', 'firstName'])
                    }
                  >
                    {i18n.reports.nonSsnChildren.personOid}
                  </SortableTh>
                  <SortableTh
                    sorted={
                      isEqual(sortColumns, [
                        'vardaOid',
                        'lastName',
                        'firstName'
                      ])
                        ? sortDirection
                        : undefined
                    }
                    onClick={() =>
                      sortBy(['vardaOid', 'lastName', 'firstName'])
                    }
                  >
                    {i18n.reports.nonSsnChildren.vardaOid}
                  </SortableTh>
                </Tr>
              </Thead>
              <Tbody>
                {rows.map((row) => (
                  <Tr data-qa="non-ssn-child-row" key={row.childId}>
                    <Td data-qa="child-name">
                      <Link to={`/child-information/${row.childId}`}>
                        {row.lastName} {row.firstName}
                      </Link>
                    </Td>
                    <Td data-qa="date-of-birth">{row.dateOfBirth.format()}</Td>
                    <Td data-qa="person-oid">{row.existingPersonOid}</Td>
                    <Td data-qa="varda-oid">{row.vardaOid}</Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
            <RowCountInfo rowCount={rows.length} />
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
