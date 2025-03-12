// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router'
import styled from 'styled-components'

import { DecisionsReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { Gap } from 'lib-components/white-space'

import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { PeriodFilters } from '../../types/reports'
import { distinct, reducePropertySum } from '../../utils'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import { FilterLabel, FilterRow, TableFooter, TableScrollable } from './common'
import { decisionReportQuery } from './queries'

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
  const [filters, setFilters] = useState<PeriodFilters>({
    from: LocalDate.todayInSystemTz(),
    to: LocalDate.todayInSystemTz().addMonths(4)
  })

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = useCallback(
    (row: DecisionsReportRow): boolean =>
      !(
        displayFilters.careArea && row.careAreaName !== displayFilters.careArea
      ),
    [displayFilters.careArea]
  )

  const rows = useQueryResult(decisionReportQuery(filters))

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
                  label: i18n.reports.decisions.daycareUnder3,
                  value: (row) => row.daycareUnder3
                },
                {
                  label: i18n.reports.decisions.daycareOver3,
                  value: (row) => row.daycareOver3
                },
                {
                  label: i18n.reports.decisions.preschool,
                  value: (row) => row.preschool
                },
                {
                  label: i18n.reports.decisions.preschoolDaycare,
                  value: (row) => row.preschoolDaycare
                },
                {
                  label: i18n.reports.decisions.connectedDaycareOnly,
                  value: (row) => row.connectedDaycareOnly
                },
                {
                  label: i18n.reports.decisions.preparatory,
                  value: (row) => row.preparatory
                },
                {
                  label: i18n.reports.decisions.preparatoryDaycare,
                  value: (row) => row.preparatoryDaycare
                },
                {
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
              } ${filters.from.formatIso()}-${filters.to.formatIso()}.csv`}
            />
            <TableScrollable data-qa="report-application-table">
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.unitProviderType}</Th>
                  <Th>{i18n.reports.decisions.daycareUnder3}</Th>
                  <Th>{i18n.reports.decisions.daycareOver3}</Th>
                  <Th>{i18n.reports.decisions.preschool}</Th>
                  <Th>{i18n.reports.decisions.preschoolDaycare}</Th>
                  <Th>{i18n.reports.decisions.connectedDaycareOnly}</Th>
                  <Th>{i18n.reports.decisions.preparatory}</Th>
                  <Th>{i18n.reports.decisions.preparatoryDaycare}</Th>
                  <Th>{i18n.reports.decisions.club}</Th>
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
                    <Td>{row.daycareUnder3}</Td>
                    <Td>{row.daycareOver3}</Td>
                    <Td>{row.preschool}</Td>
                    <Td>{row.preschoolDaycare}</Td>
                    <Td>{row.connectedDaycareOnly}</Td>
                    <Td>{row.preparatory}</Td>
                    <Td>{row.preparatoryDaycare}</Td>
                    <Td>{row.club}</Td>
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
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.daycareUnder3)}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.daycareOver3)}
                  </Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.preschool)}</Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.preschoolDaycare)}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.connectedDaycareOnly
                    )}
                  </Td>
                  <Td>
                    {reducePropertySum(filteredRows, (r) => r.preparatory)}
                  </Td>
                  <Td>
                    {reducePropertySum(
                      filteredRows,
                      (r) => r.preparatoryDaycare
                    )}
                  </Td>
                  <Td>{reducePropertySum(filteredRows, (r) => r.club)}</Td>
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
