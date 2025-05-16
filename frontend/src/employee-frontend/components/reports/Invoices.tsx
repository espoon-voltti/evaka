// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import range from 'lodash/range'
import React, { useState } from 'react'

import type { InvoiceReportRow } from 'lib-common/generated/api-types/reports'
import { formatCents } from 'lib-common/money'
import { useQueryResult } from 'lib-common/query'
import type { Arg0 } from 'lib-common/types'
import YearMonth from 'lib-common/year-month'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Th, Tr, Td, Thead, Tbody } from 'lib-components/layout/Table'

import ReportDownload from '../../components/reports/ReportDownload'
import type { getInvoiceReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { FilterLabel, FilterRow, TableScrollable } from './common'
import { invoicesReportQuery } from './queries'

const previousMonth = YearMonth.todayInHelsinkiTz().subMonths(1)
const monthOptions = range(1, 13)
const yearOptions = range(previousMonth.year, previousMonth.year - 4, -1)

type InvoiceReportFilters = Arg0<typeof getInvoiceReport>

export default React.memo(function ReportInvoices() {
  const { i18n } = useTranslation()
  const [filters, setFilters] = useState<InvoiceReportFilters>({
    yearMonth: previousMonth
  })
  const report = useQueryResult(invoicesReportQuery(filters))

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.invoices.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.invoices.period}</FilterLabel>
          <Combobox
            items={monthOptions}
            selectedItem={filters.yearMonth.month}
            onChange={(month) => {
              if (month !== null) {
                setFilters({
                  ...filters,
                  yearMonth: new YearMonth(filters.yearMonth.year, month)
                })
              }
            }}
            getItemLabel={(month) => i18n.datePicker.months[month - 1]}
          />
          <Combobox
            items={yearOptions}
            selectedItem={filters.yearMonth.year}
            onChange={(year) => {
              if (year !== null) {
                setFilters({
                  ...filters,
                  yearMonth: new YearMonth(year, filters.yearMonth.month)
                })
              }
            }}
          />
        </FilterRow>
        {renderResult(report, (report) => (
          <>
            <ReportDownload<InvoiceReportRow>
              data={report.reportRows}
              columns={[
                { label: 'Alue', value: (row) => row.areaCode },
                { label: 'Laskuja', value: (row) => row.amountOfInvoices },
                { label: 'Summa', value: (row) => row.totalSumCents },
                { label: 'Hetuttomia', value: (row) => row.amountWithoutSSN },
                {
                  label: 'Osoitteettomia',
                  value: (row) => row.amountWithoutAddress
                },
                {
                  label: 'Nollalaskuja',
                  value: (row) => row.amountWithZeroPrice
                }
              ]}
              filename={`Laskujen_täsmäytys ${filters.yearMonth.formatIso()}.csv`}
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
                {orderBy(report.reportRows, ['areaCode']).map(
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
                  <Td />
                  <Td>{report.totalAmountOfInvoices}</Td>
                  <Td>{formatCents(report.totalSumCents)}</Td>
                  <Td>{report.totalAmountWithoutSSN}</Td>
                  <Td>{report.totalAmountWithoutAddress}</Td>
                  <Td>{report.totalAmountWithZeroPrice}</Td>
                </Tr>
              </Tbody>
            </TableScrollable>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
