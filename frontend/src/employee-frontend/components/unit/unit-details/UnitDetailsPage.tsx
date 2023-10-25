// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'

import { combine, Loading, Result } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import { useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Button from 'lib-components/atoms/buttons/Button'
import MutateButton, {
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import { getEmployees } from '../../../api/employees'
import UnitEditor from '../../../components/unit/unit-details/UnitEditor'
import { useTranslation } from '../../../state/i18n'
import { FinanceDecisionHandlerOption } from '../../../state/invoicing-ui'
import { TitleContext, TitleState } from '../../../state/title'
import { renderResult } from '../../async-rendering'
import { areaQuery, unitQuery, updateUnitMutation } from '../queries'

export default React.memo(function UnitDetailsPage() {
  const { id } = useNonNullableParams<{ id: string }>()
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const unit = useQueryResult(unitQuery(id))
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
                    type="submit"
                    preventDefault
                    mutation={updateUnitMutation}
                    onClick={() => {
                      const fields = getFormData()
                      return fields ? { id, fields } : cancelMutation
                    }}
                    onSuccess={useEditable.off}
                    disabled={!isValid}
                    text={i18n.common.save}
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
