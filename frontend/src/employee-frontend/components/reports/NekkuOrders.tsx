// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useRef, useState } from 'react'

import { Failure, Loading, Result, Success, wrapResult } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { NekkuOrderRow } from 'lib-common/generated/api-types/reports'
import { DaycareId, GroupId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import Title from 'lib-components/atoms/Title'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'

import { getNekkuOrderReportByUnit } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { FlexRow } from '../common/styled/containers'
import { daycaresQuery, unitGroupsQuery } from '../unit/queries'

import { FilterLabel, FilterRow } from './common'

const getNekkuOrderReportByUnitResult = wrapResult(getNekkuOrderReportByUnit)

interface NekkuOrderReportFilters {
  range: FiniteDateRange
  groupIds: GroupId[]
}

export default React.memo(function NekkuOrders() {
  const { lang, i18n } = useTranslation()

  const [unitId, setUnitId] = useState<DaycareId | null>(null)
  const [filters, setFilters] = useState<NekkuOrderReportFilters>(() => {
    const defaultDate = LocalDate.todayInSystemTz()
    return {
      range: new FiniteDateRange(
        defaultDate.startOfWeek(),
        defaultDate.endOfWeek()
      ),
      groupIds: []
    }
  })

  const tooLongRange = filters.range.end.isAfter(
    filters.range.start.addMonths(1)
  )

  const units = useQueryResult(daycaresQuery({ includeClosed: true }))
  const groups = useQueryResult(
    unitId ? unitGroupsQuery({ daycareId: unitId }) : constantQuery([])
  )

  const [report, setReport] = useState<Result<NekkuOrderRow[]>>(Success.of([]))

  const autoScrollRef = useRef<HTMLTableRowElement>(null)

  const fetchNekkuOrdersReport = useCallback(() => {
    if (tooLongRange) {
      return Promise.resolve(
        Failure.of<NekkuOrderRow[]>({
          message: 'Too long range'
        })
      )
    } else if (unitId == null) {
      return Promise.resolve(Loading.of<NekkuOrderRow[]>())
    } else {
      setReport(Loading.of())
      return getNekkuOrderReportByUnitResult({
        unitId,
        start: filters.range.start,
        end: filters.range.end,
        groupIds: filters.groupIds
      })
    }
  }, [
    tooLongRange,
    unitId,
    filters.range.start,
    filters.range.end,
    filters.groupIds
  ])

  useEffect(() => {
    scrollRefIntoView(autoScrollRef)
  }, [report])

  const filteredUnits = units
    .map((data) => data.sort((a, b) => a.name.localeCompare(b.name, lang)))
    .getOrElse([])

  const filteredGroups = groups
    .map((data) => data.sort((a, b) => a.name.localeCompare(b.name, lang)))
    .getOrElse([])

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.nekkuOrders.title}</Title>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <DateRangePicker
              start={filters.range.start}
              end={filters.range.end}
              onChange={(start, end) => {
                if (start !== null && end !== null) {
                  setFilters({
                    ...filters,
                    range: new FiniteDateRange(start, end)
                  })
                }
              }}
              locale={lang}
              required={true}
            />
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.unitName}</FilterLabel>
          <FlexRow>
            <Combobox
              items={filteredUnits}
              onChange={(selectedItem) => {
                setUnitId(selectedItem !== null ? selectedItem.id : null)
                setFilters({ ...filters, groupIds: [] })
              }}
              selectedItem={
                filteredUnits.find((unit) => unit.id === unitId) ?? null
              }
              getItemLabel={(item) => item.name}
              placeholder={i18n.filters.unitPlaceholder}
            />
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.groupName}</FilterLabel>
          <div style={{ width: '100%' }}>
            <MultiSelect
              options={filteredGroups}
              onChange={(selectedItems) =>
                setFilters({
                  ...filters,
                  groupIds: selectedItems.map((selectedItem) => selectedItem.id)
                })
              }
              value={filteredGroups.filter((group) =>
                filters.groupIds.includes(group.id)
              )}
              getOptionId={(group) => group.id}
              getOptionLabel={(group) => group.name}
              placeholder=""
              isClearable={true}
            />
          </div>
        </FilterRow>
        <FilterRow>
          <AsyncButton
            primary
            disabled={unitId === null}
            text={i18n.common.search}
            onClick={fetchNekkuOrdersReport}
            onSuccess={(newReport) => setReport(Success.of(newReport))}
            data-qa="send-button"
          />
        </FilterRow>

        {tooLongRange && (
          <div>{i18n.reports.attendanceReservation.tooLongRange}</div>
        )}
      </ContentArea>
    </Container>
  )
})
