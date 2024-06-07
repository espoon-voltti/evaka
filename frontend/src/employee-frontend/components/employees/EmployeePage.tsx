// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPlus, faTimes, faTrash } from 'Icons'
import sortBy from 'lodash/sortBy'
import React, { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { combine } from 'lib-common/api'
import { globalRoles } from 'lib-common/api-types/employee-auth'
import { array, value } from 'lib-common/form/form'
import { useForm } from 'lib-common/form/hooks'
import { Daycare } from 'lib-common/generated/api-types/daycare'
import { EmployeeWithDaycareRoles } from 'lib-common/generated/api-types/pis'
import { UserRole } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'
import { FlexRow } from '../common/styled/containers'
import { unitsQuery } from '../unit/queries'

import DaycareRolesModal from './DaycareRolesModal'
import {
  deleteEmployeeDaycareRolesMutation,
  deleteEmployeeMobileDeviceMutation,
  employeeDetailsQuery,
  updateEmployeeGlobalRolesMutation
} from './queries'

const globalRolesForm = array(value<UserRole>())

const GlobalRolesForm = React.memo(function GlobalRolesForm({
  employee,
  onSuccess,
  onCancel
}: {
  employee: EmployeeWithDaycareRoles
  onSuccess: () => void
  onCancel: () => void
}) {
  const { i18n } = useTranslation()
  const boundForm = useForm(
    globalRolesForm,
    () => employee.globalRoles,
    i18n.validationError
  )

  return (
    <FixedSpaceColumn spacing="m">
      <FixedSpaceColumn spacing="xs">
        {globalRoles.map((role) => (
          <Checkbox
            key={role}
            label={i18n.roles.adRoles[role]}
            checked={boundForm.value().includes(role)}
            onChange={(checked) => {
              if (checked) {
                boundForm.update((prev) => [
                  ...prev.filter((r) => r !== role),
                  role
                ])
              } else {
                boundForm.update((prev) => prev.filter((r) => r !== role))
              }
            }}
          />
        ))}
      </FixedSpaceColumn>
      <FixedSpaceRow>
        <LegacyButton text={i18n.common.cancel} onClick={onCancel} />
        <MutateButton
          primary
          text={i18n.common.save}
          mutation={updateEmployeeGlobalRolesMutation}
          onClick={() => ({ id: employee.id, body: boundForm.value() })}
          onSuccess={onSuccess}
        />
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
})

const EmployeePage = React.memo(function EmployeePage({
  employee,
  units
}: {
  employee: EmployeeWithDaycareRoles
  units: Daycare[]
}) {
  const { i18n } = useTranslation()
  const [editingGlobalRoles, setEditingGlobalRoles] = useState(false)
  const [rolesModalOpen, setRolesModalOpen] = useState(false)

  const sortedRoles = useMemo(
    () => sortBy(employee.daycareRoles, ({ daycareName }) => daycareName),
    [employee.daycareRoles]
  )

  return (
    <div>
      {rolesModalOpen && (
        <DaycareRolesModal
          employeeId={employee.id}
          units={units}
          onClose={() => setRolesModalOpen(false)}
        />
      )}
      <Title size={2}>
        {employee.firstName} {employee.lastName}
      </Title>
      <span>{employee.email}</span>

      <Gap />

      <Title size={3}>{i18n.employees.editor.globalRoles}</Title>
      {editingGlobalRoles ? (
        <GlobalRolesForm
          employee={employee}
          onSuccess={() => setEditingGlobalRoles(false)}
          onCancel={() => setEditingGlobalRoles(false)}
        />
      ) : (
        <FixedSpaceColumn spacing="m">
          <div>
            {employee.globalRoles.length > 0
              ? globalRoles
                  .filter((r) => employee.globalRoles.includes(r))
                  .map((r) => i18n.roles.adRoles[r])
                  .join(', ')
              : '-'}
          </div>
          <Button
            appearance="inline"
            onClick={() => setEditingGlobalRoles(true)}
            text={i18n.common.edit}
          />
        </FixedSpaceColumn>
      )}

      <Gap />

      <Title size={3}>{i18n.employees.editor.unitRoles.title}</Title>
      <FlexRow justifyContent="space-between">
        <Button
          appearance="inline"
          onClick={() => setRolesModalOpen(true)}
          text={i18n.employees.editor.unitRoles.addRoles}
          icon={faPlus}
          disabled={editingGlobalRoles}
        />
        <ConfirmedMutation
          buttonStyle="INLINE"
          buttonText={i18n.employees.editor.unitRoles.deleteAll}
          icon={faTimes}
          confirmationTitle={i18n.employees.editor.unitRoles.deleteAllConfirm}
          mutation={deleteEmployeeDaycareRolesMutation}
          onClick={() => ({ id: employee.id, daycareId: null })}
          disabled={editingGlobalRoles}
        />
      </FlexRow>
      <Table>
        <Thead>
          <Tr>
            <Th>{i18n.employees.editor.unitRoles.unit}</Th>
            <Th>{i18n.employees.editor.unitRoles.role}</Th>
            <Th />
          </Tr>
        </Thead>
        <Tbody>
          {sortedRoles.map(({ daycareId, daycareName, role }) => (
            <Tr key={`${daycareId}/${role}`}>
              <Td>
                <Link to={`/units/${daycareId}`}>{daycareName}</Link>
              </Td>
              <Td>{i18n.roles.adRoles[role]}</Td>
              <Td>
                <ConfirmedMutation
                  buttonStyle="ICON"
                  icon={faTrash}
                  buttonAltText={i18n.common.remove}
                  confirmationTitle={
                    i18n.employees.editor.unitRoles.deleteConfirm
                  }
                  mutation={deleteEmployeeDaycareRolesMutation}
                  onClick={() => ({
                    id: employee.id,
                    daycareId: daycareId
                  })}
                  disabled={editingGlobalRoles}
                />
              </Td>
            </Tr>
          ))}
        </Tbody>
      </Table>
      <Gap />

      <Title size={3}>{i18n.employees.editor.mobile.title}</Title>
      <Table>
        <Thead>
          <Tr>
            <Th>{i18n.employees.editor.mobile.name}</Th>
            <Th />
          </Tr>
        </Thead>
        <Tbody>
          {employee.personalMobileDevices.map(({ id, name }) => (
            <Tr key={id}>
              <Td>{name.trim() || i18n.employees.editor.mobile.nameless}</Td>
              <Td>
                <ConfirmedMutation
                  buttonStyle="ICON"
                  icon={faTrash}
                  buttonAltText={i18n.common.remove}
                  confirmationTitle={i18n.employees.editor.mobile.deleteConfirm}
                  mutation={deleteEmployeeMobileDeviceMutation}
                  onClick={() => ({
                    id: id,
                    employeeId: employee.id
                  })}
                  disabled={editingGlobalRoles}
                />
              </Td>
            </Tr>
          ))}
        </Tbody>
      </Table>
    </div>
  )
})

export default React.memo(function EmployeePageLoader() {
  const { i18n } = useTranslation()
  const { id } = useRouteParams(['id'])
  const employee = useQueryResult(employeeDetailsQuery({ id }))
  const units = useQueryResult(unitsQuery({ includeClosed: false }))

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {renderResult(combine(employee, units), ([employee, units]) => (
          <EmployeePage employee={employee} units={units} />
        ))}
      </ContentArea>
    </Container>
  )
})
