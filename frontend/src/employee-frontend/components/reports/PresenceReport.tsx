// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'

import { Loading, Result, wrapResult } from 'lib-common/api'
import { PresenceReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'

import ReportDownload from '../../components/reports/ReportDownload'
import { getPresenceReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { PeriodFilters } from '../../types/reports'
import { FlexRow } from '../common/styled/containers'

import { FilterLabel, FilterRow } from './common'

const getPresenceReportResult = wrapResult(getPresenceReport)

export default React.memo(function Presences() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<PresenceReportRow[]>>(Loading.of())
  const [filters, setFilters] = useState<PeriodFilters>({
    from: LocalDate.todayInSystemTz().subWeeks(1),
    to: LocalDate.todayInSystemTz()
  })

  useEffect(() => {
    setRows(Loading.of())
    void getPresenceReportResult(filters).then(setRows)
  }, [filters])

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

        {rows.isLoading && <Loader />}
        {rows.isFailure && (
          <span>
            {i18n.common.loadingFailed}. {i18n.reports.presence.info}
          </span>
        )}
        {rows.isSuccess && (
          <>
            <ReportDownload
              data={rows.value.map((row) => ({
                ...row,
                date: row.date.formatIso(),
                present:
                  row.present === true
                    ? 'kyllä'
                    : row.present === false
                      ? 'ei'
                      : null
              }))}
              headers={[
                {
                  label: i18n.reports.presence.date,
                  key: 'date'
                },
                {
                  label: i18n.reports.presence.SSN,
                  key: 'socialSecurityNumber'
                },
                {
                  label: i18n.reports.presence.daycareId,
                  key: 'daycareId'
                },
                {
                  label: i18n.reports.presence.daycareGroupName,
                  key: 'daycareGroupName'
                },
                {
                  label: i18n.reports.presence.present,
                  key: 'present'
                }
              ]}
              filename={`${
                i18n.reports.presence.title
              } ${filters.from.formatIso()}-${filters.to.formatIso()}.csv`}
            />
          </>
        )}
      </ContentArea>
    </Container>
  )
})
