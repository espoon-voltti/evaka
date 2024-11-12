// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import orderBy from 'lodash/orderBy'
import range from 'lodash/range'
import uniqBy from 'lodash/uniqBy'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { number } from 'lib-common/form/fields'
import { object, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { StartingPlacementsRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { Arg0 } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  SortableTh,
  SortDirection,
  Tbody,
  Td,
  Thead,
  Tr
} from 'lib-components/layout/Table'

import ReportDownload from '../../components/reports/ReportDownload'
import { getStartingPlacementsReport } from '../../generated/api-clients/reports'
import { Translations, useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'
import { startingPlacementsReportQuery } from './queries'

type PlacementsReportFilters = Arg0<typeof getStartingPlacementsReport>

const StyledTd = styled(Td)`
  white-space: normal;
  max-width: 650px;
`

const Wrapper = styled.div`
  width: 100%;
`

const monthOptions = range(1, 13)
const yearOptions = range(
  LocalDate.todayInSystemTz().year + 1,
  LocalDate.todayInSystemTz().year - 4,
  -1
)

const filterForm = object({
  month: required(number()),
  year: required(number())
})

function getFilename(i18n: Translations, year: number, month: number) {
  const time = LocalDate.of(year, month, 1).formatExotic('yyyy-MM')
  return `${i18n.reports.startingPlacements.reportFileName}-${time}.csv`
}

interface DisplayFilters {
  careArea: string
  unit: string
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: '',
  unit: ''
}

export default React.memo(function StartingPlacementsReport() {
  const { i18n } = useTranslation()
  const today = LocalDate.todayInSystemTz()

  const filters = useForm(
    filterForm,
    () => ({
      year: today.year,
      month: today.month
    }),
    i18n.validationErrors
  )

  const { month, year } = useFormFields(filters)

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.startingPlacements.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <Wrapper>
              <Combobox
                items={monthOptions}
                selectedItem={month.value()}
                onChange={(newMonth) => {
                  if (newMonth !== null) {
                    month.set(newMonth)
                  }
                }}
                placeholder={i18n.common.month}
                getItemLabel={(month) => i18n.datePicker.months[month - 1]}
              />
            </Wrapper>
            <Wrapper>
              <Combobox
                items={yearOptions}
                selectedItem={year.value()}
                onChange={(newYear) => {
                  if (newYear !== null) {
                    year.set(newYear)
                  }
                }}
                placeholder={i18n.common.year}
              />
            </Wrapper>
          </FlexRow>
        </FilterRow>
        <StartingPlacements filters={filters.value()} />
      </ContentArea>
    </Container>
  )
})

type SortColumn = keyof StartingPlacementsRow | 'childName'
const StartingPlacements = React.memo(function StartingPlacements({
  filters
}: {
  filters: PlacementsReportFilters
}) {
  const { i18n } = useTranslation()
  const result = useQueryResult(startingPlacementsReportQuery(filters))
  useEffect(() => {
    setDisplayFilters(emptyDisplayFilters)
  }, [filters])

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const areaFilter = (row: StartingPlacementsRow): boolean =>
    !(displayFilters.careArea && row.careAreaName !== displayFilters.careArea)
  const displayFilter = (row: StartingPlacementsRow): boolean =>
    areaFilter(row) &&
    !(displayFilters.unit && row.unitName !== displayFilters.unit)

  const [sort, setSort] = useState<{
    columns: SortColumn[]
    direction: SortDirection
  }>({
    columns: ['careAreaName', 'unitName', 'childName'],
    direction: 'ASC'
  })

  const sortBy = useCallback(
    (columns: SortColumn[]) => {
      if (isEqual(sort.columns, columns)) {
        setSort({
          columns,
          direction: sort.direction === 'ASC' ? 'DESC' : 'ASC'
        })
      } else {
        setSort({ columns, direction: 'ASC' })
      }
    },
    [sort.columns, sort.direction]
  )
  const sorted = useCallback(
    (columns: SortColumn[]) =>
      isEqual(sort.columns, columns) ? sort.direction : undefined,
    [sort.columns, sort.direction]
  )

  const sortedResult = useMemo(
    () =>
      result.map((rawResult) =>
        orderBy(
          rawResult.map((row) => ({
            ...row,
            childName: `${row.lastName} ${row.firstName}`
          })),
          sort.columns,
          [sort.direction === 'ASC' ? 'asc' : 'desc']
        )
      ),
    [sort, result]
  )

  return (
    <>
      {renderResult(sortedResult, (sortedRows) => {
        const filteredRows = sortedRows.filter(displayFilter)
        return (
          <>
            <FilterRow>
              <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
              <FlexRow>
                <Wrapper>
                  <Combobox
                    items={[
                      { value: '', label: i18n.common.all },
                      ...uniqBy(sortedRows, (r) => r.careAreaName).map((s) => ({
                        value: s.careAreaName,
                        label: s.careAreaName
                      }))
                    ]}
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
                    placeholder={
                      i18n.reports.occupancies.filters.areaPlaceholder
                    }
                    getItemLabel={(item) => item.label}
                  />
                </Wrapper>
              </FlexRow>
            </FilterRow>

            <FilterRow>
              <FilterLabel>{i18n.reports.common.unitName}</FilterLabel>
              <FlexRow>
                <Wrapper>
                  <Combobox
                    items={[
                      { value: '', label: i18n.common.all },
                      ...uniqBy(
                        sortedRows.filter(areaFilter),
                        (r) => r.unitName
                      ).map((s) => ({ value: s.unitName, label: s.unitName }))
                    ]}
                    onChange={(option) =>
                      option
                        ? setDisplayFilters({
                            ...displayFilters,
                            unit: option.value
                          })
                        : undefined
                    }
                    selectedItem={
                      displayFilters.unit !== ''
                        ? {
                            label: displayFilters.unit,
                            value: displayFilters.unit
                          }
                        : {
                            label: i18n.common.all,
                            value: ''
                          }
                    }
                    placeholder={
                      i18n.reports.occupancies.filters.unitPlaceholder
                    }
                    getItemLabel={(item) => item.label}
                  />
                </Wrapper>
              </FlexRow>
            </FilterRow>

            <ReportDownload
              data={filteredRows.map((row) => ({
                careAreaName: row.careAreaName,
                unitName: row.unitName,
                firstName: row.firstName,
                lastName: row.lastName,
                placementStart: row.placementStart.format()
              }))}
              headers={[
                {
                  label: i18n.reports.common.careAreaName,
                  key: 'careAreaName'
                },
                { label: i18n.reports.common.unitName, key: 'unitName' },
                {
                  label: i18n.reports.startingPlacements.childLastName,
                  key: 'lastName'
                },
                {
                  label: i18n.reports.startingPlacements.childFirstName,
                  key: 'firstName'
                },
                {
                  label: i18n.reports.startingPlacements.placementStart,
                  key: 'placementStart'
                }
              ]}
              filename={getFilename(i18n, filters.year, filters.month)}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <SortableTh
                    sorted={sorted(['careAreaName', 'unitName', 'childName'])}
                    onClick={() =>
                      sortBy(['careAreaName', 'unitName', 'childName'])
                    }
                  >
                    {i18n.reports.common.careAreaName}
                  </SortableTh>
                  <SortableTh
                    sorted={sorted(['unitName', 'childName'])}
                    onClick={() => sortBy(['unitName', 'childName'])}
                  >
                    {i18n.reports.common.unitName}
                  </SortableTh>
                  <SortableTh
                    sorted={sorted(['childName'])}
                    onClick={() => sortBy(['childName'])}
                  >
                    {i18n.reports.common.childName}
                  </SortableTh>
                  <SortableTh
                    sorted={sorted(['placementStart', 'childName'])}
                    onClick={() => sortBy(['placementStart', 'childName'])}
                  >
                    {i18n.reports.startingPlacements.placementStart}
                  </SortableTh>
                </Tr>
              </Thead>
              <Tbody>
                {filteredRows.map((row) => (
                  <Tr key={row.placementId} data-qa="report-row">
                    <StyledTd data-qa="area-name">{row.careAreaName}</StyledTd>
                    <StyledTd data-qa="unit-name">{row.unitName}</StyledTd>
                    <StyledTd>
                      <Link
                        data-qa="child-name"
                        to={`/child-information/${row.childId}`}
                      >
                        {row.childName}
                      </Link>
                    </StyledTd>
                    <StyledTd data-qa="placement-start-date">
                      {row.placementStart.format()}
                    </StyledTd>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
            <RowCountInfo rowCount={filteredRows.length} />
          </>
        )
      })}
    </>
  )
})
