// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useHistory } from 'react-router-dom'

import { combine, Loading, Result } from 'lib-common/api'
import { DaycareCareArea } from 'lib-common/generated/api-types/daycare'
import { Container, ContentArea } from 'lib-components/layout/Container'

import { getAreas } from '../../../api/daycare'
import { getEmployees } from '../../../api/employees'
import { createDaycare, DaycareFields } from '../../../api/unit'
import UnitEditor from '../../../components/unit/unit-details/UnitEditor'
import { FinanceDecisionHandlerOption } from '../../../state/invoicing-ui'
import { renderResult } from '../../async-rendering'

export default React.memo(function CreateUnitPage() {
  const history = useHistory()
  const [areas, setAreas] = useState<Result<DaycareCareArea[]>>(Loading.of())
  const [financeDecisionHandlerOptions, setFinanceDecisionHandlerOptions] =
    useState<Result<FinanceDecisionHandlerOption[]>>(Loading.of())
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
        history.push(`/units/${result.value}/unit-info`)
      }
    })
  }

  return (
    <Container>
      <ContentArea opaque>
        {renderResult(
          combine(areas, financeDecisionHandlerOptions),
          ([areas, financeDecisionHandlerOptions]) => (
            <UnitEditor
              editable={true}
              areas={areas}
              financeDecisionHandlerOptions={financeDecisionHandlerOptions}
              unit={undefined}
              submit={submitState}
              onSubmit={onSubmit}
              onClickCancel={() => window.history.back()}
            />
          )
        )}
      </ContentArea>
    </Container>
  )
})
