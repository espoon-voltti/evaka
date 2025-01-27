// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import range from 'lodash/range'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import {
  RegionalSurveyReportAgeStatisticsResult,
  RegionalSurveyReportResult
} from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow } from './common'
import {
  tampereRegionalSurveyAgeReport,
  tampereRegionalSurveyMonthlyReport
} from './queries'

interface ReportQueryParams {
  year: number
}

interface ReportSelection {
  monthlyStatistics: boolean
  ageStatistics: boolean
}

const emptyReportSelection = {
  monthlyStatistics: false,
  ageStatistics: false
}

export default React.memo(function TampereRegionalSurveyReport() {
  const { i18n } = useTranslation()
  const t = i18n.reports.tampereRegionalSurvey
  const today = LocalDate.todayInHelsinkiTz()

  const [selectedYear, setSelectedYear] = useState<number | null>(
    today.year - 1
  )

  const yearOptions = range(
    LocalDate.todayInSystemTz().year,
    LocalDate.todayInSystemTz().year - 4,
    -1
  )

  const [activeParams, setActiveParams] = useState<ReportQueryParams | null>(
    null
  )

  const emptyMonthlyValue: RegionalSurveyReportResult = {
    monthlyCounts: [],
    year: 0
  }

  const emptyAgeValue: RegionalSurveyReportAgeStatisticsResult = {
    ageStatistics: [],
    year: 0
  }

  const [reportSelection, setReportSelection] =
    useState<ReportSelection>(emptyReportSelection)

  const monthlyStatisticsResult = useQueryResult(
    activeParams && reportSelection.monthlyStatistics
      ? tampereRegionalSurveyMonthlyReport(activeParams)
      : constantQuery(emptyMonthlyValue)
  )

  const ageStatisticsResult = useQueryResult(
    activeParams && reportSelection.ageStatistics
      ? tampereRegionalSurveyAgeReport(activeParams)
      : constantQuery(emptyAgeValue)
  )

  const fetchMonthlyResults = useCallback(() => {
    if (selectedYear) {
      setReportSelection({ ...reportSelection, monthlyStatistics: true })
      setActiveParams({ year: selectedYear })
    }
  }, [selectedYear, reportSelection])

  const fetchAgeResults = useCallback(() => {
    if (selectedYear) {
      setReportSelection({ ...reportSelection, ageStatistics: true })
      setActiveParams({ year: selectedYear })
    }
  }, [selectedYear, reportSelection])

  const changeYear = useCallback(
    (newYear: number | null) => {
      if (selectedYear !== newYear) {
        setSelectedYear(newYear)
        setReportSelection(emptyReportSelection)
      }
    },
    [selectedYear]
  )

  const sortedMonthlyReportResult = useMemo(
    () =>
      monthlyStatisticsResult.map((result) => {
        return {
          year: result.year,
          monthlyCounts: orderBy(result.monthlyCounts, [(r) => r.month])
        }
      }),
    [monthlyStatisticsResult]
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{t.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <Combobox
              fullWidth
              items={yearOptions}
              selectedItem={selectedYear}
              onChange={changeYear}
              placeholder={i18n.common.year}
            />
          </FlexRow>
        </FilterRow>
        <Gap size="m" />
        <FixedSpaceColumn spacing="xs">
          {!!selectedYear && <H2>{`${t.reportLabel} ${selectedYear}`}</H2>}
          <ReportRow spacing="L" alignItems="center" fullWidth>
            <ReportLabel>{t.monthlyReport}</ReportLabel>
            <Button
              primary
              disabled={!selectedYear}
              text={i18n.common.search}
              onClick={fetchMonthlyResults}
              data-qa="fetch-monthly-button"
            />
            {renderResult(sortedMonthlyReportResult, (result) => {
              return result.monthlyCounts.length > 0 &&
                result.year === selectedYear ? (
                <ReportDownload
                  data={result.monthlyCounts.map((row) => ({
                    ...row,
                    month: i18n.common.datetime.months[row.month - 1] ?? ''
                  }))}
                  headers={[
                    { label: t.monthlyColumns.month, key: 'month' },
                    {
                      label: t.monthlyColumns.municipalOver3FullTimeCount,
                      key: 'municipalOver3FullTimeCount'
                    },
                    {
                      label: t.monthlyColumns.municipalOver3PartTimeCount,
                      key: 'municipalOver3PartTimeCount'
                    },
                    {
                      label: t.monthlyColumns.municipalUnder3FullTimeCount,
                      key: 'municipalUnder3FullTimeCount'
                    },
                    {
                      label: t.monthlyColumns.municipalUnder3PartTimeCount,
                      key: 'municipalUnder3PartTimeCount'
                    },
                    {
                      label: t.monthlyColumns.familyOver3Count,
                      key: 'familyOver3Count'
                    },
                    {
                      label: t.monthlyColumns.familyUnder3Count,
                      key: 'familyUnder3Count'
                    },
                    {
                      label: t.monthlyColumns.municipalShiftCareCount,
                      key: 'municipalShiftCareCount'
                    },
                    {
                      label: t.monthlyColumns.assistanceCount,
                      key: 'assistanceCount'
                    }
                  ]}
                  filename={`${t.reportLabel} ${selectedYear} - ${t.monthlyReport}.csv`}
                />
              ) : null
            })}
          </ReportRow>
          <ReportRow spacing="L" alignItems="center" fullWidth>
            <ReportLabel>{t.ageStatisticsReport}</ReportLabel>
            <Button
              primary
              disabled={!selectedYear}
              text={i18n.common.search}
              onClick={fetchAgeResults}
              data-qa="fetch-age-button"
            />
            {renderResult(ageStatisticsResult, (result) => {
              return result.ageStatistics.length > 0 &&
                result.year === selectedYear ? (
                <ReportDownload
                  data={result.ageStatistics}
                  headers={[
                    {
                      label: t.ageStatisticColumns.voucherUnder3Count,
                      key: 'voucherUnder3Count'
                    },
                    {
                      label: t.ageStatisticColumns.voucherOver3Count,
                      key: 'voucherOver3Count'
                    },
                    {
                      label: t.ageStatisticColumns.purchasedUnder3Count,
                      key: 'purchasedUnder3Count'
                    },
                    {
                      label: t.ageStatisticColumns.purchasedOver3Count,
                      key: 'purchasedOver3Count'
                    },
                    {
                      label: t.ageStatisticColumns.clubUnder3Count,
                      key: 'clubUnder3Count'
                    },
                    {
                      label: t.ageStatisticColumns.clubOver3Count,
                      key: 'clubOver3Count'
                    },
                    {
                      label: t.ageStatisticColumns.nonNativeLanguageUnder3Count,
                      key: 'nonNativeLanguageUnder3Count'
                    },
                    {
                      label: t.ageStatisticColumns.nonNativeLanguageOver3Count,
                      key: 'nonNativeLanguageOver3Count'
                    },
                    {
                      label: t.ageStatisticColumns.effectiveCareDaysUnder3Count,
                      key: 'effectiveCareDaysUnder3Count'
                    },
                    {
                      label: t.ageStatisticColumns.effectiveCareDaysOver3Count,
                      key: 'effectiveCareDaysOver3Count'
                    },
                    {
                      label:
                        t.ageStatisticColumns
                          .effectiveFamilyDaycareDaysUnder3Count,
                      key: 'effectiveFamilyDaycareDaysUnder3Count'
                    },
                    {
                      label:
                        t.ageStatisticColumns
                          .effectiveFamilyDaycareDaysOver3Count,
                      key: 'effectiveFamilyDaycareDaysOver3Count'
                    }
                  ]}
                  filename={`${t.reportLabel} ${selectedYear} - ${t.ageStatisticsReport}.csv`}
                />
              ) : null
            })}
          </ReportRow>
        </FixedSpaceColumn>
      </ContentArea>
    </Container>
  )
})

const ReportLabel = styled(Label)`
  width: 200px;
`

const ReportRow = styled(FixedSpaceRow)`
  min-height: 105px;
`
