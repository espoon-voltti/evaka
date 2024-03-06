// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'

import { combine, Loading, Result, wrapResult } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import { useQueryResult } from 'lib-common/query'
import useRequiredParams from 'lib-common/useRequiredParams'
import Button from 'lib-components/atoms/buttons/Button'
import MutateButton, {
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import UnitEditor from '../../../components/unit/unit-details/UnitEditor'
import { getEmployees } from '../../../generated/api-clients/pis'
import { useTranslation } from '../../../state/i18n'
import { FinanceDecisionHandlerOption } from '../../../state/invoicing-ui'
import { TitleContext, TitleState } from '../../../state/title'
import { renderResult } from '../../async-rendering'
import { areaQuery, unitQuery, updateUnitMutation } from '../queries'

const getEmployeesResult = wrapResult(getEmployees)

export default React.memo(function UnitDetailsPage() {
  const { id } = useRequiredParams('id')
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const unit = useQueryResult(unitQuery({ daycareId: id }))
  const areas = useQueryResult(areaQuery())
  const [financeDecisionHandlerOptions, setFinanceDecisionHandlerOptions] =
    useState<Result<FinanceDecisionHandlerOption[]>>(Loading.of())
  const [editable, useEditable] = useBoolean(false)
  useEffect(() => {
    if (unit.isSuccess) {
      setTitle(unit.value.daycare.name)
    }
  }, [setTitle, unit])

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
  }, [id])

  return (
    <Container>
      <ContentArea opaque>
        <Gap size="xs" />
        {renderResult(
          combine(areas, unit, financeDecisionHandlerOptions),
          ([areas, unit, financeDecisionHandlerOptions]) => (
            <UnitEditor
              editable={editable}
              areas={areas}
              financeDecisionHandlerOptions={financeDecisionHandlerOptions}
              unit={unit.daycare}
              onClickEdit={useEditable.on}
            >
              {(getFormData, isValid) => (
                <>
                  <Button onClick={useEditable.off} text={i18n.common.cancel} />
                  <MutateButton
                    primary
                    preventDefault
                    mutation={updateUnitMutation}
                    onClick={() => {
                      const body = getFormData()
                      return body ? { daycareId: id, body } : cancelMutation
                    }}
                    onSuccess={useEditable.off}
                    disabled={!isValid}
                    text={i18n.common.save}
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
