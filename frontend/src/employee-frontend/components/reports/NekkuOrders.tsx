// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'

import type { Result } from 'lib-common/api'
import { Failure, Loading, Success, wrapResult } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import type { NekkuOrderRow } from 'lib-common/generated/api-types/reports'
import type { DaycareId, GroupId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'

import { getNekkuOrderReportByUnit } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { daycaresQuery, unitGroupsQuery } from '../unit/queries'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'

const getNekkuOrderReportByUnitResult = wrapResult(getNekkuOrderReportByUnit)

type mealTimeOptions = 'BREAKFAST' | 'LUNCH' | 'SNACK' | 'DINNER' | 'SUPPER'

type mealTypeOptions = 'DEFAULT' | 'VEGAN' | 'VEGETABLE'

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

  const sortedUnits = useMemo(
    () =>
      units
        .map((data) =>
          data
            .filter(
              (value) =>
                (value.openingDate == null ||
                  value.openingDate <= filters.range.end) &&
                (value.closingDate == null ||
                  filters.range.start <= value.closingDate)
            )
            .sort((a, b) => a.name.localeCompare(b.name, lang))
        )
        .getOrElse([]),
    [filters, units, lang]
  )

  const sortedGroups = useMemo(
    () =>
      groups
        .map((data) =>
          data
            .filter(
              (value) =>
                (value.startDate == null ||
                  value.startDate <= filters.range.end) &&
                (value.endDate == null || filters.range.start <= value.endDate)
            )
            .sort((a, b) => a.name.localeCompare(b.name, lang))
        )
        .getOrElse([]),
    [filters, groups, lang]
  )

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
              items={sortedUnits}
              onChange={(selectedItem) => {
                setUnitId(selectedItem !== null ? selectedItem.id : null)
                setFilters({ ...filters, groupIds: [] })
              }}
              selectedItem={
                sortedUnits.find((unit) => unit.id === unitId) ?? null
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
              options={sortedGroups}
              onChange={(selectedItems) =>
                setFilters({
                  ...filters,
                  groupIds: selectedItems.map((selectedItem) => selectedItem.id)
                })
              }
              value={sortedGroups.filter((group) =>
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

        {tooLongRange && <div>{i18n.reports.nekkuOrders.tooLongRange}</div>}
        {renderResult(report, (report) => (
          <>
            <ReportDownload
              data={report}
              columns={[
                {
                  label: i18n.reports.common.date,
                  value: (row) => row.date.toString()
                },
                {
                  label: i18n.reports.common.groupName,
                  value: (row) => row.groupName
                },
                {
                  label: i18n.reports.nekkuOrders.sku,
                  value: (row) => row.sku
                },
                {
                  label: i18n.reports.nekkuOrders.quantity,
                  value: (row) => row.quantity
                },
                {
                  label: i18n.reports.nekkuOrders.mealTime,
                  value: (row) =>
                    row.mealTime
                      .map((mealTimeValue) => {
                        return i18n.reports.nekkuOrders.mealTimeValues[
                          mealTimeValue as mealTimeOptions
                        ]
                      })
                      .join(', ')
                },
                {
                  label: i18n.reports.nekkuOrders.mealType,
                  value: (row) =>
                    i18n.reports.nekkuOrders.mealTypeValues[
                      (row.mealType ?? 'DEFAULT') as mealTypeOptions
                    ]
                },
                {
                  label: i18n.reports.nekkuOrders.specialDiets,
                  value: (row) => row.specialDiets
                },
                {
                  label: i18n.reports.nekkuOrders.nekkuOrderInfo,
                  value: (row) => row.nekkuOrderInfo
                }
              ]}
              filename={`${i18n.reports.nekkuOrders.title}.csv`}
            />
            <TableScrollable data-qa="report-nekkuorders">
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.date}</Th>
                  <Th>{i18n.reports.common.groupName}</Th>
                  <Th>{i18n.reports.nekkuOrders.sku}</Th>
                  <Th>{i18n.reports.nekkuOrders.quantity}</Th>
                  <Th>{i18n.reports.nekkuOrders.mealTime}</Th>
                  <Th>{i18n.reports.nekkuOrders.mealType}</Th>
                  <Th>{i18n.reports.nekkuOrders.specialDiets}</Th>
                  <Th>{i18n.reports.nekkuOrders.nekkuOrderInfo}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {report.map((row, index) => (
                  <Tr key={`${index}`} data-qa="data-rows">
                    <Td>{row.date.toString()}</Td>
                    <Td>{row.groupName}</Td>
                    <Td>{row.sku}</Td>
                    <Td>{row.quantity}</Td>
                    <Td>
                      {row.mealTime
                        .map((mealTimeValue) => {
                          return i18n.reports.nekkuOrders.mealTimeValues[
                            mealTimeValue as mealTimeOptions
                          ]
                        })
                        .join(', ')}
                    </Td>
                    <Td>
                      {
                        i18n.reports.nekkuOrders.mealTypeValues[
                          (row.mealType ?? 'DEFAULT') as mealTypeOptions
                        ]
                      }
                    </Td>
                    <Td>{row.specialDiets}</Td>
                    <Td>{row.nekkuOrderInfo}</Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
