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
import ReportDownload from '../../components/reports/ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import LocalDate from 'lib-common/local-date'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { Link } from 'react-router-dom'

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
          <DatePickerDeprecated
            date={filters.date}
            onChange={(date) => setFilters({ date })}
          />
        </FilterRow>
        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
            <ReportDownload
              data={rows.value.map((row) => ({
                ...row
              }))}
              headers={[
                {
                  label: i18n.reports.vardaErrors.error,
                  key: 'error'
                },
                {
                  label: i18n.reports.vardaErrors.updated,
                  key: 'updated'
                },
                {
                  label: i18n.reports.vardaErrors.updated,
                  key: 'updated'
                }
              ]}
              filename={`${
                i18n.reports.vardaErrors.title
              }-${filters.date.formatIso()}.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
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
                      <Link to={`/child-information/${row.childId}`}>
                        {row.childId}
                      </Link>
                    </Td>

                    <Td>{row.errors}</Td>
                    <Td>{row.serviceNeedId}</Td>
                    <Td>{row.updated.toISOString()}</Td>
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
