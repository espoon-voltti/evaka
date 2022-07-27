// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'

import type { Result } from 'lib-common/api'
import { Loading } from 'lib-common/api'
import type { RawReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'

import type { PeriodFilters } from '../../api/reports'
import { getRawReport, sendPatuReport } from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { FlexRow } from '../common/styled/containers'

import { FilterLabel, FilterRow } from './common'

export default React.memo(function Raw() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<RawReportRow[]>>(Loading.of())
  const [filters, setFilters] = useState<PeriodFilters>({
    from: LocalDate.todayInSystemTz(),
    to: LocalDate.todayInSystemTz()
  })
  const invertedRange = filters.to.isBefore(filters.from)
  const tooLongRange = filters.to.isAfter(filters.from.addDays(7))

  useEffect(() => {
    setRows(Loading.of())
    if (!invertedRange && !tooLongRange) {
      void getRawReport(filters).then(setRows)
    }
  }, [filters, invertedRange, tooLongRange])

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
    return sendPatuReport(filters)
  }

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

        {invertedRange ? (
          <span>Virheellinen aikaväli</span>
        ) : tooLongRange ? (
          <span>Liian pitkä aikaväli (max 7 päivää)</span>
        ) : (
          <>
            {rows.isLoading && <Loader />}
            {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
            {rows.isSuccess && (
              <>
                <ReportDownload
                  data={rows.value.map((row) => ({
                    ...row,
                    day: row.day.format(),
                    childLink: `${window.location.protocol}//${window.location.host}/employee/child-information/${row.childId}`,
                    dateOfBirth: row.dateOfBirth?.format(),
                    placementType: i18n.placement.type[row.placementType],
                    unitType:
                      row.unitType &&
                      i18n.reports.common.unitTypes[row.unitType],
                    unitProviderType:
                      i18n.reports.common.unitProviderTypes[
                        row.unitProviderType
                      ],
                    caretakersPlanned: mapFloat(row.caretakersPlanned),
                    caretakersRealized: mapFloat(row.caretakersRealized),
                    hasServiceNeed: mapYesNo(row.hasServiceNeed),
                    partDay: mapYesNo(row.partDay),
                    partWeek: mapYesNo(row.partWeek),
                    shiftCare: mapYesNo(row.shiftCare),
                    hoursPerWeek: mapFloat(row.hoursPerWeek),
                    hasAssistanceNeed: mapYesNo(row.hasAssistanceNeed),
                    capacityFactor: mapFloat(row.capacityFactor),
                    capacity: mapFloat(row.capacity),
                    absencePaid:
                      row.absencePaid &&
                      i18n.absences.absenceTypes[row.absencePaid],
                    absenceFree:
                      row.absenceFree &&
                      i18n.absences.absenceTypes[row.absenceFree]
                  }))}
                  headers={[
                    { label: 'Päivämäärä', key: 'day' },
                    { label: 'Lapsen id', key: 'childId' },
                    { label: 'Linkki lapseen', key: 'childLink' },
                    { label: 'Sukunimi', key: 'lastName' },
                    { label: 'Etunimi', key: 'firstName' },
                    { label: 'Syntymäaika', key: 'dateOfBirth' },
                    { label: 'Ikä', key: 'age' },
                    { label: 'Kieli', key: 'language' },
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
                    { label: 'Tuentarpeen kerroin', key: 'capacityFactor' },
                    { label: 'Lapsen kapasiteetti', key: 'capacity' },
                    { label: 'Poissa maksullisesta', key: 'absencePaid' },
                    { label: 'Poissa maksuttomasta', key: 'absenceFree' }
                  ]}
                  filename={`${
                    i18n.reports.raw.title
                  } ${filters.from.formatIso()}-${filters.to.formatIso()}.csv`}
                />
              </>
            )}
          </>
        )}
        <div hidden>
          <AsyncButton
            primary
            text="Lähetä patu-raportti"
            onClick={submitPatuReport}
            // eslint-disable-next-line @typescript-eslint/no-empty-function
            onSuccess={() => {}}
          />
        </div>
      </ContentArea>
    </Container>
  )
})
