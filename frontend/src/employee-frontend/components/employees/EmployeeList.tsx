// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { EmployeeWithDaycareRoles } from 'lib-common/generated/api-types/pis'
import { ExpandableList } from 'lib-components/atoms/ExpandableList'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { fontWeights } from 'lib-components/typography'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'

import { activateEmployeeMutation, deactivateEmployeeMutation } from './queries'

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

const Details = styled.div`
  font-size: 14px;
  color: ${colors.grayscale.g70};
`

const StyledUl = styled.ul`
  margin-top: 0;
`

interface Props {
  employees: EmployeeWithDaycareRoles[]
}

export function EmployeeList({ employees }: Props) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const rows = employees.map(
    ({
      daycareRoles,
      daycareGroupRoles,
      email,
      firstName,
      globalRoles,
      id,
      lastName,
      lastLogin,
      externalId,
      employeeNumber,
      temporaryUnitName,
      active
    }) => (
      <LinkTr key={id} onClick={() => navigate(`/employees/${id}`)}>
        <Td>
          <Name data-qa="employee-name">
            {lastName} {firstName}
          </Name>
          <Email>{email}</Email>
          {!!externalId && <Details>{externalId}</Details>}
          {!!employeeNumber && (
            <Details>
              {i18n.employees.employeeNumber}: {employeeNumber}
            </Details>
          )}
          {!!temporaryUnitName && (
            <Details>
              {i18n.employees.temporary}: {temporaryUnitName}
            </Details>
          )}
        </Td>
        <Td>
          <ExpandableList rowsToOccupy={3} i18n={i18n.common.expandableList}>
            {[
              ...sortBy(globalRoles.map((r) => i18n.roles.adRoles[r])),
              ...sortBy(daycareRoles, 'daycareName').map((r) => {
                const groupRoles = daycareGroupRoles.filter(
                  (gr) => gr.daycareId === r.daycareId
                )

                return (
                  <>
                    <Link to={`/units/${r.daycareId}`}>{r.daycareName}</Link> (
                    {i18n.roles.adRoles[r.role]?.toLowerCase()})
                    {groupRoles.length > 0 && (
                      <StyledUl>
                        <li>
                          {groupRoles.map((gr) => gr.groupName).join(', ')}
                        </li>
                      </StyledUl>
                    )}
                  </>
                )
              }),
              daycareGroupRoles
                .filter(
                  (gr) =>
                    !daycareRoles.some((r) => r.daycareId === gr.daycareId)
                )
                .map((gr) => (
                  <AlertBox
                    key={gr.groupId}
                    noMargin
                    thin
                    message={`Luvitettu vain ryhmään: ${gr.daycareName} / ${gr.groupName}`}
                  />
                ))
            ].map((role, i) => (
              <div key={i}>{role}</div>
            ))}
          </ExpandableList>
        </Td>
        <Td>{lastLogin?.format() ?? '-'}</Td>
        <Td>
          {active ? (
            <ConfirmedMutation
              buttonText={i18n.employees.deactivate}
              confirmationTitle={i18n.employees.deactivateConfirm}
              mutation={deactivateEmployeeMutation}
              onClick={() => ({ id })}
              data-qa="deactivate-button"
            />
          ) : (
            <ConfirmedMutation
              buttonText={i18n.employees.activate}
              confirmationTitle={i18n.employees.activateConfirm}
              mutation={activateEmployeeMutation}
              onClick={() => ({ id })}
              data-qa="activate-button"
            />
          )}
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
            <Th>{i18n.employees.lastLogin}</Th>
          </Tr>
        </Thead>
        <Tbody>{rows}</Tbody>
      </Table>
    </>
  )
}
