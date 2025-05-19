// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'

import { combine } from 'lib-common/api'
import type FiniteDateRange from 'lib-common/finite-date-range'
import type {
  Daycare,
  DaycareGroup,
  PreschoolTerm
} from 'lib-common/generated/api-types/daycare'
import type { SortDirection } from 'lib-common/generated/api-types/invoicing'
import type { GroupId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  SortableTh,
  Tbody,
  Td,
  Th,
  Thead,
  Tr
} from 'lib-components/layout/Table'

import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { preschoolTermsQuery } from '../holiday-term-periods/queries'
import { unitGroupsQuery, daycaresQuery } from '../unit/queries'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { preschoolAbsenceReportQuery } from './queries'

export default React.memo(function PreschoolAbsenceReport() {
  const { i18n } = useTranslation()

  const { roles } = useContext(UserContext)
  const allOption = useMemo(
    () => ({ name: i18n.common.all, id: null }),
    [i18n.common.all]
  )
  const [selectedUnit, setSelectedUnit] = useState<Daycare | null>(null)
  const [selectedGroup, setSelectedGroup] = useState<
    DaycareGroup | null | { name: string; id: GroupId | null }
  >(allOption)
  const [selectedTerm, setSelectedTerm] = useState<PreschoolTerm | null>(null)
  const units = useQueryResult(daycaresQuery({ includeClosed: true }))
  const groups = useQueryResult(
    selectedUnit?.id
      ? unitGroupsQuery({ daycareId: selectedUnit.id })
      : constantQuery([])
  )
  const terms = useQueryResult(preschoolTermsQuery())

  const isSvUnit = useMemo(
    () => selectedUnit?.language === 'sv',
    [selectedUnit]
  )
  const termOptions = useMemo(
    () =>
      terms.map((t) => {
        const today = LocalDate.todayInHelsinkiTz()
        const visibleTerms = t.filter((pt) => {
          if (roles.includes('ADMIN')) return true
          const termStart = isSvUnit
            ? pt.swedishPreschool.start
            : pt.finnishPreschool.start
          return (
            termStart.isEqualOrBefore(today) &&
            termStart.isAfter(today.subYears(2))
          )
        })
        return orderBy(visibleTerms, (term) =>
          isSvUnit ? term.swedishPreschool.start : term.finnishPreschool.start
        )
      }),
    [terms, isSvUnit, roles]
  )

  const groupOptions = useMemo(
    () => groups.map((g) => [allOption, ...orderBy(g, (item) => item.name)]),
    [groups, allOption]
  )

  const daycareOptions = useMemo(
    () =>
      units.map((d) => {
        const preschoolUnits = d.filter((u) => u.type.includes('PRESCHOOL'))
        return orderBy(preschoolUnits, (item) => item.name)
      }),
    [units]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.preschoolAbsences.title}</Title>
        {renderResult(
          combine(daycareOptions, termOptions, groupOptions),
          ([unitResult, termResult, groupResult]) => (
            <>
              <FilterRow>
                <FilterLabel>
                  {
                    i18n.reports.preschoolAbsences.filters.daycareSelection
                      .label
                  }
                </FilterLabel>
                <FlexRow>
                  <Combobox
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
                  {i18n.reports.preschoolAbsences.filters.preschoolTerm.label}
                </FilterLabel>
                <FlexRow>
                  <Combobox
                    items={termResult}
                    onChange={setSelectedTerm}
                    disabled={!selectedUnit}
                    selectedItem={selectedTerm}
                    getItemLabel={(item) =>
                      isSvUnit
                        ? item.swedishPreschool.format()
                        : item.finnishPreschool.format()
                    }
                    placeholder={
                      i18n.reports.preschoolAbsences.filters.preschoolTerm
                        .placeholder
                    }
                    data-qa="term-select"
                    getItemDataQa={({ id }) => `term-${id}`}
                  />
                </FlexRow>
              </FilterRow>
              <FilterRow>
                <FilterLabel>
                  {i18n.reports.preschoolAbsences.filters.groupSelection.label}
                </FilterLabel>
                <FlexRow>
                  <Combobox
                    items={groupResult}
                    onChange={setSelectedGroup}
                    selectedItem={selectedGroup}
                    getItemLabel={(item) => item.name}
                    placeholder={
                      i18n.reports.preschoolAbsences.filters.groupSelection
                        .placeholder
                    }
                  />
                </FlexRow>
              </FilterRow>
              {selectedTerm && selectedUnit && (
                <PreschoolAbsenceGrid
                  term={
                    isSvUnit
                      ? selectedTerm.swedishPreschool
                      : selectedTerm.finnishPreschool
                  }
                  daycare={selectedUnit}
                  groupId={selectedGroup?.id}
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
  TOTAL: number
  OTHER_ABSENCE: number
  SICKLEAVE: number
  UNKNOWN_ABSENCE: number
}
type ReportColumnKey = keyof ReportDisplayRow

const PreschoolAbsenceGrid = ({
  term,
  daycare,
  groupId
}: {
  term: FiniteDateRange
  daycare: Daycare
  groupId?: GroupId | null
}) => {
  const { i18n } = useTranslation()

  const today = LocalDate.todayInHelsinkiTz()
  const reportResult = useQueryResult(
    preschoolAbsenceReportQuery({
      termStart: term.start,
      termEnd: term.end.isAfter(today) ? today : term.end,
      unitId: daycare.id,
      groupId: groupId ?? null
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
          TOTAL:
            (row.hourlyTypeResults.OTHER_ABSENCE ?? 0) +
            (row.hourlyTypeResults.SICKLEAVE ?? 0) +
            (row.hourlyTypeResults.UNKNOWN_ABSENCE ?? 0),
          OTHER_ABSENCE: row.hourlyTypeResults.OTHER_ABSENCE ?? 0,
          SICKLEAVE: row.hourlyTypeResults.SICKLEAVE ?? 0,
          UNKNOWN_ABSENCE: row.hourlyTypeResults.UNKNOWN_ABSENCE ?? 0
        }))
        return orderBy(displayRows, sortColumns, [
          sortDirection === 'ASC' ? 'asc' : 'desc'
        ])
      }),
    [reportResult, sortColumns, sortDirection]
  )

  return renderResult(sortedReportResult, (report) => (
    <>
      <ReportDownload
        data={report}
        columns={[
          {
            label: i18n.reports.preschoolAbsences.firstName,
            value: (row) => row.firstName
          },
          {
            label: i18n.reports.preschoolAbsences.lastName,
            value: (row) => row.lastName
          },
          {
            label: i18n.reports.preschoolAbsences.total,
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
            label: i18n.absences.absenceTypes.UNKNOWN_ABSENCE,
            value: (row) => row.UNKNOWN_ABSENCE
          }
        ]}
        filename={`${i18n.reports.preschoolAbsences.title} ${daycare.name} ${term.format()}.csv`}
      />
      <TableScrollable>
        <Thead>
          <Tr>
            <Th>{i18n.reports.preschoolAbsences.firstName}</Th>
            <SortableTh
              sorted={
                isEqual(sortColumns, ['lastName', 'firstName', 'childId'])
                  ? sortDirection
                  : undefined
              }
              onClick={() => sortBy(['lastName', 'firstName', 'childId'])}
            >
              {i18n.reports.preschoolAbsences.lastName}
            </SortableTh>
            <SortableTh
              sorted={
                isEqual(sortColumns, [
                  'TOTAL',
                  'lastName',
                  'firstName',
                  'childId'
                ])
                  ? sortDirection
                  : undefined
              }
              onClick={() =>
                sortBy(['TOTAL', 'lastName', 'firstName', 'childId'])
              }
            >{`${i18n.reports.preschoolAbsences.total} ${i18n.reports.preschoolAbsences.hours}`}</SortableTh>
            <SortableTh
              sorted={
                isEqual(sortColumns, [
                  'OTHER_ABSENCE',
                  'lastName',
                  'firstName',
                  'childId'
                ])
                  ? sortDirection
                  : undefined
              }
              onClick={() =>
                sortBy(['OTHER_ABSENCE', 'lastName', 'firstName', 'childId'])
              }
            >{`${i18n.absences.absenceTypes.OTHER_ABSENCE} ${i18n.reports.preschoolAbsences.hours}`}</SortableTh>
            <SortableTh
              sorted={
                isEqual(sortColumns, [
                  'SICKLEAVE',
                  'lastName',
                  'firstName',
                  'childId'
                ])
                  ? sortDirection
                  : undefined
              }
              onClick={() =>
                sortBy(['SICKLEAVE', 'lastName', 'firstName', 'childId'])
              }
            >{`${i18n.absences.absenceTypes.SICKLEAVE} ${i18n.reports.preschoolAbsences.hours}`}</SortableTh>
            <SortableTh
              sorted={
                isEqual(sortColumns, [
                  'UNKNOWN_ABSENCE',
                  'lastName',
                  'firstName',
                  'childId'
                ])
                  ? sortDirection
                  : undefined
              }
              onClick={() =>
                sortBy(['UNKNOWN_ABSENCE', 'lastName', 'firstName', 'childId'])
              }
            >{`${i18n.absences.absenceTypes.UNKNOWN_ABSENCE} ${i18n.reports.preschoolAbsences.hours}`}</SortableTh>
          </Tr>
        </Thead>
        <Tbody>
          {report.length > 0 ? (
            report.map((row, rowIndex) => (
              <Tr key={`${rowIndex}`} data-qa="preschool-absence-row">
                <Td data-qa="first-name-column">{row.firstName}</Td>
                <Td data-qa="last-name-column">{row.lastName}</Td>
                <Td data-qa="total-column">
                  {row.UNKNOWN_ABSENCE + row.OTHER_ABSENCE + row.SICKLEAVE}
                </Td>
                <Td data-qa="other-absence-column">{row.OTHER_ABSENCE}</Td>
                <Td data-qa="sickleave-column">{row.SICKLEAVE}</Td>
                <Td data-qa="unknown-absence-column">{row.UNKNOWN_ABSENCE}</Td>
              </Tr>
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
