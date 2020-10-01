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
import { Link } from 'react-router-dom'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { ApplicationsReportRow } from '~types/reports'
import { getApplicationsReport, PeriodFilters } from '~api/reports'
import ReportDownload from '~components/reports/ReportDownload'
import {
  FilterLabel,
  FilterRow,
  TableFooter,
  TableScrollable
} from '~components/reports/common'
import { DatePicker } from '~components/common/DatePicker'
import { distinct, reducePropertySum } from 'utils'
import SelectWithIcon from 'components/common/Select'
import LocalDate from '@evaka/lib-common/src/local-date'
import { FlexRow } from 'components/common/styled/containers'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

function Applications() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<ApplicationsReportRow[]>>(Loading())
  const [filters, setFilters] = useState<PeriodFilters>({
    from: LocalDate.today(),
    to: LocalDate.today().addMonths(4)
  })

  const [displayFilters, setDisplayFilters] = useState<DisplayFilters>(
    emptyDisplayFilters
  )
  const displayFilter = (row: ApplicationsReportRow): boolean => {
    return !(
      displayFilters.careArea && row.careAreaName !== displayFilters.careArea
    )
  }

  useEffect(() => {
    setRows(Loading())
    setDisplayFilters(emptyDisplayFilters)
    void getApplicationsReport(filters).then(setRows)
  }, [filters])

  const filteredRows = useMemo(
    () => (isSuccess(rows) ? rows.data.filter(displayFilter) : []),
    [rows, displayFilters]
  )

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.applications.title}</Title>

        <FilterRow>
          <FilterLabel>
            {i18n.reports.applications.preferredStartingDate}
          </FilterLabel>
          <FlexRow>
            <DatePicker
              date={filters.from}
              onChange={(from) => setFilters({ ...filters, from })}
              type="half-width"
            />
            <span>{' - '}</span>
            <DatePicker
              date={filters.to}
              onChange={(to) => setFilters({ ...filters, to })}
              type="half-width"
            />
          </FlexRow>
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <SelectWithIcon
            options={[
              { id: '', label: '' },
              ...(isSuccess(rows)
                ? distinct(
                    rows.data.map((row) => row.careAreaName)
                  ).map((s) => ({ id: s, label: s }))
                : [])
            ]}
            value={displayFilters.careArea}
            onChange={(e) =>
              setDisplayFilters({ ...displayFilters, careArea: e.target.value })
            }
            fullWidth
          />
        </FilterRow>

        {isLoading(rows) && <Loader />}
        {isFailure(rows) && <span>{i18n.common.loadingFailed}</span>}
        {isSuccess(rows) && (
          <>
            <ReportDownload
              data={filteredRows.map((row) => ({
                ...row,
                unitProviderType: String(
                  i18n.reports.common.unitProviderTypes[row.unitProviderType]
                )
              }))}
              headers={[
                {
                  label: i18n.reports.common.careAreaName,
                  key: 'careAreaName'
                },
                { label: i18n.reports.common.unitName, key: 'unitName' },
                {
                  label: i18n.reports.common.unitProviderType,
                  key: 'unitProviderType'
                },
                {
                  label: i18n.reports.applications.under3Years,
                  key: 'under3Years'
                },
                {
                  label: i18n.reports.applications.over3Years,
                  key: 'over3Years'
                },
                {
                  label: i18n.reports.applications.preschool,
                  key: 'preschool'
                },
                {
                  label: i18n.reports.applications.club,
                  key: 'club'
                },
                { label: i18n.reports.applications.totalChildren, key: 'total' }
              ]}
              filename={`${
                i18n.reports.applications.title
              } ${filters.from.formatIso()}-${filters.to.formatIso()}.csv`}
            />
            <TableScrollable>
              <Table.Head>
                <Table.Row>
                  <Table.Th>{i18n.reports.common.careAreaName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitName}</Table.Th>
                  <Table.Th>{i18n.reports.common.unitProviderType}</Table.Th>
                  <Table.Th>{i18n.reports.applications.under3Years}</Table.Th>
                  <Table.Th>{i18n.reports.applications.over3Years}</Table.Th>
                  <Table.Th>{i18n.reports.applications.preschool}</Table.Th>
                  <Table.Th>{i18n.reports.applications.club}</Table.Th>
                  <Table.Th>{i18n.reports.applications.totalChildren}</Table.Th>
                </Table.Row>
              </Table.Head>
              <Table.Body>
                {filteredRows.map((row: ApplicationsReportRow) => (
                  <Table.Row key={row.unitId}>
                    <Table.Td>{row.careAreaName}</Table.Td>
                    <Table.Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Table.Td>
                    <Table.Td>
                      {
                        i18n.reports.common.unitProviderTypes[
                          row.unitProviderType
                        ]
                      }
                    </Table.Td>
                    <Table.Td>{row.under3Years}</Table.Td>
                    <Table.Td>{row.over3Years}</Table.Td>
                    <Table.Td>{row.preschool}</Table.Td>
                    <Table.Td>{row.club}</Table.Td>
                    <Table.Td>{row.total}</Table.Td>
                  </Table.Row>
                ))}
              </Table.Body>
              <TableFooter>
                <Table.Row>
                  <Table.Td className="bold">
                    {i18n.reports.common.total}
                  </Table.Td>
                  <Table.Td />
                  <Table.Td />
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.under3Years)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.over3Years)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.preschool)}
                  </Table.Td>
                  <Table.Td>
                    {reducePropertySum(filteredRows, (r) => r.total)}
                  </Table.Td>
                </Table.Row>
              </TableFooter>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default Applications
