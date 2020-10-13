// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'

import { Container, ContentArea } from '~components/shared/layout/Container'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import { Th, Tr, Td, Thead, Tbody } from '~components/shared/layout/Table'
import { useTranslation } from '~state/i18n'
import { Link, useParams } from 'react-router-dom'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { FamilyContactsReportRow } from '~types/reports'
import { getFamilyContactsReport } from '~api/reports'
import ReturnButton from 'components/shared/atoms/buttons/ReturnButton'
import ReportDownload from '~components/reports/ReportDownload'
import { TableScrollable } from 'components/reports/common'
import { UUID } from '~types'
import { getDaycare, UnitResponse } from '~api/unit'
import _ from 'lodash'

function FamilyContacts() {
  const { unitId } = useParams<{ unitId: UUID }>()
  const { i18n } = useTranslation()
  const [rows, setRows] = useState<Result<FamilyContactsReportRow[]>>(Loading())
  const [unit, setUnit] = useState<Result<UnitResponse>>(Loading())

  useEffect(() => {
    setRows(Loading())
    setUnit(Loading())
    void getFamilyContactsReport(unitId).then(setRows)
    void getDaycare(unitId).then(setUnit)
  }, [])

  return (
    <Container>
      <ReturnButton />
      <ContentArea opaque>
        {isSuccess(unit) && <Title size={1}>{unit.data.daycare.name}</Title>}

        {isLoading(rows) && <Loader />}
        {isFailure(rows) && <span>{i18n.common.loadingFailed}</span>}
        {isSuccess(rows) && (
          <>
            <ReportDownload
              data={_.flatMap(rows.data, (row) => [
                {
                  name: row.lastName,
                  ssn: row.ssn,
                  group: row.group,
                  address: row.streetAddress,
                  headOfChild: row.headOfChild
                    ? `${row.headOfChild.lastName} ${row.headOfChild.firstName}`
                    : '',
                  guardian1: row.guardian1
                    ? `${row.guardian1.lastName} ${row.guardian1.firstName}`
                    : '',
                  guardian2: row.guardian2
                    ? `${row.guardian2.lastName} ${row.guardian2.firstName}`
                    : ''
                },
                {
                  name: row.firstName,
                  ssn: '',
                  group: '',
                  address: `${row.postalCode} ${row.postOffice}`,
                  headOfChild: row.headOfChild?.phone ?? '',
                  guardian1: row.guardian1?.phone ?? '',
                  guardian2: row.guardian2?.phone ?? ''
                },
                {
                  name: '',
                  ssn: '',
                  group: '',
                  address: '',
                  headOfChild: row.headOfChild?.email ?? '',
                  guardian1: row.guardian1?.email ?? '',
                  guardian2: row.guardian2?.email ?? ''
                }
              ])}
              headers={[
                { label: i18n.reports.familyContacts.name, key: 'name' },
                { label: i18n.reports.familyContacts.ssn, key: 'ssn' },
                { label: i18n.reports.familyContacts.group, key: 'group' },
                { label: i18n.reports.familyContacts.address, key: 'address' },
                {
                  label: i18n.reports.familyContacts.headOfChild,
                  key: 'headOfChild'
                },
                {
                  label: i18n.reports.familyContacts.guardian1,
                  key: 'guardian1'
                },
                {
                  label: i18n.reports.familyContacts.guardian2,
                  key: 'guardian2'
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
                {rows.data.map((row) => (
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
