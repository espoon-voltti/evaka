// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { Container, ContentArea } from 'lib-components/layout/Container'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import { useTranslation } from '../../state/i18n'
import { Loading, Result } from 'lib-common/api'
import { VardaErrorReportRow } from '../../types/reports'
import { DateFilters, getVardaErrorsReport } from '../../api/reports'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import LocalDate from 'lib-common/local-date'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { Link } from 'react-router-dom'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import styled from 'styled-components'
import FiniteDateRange from 'lib-common/finite-date-range'

const FlatList = styled.ul`
  list-style: none;
  padding-left: 0;
  margin-top: 0;
`

function VardaErrors() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<VardaErrorReportRow[]>>(Loading.of())
  const [filters, setFilters] = useState<DateFilters>({
    date: LocalDate.today().subDays(1)
  })

  useEffect(() => {
    setRows(Loading.of())
    void getVardaErrorsReport(filters).then(setRows)
  }, [filters])

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.vardaErrors.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.date}</FilterLabel>
          <DatePicker
            id="start-date"
            date={filters.date.toString()}
            onChange={(date) => {
              const parsed = LocalDate.parseFiOrNull(date)
              if (parsed) setFilters({ date: parsed })
            }}
            locale={'fi'}
          />
        </FilterRow>
        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.vardaErrors.created}</Th>
                  <Th>{i18n.reports.vardaErrors.child}</Th>
                  <Th>{i18n.reports.vardaErrors.error}</Th>
                  <Th>{i18n.reports.vardaErrors.serviceNeed}</Th>
                  <Th>{i18n.reports.vardaErrors.updated}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.value.map((row: VardaErrorReportRow) => (
                  <Tr key={`${row.serviceNeedId}`}>
                    <Td>
                      {LocalDate.fromSystemTzDate(row.created).format()}{' '}
                      {row.created.toLocaleTimeString()}
                    </Td>
                    <Td>
                      <Link to={`/child-information/${row.childId}`}>
                        {row.childId}
                      </Link>
                    </Td>

                    <Td>{row.errors.join('\n')}</Td>
                    <Td>
                      <FlatList>
                        <li>{row.serviceNeedOptionName}</li>
                        <li>
                          {FiniteDateRange.parseJson({
                            start: row.serviceNeedStartDate,
                            end: row.serviceNeedEndDate
                          }).format()}
                        </li>
                        <li>{row.serviceNeedId}</li>
                      </FlatList>
                    </Td>
                    <Td>
                      {LocalDate.fromSystemTzDate(row.updated).format()}{' '}
                      {row.updated.toLocaleTimeString()}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default VardaErrors
