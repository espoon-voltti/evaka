// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import range from 'lodash/range'
import React, { useMemo } from 'react'
import { Link } from 'react-router-dom'

import { object, oneOf, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { ExceededServiceNeedReportUnit } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { queryOrDefault, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'

import { useTranslation } from '../../state/i18n'
import { formatName } from '../../utils'
import { renderResult } from '../async-rendering'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow } from './common'
import {
  exceededServiceNeedReportUnitsQuery,
  exceededServiceNeedsReportRowsQuery
} from './queries'

export default React.memo(function ReportExceededServiceNeeds() {
  const { i18n } = useTranslation()
  const units = useQueryResult(exceededServiceNeedReportUnitsQuery())
  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.exceededServiceNeed.title}</Title>
        {renderResult(units, (units) => (
          <Report units={units} />
        ))}
      </ContentArea>
    </Container>
  )
})

const filtersForm = object({
  unitId: required(oneOf<string>()),
  year: required(oneOf<number>()),
  month: required(oneOf<number>())
})

const yearOptions = (max: number, min: number) => ({
  domValue: max.toString(),
  options: range(max, min - 1, -1).map((year) => ({
    value: year,
    domValue: year.toString(),
    label: year.toString()
  }))
})
const monthOptions = (initialMonth: number, monthNames: string[]) => ({
  domValue: initialMonth.toString(),
  options: monthNames.map((name, index) => ({
    value: index + 1,
    domValue: (index + 1).toString(),
    label: name
  }))
})

const Report = React.memo(function Report({
  units
}: {
  units: ExceededServiceNeedReportUnit[]
}) {
  const { i18n } = useTranslation()
  const today = LocalDate.todayInHelsinkiTz()
  const unitOptions = useMemo(
    () =>
      units.map((unit) => ({
        domValue: unit.id,
        label: unit.name,
        value: unit.id
      })),
    [units]
  )
  const form = useForm(
    filtersForm,
    () => ({
      unitId: {
        domValue: unitOptions[0]?.domValue ?? '',
        options: unitOptions
      },
      year: yearOptions(today.year, 2020),
      month: monthOptions(today.month, i18n.datePicker.months)
    }),
    i18n.validationErrors
  )
  const { unitId, year, month } = useFormFields(form)

  const params = form.isValid() ? form.value() : undefined
  const unitName = useMemo(
    () => units.find((u) => u.id === params?.unitId)?.name,
    [params, units]
  )
  const reportRows = useQueryResult(
    queryOrDefault(exceededServiceNeedsReportRowsQuery, null)(params)
  )

  return (
    <>
      <div>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.period}</FilterLabel>
          <SelectF bind={month} />
          <SelectF bind={year} />
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.unitName}</FilterLabel>
          <SelectF bind={unitId} />
        </FilterRow>
      </div>
      <div>
        {renderResult(reportRows, (rows) =>
          rows ? (
            <>
              <ReportDownload
                data={[
                  [
                    i18n.reports.common.childName,
                    i18n.reports.exceededServiceNeed.serviceNeedHours,
                    i18n.reports.exceededServiceNeed.usedServiceHours,
                    i18n.reports.exceededServiceNeed.excessHours
                  ],
                  ...rows.map((row) => [
                    formatName(
                      row.childFirstName,
                      row.childLastName,
                      i18n,
                      true
                    ),
                    row.serviceNeedHoursPerMonth,
                    row.usedServiceHours
                  ])
                ]}
                filename={() => {
                  if (!params || !unitName)
                    throw new Error('BUG: missing params')
                  return getFilename(params.year, params.month, unitName)
                }}
              />
              <Table>
                <Thead>
                  <Tr>
                    <Th>{i18n.reports.common.childName}</Th>
                    <Th>{i18n.reports.exceededServiceNeed.serviceNeedHours}</Th>
                    <Th>{i18n.reports.exceededServiceNeed.usedServiceHours}</Th>
                    <Th>{i18n.reports.exceededServiceNeed.excessHours}</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {rows.length > 0 ? (
                    rows.map((row) => (
                      <Tr key={row.childId}>
                        <Td>
                          <Link to={`/child-information/${row.childId}`}>
                            {formatName(
                              row.childFirstName,
                              row.childLastName,
                              i18n,
                              true
                            )}
                          </Link>
                        </Td>
                        <Td>{row.serviceNeedHoursPerMonth}</Td>
                        <Td>{row.usedServiceHours}</Td>
                        <Td>{row.excessHours}</Td>
                      </Tr>
                    ))
                  ) : (
                    <Td colSpan={4}>{i18n.common.noResults}</Td>
                  )}
                </Tbody>
              </Table>
            </>
          ) : null
        )}
      </div>
    </>
  )
})

function getFilename(year: number, month: number, unitName: string) {
  return `palveluntarpeen_ylitykset-${year}_${month.toString().padStart(2, '0')}-${unitName}.csv`.replace(
    / /g,
    '_'
  )
}
