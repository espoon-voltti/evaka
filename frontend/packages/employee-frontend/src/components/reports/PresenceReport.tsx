// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'

import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import { useTranslation } from '~state/i18n'
import { Loading, Result } from '~api'
import { PresenceReportRow } from '~types/reports'
import { getPresenceReport, PeriodFilters } from '~api/reports'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
import { FilterLabel, FilterRow } from '~components/reports/common'
import { DatePicker } from '~components/common/DatePicker'
import LocalDate from '@evaka/lib-common/src/local-date'
import { FlexRow } from 'components/common/styled/containers'

function Presences() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<PresenceReportRow[]>>(Loading.of())
  const [filters, setFilters] = useState<PeriodFilters>({
    from: LocalDate.today().subWeeks(1),
    to: LocalDate.today()
  })

  useEffect(() => {
    setRows(Loading.of())
    void getPresenceReport(filters).then(setRows)
  }, [filters])

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.presence.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <DatePicker
              date={filters.from}
              onChange={(from) => setFilters({ ...filters, from })}
              type="half-width"
            />
            <span>{' - '}</span>
            <DatePicker
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
                date: row.date.format('dd/MM/yyyy'),
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
}

export default Presences
