// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { Result, wrapResult } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Select from 'lib-components/atoms/dropdowns/Select'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faArrowRight } from 'lib-icons'

import { getSelectableFinanceDecisionHandlers } from '../../generated/api-clients/invoicing'

const getSelectableFinanceDecisionHandlersResult = wrapResult(
  getSelectableFinanceDecisionHandlers
)

interface Props {
  onResolve: (decisionHandlerId: UUID | undefined) => Promise<Result<void>>
  onReject: () => void
  checkedIds: UUID[]
}

interface Selection {
  value: string
  label: string
}

export default React.memo(function FinanceDecisionHandlerSelectModal(
  props: Props
) {
  const { i18n, lang } = useTranslation()
  const [selectedFinanceDecisionHandler, setFinanceDecisionHandler] =
    useState<string>()
  const [financeDecisionHandlersResult] = useApiState(
    getSelectableFinanceDecisionHandlersResult,
    []
  )
  const [error, setError] = useState<string>()

  const onResolve = async () =>
    await props.onResolve(selectedFinanceDecisionHandler)

  return (
    <FormModal
      icon={faArrowRight}
      title={i18n.financeDecisions.handlerSelectModal.title}
      resolveAction={async () => {
        const result = await onResolve()
        if (result.isFailure) {
          setError(
            result.errorCode === 'WAITING_FOR_MANUAL_SENDING'
              ? i18n.feeDecisions.buttons.errors.WAITING_FOR_MANUAL_SENDING
              : i18n.common.error.unknown
          )
        }
      }}
      resolveLabel={i18n.financeDecisions.handlerSelectModal.resolve(
        props.checkedIds.length
      )}
      rejectAction={props.onReject}
      rejectLabel={i18n.common.cancel}
    >
      <Label>{i18n.financeDecisions.handlerSelectModal.label}</Label>
      {financeDecisionHandlersResult.mapAll({
        loading: () => null,
        failure: () => (
          <ErrorMessage>
            {i18n.financeDecisions.handlerSelectModal.error}
          </ErrorMessage>
        ),
        success: (financeDecisionHandlers) => {
          const items = [
            undefined,
            ...financeDecisionHandlers
              .map((handler) => ({
                value: handler.id,
                label: `${handler.firstName} ${handler.lastName}`
              }))
              .sort((a, b) => a.label.localeCompare(b.label, lang))
          ]
          return (
            <Select
              items={items}
              selectedItem={items.find(
                (item) => item?.value === selectedFinanceDecisionHandler
              )}
              onChange={(item) => setFinanceDecisionHandler(item?.value)}
              getItemValue={(item: Selection | undefined) => item?.value ?? ''}
              getItemLabel={(item) =>
                item?.label ?? i18n.financeDecisions.handlerSelectModal.default
              }
              data-qa="finance-decision-handler-select"
            />
          )
        }
      })}
      <Gap size="s" />
      <CheckedRowsInfo>
        {i18n.financeDecisions.handlerSelectModal.decisionCount(
          props.checkedIds.length
        )}
      </CheckedRowsInfo>
      {error ? (
        <>
          <Gap size="s" />
          <ErrorMessage>{error}</ErrorMessage>
        </>
      ) : null}
    </FormModal>
  )
})

const CheckedRowsInfo = styled.div`
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-style: italic;
`

const ErrorMessage = styled.div`
  color: ${(p) => p.theme.colors.accents.a2orangeDark};
`
