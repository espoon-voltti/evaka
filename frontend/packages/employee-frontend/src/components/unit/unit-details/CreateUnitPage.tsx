// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useHistory } from 'react-router-dom'
import UnitEditor from '~components/unit/unit-details/UnitEditor'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { CareArea } from '~types/unit'
import { getAreas } from '~api/daycare'
import { getEmployees } from '~api/employees'
import {
  Container,
  ContentArea
} from '@evaka/lib-components/src/layout/Container'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import { createDaycare, DaycareFields } from '~api/unit'
import { useTranslation } from '~state/i18n'
import { FinanceDecisionHandlerOption } from '~state/invoicing-ui'

export default function CreateUnitPage(): JSX.Element {
  const history = useHistory()
  const { i18n } = useTranslation()
  const [areas, setAreas] = useState<Result<CareArea[]>>(Loading.of())
  const [
    financeDecisionHandlerOptions,
    setFinanceDecisionHandlerOptions
  ] = useState<Result<FinanceDecisionHandlerOption[]>>(Loading.of())
  const [submitState, setSubmitState] = useState<Result<void> | undefined>(
    undefined
  )

  useEffect(() => {
    void getAreas().then(setAreas)
    void getEmployees().then((employeesResponse) => {
      setFinanceDecisionHandlerOptions(
        employeesResponse.map((employees) =>
          employees.map((employee) => ({
            value: employee.id,
            label: `${employee.firstName ?? ''} ${employee.lastName ?? ''}${
              employee.email ? ` (${employee.email})` : ''
            }`
          }))
        )
      )
    })
  }, [])

  const onSubmit = (fields: DaycareFields) => {
    setSubmitState(Loading.of())
    void createDaycare(fields).then((result) => {
      setSubmitState(result.map(() => undefined))
      if (result.isSuccess) {
        history.push(`/employee/units/${result.value}/details`)
      }
    })
  }

  return (
    <Container>
      <ContentArea opaque>
        {areas.isLoading && <Loader />}
        {areas.isFailure && <div>{i18n.common.error.unknown}</div>}
        {areas.isSuccess && financeDecisionHandlerOptions.isSuccess && (
          <UnitEditor
            editable={true}
            areas={areas.value}
            financeDecisionHandlerOptions={financeDecisionHandlerOptions.value}
            unit={undefined}
            submit={submitState}
            onSubmit={onSubmit}
            onClickCancel={() => window.history.back()}
          />
        )}
      </ContentArea>
    </Container>
  )
}
