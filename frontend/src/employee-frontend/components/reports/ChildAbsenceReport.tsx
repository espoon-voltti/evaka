// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'

import { combine } from 'lib-common/api'
import type FiniteDateRange from 'lib-common/finite-date-range'
import { boolean, localDateRange } from 'lib-common/form/fields'
import { object, required, value } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import type {
  AreaJSON,
  Daycare,
  DaycareGroup
} from 'lib-common/generated/api-types/daycare'
import type { SortDirection } from 'lib-common/generated/api-types/invoicing'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { GroupId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  SortableTh,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from 'lib-components/layout/Table'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'

import { areasQuery } from '../../queries'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { hasGlobalAction } from '../../utils/roles'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { unitGroupsQuery, daycaresQuery } from '../unit/queries'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { childAbsenceReportQuery } from './queries'

const filterForm = object({
  range: required(localDateRange()),
  area: value<AreaJSON | null>(),
  unit: value<Daycare | null>(),
  group: value<DaycareGroup | { name: string; id: GroupId | null }>(),
  includeClosed: boolean()
})

export default React.memo(function ChildAbsenceReport() {
  const { i18n, lang } = useTranslation()

  const { user } = useContext(UserContext)
  const allOption = useMemo(
    () => ({ name: i18n.common.all, id: null }),
    [i18n.common.all]
  )

  const filters = useForm(
    filterForm,
    () => ({
      range: localDateRange.fromDates(
        LocalDate.todayInSystemTz().subDays(30),
        LocalDate.todayInSystemTz()
      ),
      area: null,
      unit: null,
      group: allOption,
      includeClosed: true
    }),
    i18n.validationErrors
  )
  const { range, area, unit, group, includeClosed } = useFormFields(filters)

  const selectArea = (selected: AreaJSON | null) =>
    filters.update((s) => ({
      ...s,
      area: selected,
      unit: null,
      group: allOption
    }))

  const selectUnit = (selected: Daycare | null) =>
    filters.update((s) => ({
      ...s,
      unit: selected,
      area: null,
      group: allOption
    }))

  const areas = useQueryResult(areasQuery())
  const units = useQueryResult(
    daycaresQuery({ includeClosed: includeClosed.state })
  )
  const groups = useQueryResult(
    unit.state
      ? unitGroupsQuery({
          daycareId: unit.state.id,
          includeClosed: includeClosed.state
        })
      : constantQuery([])
  )

  const groupOptions = useMemo(
    () => groups.map((g) => [allOption, ...orderBy(g, (item) => item.name)]),
    [groups, allOption]
  )

  const daycareOptions = useMemo(
    () => units.map((d) => orderBy(d, (item) => item.name)),
    [units]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea $opaque>
        <Title size={1}>{i18n.reports.childAbsences.title}</Title>
        {renderResult(
          combine(areas, daycareOptions, groupOptions),
          ([areaResult, unitResult, groupResult]) => (
            <>
              {hasGlobalAction(user, 'READ_CHILD_ABSENCE_REPORT_FOR_AREA') && (
                <FilterRow>
                  <FilterLabel>
                    {i18n.reports.childAbsences.filters.areaSelection.label}
                  </FilterLabel>
                  <FlexRow>
                    <Combobox
                      items={areaResult}
                      onChange={selectArea}
                      selectedItem={area.state}
                      getItemLabel={(item) => item.name}
                      placeholder={
                        i18n.reports.childAbsences.filters.areaSelection
                          .placeHolder
                      }
                      data-qa="area-select"
                      getItemDataQa={({ id }) => `area-${id}`}
                    />
                  </FlexRow>
                </FilterRow>
              )}
              <FilterRow>
                <FilterLabel>
                  {i18n.reports.childAbsences.filters.daycareSelection.label}
                </FilterLabel>
                <FlexRow>
                  <Combobox
                    items={unitResult}
                    onChange={selectUnit}
                    selectedItem={unit.state}
                    getItemLabel={(item) => item.name}
                    placeholder={i18n.filters.unitPlaceholder}
                    data-qa="unit-select"
                    getItemDataQa={({ id }) => `unit-${id}`}
                  />
                </FlexRow>
              </FilterRow>
              <FilterRow>
                <FilterLabel>
                  {i18n.reports.childAbsences.filters.range}
                </FilterLabel>
                <FlexRow>
                  <DateRangePickerF
                    bind={range}
                    locale={lang}
                    data-qa="date-range"
                  />
                </FlexRow>
              </FilterRow>
              <FilterRow>
                <FilterLabel>
                  {i18n.reports.childAbsences.filters.groupSelection.label}
                </FilterLabel>
                <FlexRow>
                  <Combobox
                    items={groupResult}
                    onChange={(selected) => group.set(selected ?? allOption)}
                    selectedItem={group.state}
                    getItemLabel={(item) => item.name}
                    placeholder={
                      i18n.reports.childAbsences.filters.groupSelection
                        .placeholder
                    }
                    data-qa="group-select"
                    getItemDataQa={({ id }) => `group-${id}`}
                  />
                </FlexRow>
              </FilterRow>
              <FilterRow>
                <FilterLabel />
                <FlexRow>
                  <CheckboxF
                    bind={includeClosed}
                    label={i18n.reports.childAbsences.filters.includeClosed}
                    data-qa="filter-by-closed"
                  />
                </FlexRow>
              </FilterRow>
              {range.isValid() && (unit.state || area.state) && (
                <ChildAbsenceGrid
                  range={range.value()}
                  area={area.state}
                  daycare={unit.state}
                  groupId={group.state.id}
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
  childId: string
  firstName: string
  lastName: string
  placementType: PlacementType
  daycareName: string
  groupName: string
  TOTAL: number
  OTHER_ABSENCE: number
  SICKLEAVE: number
  PLANNED_ABSENCE: number
  UNKNOWN_ABSENCE: number
}
type ReportColumnKey = keyof ReportDisplayRow

const ChildAbsenceGrid = ({
  range,
  area,
  daycare,
  groupId
}: {
  range: FiniteDateRange
  area: AreaJSON | null
  daycare: Daycare | null
  groupId?: GroupId | null
}) => {
  const { i18n } = useTranslation()

  const reportResult = useQueryResult(
    childAbsenceReportQuery({
      body: {
        range,
        areaId: area?.id ?? null,
        unitId: daycare?.id ?? null,
        groupId: groupId ?? null
      }
    })
  )

  const [sortColumns, setSortColumns] = useState<ReportColumnKey[]>([
    'lastName',
    'firstName',
    'childId'
  ])
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')

  const sortBy = useCallback(
    (columns: ReportColumnKey[]) => {
      if (isEqual(sortColumns, columns)) {
        setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
      } else {
        setSortColumns(columns)
        setSortDirection('ASC')
      }
    },
    [sortColumns, sortDirection]
  )

  const sortedReportResult = useMemo(
    () =>
      reportResult.map((rows) => {
        const displayRows: ReportDisplayRow[] = rows.map((row) => ({
          childId: row.childId,
          firstName: row.firstName,
          lastName: row.lastName,
          placementType: row.placementType,
          daycareName: row.daycareName,
          groupName: row.groupName,
          TOTAL:
            (row.absenceCountsByType.OTHER_ABSENCE ?? 0) +
            (row.absenceCountsByType.SICKLEAVE ?? 0) +
            (row.absenceCountsByType.PLANNED_ABSENCE ?? 0) +
            (row.absenceCountsByType.UNKNOWN_ABSENCE ?? 0),
          OTHER_ABSENCE: row.absenceCountsByType.OTHER_ABSENCE ?? 0,
          SICKLEAVE: row.absenceCountsByType.SICKLEAVE ?? 0,
          PLANNED_ABSENCE: row.absenceCountsByType.PLANNED_ABSENCE ?? 0,
          UNKNOWN_ABSENCE: row.absenceCountsByType.UNKNOWN_ABSENCE ?? 0
        }))
        return orderBy(displayRows, sortColumns, [
          sortDirection === 'ASC' ? 'asc' : 'desc'
        ])
      }),
    [reportResult, sortColumns, sortDirection]
  )

  const sortable = (column: ReportColumnKey, label: string) => {
    const columns = [
      ...new Set<ReportColumnKey>([column, 'lastName', 'firstName', 'childId'])
    ]
    return (
      <SortableTh
        sorted={isEqual(sortColumns, columns) ? sortDirection : undefined}
        onClick={() => sortBy(columns)}
      >
        {label}
      </SortableTh>
    )
  }

  return renderResult(sortedReportResult, (report) => (
    <>
      <ReportDownload
        data={report}
        columns={[
          {
            label: i18n.reports.childAbsences.firstName,
            value: (row) => row.firstName
          },
          {
            label: i18n.reports.childAbsences.lastName,
            value: (row) => row.lastName
          },
          {
            label: i18n.reports.common.placementType,
            value: (row) => i18n.placement.type[row.placementType]
          },
          {
            label: i18n.reports.childAbsences.daycareName,
            value: (row) => row.daycareName
          },
          {
            label: i18n.reports.childAbsences.groupName,
            value: (row) => row.groupName
          },
          {
            label: i18n.reports.childAbsences.total,
            value: (row) => row.TOTAL
          },
          {
            label: i18n.absences.absenceTypes.OTHER_ABSENCE,
            value: (row) => row.OTHER_ABSENCE
          },
          {
            label: i18n.absences.absenceTypes.SICKLEAVE,
            value: (row) => row.SICKLEAVE
          },
          {
            label: i18n.absences.absenceTypes.PLANNED_ABSENCE,
            value: (row) => row.PLANNED_ABSENCE
          },
          {
            label: i18n.absences.absenceTypes.UNKNOWN_ABSENCE,
            value: (row) => row.UNKNOWN_ABSENCE
          }
        ]}
        filename={`${i18n.reports.childAbsences.title} ${daycare?.name ?? area?.name} ${range.format()}.csv`}
      />
      <TableScrollable>
        <Thead>
          <Tr>
            <Th>{i18n.reports.childAbsences.firstName}</Th>
            {sortable('lastName', i18n.reports.childAbsences.lastName)}
            {sortable('placementType', i18n.reports.common.placementType)}
            {sortable('daycareName', i18n.reports.childAbsences.daycareName)}
            {sortable('groupName', i18n.reports.childAbsences.groupName)}
            {sortable(
              'TOTAL',
              `${i18n.reports.childAbsences.total} ${i18n.reports.childAbsences.days}`
            )}
            {sortable(
              'OTHER_ABSENCE',
              `${i18n.absences.absenceTypes.OTHER_ABSENCE} ${i18n.reports.childAbsences.days}`
            )}
            {sortable(
              'SICKLEAVE',
              `${i18n.absences.absenceTypes.SICKLEAVE} ${i18n.reports.childAbsences.days}`
            )}
            {sortable(
              'PLANNED_ABSENCE',
              `${i18n.absences.absenceTypes.PLANNED_ABSENCE} ${i18n.reports.childAbsences.days}`
            )}
            {sortable(
              'UNKNOWN_ABSENCE',
              `${i18n.absences.absenceTypes.UNKNOWN_ABSENCE} ${i18n.reports.childAbsences.days}`
            )}
          </Tr>
        </Thead>
        <Tbody>
          {report.length > 0 ? (
            report.map((row) => (
              <Tr key={row.childId} data-qa="child-absence-row">
                <Td data-qa="first-name-column">{row.firstName}</Td>
                <Td data-qa="last-name-column">{row.lastName}</Td>
                <Td data-qa="placement-type-column">
                  {i18n.placement.type[row.placementType]}
                </Td>
                <Td data-qa="daycare-name-column">{row.daycareName}</Td>
                <Td data-qa="group-name-column">{row.groupName}</Td>
                <Td data-qa="total-column">{row.TOTAL}</Td>
                <Td data-qa="other-absence-column">{row.OTHER_ABSENCE}</Td>
                <Td data-qa="sickleave-column">{row.SICKLEAVE}</Td>
                <Td data-qa="planned-absence-column">{row.PLANNED_ABSENCE}</Td>
                <Td data-qa="unknown-absence-column">{row.UNKNOWN_ABSENCE}</Td>
              </Tr>
            ))
          ) : (
            <Tr>
              <Td colSpan={10}>{i18n.common.noResults}</Td>
            </Tr>
          )}
        </Tbody>
      </TableScrollable>
    </>
  ))
}
