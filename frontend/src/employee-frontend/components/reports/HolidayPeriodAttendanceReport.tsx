// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { Daycare } from 'lib-common/generated/api-types/daycare'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { ChildWithName } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { formatFirstName } from 'lib-common/names'
import { useQueryResult } from 'lib-common/query'
import { capitalizeFirstLetter } from 'lib-common/string'
import Title from 'lib-components/atoms/Title'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Button } from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import { P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faChevronRight, faChevronDown } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { holidayPeriodsQuery } from '../holiday-term-periods/queries'
import { unitsQuery } from '../unit/queries'

import { FilterLabel, FilterRow, TableScrollable } from './common'
import { holidayPeriodAttendanceReportQuery } from './queries'

export default React.memo(function HolidayPeriodAttendanceReport() {
  const { i18n } = useTranslation()

  const [selectedUnit, setSelectedUnit] = useState<Daycare | null>(null)
  const [selectedPeriod, setSelectedPeriod] = useState<HolidayPeriod | null>(
    null
  )

  const units = useQueryResult(unitsQuery({ includeClosed: false }))
  const periods = useQueryResult(holidayPeriodsQuery())

  const periodOptions = useMemo(
    () =>
      periods.map((g) =>
        orderBy(
          g.filter((p) => p.period.end > LocalDate.todayInHelsinkiTz()),
          (item) => item.period
        )
      ),
    [periods]
  )

  const daycareOptions = useMemo(
    () =>
      units.map((d) => {
        return orderBy(d, (item) => item.name)
      }),
    [units]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.holidayPeriodAttendance.title}</Title>
        {renderResult(
          combine(daycareOptions, periodOptions),
          ([unitResult, periodResult]) => (
            <>
              <FilterRow>
                <FilterLabel>
                  {i18n.reports.holidayPeriodAttendance.unitFilter}
                </FilterLabel>
                <FlexRow>
                  <Combobox
                    fullWidth
                    items={unitResult}
                    onChange={setSelectedUnit}
                    selectedItem={selectedUnit}
                    getItemLabel={(item) => item.name}
                    placeholder={i18n.filters.unitPlaceholder}
                    data-qa="unit-select"
                    getItemDataQa={({ id }) => `unit-${id}`}
                  />
                </FlexRow>
              </FilterRow>
              <FilterRow>
                <FilterLabel>
                  {i18n.reports.holidayPeriodAttendance.periodFilter}
                </FilterLabel>
                <FlexRow>
                  <Combobox
                    fullWidth
                    items={periodResult}
                    onChange={setSelectedPeriod}
                    selectedItem={selectedPeriod}
                    getItemLabel={(item) => item.period.format()}
                    placeholder={
                      i18n.reports.holidayPeriodAttendance
                        .periodFilterPlaceholder
                    }
                    data-qa="period-select"
                    getItemDataQa={({ id }) => `term-${id}`}
                  />
                </FlexRow>
              </FilterRow>
              <Gap />
              {selectedPeriod && selectedUnit && (
                <HolidayPeriodAttendanceReportGrid
                  daycare={selectedUnit}
                  period={selectedPeriod}
                />
              )}
            </>
          )
        )}
      </ContentArea>
    </Container>
  )
})

type ReportDisplayRow = {
  date: LocalDate
  presentChildren: ChildWithName[]
  assistanceChildren: ChildWithName[]
  coefficientSum: number
  requiredStaffCount: number
  absentCount: number
  noResponseChildren: ChildWithName[]
}

const HolidayPeriodAttendanceReportGrid = React.memo(
  function HolidayPeriodAttendanceReportGrid({
    daycare,
    period
  }: {
    daycare: Daycare
    period: HolidayPeriod
  }) {
    const { i18n } = useTranslation()
    const reportResult = useQueryResult(
      holidayPeriodAttendanceReportQuery({
        unitId: daycare.id,
        periodId: period.id
      })
    )
    const [expandingInfo, setExpandingInfo] = useState<boolean>(false)

    const sortedReportResult = useMemo(
      () =>
        reportResult.map((rows) => {
          const displayRows: ReportDisplayRow[] = rows.map((row) => ({
            date: row.date,
            presentChildren: orderChildren(row.presentChildren),
            assistanceChildren: orderChildren(row.assistanceChildren),
            coefficientSum: row.presentOccupancyCoefficient,
            requiredStaffCount: row.requiredStaff,
            absentCount: row.absentCount,
            noResponseChildren: orderChildren(row.noResponseChildren)
          }))
          return orderBy(displayRows, ['date'], ['asc'])
        }),
      [reportResult]
    )

    return renderResult(sortedReportResult, (report) => (
      <>
        {expandingInfo && (
          <ExpandingInfoBox
            info={i18n.reports.holidayPeriodAttendance.occupancyColumnInfo}
            close={() => setExpandingInfo(false)}
          />
        )}
        <TableScrollable>
          <Thead>
            <Tr>
              <TinyTh />
              <ShortTh>
                {i18n.reports.holidayPeriodAttendance.dateColumn}
              </ShortTh>
              <Th>{i18n.reports.holidayPeriodAttendance.presentColumn}</Th>
              <Th>{i18n.reports.holidayPeriodAttendance.assistanceColumn}</Th>
              <ShortTh>
                {i18n.reports.holidayPeriodAttendance.occupancyColumn}
                <InfoButton
                  onClick={() => setExpandingInfo(true)}
                  aria-label={i18n.common.openExpandingInfo}
                />
              </ShortTh>
              <ShortTh>
                {i18n.reports.holidayPeriodAttendance.staffColumn}
              </ShortTh>
              <ShortTh>
                {i18n.reports.holidayPeriodAttendance.absentColumn}
              </ShortTh>
              <Th>{i18n.reports.holidayPeriodAttendance.noResponseColumn}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {report.length > 0 ? (
              report.map((row, rowIndex) => (
                <DailyPeriodAttendanceRow row={row} key={rowIndex} />
              ))
            ) : (
              <Tr>
                <Td colSpan={6}>{i18n.common.noResults}</Td>
              </Tr>
            )}
          </Tbody>
        </TableScrollable>
      </>
    ))
  }
)

