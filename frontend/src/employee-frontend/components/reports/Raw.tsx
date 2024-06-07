// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useState } from 'react'

import { Result, Success, wrapResult } from 'lib-common/api'
import { RawReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import Title from 'lib-components/atoms/Title'
import { LegacyAsyncButton } from 'lib-components/atoms/buttons/LegacyAsyncButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'

import ReportDownload from '../../components/reports/ReportDownload'
import {
  getRawReport,
  sendPatuReport
} from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { PeriodFilters } from '../../types/reports'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import { FilterLabel, FilterRow } from './common'

const getRawReportResult = wrapResult(getRawReport)
const sendPatuReportResult = wrapResult(sendPatuReport)

export default React.memo(function Raw() {
  const { i18n } = useTranslation()
  const [filters, setFilters] = useState<PeriodFilters>({
    from: LocalDate.todayInSystemTz(),
    to: LocalDate.todayInSystemTz()
  })
  const invertedRange = filters.to.isBefore(filters.from)
  const tooLongRange = filters.to.isAfter(filters.from.addDays(7))

  const [report, setReport] = useState<Result<RawReportRow[]>>(Success.of([]))
  const { user } = useContext(UserContext)

  const fetchRawReport = useCallback(
    () => getRawReportResult(filters),
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

  const submitPatuReport = () => sendPatuReportResult(filters)

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.raw.title}</Title>
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
        <FilterRow>
          <LegacyAsyncButton
            primary
            text={i18n.common.search}
            onClick={fetchRawReport}
            onSuccess={(newReport) => setReport(Success.of(newReport))}
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
              data={rows.map((row) => ({
                ...row,
                day: row.day.format(),
                childLink: `${window.location.protocol}//${window.location.host}/employee/child-information/${row.childId}`,
                hasSocialSecurityNumber: mapYesNo(row.hasSocialSecurityNumber),
                dateOfBirth: row.dateOfBirth?.format(),
                placementType: i18n.placement.type[row.placementType],
                unitType:
                  row.unitType && i18n.reports.common.unitTypes[row.unitType],
                unitProviderType:
                  i18n.reports.common.unitProviderTypes[row.unitProviderType],
                caretakersPlanned: mapFloat(row.caretakersPlanned),
                caretakersRealized: mapFloat(row.caretakersRealized),
                hasServiceNeed: mapYesNo(row.hasServiceNeed),
                partDay: mapYesNo(row.partDay),
                partWeek: mapYesNo(row.partWeek),
                shiftCare: mapYesNo(row.shiftCare),
                hoursPerWeek: mapFloat(row.hoursPerWeek),
                hasAssistanceNeed: mapYesNo(row.hasAssistanceNeed),
                assistanceNeedVoucherCoefficient: mapFloat(
                  row.assistanceNeedVoucherCoefficient
                ),
                capacityFactor: mapFloat(row.capacityFactor),
                groupSize: mapFloat(row.capacity),
                realizedCapacity: mapFloat(row.realizedCapacity),
                absencePaid:
                  row.absencePaid &&
                  i18n.absences.absenceTypes[row.absencePaid],
                absenceFree:
                  row.absenceFree && i18n.absences.absenceTypes[row.absenceFree]
              }))}
              headers={[
                { label: 'Päivämäärä', key: 'day' },
                { label: 'Lapsen id', key: 'childId' },
                { label: 'Linkki lapseen', key: 'childLink' },
                { label: 'Sukunimi', key: 'lastName' },
                { label: 'Etunimi', key: 'firstName' },
                { label: 'Hetu', key: 'hasSocialSecurityNumber' },
                { label: 'Syntymäaika', key: 'dateOfBirth' },
                { label: 'Ikä', key: 'age' },
                { label: 'Kieli', key: 'language' },
                { label: 'Postinumero', key: 'postalCode' },
                { label: 'Kotikunta', key: 'postOffice' },
                { label: 'Sijoitustyyppi', key: 'placementType' },
                { label: 'Sijoitettu yksikköön', key: 'unitId' },
                { label: 'Yksikkö', key: 'unitName' },
                { label: 'Palvelualue', key: 'careArea' },
                { label: 'Toimintamuoto', key: 'unitType' },
                { label: 'Järjestämismuoto', key: 'unitProviderType' },
                { label: 'Kustannuspaikka', key: 'costCenter' },
                { label: 'Sijoitettu ryhmään', key: 'daycareGroupId' },
                { label: 'Ryhmä', key: 'groupName' },
                {
                  label: 'Henkilökuntaa ryhmässä',
                  key: 'caretakersPlanned'
                },
                { label: 'Henkilökuntaa läsnä', key: 'caretakersRealized' },
                { label: 'Varahoidossa yksikössä', key: 'backupUnitId' },
                { label: 'Varahoidossa ryhmässä', key: 'backupGroupId' },
                { label: 'Palveluntarve merkitty', key: 'hasServiceNeed' },
                { label: 'Osapäiväinen', key: 'partDay' },
                { label: 'Osaviikkoinen', key: 'partWeek' },
                { label: 'Vuorohoito', key: 'shiftCare' },
                { label: 'Tunteja viikossa', key: 'hoursPerWeek' },
                { label: 'Tuentarve', key: 'hasAssistanceNeed' },
                {
                  label: 'Palvelusetelikerroin',
                  key: 'assistanceNeedVoucherCoefficient'
                },
                { label: 'Tuentarpeen kerroin', key: 'capacityFactor' },
                { label: 'Lapsen kapasiteetti', key: 'capacity' },
                {
                  label: 'Lapsen kapasiteetti (käyttö)',
                  key: 'realizedCapacity'
                },
                { label: 'Poissa maksullisesta', key: 'absencePaid' },
                { label: 'Poissa maksuttomasta', key: 'absenceFree' },
                { label: 'Henkilöstömitoitus', key: 'staffDimensioning' }
              ]}
              filename={`${
                i18n.reports.raw.title
              } ${filters.from.formatIso()}-${filters.to.formatIso()}.csv`}
            />
          ))
        )}
        {user?.accessibleFeatures.submitPatuReport && (
          <div>
            <LegacyAsyncButton
              primary
              text="Lähetä patu-raportti"
              onClick={submitPatuReport}
              // eslint-disable-next-line @typescript-eslint/no-empty-function
              onSuccess={() => {}}
            />
          </div>
        )}
      </ContentArea>
    </Container>
  )
})
