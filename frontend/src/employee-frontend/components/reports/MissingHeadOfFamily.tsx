// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'

import { boolean, localDate2 } from 'lib-common/form/fields'
import { object, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { DatePickerF2 } from 'lib-components/molecules/date-picker/DatePicker'
import { featureFlags } from 'lib-customizations/employee'

import ReportDownload from '../../components/reports/ReportDownload'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { FilterLabel, FilterRow, RowCountInfo, TableScrollable } from './common'
import { missingHeadOfFamilyReportQuery } from './queries'

const filterForm = object({
  startDate: required(localDate2()),
  endDate: localDate2(),
  showIntentionalDuplicates: boolean()
})

export default React.memo(function MissingHeadOfFamily() {
  const { i18n } = useTranslation()

  const filters = useForm(
    filterForm,
    () => ({
      startDate: {
        value: LocalDate.todayInSystemTz().subMonths(1).withDate(1).format(),
        config: undefined
      },
      endDate: {
        value: LocalDate.todayInSystemTz()
          .addMonths(2)
          .lastDayOfMonth()
          .format(),
        config: undefined
      },
      showIntentionalDuplicates: false
    }),
    i18n.validationErrors
  )
  const { startDate, endDate, showIntentionalDuplicates } =
    useFormFields(filters)

  const rows = useQueryResult(missingHeadOfFamilyReportQuery(filters.value()))

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.missingHeadOfFamily.title}</Title>

        <FilterRow>
          <FilterLabel>{i18n.reports.common.startDate}</FilterLabel>
          <DatePickerF2 bind={startDate} locale="fi" />
        </FilterRow>
        <FilterRow>
          <FilterLabel>{i18n.reports.common.endDate}</FilterLabel>
          <DatePickerF2 bind={endDate} locale="fi" />
        </FilterRow>

        {featureFlags.experimental?.personDuplicate && (
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
              filename={`Puuttuvat päämiehet ${filters
                .value()
                .startDate.formatIso()}-${
                filters.value().endDate?.formatIso() ?? ''
              }.csv`}
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
