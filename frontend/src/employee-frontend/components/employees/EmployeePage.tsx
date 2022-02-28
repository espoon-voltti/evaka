// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'

import { Loading, Result } from 'lib-common/api'
import { GlobalRole, globalRoles } from 'lib-common/api-types/employee-auth'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { SpinnerSegment } from 'lib-components/atoms/state/Spinner'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { getEmployeeDetails, updateEmployee } from '../../api/employees'
import { useTranslation } from '../../state/i18n'
import { EmployeeUser } from '../../types/employee'

interface FormData {
  globalRoles: GlobalRole[]
}

export default React.memo(function EmployeePage() {
  const { i18n } = useTranslation()
  const { id } = useParams<{ id: string }>()
  const [employee, setEmployee] = useState<Result<EmployeeUser>>(Loading.of())
  const [form, setForm] = useState<FormData | null>(null)

  const loadEmployee = useRestApi(getEmployeeDetails, setEmployee)

  useEffect(() => {
    loadEmployee(id)
  }, [loadEmployee, id])

  useEffect(() => {
    if (employee.isSuccess && form === null) {
      setForm({
        globalRoles: [...employee.value.globalRoles]
      })
    }
  }, [employee, form, setForm])

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {employee.isLoading && <SpinnerSegment />}
        {employee.isFailure && <ErrorSegment />}
        {employee.isSuccess && form && (
          <>
            <Title size={2}>
              {employee.value.firstName} {employee.value.lastName}
            </Title>
            <span>{employee.value.email}</span>

            <Gap />

            <Title size={3}>{i18n.employees.editor.roles}</Title>
            <FixedSpaceColumn spacing="xs">
              {globalRoles.map((role) => (
                <Checkbox
                  key={role}
                  label={i18n.roles.adRoles[role]}
                  checked={form.globalRoles.includes(role)}
                  onChange={(checked) => {
                    if (checked) {
                      setForm({
                        ...form,
                        globalRoles: [...form.globalRoles, role]
                      })
                    } else {
                      setForm({
                        ...form,
                        globalRoles: form.globalRoles.filter((r) => r !== role)
                      })
                    }
                  }}
                />
              ))}
            </FixedSpaceColumn>

            <Gap />

            <AsyncButton
              primary
              text={i18n.common.save}
              onClick={() => updateEmployee(id, form.globalRoles)}
              onSuccess={() => {
                loadEmployee(id)
                setForm(null)
              }}
            />
          </>
        )}
      </ContentArea>
    </Container>
  )
})
