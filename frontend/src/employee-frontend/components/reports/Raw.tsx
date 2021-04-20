// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { Container, ContentArea } from 'lib-components/layout/Container'
import Loader from 'lib-components/atoms/Loader'
import Title from 'lib-components/atoms/Title'
import { useTranslation } from '../../state/i18n'
import { Loading, Result } from 'lib-common/api'
import { RawReportRow } from '../../types/reports'
import { getRawReport, PeriodFilters } from '../../api/reports'
import ReportDownload from '../../components/reports/ReportDownload'
import { FilterLabel, FilterRow } from '../../components/reports/common'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import LocalDate from 'lib-common/local-date'
import { FlexRow } from '../../components/common/styled/containers'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'

function Raw() {
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<RawReportRow[]>>(Loading.of())
  const [filters, setFilters] = useState<PeriodFilters>({
    from: LocalDate.today(),
    to: LocalDate.today()
  })
  const invertedRange = filters.to.isBefore(filters.from)
  const tooLongRange = filters.to.isAfter(filters.from.addDays(7))

  useEffect(() => {
    setRows(Loading.of())
    if (!invertedRange && !tooLongRange) {
      void getRawReport(filters).then(setRows)
    }
  }, [filters])

  const mapYesNo = (value: boolean | null | undefined) => {
    if (value === true) {
      return i18n.common.yes
    } else if (value === false) {
      return i18n.common.no
    } else {
      return null
    }
  }

  const mapFloat = (value: number | null) => {
    if (value === null) {
      return null
    } else {
      return value.toString().replace(/\./, ',')
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
                    childLink: `https://espoonvarhaiskasvatus.fi/employee/child-information/${row.childId}`,
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
                    preparatory: mapYesNo(row.preparatory),
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
                    { label: 'Valmistava', key: 'preparatory' },
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
      </ContentArea>
    </Container>
  )
}

export default Raw
