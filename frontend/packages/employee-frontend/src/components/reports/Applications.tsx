// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useMemo, useState } from 'react'
import ReactSelect from 'react-select'
import styled from 'styled-components'
import { Link } from 'react-router-dom'

import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from '~components/shared/layout/Table'
import { reactSelectStyles } from '~components/shared/utils'
import { useTranslation } from '~state/i18n'
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
import LocalDate from '@evaka/lib-common/src/local-date'
import { FlexRow } from 'components/common/styled/containers'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

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
          <Wrapper>
            <ReactSelect
              options={[
                ...(isSuccess(rows)
                  ? distinct(
                      rows.data.map((row) => row.careAreaName)
                    ).map((s) => ({ value: s, label: s }))
                  : [])
              ]}
              onChange={(option) =>
                option && 'value' in option
                  ? setDisplayFilters({
                      ...displayFilters,
                      careArea: option.value
                    })
                  : undefined
              }
              value={
                displayFilters.careArea !== ''
                  ? {
                      label: displayFilters.careArea,
                      value: displayFilters.careArea
                    }
                  : undefined
              }
              styles={reactSelectStyles}
              placeholder={i18n.reports.occupancies.filters.areaPlaceholder}
            />
          </Wrapper>
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
                    <Td>{row.careAreaName}</Td>
                    <Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Td>
                    <Td>
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
                  <Td>{reducePropertySum(filteredRows, (r) => r.total)}</Td>
                </Tr>
              </TableFooter>
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
}

export default Applications
