// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'wouter'

import { localDate } from 'lib-common/form/fields'
import { object, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import Title from 'lib-components/atoms/Title'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Th, Tr, Td, Thead, Tbody } from 'lib-components/layout/Table'
import { PersonName } from 'lib-components/molecules/PersonNames'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { daycareQuery } from '../unit/queries'

import ReportDownload from './ReportDownload'
import { FilterLabel, FilterRow, TableScrollable } from './common'
import { familyContactsReportQuery } from './queries'

const filterForm = object({
  date: required(localDate())
})

export default React.memo(function FamilyContacts() {
  const unitId = useIdRouteParam<DaycareId>('unitId')
  const { i18n, lang } = useTranslation()

  const filters = useForm(
    filterForm,
    () => ({
      date: localDate.fromDate(LocalDate.todayInSystemTz())
    }),
    i18n.validationErrors
  )
  const { date } = useFormFields(filters)

  const rows = useQueryResult(
    filters.isValid()
      ? familyContactsReportQuery({ unitId, date: date.value() })
      : constantQuery([])
  )

  const unit = useQueryResult(daycareQuery({ daycareId: unitId }))

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {renderResult(unit, (unit) => (
          <Title size={1}>{unit.daycare.name}</Title>
        ))}

        <FilterRow>
          <FilterLabel>{i18n.reports.familyContacts.date}</FilterLabel>
          <DatePickerF bind={date} locale={lang} info={date.inputInfo()} />
        </FilterRow>

        {renderResult(rows, (rows) => (
          <>
            <ReportDownload
              data={rows.map((row) => ({
                name: formatPersonName(row, 'Last First'),
                ssn: row.ssn,
                groupName: row.groupName,
                address: `${row.streetAddress}, ${row.postalCode} ${row.postOffice}`,
                headOfChildName: row.headOfChild
                  ? formatPersonName(row.headOfChild, 'Last First')
                  : '',
                headOfChildPhone: row.headOfChild?.phone ?? '',
                headOfChildEmail: row.headOfChild?.email ?? '',

                guardian1Name: row.guardian1
                  ? formatPersonName(row.guardian1, 'Last First')
                  : '',
                guardian1Phone: row.guardian1?.phone ?? '',
                guardian1Email: row.guardian1?.email ?? '',

                guardian2Name: row.guardian2
                  ? formatPersonName(row.guardian2, 'Last First')
                  : '',
                guardian2Phone: row.guardian2?.phone ?? '',
                guardian2Email: row.guardian2?.email ?? ''
              }))}
              columns={[
                {
                  label: i18n.reports.familyContacts.name,
                  value: (row) => row.name
                },
                {
                  label: i18n.reports.familyContacts.ssn,
                  value: (row) => row.ssn
                },
                {
                  label: i18n.reports.familyContacts.group,
                  value: (row) => row.groupName
                },
                {
                  label: i18n.reports.familyContacts.address,
                  value: (row) => row.address
                },
                {
                  label: i18n.reports.familyContacts.headOfChild,
                  value: (row) => row.headOfChildName
                },
                {
                  label: `${i18n.reports.familyContacts.headOfChild}: ${i18n.reports.familyContacts.phone}`,
                  value: (row) => row.headOfChildPhone
                },
                {
                  label: `${i18n.reports.familyContacts.headOfChild}: ${i18n.reports.familyContacts.email}`,
                  value: (row) => row.headOfChildEmail
                },
                {
                  label: i18n.reports.familyContacts.guardian1,
                  value: (row) => row.guardian1Name
                },
                {
                  label: `${i18n.reports.familyContacts.guardian1}: ${i18n.reports.familyContacts.phone}`,
                  value: (row) => row.guardian1Phone
                },
                {
                  label: `${i18n.reports.familyContacts.guardian1}: ${i18n.reports.familyContacts.email}`,
                  value: (row) => row.guardian1Email
                },
                {
                  label: i18n.reports.familyContacts.guardian2,
                  value: (row) => row.guardian2Name
                },
                {
                  label: `${i18n.reports.familyContacts.guardian2}: ${i18n.reports.familyContacts.phone}`,
                  value: (row) => row.guardian2Phone
                },
                {
                  label: `${i18n.reports.familyContacts.guardian2}: ${i18n.reports.familyContacts.email}`,
                  value: (row) => row.guardian2Email
                }
              ]}
              filename="Perheiden yhteystiedot.csv"
            />
            <TableScrollable>
              <Thead>
                <Tr>
                  <Th>{i18n.reports.familyContacts.name}</Th>
                  <Th>{i18n.reports.familyContacts.ssn}</Th>
                  <Th>{i18n.reports.familyContacts.group}</Th>
                  <Th>{i18n.reports.familyContacts.address}</Th>
                  <Th>{i18n.reports.familyContacts.headOfChild}</Th>
                  <Th>{i18n.reports.familyContacts.guardian1}</Th>
                  <Th>{i18n.reports.familyContacts.guardian2}</Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.map((row) => (
                  <Tr key={row.id}>
                    <Td>
                      <Link to={`/child-information/${row.id}`}>
                        <PersonName person={row} format="Last First" />
                      </Link>
                    </Td>
                    <Td>{row.ssn}</Td>
                    <Td>{row.groupName}</Td>
                    <Td>
                      {`${row.streetAddress}, ${row.postalCode} ${row.postOffice}`}
                    </Td>
                    <Td>
                      {row.headOfChild && (
                        <>
                          <div>
                            <PersonName
                              person={row.headOfChild}
                              format="Last First"
                            />
                          </div>
                          <div>{row.headOfChild.phone}</div>
                          <div>{row.headOfChild.email}</div>
                        </>
                      )}
                    </Td>
                    <Td>
                      {row.guardian1 && (
                        <>
                          <div>
                            <PersonName
                              person={row.guardian1}
                              format="Last First"
                            />
                          </div>
                          <div>{row.guardian1.phone}</div>
                          <div>{row.guardian1.email}</div>
                        </>
                      )}
                    </Td>
                    <Td>
                      {row.guardian2 && (
                        <>
                          <div>
                            <PersonName
                              person={row.guardian2}
                              format="Last First"
                            />
                          </div>
                          <div>{row.guardian2.phone}</div>
                          <div>{row.guardian2.email}</div>
                        </>
                      )}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </TableScrollable>
          </>
        ))}
      </ContentArea>
    </Container>
  )
})