type TooltipProps = { fullText: string }
const TooltippedChildListItem = React.memo(function TooltippedChildListItem(
  props: TooltipProps
) {
  return (
    <div>
      <Tooltip tooltip={props.fullText}>
        <ChildListLabel data-qa="child-name">{props.fullText}</ChildListLabel>
      </Tooltip>
    </div>
  )
})

type StatRowProps = { row: ReportDisplayRow }
const DailyPeriodAttendanceRow = React.memo(function DailyPeriodAttendanceRow(
  props: StatRowProps
) {
  const [isExpanded, setIsExpanded] = useState<boolean>(false)
  const { lang, i18n } = useTranslation()
  const { row } = props
  return (
    <Tr data-qa="holiday-period-attendance-row">
      <TinyTd data-qa="resize-column">
        <Button
          text=""
          aria-label={i18n.reports.holidayPeriodAttendance.showMoreButton}
          onClick={() => setIsExpanded(!isExpanded)}
          icon={isExpanded ? faChevronDown : faChevronRight}
          appearance="link"
        />
      </TinyTd>
      <ShortTd data-qa="date-column">
        {capitalizeFirstLetter(row.date.format('EEEEEE dd.MM.yyyy', lang))}
      </ShortTd>
      <Td data-qa="present-children-column">
        <ChildList list={row.presentChildren} showFull={isExpanded} />
      </Td>
      <Td data-qa="assistance-children-column">
        <ChildList list={row.assistanceChildren} showFull={isExpanded} />
      </Td>
      <ShortTd data-qa="coefficient-sum-column">{row.coefficientSum}</ShortTd>
      <ShortTd data-qa="staff-count-column">{row.requiredStaffCount}</ShortTd>
      <ShortTd data-qa="absence-count-column">{row.absentCount}</ShortTd>
      <Td data-qa="no-response-children-column">
        <ChildList list={row.noResponseChildren} showFull={isExpanded} />
      </Td>
    </Tr>
  )
})

type ChildListProps = { list: ChildWithName[]; showFull: boolean }
const ChildList = React.memo(function ChildList({
  list,
  showFull
}: ChildListProps) {
  const { i18n } = useTranslation()
  const shortMaxLength = 5
  const extraCount = list.length - shortMaxLength
  const visibleList = useMemo(
    () =>
      !showFull && list.length > shortMaxLength
        ? list.slice(0, shortMaxLength)
        : list,
    [list, showFull]
  )

  return (
    <CellWrapper>
      {visibleList.map((c, i) => (
        <TooltippedChildListItem
          key={c.id}
          fullText={`${i + 1}. ${c.lastName} ${formatFirstName(c)}`}
        />
      ))}
      {!showFull && extraCount > 0 && (
        <P
          noMargin
        >{`+${extraCount} ${i18n.reports.holidayPeriodAttendance.moreText}`}</P>
      )}
    </CellWrapper>
  )
})

const orderChildren = (children: ChildWithName[]) =>
  orderBy(children, [(c) => c.lastName, (c) => c.firstName, (c) => c.firstName])

const CellWrapper = styled.div`
  display: flex;
  flex-direction: column;
`

const ChildListLabel = styled.p`
  margin: 0;
  white-space: nowrap;
  text-overflow: ellipsis;
  max-width: 200px;
  overflow: hidden;
`

const ShortTd = styled(Td)`
  max-width: 100px;
`

const ShortTh = styled(Th)`
  max-width: 90px;
`

const TinyTh = styled(Th)`
  max-width: 50px;
`
const TinyTd = styled(Td)`
  max-width: 50px;
`
