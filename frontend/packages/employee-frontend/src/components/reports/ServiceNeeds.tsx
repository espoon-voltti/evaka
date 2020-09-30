// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import {
  Container,
  ContentArea,
  Loader,
  Table,
  Title
} from '~components/shared/alpha'
import { useTranslation } from '~state/i18n'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { ServiceNeedReportRow } from '~types/reports'
import { DateFilters, getServiceNeedReport } from '~api/reports'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
import { DatePicker } from '~components/common/DatePicker'
import {
  FilterLabel,
  FilterRow,
  TableScrollable
} from '~components/reports/common'
import LocalDate from '@evaka/lib-common/src/local-date'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

function ServiceNeeds() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<ServiceNeedReportRow[]>>(Loading())
  const [filters, setFilters] = useState<DateFilters>({
    date: LocalDate.today()
  })

  const [displayFilters, setDisplayFilters] = useState<DisplayFilters>(
    emptyDisplayFilters
  )
  const displayFilter = (row: ServiceNeedReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading())
    setDisplayFilters(emptyDisplayFilters)
    void getServiceNeedReport(filters).then(setRows)
  }, [filters])

  const filteredRows = useMemo(
    () => (isSuccess(rows) ? rows.data.filter(displayFilter) : []),
    [rows, displayFilters]
  )

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.serviceNeeds.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.date}</FilterLabel>
          <DatePicker
            date={filters.date}
            onChange={(date) => setFilters({ date })}
          />
        </FilterRow>

        {isLoading(rows) && <Loader />}
        {isFailure(rows) && <span>{i18n.common.loadingFailed}</span>}
        {isSuccess(rows) && (
          <>
            <ReportDownload
              data={rows.data.map((row) => ({
                ...row
              }))}
              headers={[
                { label: 'Palvelualue', key: 'careAreaName' },
                { label: 'Yksikkö', key: 'unitName' },
                { label: 'Ikä', key: 'age' },
                { label: 'Kokopäiväinen', key: 'fullDay' },
                { label: 'Osapäiväinen', key: 'partDay' },
                { label: 'Kokoviikkoinen', key: 'fullWeek' },
                { label: 'Osaviikkoinne', key: 'partWeek' },
                { label: 'Vuorohoito', key: 'shiftCare' },
                { label: 'Palveluntarve puuttuu', key: 'missingServiceNeed' },
                { label: 'Lapsia yhteensä', key: 'total' }
              ]}
              filename={`Lapsien palvelutarpeet ja iät yksiköissä ${filters.date.formatIso()}.csv`}
            />
            <TableScrollable>
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.reports.common.careAreaName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitType}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitProviderType}</Table.Th>
                  <Table.Th>{i18n.reports.serviceNeeds.age}</Table.Th>
                  <Table.Th>{i18n.reports.serviceNeeds.fullDay}</Table.Th>
                  <Table.Th>{i18n.reports.serviceNeeds.partDay}</Table.Th>
                  <Table.Th>{i18n.reports.serviceNeeds.fullWeek}</Table.Th>
                  <Table.Th>{i18n.reports.serviceNeeds.partWeek}</Table.Th>
                  <Table.Th>{i18n.reports.serviceNeeds.shiftCare}</Table.Th>
                  <Table.Th>
                    {i18n.reports.serviceNeeds.missingServiceNeed}
                  </Table.Th>
                  <Table.Th>{i18n.reports.serviceNeeds.total}</Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {filteredRows.map((row: ServiceNeedReportRow) => (
                  <Table.Row key={row.unitName}>
                    <Table.Td>{row.careAreaName}</Table.Td>
                    <Table.Td>{row.unitName}</Table.Td>
                    <Table.Td>
                      {row.unitType
                        ? i18n.reports.common.unitTypes[row.unitType]
                        : ''}
                    </Table.Td>
                    <Table.Td>
                      {
                        i18n.reports.common.unitProviderTypes[
                          row.unitProviderType
                        ]
                      }
                    </Table.Td>
                    <Table.Td>{row.age}</Table.Td>
                    <Table.Td>{row.fullDay}</Table.Td>
                    <Table.Td>{row.partDay}</Table.Td>
                    <Table.Td>{row.fullWeek}</Table.Td>
                    <Table.Td>{row.partWeek}</Table.Td>
                    <Table.Td>{row.shiftCare}</Table.Td>
                    <Table.Td>{row.missingServiceNeed}</Table.Td>
                    <Table.Td>{row.total}</Table.Td>
                  </Table.Row>
                ))}
              </Table.Body>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default ServiceNeeds
