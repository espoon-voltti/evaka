// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import React, { useCallback, useContext, useMemo, useState } from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import { localDateRange } from 'lib-common/form/fields'
import { object, required, transformed } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import type { StateOf } from 'lib-common/form/types'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'
import type { RawReportRow } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import type { Arg0 } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { featureFlags } from 'lib-customizations/employee'

import type { getRawReport } from '../../generated/api-clients/reports'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow } from './common'
import { rawReportQuery, sendPatuReportMutation } from './queries'

const model = transformed(
  object({ range: required(localDateRange()) }),
  ({ range }) => {
    if (range.durationInDays() > 7) {
      return ValidationError.field('range', 'tooLongRange')
    }
    return ValidationSuccess.of({
      from: range.start,
      to: range.end
    })
  }
)
const initialState = (): StateOf<typeof model> => ({
  range: localDateRange.fromRange(
    new FiniteDateRange(
      LocalDate.todayInSystemTz(),
      LocalDate.todayInSystemTz()
    )
  )
})

export default React.memo(function Raw() {
  const { i18n } = useTranslation()
  const form = useForm(model, initialState, {
    ...i18n.validationErrors,
    tooLongRange: 'Liian pitkä aikaväli (max 7 päivää)'
  })
  const { range } = useFormFields(form)
  const [filters, setFilters] = useState<Arg0<typeof getRawReport>>()
  const dirty = useMemo(
    () => !form.isValid() || !isEqual(form.value(), filters),
    [filters, form]
  )
  const report = useQueryResult(
    filters !== undefined
      ? rawReportQuery(filters)
      : constantQuery<RawReportRow[]>([])
  )

  const { user } = useContext(UserContext)

  const fetchRawReport = useCallback(() => {
    setFilters(form.value())
  }, [form])

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

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.raw.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <FlexRow>
            <DateRangePickerF bind={range} locale="fi" />
          </FlexRow>
        </FilterRow>
        <FilterRow>
          <Button
            primary
            text={i18n.common.search}
            onClick={fetchRawReport}
            disabled={!form.isValid()}
            data-qa="send-button"
          />
        </FilterRow>
        {renderResult(report, (rows) => (
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
            } ${filters?.from?.formatIso()}-${filters?.to?.formatIso()}.csv`}
            disabled={dirty}
          />
        ))}
        {user?.accessibleFeatures.submitPatuReport && (
          <div>
            <MutateButton
              primary
              text="Lähetä patu-raportti"
              mutation={sendPatuReportMutation}
              onClick={() => ({
                from: LocalDate.parseFiOrThrow(range.state.start),
                to: LocalDate.parseFiOrThrow(range.state.end)
              })}
            />
          </div>
        )}
      </ContentArea>
    </Container>
  )
})
