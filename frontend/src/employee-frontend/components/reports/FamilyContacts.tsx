// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'

import {
  Container,
  ContentArea
} from '@evaka/lib-components/layout/Container'
import Loader from '@evaka/lib-components/atoms/Loader'
import Title from '@evaka/lib-components/atoms/Title'
import {
  Th,
  Tr,
  Td,
  Thead,
  Tbody
} from '@evaka/lib-components/layout/Table'
import { useTranslation } from '../../state/i18n'
import { Link, useParams } from 'react-router-dom'
import { Loading, Result } from '@evaka/lib-common/api'
import { FamilyContactsReportRow } from '../../types/reports'
import { getFamilyContactsReport } from '../../api/reports'
import ReturnButton from '@evaka/lib-components/atoms/buttons/ReturnButton'
import ReportDownload from '../../components/reports/ReportDownload'
import { TableScrollable } from '../../components/reports/common'
import { UUID } from '../../types'
import { getDaycare, UnitResponse } from '../../api/unit'

function FamilyContacts() {
  const { unitId } = useParams<{ unitId: UUID }>()
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<FamilyContactsReportRow[]>>(
    Loading.of()
  )
  const [unit, setUnit] = useState<Result<UnitResponse>>(Loading.of())

  useEffect(() => {
    setRows(Loading.of())
    setUnit(Loading.of())
    void getFamilyContactsReport(unitId).then(setRows)
    void getDaycare(unitId).then(setUnit)
  }, [])

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {unit.isSuccess && <Title size={1}>{unit.value.daycare.name}</Title>}

        {rows.isLoading && <Loader />}
        {rows.isFailure && <span>{i18n.common.loadingFailed}</span>}
        {rows.isSuccess && (
          <>
            <ReportDownload
              data={rows.value.map((row) => ({
                name: `${row.lastName} ${row.firstName}`,
                ssn: row.ssn,
                group: row.group,
                address: `${row.streetAddress}, ${row.postalCode} ${row.postOffice}`,
                headOfChildName: row.headOfChild
                  ? `${row.headOfChild.lastName} ${row.headOfChild.firstName}`
                  : '',
                headOfChildPhone: row.headOfChild?.phone ?? '',
                headOfChildEmail: row.headOfChild?.email ?? '',

                guardian1Name: row.guardian1
                  ? `${row.guardian1.lastName} ${row.guardian1.firstName}`
                  : '',
                guardian1Phone: row.guardian1?.phone ?? '',
                guardian1Email: row.guardian1?.email ?? '',

                guardian2Name: row.guardian2
                  ? `${row.guardian2.lastName} ${row.guardian2.firstName}`
                  : '',
                guardian2Phone: row.guardian2?.phone ?? '',
                guardian2Email: row.guardian2?.email ?? ''
              }))}
              headers={[
                { label: i18n.reports.familyContacts.name, key: 'name' },
                { label: i18n.reports.familyContacts.ssn, key: 'ssn' },
                { label: i18n.reports.familyContacts.group, key: 'group' },
                { label: i18n.reports.familyContacts.address, key: 'address' },
                {
                  label: i18n.reports.familyContacts.headOfChild,
                  key: 'headOfChildName'
                },
                {
                  label: `${i18n.reports.familyContacts.headOfChild}: ${i18n.reports.familyContacts.phone}`,
                  key: 'headOfChildPhone'
                },
                {
                  label: `${i18n.reports.familyContacts.headOfChild}: ${i18n.reports.familyContacts.email}`,
                  key: 'headOfChildEmail'
                },
                {
                  label: i18n.reports.familyContacts.guardian1,
                  key: 'guardian1Name'
                },
                {
                  label: `${i18n.reports.familyContacts.guardian1}: ${i18n.reports.familyContacts.phone}`,
                  key: 'guardian1Phone'
                },
                {
                  label: `${i18n.reports.familyContacts.guardian1}: ${i18n.reports.familyContacts.email}`,
                  key: 'guardian1Email'
                },
                {
                  label: i18n.reports.familyContacts.guardian2,
                  key: 'guardian2Name'
                },
                {
                  label: `${i18n.reports.familyContacts.guardian2}: ${i18n.reports.familyContacts.phone}`,
                  key: 'guardian2Phone'
                },
                {
                  label: `${i18n.reports.familyContacts.guardian2}: ${i18n.reports.familyContacts.email}`,
                  key: 'guardian2Email'
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
                {rows.value.map((row) => (
                  <Tr key={row.id}>
                    <Td>
                      <Link
                        to={`/child-information/${row.id}`}
                      >{`${row.lastName} ${row.firstName}`}</Link>
                    </Td>
                    <Td>{row.ssn}</Td>
                    <Td>{row.group}</Td>
                    <Td>
                      {`${row.streetAddress}, ${row.postalCode} ${row.postOffice}`}
                    </Td>
                    <Td>
                      {row.headOfChild && (
                        <>
                          <div>{`${row.headOfChild.lastName} ${row.headOfChild.firstName}`}</div>
                          <div>{row.headOfChild.phone}</div>
                          <div>{row.headOfChild.email}</div>
                        </>
                      )}
                    </Td>
                    <Td>
                      {row.guardian1 && (
                        <>
                          <div>{`${row.guardian1.lastName} ${row.guardian1.firstName}`}</div>
                          <div>{row.guardian1.phone}</div>
                          <div>{row.guardian1.email}</div>
                        </>
                      )}
                    </Td>
                    <Td>
                      {row.guardian2 && (
                        <>
                          <div>{`${row.guardian2.lastName} ${row.guardian2.firstName}`}</div>
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
        )}
      </ContentArea>
    </Container>
  )
}

export default FamilyContacts
