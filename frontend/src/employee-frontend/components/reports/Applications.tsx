// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result, wrapResult } from 'lib-common/api'
import { ApplicationsReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Th, Tr, Td, Thead, Tbody } from 'lib-components/layout/Table'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { Gap } from 'lib-components/white-space'

import { getApplicationsReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { PeriodFilters } from '../../types/reports'
import { distinct, reducePropertySum } from '../../utils'
import { FlexRow } from '../common/styled/containers'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableFooter, TableScrollable } from './common'

const getApplicationsReportResult = wrapResult(getApplicationsReport)

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

export default React.memo(function Applications() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<ApplicationsReportRow[]>>(
    Loading.of()
  )
  const [filters, setFilters] = useState<PeriodFilters>({
    from: LocalDate.todayInSystemTz(),
    to: LocalDate.todayInSystemTz().addMonths(4)
  })

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = (row: ApplicationsReportRow): boolean =>
    !(displayFilters.careArea && row.careAreaName !== displayFilters.careArea)

  useEffect(() => {
    setRows(Loading.of())
    void getApplicationsReportResult(filters).then(setRows)
  }, [filters])

  const filteredRows: ApplicationsReportRow[] = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)).getOrElse([]),
    [rows, displayFilters] // eslint-disable-line react-hooks/exhaustive-deps
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.applications.title}</Title>

        <FilterRow>
          <FilterLabel>
            {i18n.reports.applications.preferredStartingDate}
          </FilterLabel>
          <FlexRow>
            <DatePickerDeprecated
              date={filters.from}
              onChange={(from) => setFilters({ ...filters, from })}
              type="half-width"
              data-qa="datepicker-from"
            />
            <span>{' - '}</span>
            <DatePickerDeprecated
              date={filters.to}
              onChange={(to) => setFilters({ ...filters, to })}
              type="half-width"
              data-qa="datepicker-to"
            />
          </FlexRow>
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <Wrapper data-qa="select-area">
            <Combobox
              items={[
                { label: i18n.common.all, value: '' },
                ...rows
                  .map((rs) =>
                    distinct(rs.map((row) => row.careAreaName)).map((s) => ({
                      value: s,
                      label: s
                    }))
                  )
                  .getOrElse([])
              ]}
              selectedItem={
                displayFilters.careArea !== ''
                  ? {
                      label: displayFilters.careArea,
                      value: displayFilters.careArea
                    }
                  : {
                      label: i18n.common.all,
                      value: ''
                    }
              }
              placeholder={i18n.reports.occupancies.filters.areaPlaceholder}
              onChange={(item) =>
                setDisplayFilters({
                  ...displayFilters,
                  careArea: item?.value ?? ''
                })
              }
              getItemLabel={(item) => item.label}
            />
          </Wrapper>
        </FilterRow>

        <Gap />
        <InfoBox message={i18n.reports.applications.ageInfo} thin />

        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
            <ReportDownload
              data={filteredRows.map((row) => ({
                ...row,
                unitProviderType:
                  i18n.reports.common.unitProviderTypes[row.unitProviderType]
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
            <TableScrollable data-qa="report-application-table">
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.unitProviderType}</Th>
                  <Th>{i18n.reports.applications.under3Years}</Th>
                  <Th>{i18n.reports.applications.over3Years}</Th>
                  <Th>{i18n.reports.applications.preschool}</Th>
                  <Th>{i18n.reports.applications.club}</Th>
                  <Th>{i18n.reports.applications.totalChildren}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row: ApplicationsReportRow) => (
                  <Tr key={row.unitId}>
                    <Td data-qa="care-area-name">{row.careAreaName}</Td>
                    <Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Td>
                    <Td data-qa="unit-provider-type">
                      {
                        i18n.reports.common.unitProviderTypes[
                          row.unitProviderType
                        ]
                      }
                    </Td>
                    <Td>{row.under3Years}</Td>
                    <Td>{row.over3Years}</Td>
                    <Td>{row.preschool}</Td>
                    <Td>{row.club}</Td>
                    <Td>{row.total}</Td>
                  </Tr>
                ))}
              </Tbody>
              <TableFooter>
                <Tr>
                  <Td className="bold">{i18n.reports.common.total}</Td>
                  <Td />
                  <Td />
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.under3Years)}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.over3Years)}
                  </Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.preschool)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.club)}</Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.total)}</Td>
                </Tr>
              </TableFooter>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
})
