// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import { ExpandableList } from 'lib-components/atoms/ExpandableList'
import Loader from 'lib-components/atoms/Loader'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { sortBy } from 'lodash'
import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import { useTranslation } from '../../state/i18n'
import { EmployeeUser as Employee } from '../../types/employee'

const Name = styled.div`
  font-weight: 600;
`

const Email = styled.div`
  font-weight: 600;
  font-size: 14px;
`

interface Props {
  employees?: Result<Employee[]>
}

export function EmployeeList({ employees }: Props) {
  const { i18n } = useTranslation()

  const rows =
    employees?.isSuccess &&
    employees.value.map(
      ({ daycareRoles, email, firstName, globalRoles, id, lastName }) => (
        <Tr key={id}>
          <Td>
            <Name data-qa="employee-name">
              {lastName} {firstName}
            </Name>
            <Email>{email}</Email>
          </Td>
          <Td>
            <ExpandableList rowsToOccupy={3} i18n={i18n.common.expandableList}>
              {[
                ...sortBy(globalRoles.map((r) => i18n.roles.adRoles[r])),
                ...sortBy(daycareRoles, 'daycareName').map((r, i) => (
                  <>
                    <Link to={`/units/${r.daycareId}`}>{r.daycareName}</Link> (
                    {i18n.roles.adRoles[r.role]?.toLowerCase()})
                  </>
                ))
              ].map((role, i) => (
                <div key={i}>{role}</div>
              ))}
            </ExpandableList>
          </Td>
        </Tr>
      )
    )

  return (
    <>
      <Table>
        <Thead>
          <Tr>
            <Th>{i18n.employees.name}</Th>
            <Th>{i18n.employees.rights}</Th>
          </Tr>
        </Thead>
        <Tbody>{rows}</Tbody>
      </Table>
      {employees?.isLoading && <Loader />}
      {employees?.isFailure && <div>{i18n.common.error.unknown}</div>}
    </>
  )
}
