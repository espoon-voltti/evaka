// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { localDateRange } from 'lib-common/form/fields'
import { object, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import LocalDate from 'lib-common/local-date'
import { queryOrDefault, useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'

import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { FilterLabel, FilterRow } from './common'
import { childAttendanceReportQuery } from './queries'

const filterForm = object({
  range: required(localDateRange())
})

export default React.memo(function ChildAttendanceReport() {
  const { i18n, lang } = useTranslation()
  const { childId } = useRouteParams(['childId'])

  const filters = useForm(
    filterForm,
    () => ({
      range: localDateRange.fromDates(
        LocalDate.todayInSystemTz().subDays(7),
        LocalDate.todayInSystemTz().addDays(7)
      )
    }),
    i18n.validationErrors
  )
  const { range } = useFormFields(filters)

  const rowsResult = useQueryResult(
    queryOrDefault(
      childAttendanceReportQuery,
      []
    )(
      range.isValid()
        ? { childId: childId, from: range.value().start, to: range.value().end }
        : null
    )
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.childAttendance.title}</Title>

        <FilterRow>
          <FilterLabel>{i18n.reports.childAttendance.range}</FilterLabel>
          <DateRangePickerF bind={range} locale={lang} />
        </FilterRow>

        {renderResult(rowsResult, (rows) => (
          <>
            <ReportDownload
              data={rows.map((row) => ({
                date: row.date.format(),
                reservations: row.reservations
                  .map((r) => r.format())
                  .join(', '),
                attendances: row.attendances.map((a) => a.format()).join(', '),
                absenceBillable: row.billableAbsence
                  ? i18n.absences.absenceTypes[row.billableAbsence]
                  : '',
                absenceNonbillable: row.nonbillableAbsence
                  ? i18n.absences.absenceTypes[row.nonbillableAbsence]
                  : ''
              }))}
              headers={[
                { key: 'date', label: i18n.reports.childAttendance.date },
                {
                  key: 'reservations',
                  label: i18n.reports.childAttendance.reservations
                },
                {
                  key: 'attendances',
                  label: i18n.reports.childAttendance.attendances
                },
                {
                  key: 'absenceBillable',
                  label: i18n.reports.childAttendance.absenceBillable
                },
                {
                  key: 'absenceNonbillable',
                  label: i18n.reports.childAttendance.absenceNonbillable
                }
              ]}
              filename={`${i18n.reports.childAttendance.title}.csv`}
            />
            <Table>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.childAttendance.date}</Th>
                  <Th>{i18n.reports.childAttendance.reservations}</Th>
                  <Th>{i18n.reports.childAttendance.attendances}</Th>
                  <Th>{i18n.reports.childAttendance.absenceBillable}</Th>
                  <Th>{i18n.reports.childAttendance.absenceNonbillable}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.map((row) => (
                  <Tr key={row.date.formatIso()}>
                    <Td>{row.date.format()}</Td>
                    <Td>
                      <StyledUl>
                        {row.reservations.map((r, i) => (
                          <li key={i}>{r.format()}</li>
                        ))}
                      </StyledUl>
                    </Td>
                    <Td>
                      <StyledUl>
                        {row.attendances.map((a, i) => (
                          <li key={i}>{a.format()}</li>
                        ))}
                      </StyledUl>
                    </Td>
                    <Td>
                      {row.billableAbsence &&
                        i18n.absences.absenceTypes[row.billableAbsence]}
                    </Td>
                    <Td>
                      {row.nonbillableAbsence &&
                        i18n.absences.absenceTypes[row.nonbillableAbsence]}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})

const StyledUl = styled.ul`
  margin-block-start: 0;
  margin-block-end: 0;
`
