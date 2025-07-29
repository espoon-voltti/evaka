// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { localDate } from 'lib-common/form/fields'
import { object, oneOf, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import type {
  AreaJSON,
  ProviderType
} from 'lib-common/generated/api-types/daycare'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { ServiceNeedReportRow } from 'lib-common/generated/api-types/reports'
import type { AreaId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { placementTypes, unitProviderTypes } from 'lib-customizations/employee'

import { areasQuery } from '../../queries'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { serviceNeedReportQuery } from './queries'

const filtersForm = object({
  date: required(localDate()),
  areaId: oneOf<AreaId>(),
  providerType: oneOf<ProviderType>(),
  placementType: oneOf<PlacementType>()
})

const supportedPlacementTypes = placementTypes.filter(
  (placementType) => placementType !== 'CLUB'
)

export default React.memo(function ServiceNeeds() {
  const areasResult = useQueryResult(areasQuery())

  return renderResult(areasResult, (areas) => (
    <ServiceNeedsInner areas={areas} />
  ))
})

const ServiceNeedsInner = (props: { areas: AreaJSON[] }) => {
  const { i18n, lang } = useTranslation()
  const form = useForm(
    filtersForm,
    () => ({
      date: localDate.fromDate(LocalDate.todayInSystemTz()),
      areaId: {
        domValue: '',
        options: props.areas.map((area) => ({
          value: area.id,
          domValue: area.id,
          label: area.name
        }))
      },
      providerType: {
        domValue: '',
        options: unitProviderTypes.map((providerType) => ({
          value: providerType,
          domValue: providerType,
          label: i18n.common.providerType[providerType]
        }))
      },
      placementType: {
        domValue: '',
        options: supportedPlacementTypes.map((placementType) => ({
          value: placementType,
          domValue: placementType,
          label: i18n.placement.type[placementType]
        }))
      }
    }),
    i18n.validationErrors
  )
  const { date, areaId, providerType, placementType } = useFormFields(form)
  const rows = useQueryResult(
    form.isValid() ? serviceNeedReportQuery(form.value()) : constantQuery([])
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.serviceNeeds.title}</Title>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.date}</FilterLabel>
          <DatePickerF bind={date} locale={lang} />
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.careAreaName}</FilterLabel>
          <SelectF bind={areaId} placeholder={i18n.common.all} />
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.unitProviderType}</FilterLabel>
          <SelectF bind={providerType} placeholder={i18n.common.all} />
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.placementType}</FilterLabel>
          <SelectF bind={placementType} placeholder={i18n.common.all} />
        </FilterRow>

        {renderResult(rows, (rows) => (
          <>
            <ReportDownload
              data={rows}
              columns={[
                { label: 'Palvelualue', value: (row) => row.careAreaName },
                { label: 'Yksikkö', value: (row) => row.unitName },
                { label: 'Ikä', value: (row) => row.age },
                { label: 'Kokopäiväinen', value: (row) => row.fullDay },
                { label: 'Osapäiväinen', value: (row) => row.partDay },
                { label: 'Kokoviikkoinen', value: (row) => row.fullWeek },
                { label: 'Osaviikkoinen', value: (row) => row.partWeek },
                { label: 'Vuorohoito', value: (row) => row.shiftCare },
                {
                  label: 'Palveluntarve puuttuu',
                  value: (row) => row.missingServiceNeed
                },
                { label: 'Lapsia yhteensä', value: (row) => row.total }
              ]}
              filename={`Lapsien palvelutarpeet ja iät yksiköissä ${date.isValid() ? date.value().formatIso() : ''}.csv`}
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.careAreaName}</Th>
                  <Th>{i18n.reports.common.unitName}</Th>
                  <Th>{i18n.reports.common.unitType}</Th>
                  <Th>{i18n.reports.common.unitProviderType}</Th>
                  <Th>{i18n.reports.serviceNeeds.age}</Th>
                  <Th>{i18n.reports.serviceNeeds.fullDay}</Th>
                  <Th>{i18n.reports.serviceNeeds.partDay}</Th>
                  <Th>{i18n.reports.serviceNeeds.fullWeek}</Th>
                  <Th>{i18n.reports.serviceNeeds.partWeek}</Th>
                  <Th>{i18n.reports.serviceNeeds.shiftCare}</Th>
                  <Th>{i18n.reports.serviceNeeds.missingServiceNeed}</Th>
                  <Th>{i18n.reports.serviceNeeds.total}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.map((row: ServiceNeedReportRow) => (
                  <Tr key={`${row.unitName}:${row.age}`}>
                    <Td>{row.careAreaName}</Td>
                    <Td>{row.unitName}</Td>
                    <Td>
                      {row.unitType
                        ? i18n.reports.common.unitTypes[row.unitType]
                        : ''}
                    </Td>
                    <Td>
                      {
                        i18n.reports.common.unitProviderTypes[
                          row.unitProviderType
                        ]
                      }
                    </Td>
                    <Td>{row.age}</Td>
                    <Td>{row.fullDay}</Td>
                    <Td>{row.partDay}</Td>
                    <Td>{row.fullWeek}</Td>
                    <Td>{row.partWeek}</Td>
                    <Td>{row.shiftCare}</Td>
                    <Td>{row.missingServiceNeed}</Td>
                    <Td>{row.total}</Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        ))}
      </ContentArea>
    </Container>
  )
}
