// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useState } from 'react'

import type { Result } from 'lib-common/api'
import { Success, wrapResult } from 'lib-common/api'
import type { RawReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import Title from 'lib-components/atoms/Title'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { DatePickerSpacer } from 'lib-components/molecules/date-picker/DateRangePicker'
import { featureFlags } from 'lib-customizations/employee'

import {
  getRawReport,
  sendPatuReport
} from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import type { PeriodFilters } from '../../types/reports'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow } from './common'

const getRawReportResult = wrapResult(getRawReport)
const sendPatuReportResult = wrapResult(sendPatuReport)

export default React.memo(function Raw() {
  const { i18n } = useTranslation()
  const [filters, setFilters] = useState<PeriodFilters>({
    from: LocalDate.todayInSystemTz(),
    to: LocalDate.todayInSystemTz()
  })
  const invertedRange =
    filters.from && filters.to && filters.to.isBefore(filters.from)
  const tooLongRange =
    filters.from && filters.to && filters.to.isAfter(filters.from.addDays(7))

  const [report, setReport] = useState<Result<RawReportRow[]>>(Success.of([]))
  const { user } = useContext(UserContext)

  const fetchRawReport = useCallback(
    () => getRawReportResult({ from: filters.from!, to: filters.to! }),
    [filters]
  )

  const mapYesNo = (value: boolean | null | undefined) => {
    if (value === true) {
      return i18n.common.yes
    } else if (value === false) {
      return i18n.common.no
    } else {
      return null
    }
  }

  const mapFloat = (value: number | null) =>
    value?.toString().replace(/\./, ',') ?? null

  const submitPatuReport = () => {
    if (filters.from && filters.to) {
      void sendPatuReportResult({ from: filters.from, to: filters.to })
    }
  }

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.raw.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <DatePicker
              date={filters.from}
              onChange={(from) => setFilters({ ...filters, from })}
              locale="fi"
            />
            <DatePickerSpacer />
            <DatePicker
              date={filters.to}
              onChange={(to) => setFilters({ ...filters, to })}
              locale="fi"
            />
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <AsyncButton
            primary
            text={i18n.common.search}
            onClick={fetchRawReport}
            onSuccess={(newReport) => setReport(Success.of(newReport))}
            disabled={filters.from === null || filters.to === null}
            data-qa="send-button"
          />
        </FilterRow>
        {invertedRange ? (
          <span>Virheellinen aikaväli</span>
        ) : tooLongRange ? (
          <span>Liian pitkä aikaväli (max 7 päivää)</span>
        ) : (
          renderResult(report, (rows) => (
            <ReportDownload
              data={rows}
              columns={[
                { label: 'Päivämäärä', value: (row) => row.day.format() },
                { label: 'Lapsen id', value: (row) => row.childId },
                {
                  label: 'Linkki lapseen',
                  value: (row) =>
                    `${window.location.protocol}//${window.location.host}/employee/child-information/${row.childId}`
                },
                { label: 'Sukunimi', value: (row) => row.lastName },
                { label: 'Etunimi', value: (row) => row.firstName },
                {
                  label: 'Hetu',
                  value: (row) => mapYesNo(row.hasSocialSecurityNumber)
                },
                {
                  label: 'Syntymäaika',
                  value: (row) => row.dateOfBirth?.format()
                },
                { label: 'Ikä', value: (row) => row.age },
                { label: 'Kieli', value: (row) => row.language },
                { label: 'Postinumero', value: (row) => row.postalCode },
                { label: 'Postitoimipaikka', value: (row) => row.postOffice },
                {
                  label: 'Sijoitustyyppi',
                  value: (row) => i18n.placement.type[row.placementType]
                },
                { label: 'Sijoitettu yksikköön', value: (row) => row.unitId },
                { label: 'Yksikkö', value: (row) => row.unitName },
                { label: 'Palvelualue', value: (row) => row.careArea },
                {
                  label: 'Toimintamuoto',
                  value: (row) =>
                    row.unitType
                      ? i18n.reports.common.unitTypes[row.unitType]
                      : ''
                },
                {
                  label: 'Järjestämismuoto',
                  value: (row) =>
                    i18n.reports.common.unitProviderTypes[row.unitProviderType]
                },
                { label: 'Kustannuspaikka', value: (row) => row.costCenter },
                {
                  label: 'Sijoitettu ryhmään',
                  value: (row) => row.daycareGroupId
                },
                { label: 'Ryhmä', value: (row) => row.groupName },
                {
                  label: 'Henkilökuntaa ryhmässä',
                  value: (row) => mapFloat(row.caretakersPlanned)
                },
                {
                  label: 'Henkilökuntaa läsnä',
                  value: (row) => mapFloat(row.caretakersRealized)
                },
                {
                  label: 'Varahoidossa yksikössä',
                  value: (row) => row.backupUnitId
                },
                {
                  label: 'Varahoidossa ryhmässä',
                  value: (row) => row.backupGroupId
                },
                {
                  label: 'Palveluntarve merkitty',
                  value: (row) => mapYesNo(row.hasServiceNeed)
                },
                { label: 'Palveluntarve', value: (row) => row.serviceNeed },
                {
                  label: 'Osapäiväinen',
                  value: (row) => mapYesNo(row.partDay)
                },
                {
                  label: 'Osaviikkoinen',
                  value: (row) => mapYesNo(row.partWeek)
                },
                {
                  label: 'Vuorohoito',
                  value: (row) => mapYesNo(row.shiftCare)
                },
                {
                  label: 'Tunteja viikossa',
                  value: (row) => mapFloat(row.hoursPerWeek)
                },
                {
                  label: 'Tuentarve',
                  value: (row) => mapYesNo(row.hasAssistanceNeed)
                },
                {
                  label: 'Palvelusetelikerroin',
                  value: (row) => mapFloat(row.assistanceNeedVoucherCoefficient)
                },
                {
                  label: 'Tuentarpeen kerroin',
                  value: (row) => mapFloat(row.capacityFactor)
                },
                {
                  label: 'Lapsen kapasiteetti',
                  value: (row) => mapFloat(row.capacity)
                },
                {
                  label: 'Lapsen kapasiteetti (käyttö)',
                  value: (row) => mapFloat(row.realizedCapacity)
                },
                {
                  label: 'Poissa maksullisesta',
                  value: (row) =>
                    row.absencePaid
                      ? i18n.absences.absenceTypes[row.absencePaid]
                      : ''
                },
                {
                  label: 'Poissa maksuttomasta',
                  value: (row) =>
                    row.absenceFree
                      ? i18n.absences.absenceTypes[row.absenceFree]
                      : ''
                },
                {
                  label: 'Henkilöstömitoitus',
                  value: (row) => row.staffDimensioning
                },
                {
                  label: 'Kotikunta',
                  value: (row) => row.municipalityOfResidence
                },
                {
                  label: 'Ryhmäkohtainen Aromin vastuuyksikkökoodi',
                  value: (row) => row.aromiCustomerId,
                  exclude: !featureFlags.aromiIntegration
                }
              ]}
              filename={`${
                i18n.reports.raw.title
              } ${filters.from?.formatIso()}-${filters.to?.formatIso()}.csv`}
            />
          ))
        )}
        {user?.accessibleFeatures.submitPatuReport && (
          <div>
            <AsyncButton
              primary
              text="Lähetä patu-raportti"
              onClick={submitPatuReport}
              onSuccess={() => undefined}
            />
          </div>
        )}
      </ContentArea>
    </Container>
  )
})
