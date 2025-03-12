// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'

import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { PeriodFilters } from '../../types/reports'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import { FilterLabel, FilterRow } from './common'
import { presenceReportQuery } from './queries'

export default React.memo(function Presences() {
  const { i18n } = useTranslation()
  const [filters, setFilters] = useState<PeriodFilters>({
    from: LocalDate.todayInSystemTz().subWeeks(1),
    to: LocalDate.todayInSystemTz()
  })

  const rows = useQueryResult(presenceReportQuery(filters))

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.presence.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <DatePickerDeprecated
              date={filters.from}
              onChange={(from) => setFilters({ ...filters, from })}
              type="half-width"
            />
            <span>{' - '}</span>
            <DatePickerDeprecated
              date={filters.to}
              onChange={(to) => setFilters({ ...filters, to })}
              type="half-width"
            />
          </FlexRow>
        </FilterRow>

        {renderResult(rows, (rows) => (
          <>
            <ReportDownload
              data={rows}
              columns={[
                {
                  label: i18n.reports.presence.date,
                  value: (row) => row.date.formatIso()
                },
                {
                  label: i18n.reports.presence.SSN,
                  value: (row) => row.socialSecurityNumber
                },
                {
                  label: i18n.reports.presence.daycareId,
                  value: (row) => row.daycareId
                },
                {
                  label: i18n.reports.presence.daycareGroupName,
                  value: (row) => row.daycareGroupName
                },
                {
                  label: i18n.reports.presence.present,
                  value: (row) =>
                    row.present === true
                      ? 'kyllä'
                      : row.present === false
                        ? 'ei'
                        : null
                }
              ]}
              filename={`${
                i18n.reports.presence.title
              } ${filters.from.formatIso()}-${filters.to.formatIso()}.csv`}
            />
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
