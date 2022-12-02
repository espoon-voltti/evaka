// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faChevronDown, faChevronUp } from 'Icons'
import sortBy from 'lodash/sortBy'
import React, { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Loading, Result } from 'lib-common/api'
import { careTypes } from 'lib-common/generated/api-types/daycare'
import {
  PlacementCountAreaResult,
  PlacementCountReportResult
} from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Tfoot, Th, Thead, Tr } from 'lib-components/layout/Table'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { unitProviderTypes } from 'lib-customizations/employee'

import {
  getPlacementCountReport,
  PlacementCountReportFilters
} from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'

import { FilterLabel, FilterRow, TableScrollable } from './common'

interface DisplayFilters {
  careAreas: { areaId: string; areaName: string }[]
}

const emptyDisplayFilters: DisplayFilters = {
  careAreas: []
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
  placementCountUnder3v: number
  placementCount3vAndOver: number
  placementCount: number
  calculatedPlacements: number
}

export default React.memo(function PlacementCount() {
  const { i18n, lang } = useTranslation()
  const [result, setResult] = useState<Result<PlacementCountReportResult>>(
    Loading.of()
  )
  const currentLocalDate = LocalDate.todayInSystemTz()
  const [filters, setFilters] = useState<PlacementCountReportFilters>({
    examinationDate: currentLocalDate,
    providerTypes: [],
    careTypes: []
  })

  const [displayFilters, setDisplayFilters] =
    useState<DisplayFilters>(emptyDisplayFilters)
  const displayFilter = (row: PlacementCountAreaResult): boolean => {
    return (
      displayFilters.careAreas.length === 0 ||
      displayFilters.careAreas.map((ca) => ca.areaId).includes(row.areaId)
    )
  }

  const [areasOpen, setAreasOpen] = useState<Record<string, boolean>>({})

  useEffect(() => {
    setResult(Loading.of())
    setDisplayFilters(emptyDisplayFilters)
    void getPlacementCountReport(filters).then(setResult)
  }, [filters])

  const countTotalsFromAreaResults = (
    filteredAreaResults: PlacementCountAreaResult[]
  ) => {
    return filteredAreaResults.reduce(
      (sum, current) => {
        return {
          placementCount: sum.placementCount + current.placementCount,
          placementCountUnder3v:
            sum.placementCountUnder3v + current.placementCountUnder3v,
          placementCount3vAndOver:
            sum.placementCount3vAndOver + current.placementCount3vAndOver,
          calculatedPlacements:
            sum.calculatedPlacements + current.calculatedPlacements
        }
      },
      {
        placementCount: 0,
        placementCount3vAndOver: 0,
        placementCountUnder3v: 0,
        calculatedPlacements: 0.0
      }
    )
  }

  const { fullAreaResults, filteredAreaResults, filteredTotals } = useMemo(
    () =>
      result
        .map((rs) => {
          const fullAreaResults = rs.areaResults
          const filteredAreaResults = fullAreaResults.filter(displayFilter)
          return {
            fullAreaResults,
            filteredAreaResults,
            filteredTotals: countTotalsFromAreaResults(filteredAreaResults)
          }
        })
        .getOrElse({
          fullAreaResults: [],
          filteredAreaResults: [],
          filteredTotals: countTotalsFromAreaResults([])
        }),
    [result, displayFilters] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const extractCsvRows = (): CsvReportRow[] => {
    const areaRows = filteredAreaResults.flatMap((areaResult) => {
      return [
        ...areaResult.daycareResults.map((daycareResult) => {
          return {
            ...daycareResult,
            areaName: areaResult.areaName
          }
        }),
        {
          areaName: areaResult.areaName,
          daycareName: null,
          placementCount: areaResult.placementCount,
          placementCountUnder3v: areaResult.placementCountUnder3v,
          placementCount3vAndOver: areaResult.placementCount3vAndOver,
          calculatedPlacements: areaResult.calculatedPlacements
        }
      ]
    })

    return areaRows.length > 0
      ? [
          ...areaRows,
          {
            areaName: null,
            daycareName: null,
            placementCount: filteredTotals.placementCount,
            placementCountUnder3v: filteredTotals.placementCountUnder3v,
            placementCount3vAndOver: filteredTotals.placementCount3vAndOver,
            calculatedPlacements: filteredTotals.calculatedPlacements
          }
        ]
      : []
  }

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.placementCount.title}</Title>

        <FilterRow>
          <FilterLabel>
            {i18n.reports.placementCount.examinationDate}
          </FilterLabel>
          <DatePicker
            id="examination-date"
            date={filters.examinationDate}
            onChange={(examinationDate) => {
              if (examinationDate && examinationDate <= currentLocalDate)
                setFilters({ ...filters, examinationDate })
            }}
            hideErrorsBeforeTouched
            locale={lang}
            maxDate={currentLocalDate}
            isInvalidDate={(d) =>
              d <= currentLocalDate ? null : i18n.validationErrors.dateTooLate
            }
          />
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.placementCount.providerType}</FilterLabel>
          <Wrapper>
            <MultiSelect
              options={sortBy(
                unitProviderTypes,
                (s) => i18n.common.providerType[s]
              )}
              onChange={(selectedItems) =>
                setFilters({
                  ...filters,
                  providerTypes: selectedItems
                })
              }
              value={filters.providerTypes ?? []}
              getOptionId={(providerType) => providerType}
              getOptionLabel={(providerType) =>
                i18n.common.providerType[providerType]
              }
              placeholder={i18n.common.all}
            />
          </Wrapper>
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.placementCount.careType}</FilterLabel>
          <Wrapper>
            <MultiSelect
              options={sortBy(careTypes, (s) => i18n.common.types[s])}
              onChange={(selectedItems) =>
                setFilters({
                  ...filters,
                  careTypes: selectedItems.map((selectedItem) => selectedItem)
                })
              }
              value={careTypes.filter((unitType) =>
                filters.careTypes?.includes(unitType)
              )}
              getOptionId={(unitType) => unitType}
              getOptionLabel={(unitType) => i18n.common.types[unitType]}
              placeholder={i18n.common.all}
            />
          </Wrapper>
        </FilterRow>

        <FilterRow>
          <FilterLabel>{i18n.reports.placementCount.careArea}</FilterLabel>
          <Wrapper>
            <MultiSelect
              options={fullAreaResults.map((ar) => {
                return { areaId: ar.areaId, areaName: ar.areaName }
              })}
              onChange={(selections) =>
                setDisplayFilters({
                  ...displayFilters,
                  careAreas: selections
                })
              }
              value={filteredAreaResults.map((ar) => {
                return { areaId: ar.areaId, areaName: ar.areaName }
              })}
              getOptionId={(careArea) => careArea.areaId}
              getOptionLabel={(careArea) => careArea.areaName}
              placeholder={i18n.reports.placementCount.noCareAreasFound}
            />
          </Wrapper>
        </FilterRow>

        {result.isLoading && <Loader />}
        {result.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {result.isSuccess && (
          <>
            <ReportDownload
              data={extractCsvRows()}
              headers={[
                { label: 'Alue', key: 'areaName' },
                { label: 'Yksikkö', key: 'daycareName' },
                { label: 'Alle 3v', key: 'placementCountUnder3v' },
                { label: 'Vähintään 3v', key: 'placementCount3vAndOver' },
                { label: 'Lapsia yhteensä', key: 'placementCount' },
                { label: 'Laskennallinen määrä', key: 'calculatedPlacements' }
              ]}
              filename={`sijoitusmaarat_${filters.examinationDate.formatIso()}.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.placementCount.daycaresByArea}</Th>
                  <Th>{i18n.reports.placementCount.placementsUnder3}</Th>
                  <Th>{i18n.reports.placementCount.placementsOver3}</Th>
                  <Th>{i18n.reports.placementCount.placementCount}</Th>
                  <Th>{i18n.reports.placementCount.calculatedPlacements}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {filteredAreaResults.map((area: PlacementCountAreaResult) => (
                  <React.Fragment key={area.areaName}>
                    <Tr key={`${area.areaId}`} data-qa={`${area.areaId}`}>
                      <Td>
                        <div
                          onClick={() =>
                            setAreasOpen({
                              ...areasOpen,
                              [area.areaName]: !(
                                areasOpen[area.areaName] ?? false
                              )
                            })
                          }
                        >
                          <span>
                            <AccordionIcon
                              icon={
                                areasOpen[area.areaName]
                                  ? faChevronUp
                                  : faChevronDown
                              }
                            />
                          </span>
                          <span>{area.areaName}</span>
                        </div>
                      </Td>
                      <BoldTd>{area.placementCountUnder3v}</BoldTd>
                      <BoldTd>{area.placementCount3vAndOver}</BoldTd>
                      <BoldTd>{area.placementCount}</BoldTd>
                      <BoldTd>{area.calculatedPlacements}</BoldTd>
                    </Tr>
                    {area.daycareResults.map(
                      (daycare) =>
                        areasOpen[area.areaName] && (
                          <Tr key={daycare.daycareId}>
                            <StyledSubTableTd>
                              <Link to={`/units/${daycare.daycareId}`}>
                                {daycare.daycareName}
                              </Link>
                            </StyledSubTableTd>
                            <SubTableTd>
                              {daycare.placementCountUnder3v}
                            </SubTableTd>
                            <SubTableTd>
                              {daycare.placementCount3vAndOver}
                            </SubTableTd>
                            <SubTableTd>{daycare.placementCount}</SubTableTd>
                            <SubTableTd>
                              {daycare.calculatedPlacements}
                            </SubTableTd>
                          </Tr>
                        )
                    )}
                  </React.Fragment>
                ))}
              </Tbody>

              {filteredTotals && (
                <StyledTfoot>
                  <Tr>
                    <BoldTd>{i18n.reports.common.total}</BoldTd>
                    <BoldTd>{filteredTotals.placementCountUnder3v}</BoldTd>
                    <BoldTd>{filteredTotals.placementCount3vAndOver}</BoldTd>
                    <BoldTd>{filteredTotals.placementCount}</BoldTd>
                    <BoldTd>{filteredTotals.calculatedPlacements}</BoldTd>
                  </Tr>
                </StyledTfoot>
              )}
            </TableScrollable>
          </>
        )}
      </ContentArea>
    </Container>
  )
})
