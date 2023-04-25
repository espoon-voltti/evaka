// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import { ExpandableList } from 'lib-components/atoms/ExpandableList'
import Loader from 'lib-components/atoms/Loader'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { fontWeights } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import type { EmployeeUser as Employee } from '../../types/employee'

const LinkTr = styled(Tr)`
  cursor: pointer;
`

const Name = styled.div`
  font-weight: ${fontWeights.semibold};
`

const Email = styled.div`
  font-weight: ${fontWeights.semibold};
  font-size: 14px;
`

interface Props {
  employees?: Result<Employee[]>
}

export function EmployeeList({ employees }: Props) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const rows =
    employees?.isSuccess &&
    employees.value.map(
      ({ daycareRoles, email, firstName, globalRoles, id, lastName }) => (
        <LinkTr key={id} onClick={() => navigate(`/employees/${id}`)}>
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
                ...sortBy(daycareRoles, 'daycareName').map((r) => (
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
        </LinkTr>
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
