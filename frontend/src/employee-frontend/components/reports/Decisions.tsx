// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'wouter'

import type {
  DecisionReportColumnType,
  DecisionsReportRow
} from 'lib-common/generated/api-types/reports'
import { decisionReportColumnTypes } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
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
  columns: DecisionReportColumnType[]
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
    columns: []
  })
  const includedColumns = useMemo(
    () =>
      filters.columns.length > 0 ? filters.columns : decisionReportColumnTypes,
    [filters.columns]
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
          from: filters.from,
          to: filters.to,
          columns: filters.columns.length > 0 ? filters.columns : undefined
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
            <MultiSelect
              value={filters.columns}
              options={decisionReportColumnTypes}
              getOptionId={(option) => option}
              getOptionLabel={(option) => i18n.reports.decisions[option]}
              onChange={(columns) => setFilters({ ...filters, columns })}
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
                  exclude: !includedColumns.includes('daycareUnder3'),
                  label: i18n.reports.decisions.daycareUnder3,
                  value: (row) => row.daycareUnder3
                },
                {
                  exclude: !includedColumns.includes('daycareOver3'),
                  label: i18n.reports.decisions.daycareOver3,
                  value: (row) => row.daycareOver3
                },
                {
                  exclude: !includedColumns.includes('preschool'),
                  label: i18n.reports.decisions.preschool,
                  value: (row) => row.preschool
                },
                {
                  exclude: !includedColumns.includes('preschoolDaycare'),
                  label: i18n.reports.decisions.preschoolDaycare,
                  value: (row) => row.preschoolDaycare
                },
                {
                  exclude: !includedColumns.includes('connectedDaycareOnly'),
                  label: i18n.reports.decisions.connectedDaycareOnly,
                  value: (row) => row.connectedDaycareOnly
                },
                {
                  exclude: !includedColumns.includes('preparatory'),
                  label: i18n.reports.decisions.preparatory,
                  value: (row) => row.preparatory
                },
                {
                  exclude: !includedColumns.includes('preparatoryDaycare'),
                  label: i18n.reports.decisions.preparatoryDaycare,
                  value: (row) => row.preparatoryDaycare
                },
                {
                  exclude: !includedColumns.includes('club'),
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
                  {includedColumns.includes('daycareUnder3') && (
                    <Th>{i18n.reports.decisions.daycareUnder3}</Th>
                  )}
                  {includedColumns.includes('daycareOver3') && (
                    <Th>{i18n.reports.decisions.daycareOver3}</Th>
                  )}
                  {includedColumns.includes('preschool') && (
                    <Th>{i18n.reports.decisions.preschool}</Th>
                  )}
                  {includedColumns.includes('preschoolDaycare') && (
                    <Th>{i18n.reports.decisions.preschoolDaycare}</Th>
                  )}
                  {includedColumns.includes('connectedDaycareOnly') && (
                    <Th>{i18n.reports.decisions.connectedDaycareOnly}</Th>
                  )}
                  {includedColumns.includes('preparatory') && (
                    <Th>{i18n.reports.decisions.preparatory}</Th>
                  )}
                  {includedColumns.includes('preparatoryDaycare') && (
                    <Th>{i18n.reports.decisions.preparatoryDaycare}</Th>
                  )}
                  {includedColumns.includes('club') && (
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
                    {includedColumns.includes('daycareUnder3') && (
                      <Td>{row.daycareUnder3}</Td>
                    )}
                    {includedColumns.includes('daycareOver3') && (
                      <Td>{row.daycareOver3}</Td>
                    )}
                    {includedColumns.includes('preschool') && (
                      <Td>{row.preschool}</Td>
                    )}
                    {includedColumns.includes('preschoolDaycare') && (
                      <Td>{row.preschoolDaycare}</Td>
                    )}
                    {includedColumns.includes('connectedDaycareOnly') && (
                      <Td>{row.connectedDaycareOnly}</Td>
                    )}
                    {includedColumns.includes('preparatory') && (
                      <Td>{row.preparatory}</Td>
                    )}
                    {includedColumns.includes('preparatoryDaycare') && (
                      <Td>{row.preparatoryDaycare}</Td>
                    )}
                    {includedColumns.includes('club') && <Td>{row.club}</Td>}
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
                  {includedColumns.includes('daycareUnder3') && (
                    <Td>
                      {reducePropertySum(filteredRows, (r) => r.daycareUnder3)}
                    </Td>
                  )}
                  {includedColumns.includes('daycareOver3') && (
                    <Td>
                      {reducePropertySum(filteredRows, (r) => r.daycareOver3)}
                    </Td>
                  )}
                  {includedColumns.includes('preschool') && (
                    <Td>
                      {reducePropertySum(filteredRows, (r) => r.preschool)}
                    </Td>
                  )}
                  {includedColumns.includes('preschoolDaycare') && (
                    <Td>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.preschoolDaycare
                      )}
                    </Td>
                  )}
                  {includedColumns.includes('connectedDaycareOnly') && (
                    <Td>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.connectedDaycareOnly
                      )}
                    </Td>
                  )}
                  {includedColumns.includes('preparatory') && (
                    <Td>
                      {reducePropertySum(filteredRows, (r) => r.preparatory)}
                    </Td>
                  )}
                  {includedColumns.includes('preparatoryDaycare') && (
                    <Td>
                      {reducePropertySum(
                        filteredRows,
                        (r) => r.preparatoryDaycare
                      )}
                    </Td>
                  )}
                  {includedColumns.includes('club') && (
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
