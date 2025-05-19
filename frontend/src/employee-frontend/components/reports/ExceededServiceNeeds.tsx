// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import range from 'lodash/range'
import React, { useMemo } from 'react'
import { Link } from 'react-router'

import { object, oneOf, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import type { ExceededServiceNeedReportUnit } from 'lib-common/generated/api-types/reports'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
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
  unitId: required(oneOf<DaycareId>()),
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
    params ? exceededServiceNeedsReportRowsQuery(params) : constantQuery(null)
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
                data={rows.map((row) => ({
                  childName: formatName(
                    row.childFirstName,
                    row.childLastName,
                    i18n,
                    true
                  ),
                  ...row
                }))}
                columns={[
                  {
                    label: i18n.reports.common.childName,
                    value: (row) => row.childName
                  },
                  {
                    label: i18n.reports.exceededServiceNeed.serviceNeedHours,
                    value: (row) => row.serviceNeedHoursPerMonth
                  },
                  {
                    label: i18n.reports.exceededServiceNeed.usedServiceHours,
                    value: (row) => row.usedServiceHours
                  },
                  {
                    label: i18n.reports.exceededServiceNeed.excessHours,
                    value: (row) => row.excessHours
                  }
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
                    <Th>{i18n.reports.exceededServiceNeed.groupLinkHeading}</Th>
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
                        <Td>
                          {!!row.groupId && (
                            <>
                              <Link
                                to={`/units/${row.unitId}/calendar?group=${row.groupId}&date=${year.value()}-${String(month.value()).padStart(2, '0')}-01&mode=week`}
                              >
                                {row.groupName}
                              </Link>
                            </>
                          )}
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
