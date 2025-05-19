// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'

import type { Result } from 'lib-common/api'
import { combine, Loading } from 'lib-common/api'
import { useBoolean } from 'lib-common/form/hooks'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import {
  MutateButton,
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import { areasQuery, getEmployeesQuery } from '../../../queries'
import { useTranslation } from '../../../state/i18n'
import type { FinanceDecisionHandlerOption } from '../../../state/invoicing-ui'
import type { TitleState } from '../../../state/title'
import { TitleContext } from '../../../state/title'
import { renderResult } from '../../async-rendering'
import { daycareQuery, updateUnitMutation } from '../queries'

import UnitEditor from './UnitEditor'

export default React.memo(function UnitDetailsPage() {
  const id = useIdRouteParam<DaycareId>('id')
  const { i18n } = useTranslation()
  const { setTitle } = useContext<TitleState>(TitleContext)
  const unit = useQueryResult(daycareQuery({ daycareId: id }))
  const areas = useQueryResult(areasQuery())
  const [financeDecisionHandlerOptions, setFinanceDecisionHandlerOptions] =
    useState<Result<FinanceDecisionHandlerOption[]>>(Loading.of())
  const [editable, useEditable] = useBoolean(false)
  useEffect(() => {
    if (unit.isSuccess) {
      setTitle(unit.value.daycare.name)
    }
  }, [setTitle, unit])

  const employeesResponse = useQueryResult(getEmployeesQuery())

  useEffect(() => {
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
  }, [id, employeesResponse])

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
              lastPlacementDate={unit.lastPlacementDate}
              onClickEdit={useEditable.on}
            >
              {(getFormData, isValid) => (
                <>
                  <LegacyButton
                    onClick={useEditable.off}
                    text={i18n.common.cancel}
                  />
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
