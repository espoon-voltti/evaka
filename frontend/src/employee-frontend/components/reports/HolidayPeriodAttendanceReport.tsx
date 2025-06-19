// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import type { Daycare } from 'lib-common/generated/api-types/daycare'
import type { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import type {
  ChildWithName,
  HolidayReportRow
} from 'lib-common/generated/api-types/reports'
import type {
  DaycareId,
  GroupId,
  HolidayPeriodId
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { capitalizeFirstLetter } from 'lib-common/string'
import Title from 'lib-components/atoms/Title'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Button } from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
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
import { holidayPeriodsQuery } from '../holiday-term-periods/queries'
import { unitGroupsQuery, daycaresQuery } from '../unit/queries'

import { FilterLabel, FilterRow, TableScrollable } from './common'
import { holidayPeriodAttendanceReportQuery } from './queries'

interface ReportQueryParams {
  unitId: DaycareId
  periodId: HolidayPeriodId
  groupIds: GroupId[] | null
}

export default React.memo(function HolidayPeriodAttendanceReport() {
  const { i18n } = useTranslation()

  const [selectedUnit, setSelectedUnit] = useState<Daycare | null>(null)
  const [selectedPeriod, setSelectedPeriod] = useState<HolidayPeriod | null>(
    null
  )
  const [selectedGroups, setSelectedGroups] = useState<GroupId[] | null>(null)

  const units = useQueryResult(daycaresQuery({ includeClosed: false }))
  const periods = useQueryResult(holidayPeriodsQuery())

  const periodOptions = useMemo(
    () =>
      periods.map((g) =>
        orderBy(
          g.filter((p) => p.period.end >= LocalDate.todayInHelsinkiTz()),
          (item) => item.period
        )
      ),
    [periods]
  )

  const daycareOptions = useMemo(
    () =>
      units.map((d) => {
        return orderBy(
          d.filter((u) => u.enabledPilotFeatures.includes('RESERVATIONS')),
          (item) => item.name
        )
      }),
    [units]
  )

  const groupsResult = useQueryResult(
    selectedUnit && selectedPeriod
      ? unitGroupsQuery({
          daycareId: selectedUnit.id,
          from: selectedPeriod.period.start,
          to: selectedPeriod.period.end
        })
      : constantQuery([])
  )

  const groupOptions = useMemo(
    () =>
      groupsResult.map((g) => {
        return orderBy(g, (item) => item.name)
      }),
    [groupsResult]
  )

  const [activeParams, setActiveParams] = useState<ReportQueryParams | null>(
    null
  )

  const reportResult = useQueryResult(
    activeParams
      ? holidayPeriodAttendanceReportQuery(activeParams)
      : constantQuery([])
  )

  const fetchResults = () => {
    if (selectedUnit && selectedPeriod) {
      setActiveParams({
        unitId: selectedUnit.id,
        groupIds: selectedGroups,
        periodId: selectedPeriod.id
      })
    }
  }

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

                <Combobox
                  fullWidth
                  items={unitResult}
                  onChange={(item) => {
                    setSelectedGroups([])
                    setSelectedUnit(item)
                  }}
                  selectedItem={selectedUnit}
                  getItemLabel={(item) => item.name}
                  placeholder={i18n.filters.unitPlaceholder}
                  data-qa="unit-select"
                  getItemDataQa={({ id }) => `unit-${id}`}
                />
              </FilterRow>
              <FilterRow>
                <FilterLabel>
                  {i18n.reports.holidayPeriodAttendance.periodFilter}
                </FilterLabel>

                <Combobox
                  fullWidth
                  items={periodResult}
                  onChange={(item) => {
                    setSelectedGroups([])
                    setSelectedPeriod(item)
                  }}
                  selectedItem={selectedPeriod}
                  getItemLabel={(item) => item.period.format()}
                  placeholder={
                    i18n.reports.holidayPeriodAttendance.periodFilterPlaceholder
                  }
                  data-qa="period-select"
                  getItemDataQa={({ id }) => `term-${id}`}
                />
              </FilterRow>
            </>
          )
        )}

        {renderResult(groupOptions, (groups) => (
          <FilterRow>
            <FilterLabel>
              {i18n.reports.holidayPeriodAttendance.groupFilter}
            </FilterLabel>
            <div style={{ width: '100%' }}>
              <MultiSelect
                isClearable
                options={groups}
                getOptionId={(item) => item.id}
                getOptionLabel={(item) => item.name}
                placeholder={
                  i18n.reports.holidayPeriodAttendance.groupFilterPlaceholder
                }
                onChange={(item) => setSelectedGroups(item.map((i) => i.id))}
                value={groups.filter(
                  (g) => selectedGroups?.includes(g.id) === true
                )}
                data-qa="group-select"
              />
            </div>
          </FilterRow>
        ))}
        <Gap />
        <FilterRow>
          <Button
            primary
            disabled={!(selectedUnit && selectedPeriod)}
            text={i18n.common.search}
            onClick={fetchResults}
            data-qa="send-button"
          />
        </FilterRow>
        <Gap />
        {renderResult(reportResult, (report) => (
          <HolidayPeriodAttendanceReportGrid reportResult={report} />
        ))}
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
    reportResult
  }: {
    reportResult: HolidayReportRow[]
  }) {
    const { i18n } = useTranslation()

    const [expandingInfo, infoExpansion] = useBoolean(false)

    const sortedReportResult = useMemo(() => {
      const displayRows: ReportDisplayRow[] = reportResult.map((row) => ({
        date: row.date,
        presentChildren: orderChildren(row.presentChildren),
        assistanceChildren: orderChildren(row.assistanceChildren),
        coefficientSum: row.presentOccupancyCoefficient,
        requiredStaffCount: row.requiredStaff,
        absentCount: row.absentCount,
        noResponseChildren: orderChildren(row.noResponseChildren)
      }))
      return orderBy(displayRows, [(r) => r.date], ['asc'])
    }, [reportResult])
    return (
      <>
        {expandingInfo && (
          <ExpandingInfoBox
            info={i18n.reports.holidayPeriodAttendance.occupancyColumnInfo}
            close={() => infoExpansion.off()}
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
                  onClick={() => infoExpansion.on()}
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
            {sortedReportResult.length > 0 ? (
              sortedReportResult.map((row) => (
                <DailyPeriodAttendanceRow
                  row={row}
                  key={row.date.formatIso()}
                />
              ))
            ) : (
              <Tr>
                <Td colSpan={8}>{i18n.common.noResults}</Td>
              </Tr>
            )}
          </Tbody>
        </TableScrollable>
      </>
    )
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
  const [isExpanded, expansion] = useBoolean(false)
  const { lang, i18n } = useTranslation()
  const { row } = props
  return (
    <Tr data-qa="holiday-period-attendance-row">
      <TinyTd data-qa="resize-column">
        <Button
          text=""
          aria-label={i18n.common.showMore}
          onClick={() => expansion.toggle()}
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
      <ShortTd data-qa="coefficient-sum-column">
        {row.coefficientSum.toFixed(2).replace('.', ',')}
      </ShortTd>
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
          fullText={`${i + 1}. ${formatPersonName(c, 'Last FirstFirst')}`}
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
  orderBy(children, [(c) => c.lastName, (c) => c.firstName, (c) => c.id])

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
  max-width: 90px;
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
