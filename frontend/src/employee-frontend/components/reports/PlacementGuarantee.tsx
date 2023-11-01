// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import orderBy from 'lodash/orderBy'
import sortBy from 'lodash/sortBy'
import React, { useCallback, useMemo, useState } from 'react'

import { PlacementGuaranteeReportFilters } from 'employee-frontend/api/reports'
import { useTranslation } from 'employee-frontend/state/i18n'
import FiniteDateRange from 'lib-common/finite-date-range'
import { PlacementGuaranteeReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { SortableTh, Tbody, Td, Thead, Tr } from 'lib-components/layout/Table'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'

import { unitsQuery } from '../unit/queries'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { placementGuaranteeReportQuery } from './queries'

const initialFilters: PlacementGuaranteeReportFilters = {
  date: LocalDate.todayInHelsinkiTz(),
  unitId: null
}

export default React.memo(function PlacementGuarantee() {
  const { i18n } = useTranslation()
  const [filters, setFilters] =
    useState<PlacementGuaranteeReportFilters>(initialFilters)

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.placementGuarantee.title}</Title>
        <PlacementGuaranteeFilters filters={filters} setFilters={setFilters} />
        <PlacementGuaranteeData filters={filters} />
      </ContentArea>
    </Container>
  )
})

const PlacementGuaranteeFilters = ({
  filters,
  setFilters
}: {
  filters: PlacementGuaranteeReportFilters
  setFilters: (filters: PlacementGuaranteeReportFilters) => void
}) => {
  const { i18n, lang } = useTranslation()
  const unitsResult = useQueryResult(unitsQuery())
  const sortedUnits = useMemo(
    () => unitsResult.map((units) => sortBy(units, (unit) => unit.name)),
    [unitsResult]
  )
  return (
    <>
      <FilterRow>
        <FilterLabel>{i18n.reports.common.date}</FilterLabel>
        <DatePicker
          date={filters.date}
          onChange={(selectedDate) => {
            if (selectedDate !== null) {
              setFilters({ ...filters, date: selectedDate })
            }
          }}
          hideErrorsBeforeTouched
          locale={lang}
          data-qa="date-picker"
        />
      </FilterRow>
      <FilterRow>
        <FilterLabel>{i18n.reports.common.unitName}</FilterLabel>
        {sortedUnits.mapAll({
          loading: () => <Loader />,
          failure: () => <ErrorSegment />,
          success: (units) => (
            <Combobox
              items={units}
              onChange={(selectedItem) => {
                setFilters({
                  ...filters,
                  unitId: selectedItem !== null ? selectedItem.id : null
                })
              }}
              selectedItem={
                units.find((unit) => unit.id === filters.unitId) ?? null
              }
              getItemLabel={(item) => item.name}
              placeholder={i18n.filters.unitPlaceholder}
              clearable
              data-qa="unit-selector"
            />
          )
        })}
      </FilterRow>
    </>
  )
}

const PlacementGuaranteeData = ({
  filters
}: {
  filters: PlacementGuaranteeReportFilters
}) => {
  const reportResult = useQueryResult(placementGuaranteeReportQuery(filters))
  return (
    <>
      {reportResult.mapAll({
        loading: () => <Loader />,
        failure: () => <ErrorSegment />,
        success: (rows) => (
          <PlacementGuaranteeTable date={filters.date} rows={rows} />
        )
      })}
    </>
  )
}

interface PlacementGuaranteeReportSort {
  columns: (keyof PlacementGuaranteeReportRow)[]
  direction: 'ASC' | 'DESC'
}

const PlacementGuaranteeTable = ({
  date,
  rows
}: {
  date: LocalDate
  rows: PlacementGuaranteeReportRow[]
}) => {
  const { i18n } = useTranslation()
  const [sort, setSort] = useState<PlacementGuaranteeReportSort>({
    columns: ['unitName'],
    direction: 'ASC'
  })
  const sorted = useCallback(
    (columns: (keyof PlacementGuaranteeReportRow)[]) =>
      isEqual(sort.columns, columns) ? sort.direction : undefined,
    [sort]
  )
  const sortBy = useCallback(
    (columns: (keyof PlacementGuaranteeReportRow)[]) => {
      setSort({
        columns,
        direction:
          isEqual(sort.columns, columns) && sort.direction === 'ASC'
            ? 'DESC'
            : 'ASC'
      })
    },
    [sort]
  )

  const sortedRows = orderBy(rows, sort.columns, [
    sort.direction === 'ASC' ? 'asc' : 'desc'
  ])
  return (
    <>
      <ReportDownload
        data={sortedRows}
        headers={[
          {
            label: i18n.reports.common.careAreaName,
            key: 'areaName'
          },
          {
            label: i18n.reports.common.unitName,
            key: 'unitName'
          },
          { label: i18n.reports.common.lastName, key: 'childLastName' },
          { label: i18n.reports.common.firstName, key: 'childFirstName' },
          {
            label: i18n.reports.common.startDate,
            key: 'placementStartDate'
          },
          { label: i18n.reports.common.endDate, key: 'placementEndDate' }
        ]}
        filename={`Varhaiskasvatuspaikkatakuu ${date.formatIso()}.csv`}
      />
      <TableScrollable>
        <Thead>
          <Tr>
            <SortableTh
              sorted={sorted(['areaName'])}
              onClick={() => sortBy(['areaName'])}
            >
              {i18n.reports.common.careAreaName}
            </SortableTh>
            <SortableTh
              sorted={sorted(['unitName'])}
              onClick={() => sortBy(['unitName'])}
            >
              {i18n.reports.common.unitName}
            </SortableTh>
            <SortableTh
              sorted={sorted(['childLastName', 'childFirstName'])}
              onClick={() => sortBy(['childLastName', 'childFirstName'])}
            >
              {i18n.reports.common.childName}
            </SortableTh>
            <SortableTh
              sorted={sorted(['placementStartDate', 'placementEndDate'])}
              onClick={() => sortBy(['placementStartDate', 'placementEndDate'])}
            >
              {i18n.reports.common.period}
            </SortableTh>
          </Tr>
        </Thead>
        <Tbody>
          {sortedRows.length > 0 ? (
            sortedRows.map((row) => (
              <Tr
                key={`${row.childId}-${row.placementStartDate.formatIso()}`}
                data-qa="placement-guarantee-row"
              >
                <Td data-qa="area-name">{row.areaName}</Td>
                <Td data-qa="unit-name">{row.unitName}</Td>
                <Td data-qa="child-name">
                  {row.childLastName}, {row.childFirstName}
                </Td>
                <Td data-qa="placement-period">
                  {new FiniteDateRange(
                    row.placementStartDate,
                    row.placementEndDate
                  ).format()}
                </Td>
              </Tr>
            ))
          ) : (
            <Tr>
              <Td colSpan={4}>{i18n.common.noResults}</Td>
            </Tr>
          )}
        </Tbody>
      </TableScrollable>
    </>
  )
}
