// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import * as _ from 'lodash'

import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from '~components/shared/layout/Table'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { getInvoiceReport, InvoiceReportFilters } from '~api/reports'
import { InvoiceReport, InvoiceReportRow } from '~types/reports'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
import {
  FilterLabel,
  FilterRow,
  TableScrollable
} from '~components/reports/common'
import { DatePicker } from '~components/common/DatePicker'
import { useTranslation } from '~state/i18n'
import { formatCents } from '../../utils/money'
import LocalDate from '@evaka/lib-common/src/local-date'

function ReportInvoices() {
  const { i18n } = useTranslation()
  const [report, setReport] = useState<Result<InvoiceReport>>(Loading())
  const [filters, setFilters] = useState<InvoiceReportFilters>({
    date: LocalDate.today()
  })

  useEffect(() => {
    setReport(Loading())
    void getInvoiceReport(filters).then(setReport)
  }, [filters])

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.invoices.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.date}</FilterLabel>
          <DatePicker
            date={filters.date}
            onChange={(date) => setFilters({ date })}
          />
        </FilterRow>
        {isLoading(report) && <Loader />}
        {isFailure(report) && <span>{i18n.common.loadingFailed}</span>}
        {isSuccess(report) && (
          <>
            <ReportDownload
              data={report.data.reportRows}
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
                {_.orderBy(report.data.reportRows, ['areaCode']).map(
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
                  <Td>{report.data.totalAmountOfInvoices}</Td>
                  <Td>{formatCents(report.data.totalSumCents)}</Td>
                  <Td>{report.data.totalAmountWithoutSSN}</Td>
                  <Td>{report.data.totalAmountWithoutAddress}</Td>
                  <Td>{report.data.totalAmountWithZeroPrice}</Td>
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
