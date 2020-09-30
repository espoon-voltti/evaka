// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import {
  Container,
  ContentArea,
  Loader,
  Table,
  Title
} from '~components/shared/alpha'
import * as _ from 'lodash'
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
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.reports.invoices.areaCode}</Table.Th>
                  <Table.Th>{i18n.reports.invoices.amountOfInvoices}</Table.Th>
                  <Table.Th>{i18n.reports.invoices.totalSumCents}</Table.Th>
                  <Table.Th>{i18n.reports.invoices.amountWithoutSSN}</Table.Th>
                  <Table.Th>
                    {i18n.reports.invoices.amountWithoutAddress}
                  </Table.Th>
                  <Table.Th>
                    {i18n.reports.invoices.amountWithZeroPrice}
                  </Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {_.orderBy(report.data.reportRows, ['areaCode']).map(
                  (row: InvoiceReportRow) => (
                    <Table.Row key={row.areaCode}>
                      <Table.Td>{row.areaCode}</Table.Td>
                      <Table.Td>{row.amountOfInvoices}</Table.Td>
                      <Table.Td>{formatCents(row.totalSumCents)}</Table.Td>
                      <Table.Td>{row.amountWithoutSSN}</Table.Td>
                      <Table.Td>{row.amountWithoutAddress}</Table.Td>
                      <Table.Td>{row.amountWithZeroPrice}</Table.Td>
                    </Table.Row>
                  )
                )}
                <Table.Row>
                  <Table.Td></Table.Td>
                  <Table.Td>{report.data.totalAmountOfInvoices}</Table.Td>
                  <Table.Td>{formatCents(report.data.totalSumCents)}</Table.Td>
                  <Table.Td>{report.data.totalAmountWithoutSSN}</Table.Td>
                  <Table.Td>{report.data.totalAmountWithoutAddress}</Table.Td>
                  <Table.Td>{report.data.totalAmountWithZeroPrice}</Table.Td>
                </Table.Row>
              </Table.Body>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default ReportInvoices
