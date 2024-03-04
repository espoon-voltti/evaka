// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { combine, Loading, Result, wrapResult } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'
import Button from 'lib-components/atoms/buttons/Button'
import MutateButton, {
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import { Container, ContentArea } from 'lib-components/layout/Container'

import UnitEditor from '../../../components/unit/unit-details/UnitEditor'
import { getEmployees } from '../../../generated/api-clients/pis'
import { useTranslation } from '../../../state/i18n'
import { FinanceDecisionHandlerOption } from '../../../state/invoicing-ui'
import { renderResult } from '../../async-rendering'
import { areaQuery, createUnitMutation } from '../queries'

const getEmployeesResult = wrapResult(getEmployees)

export default React.memo(function CreateUnitPage() {
  const { i18n } = useTranslation()
  const navigate = useNavigate()
  const areas = useQueryResult(areaQuery())
  const [financeDecisionHandlerOptions, setFinanceDecisionHandlerOptions] =
    useState<Result<FinanceDecisionHandlerOption[]>>(Loading.of())

  useEffect(() => {
    void getEmployeesResult().then((employeesResponse) => {
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
            >
              {(getFormData, isValid) => (
                <>
                  <Button
                    onClick={() => navigate(-1)}
                    text={i18n.common.cancel}
                  />
                  <MutateButton
                    primary
                    preventDefault
                    mutation={createUnitMutation}
                    onClick={() => {
                      const body = getFormData()
                      return body ? { body } : cancelMutation
                    }}
                    onSuccess={({ id }) => navigate(`/units/${id}/unit-info`)}
                    disabled={!isValid}
                    text={i18n.unitEditor.submitNew}
                    data-qa="save-button"
                  />
                </>
              )}
            </UnitEditor>
          )
        )}
      </ContentArea>
    </Container>
  )
})
