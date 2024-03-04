// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faChevronDown, faChevronUp } from 'Icons'
import React, { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import {
  FamilyDaycareMealAreaResult,
  FamilyDaycareMealDaycareResult
} from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { useApiState } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Tfoot, Th, Thead, Tr } from 'lib-components/layout/Table'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'

import ReportDownload from '../../components/reports/ReportDownload'
import { getFamilyDaycareMealReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { FilterLabel, FilterRow, TableScrollable } from './common'

const getFamilyDaycareMealReportResult = wrapResult(getFamilyDaycareMealReport)

interface FamilyDaycareMealCountReportFilters {
  startDate: LocalDate
  endDate: LocalDate
}

interface DisplayFilters {
  careArea: { areaId: string; areaName: string } | null
  daycare: { daycareId: string; daycareName: string }
}

const emptyDisplayFilters: DisplayFilters = {
  careArea: null,
  daycare: { daycareId: 'ALL', daycareName: 'Kaikki' }
}

const AccordionIcon = styled(FontAwesomeIcon)`
  cursor: pointer;
  color: ${(p) => p.theme.colors.grayscale.g70};
  padding-right: 1em;
`

const SubTableTd = styled(Td)`
  vertical-align: middle;
  vertical-padding: 0px;
  line-height: 1em;
`

const StyledSubTableTd = styled(SubTableTd)`
  white-space: nowrap;
  padding-left: 3em;
`
const BoldTd = styled(Td)`
  font-weight: bold;
`

const StyledTfoot = styled(Tfoot)`
  background-color: ${(props) => props.theme.colors.grayscale.g4};
`

const Wrapper = styled.div`
  width: 100%;
`

interface CsvReportRow {
  areaName: string | null
  daycareName: string | null
  firstName: string | null
  lastName: string | null
  breakfastCount: number
  lunchCount: number
  snackCount: number
  total: number
}

export default React.memo(function FamilyDaycareMealCount() {
  const { i18n, lang } = useTranslation()

  const currentLocalDate = LocalDate.todayInSystemTz()
  const beginningOfCurrentMonth = LocalDate.of(
    currentLocalDate.getYear(),
    currentLocalDate.getMonth(),
    1
  )
  const [filters, setFilters] = useState<FamilyDaycareMealCountReportFilters>({
    startDate: beginningOfCurrentMonth,
    endDate: currentLocalDate
  })

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)

  const [daycaresOpen, setDaycaresOpen] = useState<Record<string, boolean>>({})

  const [reportResult] = useApiState(
    () => getFamilyDaycareMealReportResult(filters),
    [filters]
  )

  const countTotalsFromResults = (
    filteredAreaResults: {
      breakfastCount: number
      lunchCount: number
      snackCount: number
    }[]
  ) =>
    filteredAreaResults.reduce(
      (sum, current) => ({
        breakfastCount: sum.breakfastCount + current.breakfastCount,
        lunchCount: sum.lunchCount + current.lunchCount,
        snackCount: sum.snackCount + current.snackCount
      }),
      {
        breakfastCount: 0,
        lunchCount: 0,
        snackCount: 0
      }
    )

  const filteredResults = useMemo(
    () =>
      reportResult.map((rs) => {
        const fullAreaResults = rs.areaResults
        const filteredAreaResult = displayFilters.careArea
          ? fullAreaResults.find(
              (row) => displayFilters.careArea?.areaId === row.areaId
            )
          : fullAreaResults[0]
        const fullDaycareResults = filteredAreaResult
          ? filteredAreaResult.daycareResults
          : []
        const filteredDaycareResults = fullDaycareResults.filter(
          (row: FamilyDaycareMealDaycareResult): boolean =>
            displayFilters.daycare.daycareId === 'ALL' ||
            displayFilters.daycare.daycareId === row.daycareId
        )
        return {
          fullAreaResults,
          filteredAreaResult,
          filteredAreaTotals: countTotalsFromResults(
            filteredAreaResult ? [filteredAreaResult] : []
          ),
          fullDaycareResults,
          filteredDaycareResults,
          filteredDaycareTotals: countTotalsFromResults(filteredDaycareResults)
        }
      }),
    [reportResult, displayFilters]
  )

  const extractCsvRows = (
    filteredAreaResult: FamilyDaycareMealAreaResult | undefined,
    filteredDaycareResults: FamilyDaycareMealDaycareResult[]
  ): CsvReportRow[] => {
    const areaRows = filteredAreaResult
      ? [
          ...filteredDaycareResults.flatMap((daycareResult) => [
            ...daycareResult.childResults.map((childResult) => ({
              ...childResult,
              areaName: filteredAreaResult.areaName,
              daycareName: daycareResult.daycareName,
              total:
                childResult.breakfastCount +
                childResult.lunchCount +
                childResult.snackCount
            })),
            {
              ...daycareResult,
              firstName: null,
              lastName: null,
              areaName: filteredAreaResult.areaName,
              total:
                daycareResult.breakfastCount +
                daycareResult.lunchCount +
                daycareResult.snackCount
            }
          ]),
          {
            areaName: filteredAreaResult.areaName,
            daycareName: null,
            firstName: null,
            lastName: null,
            breakfastCount: filteredAreaResult.breakfastCount,
            lunchCount: filteredAreaResult.lunchCount,
            snackCount: filteredAreaResult.snackCount,
            total:
              filteredAreaResult.breakfastCount +
              filteredAreaResult.lunchCount +
              filteredAreaResult.snackCount
          }
        ]
      : []

    return areaRows.length > 0 ? areaRows : []
  }

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.familyDaycareMealCount.title}</Title>

        <FilterRow>
          <FilterLabel>
            {i18n.reports.familyDaycareMealCount.timePeriod}
          </FilterLabel>
          <DatePicker
            id="start-date"
            date={filters.startDate}
            onChange={(startDate) => {
              if (startDate && startDate <= currentLocalDate)
                setFilters({ ...filters, startDate })
            }}
            hideErrorsBeforeTouched
            locale={lang}
            maxDate={currentLocalDate}
            isInvalidDate={(d) => {
              if (d > currentLocalDate) {
                return i18n.validationErrors.dateTooLate
              }
              if (d < filters.endDate.subMonths(6)) {
                return i18n.reports.familyDaycareMealCount.timePeriodTooLong
              }
              if (d > filters.endDate) {
                return i18n.validationErrors.dateRangeNotLinear
              }
              return null
            }}
          />
          {' - '}
          <DatePicker
            id="end-date"
            date={filters.endDate}
            onChange={(endDate) => {
              if (endDate && endDate <= currentLocalDate)
                setFilters({ ...filters, endDate })
            }}
            hideErrorsBeforeTouched
            locale={lang}
            maxDate={currentLocalDate}
            isInvalidDate={(d) => {
              if (d > currentLocalDate) {
                return i18n.validationErrors.dateTooLate
              }
              if (d > filters.startDate.addMonths(6)) {
                return i18n.reports.familyDaycareMealCount.timePeriodTooLong
              }
              if (d < filters.startDate) {
                return i18n.validationErrors.dateRangeNotLinear
              }
              return null
            }}
          />
        </FilterRow>
        {renderResult(filteredResults, (results) => {
          const {
            fullAreaResults,
            filteredAreaResult,
            fullDaycareResults,
            filteredDaycareResults,
            filteredDaycareTotals
          } = results
          return (
            <>
              <FilterRow>
                <FilterLabel>
                  {i18n.reports.familyDaycareMealCount.careArea}
                </FilterLabel>
                <Wrapper>
                  <Combobox
                    fullWidth={true}
                    clearable={false}
                    items={fullAreaResults.map((area) => ({
                      areaId: area.areaId,
                      areaName: area.areaName
                    }))}
                    selectedItem={filteredAreaResult ?? null}
                    onChange={(selectionValue) => {
                      if (selectionValue !== null) {
                        setDisplayFilters({
                          ...displayFilters,
                          careArea: selectionValue,
                          daycare: emptyDisplayFilters.daycare
                        })
                      }
                    }}
                    getItemLabel={(selectionValue) => selectionValue.areaName}
                    placeholder={
                      i18n.reports.familyDaycareMealCount.noCareAreasFound
                    }
                    disabled={fullAreaResults.length === 0}
                  />
                </Wrapper>
              </FilterRow>

              <FilterRow>
                <FilterLabel>
                  {i18n.reports.familyDaycareMealCount.daycareName}
                </FilterLabel>
                <Wrapper>
                  <Combobox
                    fullWidth={true}
                    clearable={false}
                    items={[
                      { daycareId: 'ALL', daycareName: i18n.common.all },
                      ...fullDaycareResults.map((daycare) => ({
                        daycareId: daycare.daycareId,
                        daycareName: daycare.daycareName
                      }))
                    ]}
                    selectedItem={
                      fullAreaResults.length === 0
                        ? null
                        : displayFilters.daycare
                    }
                    onChange={(selectionValue) => {
                      if (selectionValue !== null) {
                        setDisplayFilters({
                          ...displayFilters,
                          daycare: selectionValue
                        })
                      }
                    }}
                    getItemLabel={(selectionValue) =>
                      selectionValue.daycareName
                    }
                    disabled={fullAreaResults.length === 0}
                    placeholder={
                      i18n.reports.familyDaycareMealCount.noDaycaresFound
                    }
                  />
                </Wrapper>
              </FilterRow>

              <ReportDownload
                data={extractCsvRows(
                  filteredAreaResult,
                  filteredDaycareResults
                )}
                headers={[
                  {
                    label: i18n.reports.familyDaycareMealCount.careArea,
                    key: 'areaName'
                  },
                  {
                    label: i18n.reports.familyDaycareMealCount.daycareName,
                    key: 'daycareName'
                  },
                  {
                    label: i18n.reports.familyDaycareMealCount.firstName,
                    key: 'firstName'
                  },
                  {
                    label: i18n.reports.familyDaycareMealCount.lastName,
                    key: 'lastName'
                  },
                  {
                    label:
                      i18n.reports.familyDaycareMealCount.breakfastCountHeader,
                    key: 'breakfastCount'
                  },
                  {
                    label: i18n.reports.familyDaycareMealCount.lunchCountHeader,
                    key: 'lunchCount'
                  },
                  {
                    label: i18n.reports.familyDaycareMealCount.snackCountHeader,
                    key: 'snackCount'
                  },
                  {
                    label: i18n.reports.familyDaycareMealCount.totalHeader,
                    key: 'total'
                  }
                ]}
                filename={`ateriaraportti_${filters.startDate.formatIso()}-${filters.endDate.formatIso()}.csv`}
              />
              <TableScrollable>
                <Thead>
                  <Tr>
                    <Th>{i18n.reports.familyDaycareMealCount.daycareName}</Th>
                    <Th>
                      {i18n.reports.familyDaycareMealCount.breakfastCountHeader}
                    </Th>
                    <Th>
                      {i18n.reports.familyDaycareMealCount.lunchCountHeader}
                    </Th>
                    <Th>
                      {i18n.reports.familyDaycareMealCount.snackCountHeader}
                    </Th>
                    <Th>{i18n.reports.familyDaycareMealCount.totalHeader}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {filteredDaycareResults.map(
                    (daycare: FamilyDaycareMealDaycareResult) => (
                      <React.Fragment key={daycare.daycareName}>
                        <Tr key={daycare.daycareId} data-qa={daycare.daycareId}>
                          <Td>
                            <div
                              onClick={() =>
                                setDaycaresOpen({
                                  ...daycaresOpen,
                                  [daycare.daycareName]: !(
                                    daycaresOpen[daycare.daycareName] ?? false
                                  )
                                })
                              }
                            >
                              <span>
                                <AccordionIcon
                                  icon={
                                    daycaresOpen[daycare.daycareName]
                                      ? faChevronUp
                                      : faChevronDown
                                  }
                                />
                              </span>
                              <span>
                                <Link to={`/units/${daycare.daycareId}`}>
                                  {daycare.daycareName}
                                </Link>
                              </span>
                            </div>
                          </Td>
                          <BoldTd>{daycare.breakfastCount}</BoldTd>
                          <BoldTd>{daycare.lunchCount}</BoldTd>
                          <BoldTd>{daycare.snackCount}</BoldTd>
                          <BoldTd>
                            {daycare.breakfastCount +
                              daycare.lunchCount +
                              daycare.snackCount}
                          </BoldTd>
                        </Tr>
                        {daycare.childResults.map(
                          (childResult) =>
                            daycaresOpen[daycare.daycareName] && (
                              <Tr key={childResult.childId}>
                                <StyledSubTableTd>
                                  <Link
                                    to={`/child-information/${childResult.childId}`}
                                  >
                                    {`${childResult.lastName} ${childResult.firstName}`}
                                  </Link>
                                </StyledSubTableTd>
                                <SubTableTd>
                                  {childResult.breakfastCount}
                                </SubTableTd>
                                <SubTableTd>
                                  {childResult.lunchCount}
                                </SubTableTd>
                                <SubTableTd>
                                  {childResult.snackCount}
                                </SubTableTd>
                                <SubTableTd>
                                  {childResult.breakfastCount +
                                    childResult.lunchCount +
                                    childResult.snackCount}
                                </SubTableTd>
                              </Tr>
                            )
                        )}
                      </React.Fragment>
                    )
                  )}
                </Tbody>

                {filteredDaycareTotals && filteredDaycareResults.length > 1 && (
                  <StyledTfoot>
                    <Tr>
                      <BoldTd>{i18n.reports.common.total}</BoldTd>
                      <BoldTd>{filteredDaycareTotals.breakfastCount}</BoldTd>
                      <BoldTd>{filteredDaycareTotals.lunchCount}</BoldTd>
                      <BoldTd>{filteredDaycareTotals.snackCount}</BoldTd>
                      <BoldTd>
                        {filteredDaycareTotals.breakfastCount +
                          filteredDaycareTotals.lunchCount +
                          filteredDaycareTotals.snackCount}
                      </BoldTd>
                    </Tr>
                  </StyledTfoot>
                )}
              </TableScrollable>
            </>
          )
        })}
      </ContentArea>
    </Container>
  )
})
