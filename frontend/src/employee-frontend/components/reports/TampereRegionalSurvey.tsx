// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import range from 'lodash/range'
import React, { useMemo, useState } from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import { RegionalSurveyReportResult } from 'lib-common/generated/api-types/reports'
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
import { tampereRegionalSurveyReport } from './queries'

interface ReportQueryParams {
  year: number
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

  const emptyValue: RegionalSurveyReportResult = {
    monthlyCounts: [],
    year: 0
  }

  const reportResult = useQueryResult(
    activeParams
      ? tampereRegionalSurveyReport(activeParams)
      : constantQuery(emptyValue)
  )

  const fetchResults = () => {
    if (selectedYear) {
      setActiveParams({
        year: selectedYear
      })
    }
  }

  const sortedReportResult = useMemo(
    () =>
      reportResult.map((result) => {
        return {
          year: result.year,
          monthlyCounts: orderBy(result.monthlyCounts, [(r) => r.month])
        }
      }),
    [reportResult]
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
              onChange={setSelectedYear}
              placeholder={i18n.common.year}
            />
          </FlexRow>
        </FilterRow>
        <Gap />
        <FilterRow>
          <Button
            primary
            disabled={!selectedYear}
            text={i18n.common.search}
            onClick={fetchResults}
            data-qa="send-button"
          />
        </FilterRow>
        <Gap size="m" />

        {renderResult(sortedReportResult, (result) => {
          return result.monthlyCounts.length > 0 &&
            result.year === selectedYear ? (
            <FixedSpaceColumn spacing="L">
              <H2>{`${t.reportLabel} ${result.year}`}</H2>
              <FixedSpaceRow spacing="L">
                <Label>{t.monthlyReport}</Label>
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
              </FixedSpaceRow>
            </FixedSpaceColumn>
          ) : null
        })}
      </ContentArea>
    </Container>
  )
})
