// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'

import { globalRoles } from 'lib-common/api-types/employee-auth'
import { array, value } from 'lib-common/form/form'
import { useForm } from 'lib-common/form/hooks'
import { EmployeeWithDaycareRoles } from 'lib-common/generated/api-types/pis'
import { UserRole } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Title from 'lib-components/atoms/Title'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import {
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
        <Button text={i18n.common.cancel} onClick={onCancel} />
        <MutateButton
          primary
          text={i18n.common.save}
          mutation={updateEmployeeGlobalRolesMutation}
          onClick={() => ({ id: employee.id, globalRoles: boundForm.value() })}
          onSuccess={onSuccess}
        />
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
})

const EmployeePage = React.memo(function EmployeePage({
  employee
}: {
  employee: EmployeeWithDaycareRoles
}) {
  const { i18n } = useTranslation()
  const [editingGlobalRoles, setEditingGlobalRoles] = useState(false)

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <Title size={2}>
          {employee.firstName} {employee.lastName}
        </Title>
        <span>{employee.email}</span>

        <Gap />

        <Title size={3}>{i18n.employees.editor.roles}</Title>
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
            <InlineButton
              onClick={() => setEditingGlobalRoles(true)}
              text={i18n.common.edit}
            />
          </FixedSpaceColumn>
        )}
      </ContentArea>
    </Container>
  )
})

export default React.memo(function EmployeePageLoader() {
  const { i18n } = useTranslation()
  const { id } = useNonNullableParams<{ id: UUID }>()
  const employee = useQueryResult(employeeDetailsQuery(id))

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {renderResult(employee, (employee) => (
          <EmployeePage employee={employee} />
        ))}
      </ContentArea>
    </Container>
  )
})
