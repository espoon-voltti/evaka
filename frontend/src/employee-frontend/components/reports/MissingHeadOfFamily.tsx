// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'

import { boolean, openEndedLocalDateRange } from 'lib-common/form/fields'
import { object, required, transformed } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { ValidationSuccess } from 'lib-common/form/types'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { featureFlags } from 'lib-customizations/employee'

import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'
import { missingHeadOfFamilyReportQuery } from './queries'

const filterForm = transformed(
  object({
    range: required(openEndedLocalDateRange()),
    showIntentionalDuplicates: boolean()
  }),
  ({ range: { start, end }, showIntentionalDuplicates }) =>
    ValidationSuccess.of({
      from: start,
      to: end,
      showIntentionalDuplicates
    })
)

export default React.memo(function MissingHeadOfFamily() {
  const { i18n, lang } = useTranslation()

  const filters = useForm(
    filterForm,
    () => ({
      range: openEndedLocalDateRange.fromDates(
        LocalDate.todayInSystemTz().subMonths(1).withDate(1),
        LocalDate.todayInSystemTz().addMonths(2).lastDayOfMonth()
      ),
      showIntentionalDuplicates: false
    }),
    i18n.validationErrors
  )
  const { range, showIntentionalDuplicates } = useFormFields(filters)

  const rows = useQueryResult(
    filters.isValid()
      ? missingHeadOfFamilyReportQuery(filters.value())
      : constantQuery([])
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.missingHeadOfFamily.title}</Title>

        <FilterRow>
          <DateRangePickerF bind={range} locale={lang} />
        </FilterRow>

        {featureFlags.personDuplicate && (
          <FilterRow>
            <FilterLabel />
            <CheckboxF
              bind={showIntentionalDuplicates}
              label={i18n.reports.common.filters.showIntentionalDuplicates}
              data-qa="show-intentional-duplicates-checkbox"
            />
          </FilterRow>
        )}

        {renderResult(rows, (rows) => (
          <>
            <ReportDownload
              data={rows.map((row) => ({
                ...row,
                rangesWithoutHead: row.rangesWithoutHead
                  .map((range) => range.format())
                  .join(', ')
              }))}
              headers={[
                {
                  label: i18n.reports.missingHeadOfFamily.childLastName,
                  key: 'lastName'
                },
                {
                  label: i18n.reports.missingHeadOfFamily.childFirstName,
                  key: 'firstName'
                },
                {
                  label:
                    i18n.reports.missingHeadOfFamily.daysWithoutHeadOfFamily,
                  key: 'rangesWithoutHead'
                }
              ]}
              filename={
                filters.isValid()
                  ? `Puuttuvat päämiehet ${filters.value().from.formatIso()}-${
                      filters.value().to?.formatIso() ?? ''
                    }.csv`
                  : 'Puuttuvat päämiehet.csv'
              }
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.common.childName}</Th>
                  <Th>
                    {i18n.reports.missingHeadOfFamily.daysWithoutHeadOfFamily}
                  </Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.map((row) => (
                  <Tr data-qa="missing-head-of-family-row" key={row.childId}>
                    <Td data-qa="child-name">
                      <Link to={`/child-information/${row.childId}`}>
                        {row.lastName} {row.firstName}
                      </Link>
                    </Td>
                    <Td data-qa="ranges-without-head">
                      {row.rangesWithoutHead
                        .map((range) => range.format())
                        .join(', ')}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
            <RowCountInfo rowCount={rows.length} />
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
