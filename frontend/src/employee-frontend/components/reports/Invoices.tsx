// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as _ from 'lodash'
import React, { useEffect, useState } from 'react'
import { Loading, Result } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { formatCents } from 'lib-common/money'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Th, Tr, Td, Thead, Tbody } from 'lib-components/layout/Table'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { getInvoiceReport, InvoiceReportFilters } from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { InvoiceReport, InvoiceReportRow } from '../../types/reports'
import { FilterLabel, FilterRow, TableScrollable } from './common'

function ReportInvoices() {
  const { i18n } = useTranslation()
  const [report, setReport] = useState<Result<InvoiceReport>>(Loading.of())
  const [filters, setFilters] = useState<InvoiceReportFilters>({
    date: LocalDate.today()
  })

  useEffect(() => {
    setReport(Loading.of())
    void getInvoiceReport(filters).then(setReport)
  }, [filters])

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.invoices.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.date}</FilterLabel>
          <DatePickerDeprecated
            date={filters.date}
            onChange={(date) => setFilters({ date })}
          />
        </FilterRow>
        {report.isLoading && <Loader />}
        {report.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {report.isSuccess && (
          <>
            <ReportDownload
              data={report.value.reportRows}
              headers={[
                { label: 'Alue', key: 'areaCode' },
                { label: 'Laskuja', key: 'amountOfInvoices' },
                { label: 'Summa', key: 'totalSumCents' },
                { label: 'Hetuttomia', key: 'amountWithoutSSN' },
                { label: 'Osoitteettomia', key: 'amountWithoutAddress' },
                { label: 'Nollalaskuja', key: 'amountWithZeroPrice' }
              ]}
              filename={`Laskujen_täsmäytys ${filters.date.formatIso()}.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.invoices.areaCode}</Th>
                  <Th>{i18n.reports.invoices.amountOfInvoices}</Th>
                  <Th>{i18n.reports.invoices.totalSumCents}</Th>
                  <Th>{i18n.reports.invoices.amountWithoutSSN}</Th>
                  <Th>{i18n.reports.invoices.amountWithoutAddress}</Th>
                  <Th>{i18n.reports.invoices.amountWithZeroPrice}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {_.orderBy(report.value.reportRows, ['areaCode']).map(
                  (row: InvoiceReportRow) => (
                    <Tr key={row.areaCode}>
                      <Td>{row.areaCode}</Td>
                      <Td>{row.amountOfInvoices}</Td>
                      <Td>{formatCents(row.totalSumCents)}</Td>
                      <Td>{row.amountWithoutSSN}</Td>
                      <Td>{row.amountWithoutAddress}</Td>
                      <Td>{row.amountWithZeroPrice}</Td>
                    </Tr>
                  )
                )}
                <Tr>
                  <Td></Td>
                  <Td>{report.value.totalAmountOfInvoices}</Td>
                  <Td>{formatCents(report.value.totalSumCents)}</Td>
                  <Td>{report.value.totalAmountWithoutSSN}</Td>
                  <Td>{report.value.totalAmountWithoutAddress}</Td>
                  <Td>{report.value.totalAmountWithZeroPrice}</Td>
                </Tr>
              </Tbody>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default ReportInvoices
