// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'wouter'

import type { ApplicationType } from 'lib-common/generated/api-types/application'
import { applicationTypes } from 'lib-common/generated/api-types/application'
import type { DecisionsReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { DatePickerSpacer } from 'lib-components/molecules/date-picker/DateRangePicker'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import type { PeriodFilters } from '../../types/reports'
import { distinct, reducePropertySum } from '../../utils'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableFooter, TableScrollable } from './common'
import { decisionReportQuery } from './queries'

interface Filters extends PeriodFilters {
  applicationType: ApplicationType | null
}

interface DisplayFilters {
  careArea: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: ''
}

const Wrapper = styled.div`
  width: 100%;
`

export default React.memo(function Decisions() {
  const { i18n } = useTranslation()
  const [filters, setFilters] = useState<Filters>({
    from: LocalDate.todayInSystemTz(),
    to: LocalDate.todayInSystemTz().addMonths(4),
    applicationType: null
  })

  const columns = useMemo(
    () =>
      filters.applicationType !== null
        ? applicationTypeToColumnType(filters.applicationType)
        : columnTypes,
    [filters.applicationType]
  )

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = useCallback(
    (row: DecisionsReportRow): boolean =>
      !(
        displayFilters.careArea && row.careAreaName !== displayFilters.careArea
      ),
    [displayFilters.careArea]
  )

  const rows = useQueryResult(
    filters.from && filters.to
      ? decisionReportQuery({
          ...filters,
          from: filters.from,
          to: filters.to
        })
      : constantQuery([])
  )

  const filteredRows = useMemo(
    () => rows.map((rs) => rs.filter(displayFilter)),
    [rows, displayFilter]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.decisions.title}</Title>

        <FilterRow>
          <FilterLabel>{i18n.reports.decisions.sentDate}</FilterLabel>
          <FlexRow>
            <DatePicker
              date={filters.from}
              onChange={(from) => setFilters({ ...filters, from })}
              locale="fi"
              data-qa="datepicker-from"
            />
            <DatePickerSpacer />
            <DatePicker
              date={filters.to}
              onChange={(to) => setFilters({ ...filters, to })}
              locale="fi"
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
              getItemLabel={(item) => item.label}
              onChange={(option) =>
                option
                  ? setDisplayFilters({
                      ...displayFilters,
                      careArea: option.value
                    })
                  : undefined
              }
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
            />
          </Wrapper>
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.application.decisions.type}</FilterLabel>
          <Wrapper>
            <Combobox
              items={applicationTypes}
              selectedItem={filters.applicationType}
              onChange={(item) =>
                setFilters({ ...filters, applicationType: item })
              }
              clearable
              getItemLabel={(item) => i18n.common.types[item]}
              placeholder={i18n.common.all}
            />
          </Wrapper>
        </FilterRow>

        <Gap />
        <InfoBox message={i18n.reports.decisions.ageInfo} thin />

        {renderResult(filteredRows, (filteredRows) => (
          <>
            <ReportDownload
              data={filteredRows}
              columns={[
                {
                  label: i18n.reports.common.careAreaName,
                  value: (row) => row.careAreaName
                },
                {
                  label: i18n.reports.common.unitName,
                  value: (row) => row.unitName
                },
                {
                  label: i18n.reports.common.unitProviderType,
                  value: (row) =>
                    i18n.reports.common.unitProviderTypes[row.providerType]
                },
                {
                  exclude: !columns.includes('daycareUnder3'),
                  label: i18n.reports.decisions.daycareUnder3,
                  value: (row) => row.daycareUnder3
                },
                {
                  exclude: !columns.includes('daycareOver3'),
                  label: i18n.reports.decisions.daycareOver3,
                  value: (row) => row.daycareOver3
                },
                {
                  exclude: !columns.includes('preschool'),
                  label: i18n.reports.decisions.preschool,
                  value: (row) => row.preschool
                },
                {
                  exclude: !columns.includes('preschoolDaycare'),
                  label: i18n.reports.decisions.preschoolDaycare,
                  value: (row) => row.preschoolDaycare
                },
                {
                  exclude: !columns.includes('connectedDaycareOnly'),
                  label: i18n.reports.decisions.connectedDaycareOnly,
                  value: (row) => row.connectedDaycareOnly
                },
                {
                  exclude: !columns.includes('preparatory'),
                  label: i18n.reports.decisions.preparatory,
                  value: (row) => row.preparatory
                },
                {
                  exclude: !columns.includes('preparatoryDaycare'),
                  label: i18n.reports.decisions.preparatoryDaycare,
                  value: (row) => row.preparatoryDaycare
                },
                {
                  exclude: !columns.includes('club'),
                  label: i18n.reports.decisions.club,
                  value: (row) => row.club
                },
                {
                  label: i18n.reports.decisions.preference1,
                  value: (row) => row.preference1
                },
                {
                  label: i18n.reports.decisions.preference2,
                  value: (row) => row.preference2
                },
                {
                  label: i18n.reports.decisions.preference3,
                  value: (row) => row.preference3
                },
                {
                  label: i18n.reports.decisions.preferenceNone,
                  value: (row) => row.preferenceNone
                },
                {
                  label: i18n.reports.decisions.total,
                  value: (row) => row.total
                }
              ]}
              filename={`${
                i18n.reports.decisions.title
              } ${filters.from?.formatIso()}-${filters.to?.formatIso()}.csv`}
            />
            <TableScrollable data-qa="report-application-table">
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.unitProviderType}</Th>
                  {columns.includes('daycareUnder3') && (
                    <Th>{i18n.reports.decisions.daycareUnder3}</Th>
                  )}
                  {columns.includes('daycareOver3') && (
                    <Th>{i18n.reports.decisions.daycareOver3}</Th>
                  )}
                  {columns.includes('preschool') && (
                    <Th>{i18n.reports.decisions.preschool}</Th>
                  )}
                  {columns.includes('preschoolDaycare') && (
                    <Th>{i18n.reports.decisions.preschoolDaycare}</Th>
                  )}
                  {columns.includes('connectedDaycareOnly') && (
                    <Th>{i18n.reports.decisions.connectedDaycareOnly}</Th>
                  )}
                  {columns.includes('preparatory') && (
                    <Th>{i18n.reports.decisions.preparatory}</Th>
                  )}
                  {columns.includes('preparatoryDaycare') && (
                    <Th>{i18n.reports.decisions.preparatoryDaycare}</Th>
                  )}
                  {columns.includes('club') && (
                    <Th>{i18n.reports.decisions.club}</Th>
                  )}
                  <Th>{i18n.reports.decisions.preference1}</Th>
                  <Th>{i18n.reports.decisions.preference2}</Th>
                  <Th>{i18n.reports.decisions.preference3}</Th>
                  <Th>{i18n.reports.decisions.preferenceNone}</Th>
                  <Th>{i18n.reports.decisions.total}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row: DecisionsReportRow) => (
                  <Tr key={row.unitId}>
                    <Td data-qa="care-area-name">{row.careAreaName}</Td>
                    <Td>
                      <Link to={`/units/${row.unitId}`}>{row.unitName}</Link>
                    </Td>
                    <Td data-qa="unit-provider-type">
                      {i18n.reports.common.unitProviderTypes[row.providerType]}
                    </Td>
                    {columns.includes('daycareUnder3') && (
                      <Td>{row.daycareUnder3}</Td>
                    )}
                    {columns.includes('daycareOver3') && (
                      <Td>{row.daycareOver3}</Td>
                    )}
                    {columns.includes('preschool') && <Td>{row.preschool}</Td>}
                    {columns.includes('preschoolDaycare') && (
                      <Td>{row.preschoolDaycare}</Td>
                    )}
                    {columns.includes('connectedDaycareOnly') && (
                      <Td>{row.connectedDaycareOnly}</Td>
                    )}
                    {columns.includes('preparatory') && (
                      <Td>{row.preparatory}</Td>
                    )}
                    {columns.includes('preparatoryDaycare') && (
                      <Td>{row.preparatoryDaycare}</Td>
                    )}
                    {columns.includes('club') && <Td>{row.club}</Td>}
                    <Td>{row.preference1}</Td>
                    <Td>{row.preference2}</Td>
                    <Td>{row.preference3}</Td>
                    <Td>{row.preferenceNone}</Td>
                    <Td>{row.total}</Td>
                  </Tr>
                ))}
              </Tbody>
              <TableFooter>
                <Tr>
                  <Td className="bold">{i18n.reports.common.total}</Td>
                  <Td />
                  <Td />
                  {columns.includes('daycareUnder3') && (
                    <Td>
                      {reducePropertySum(filteredRows, (r) => r.daycareUnder3)}
                    </Td>
                  )}
                  {columns.includes('daycareOver3') && (
                    <Td>
                      {reducePropertySum(filteredRows, (r) => r.daycareOver3)}
                    </Td>
                  )}
                  {columns.includes('preschool') && (
                    <Td>
                      {reducePropertySum(filteredRows, (r) => r.preschool)}
                    </Td>
                  )}
                  {columns.includes('preschoolDaycare') && (
                    <Td>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.preschoolDaycare
                      )}
                    </Td>
                  )}
                  {columns.includes('connectedDaycareOnly') && (
                    <Td>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.connectedDaycareOnly
                      )}
                    </Td>
                  )}
                  {columns.includes('preparatory') && (
                    <Td>
                      {reducePropertySum(filteredRows, (r) => r.preparatory)}
                    </Td>
                  )}
                  {columns.includes('preparatoryDaycare') && (
                    <Td>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.preparatoryDaycare
                      )}
                    </Td>
                  )}
                  {columns.includes('club') && (
                    <Td>{reducePropertySum(filteredRows, (r) => r.club)}</Td>
                  )}
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.preference1)}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.preference2)}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.preference3)}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.preferenceNone)}
                  </Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.total)}</Td>
                </Tr>
              </TableFooter>
            </TableScrollable>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})

const columnTypes = [
  'daycareUnder3',
  'daycareOver3',
  'preschool',
  'preschoolDaycare',
  'preparatory',
  'preparatoryDaycare',
  'connectedDaycareOnly',
  'club'
]

type ColumnType = (typeof columnTypes)[number]

const applicationTypeToColumnType = (
  applicationType: ApplicationType
): ColumnType[] => {
  switch (applicationType) {
    case 'DAYCARE':
      return ['daycareUnder3', 'daycareOver3']
    case 'PRESCHOOL':
      return [
        'preschool',
        'preschoolDaycare',
        'preparatory',
        'preparatoryDaycare',
        'connectedDaycareOnly'
      ]
    case 'CLUB':
      return ['club']
  }
}
