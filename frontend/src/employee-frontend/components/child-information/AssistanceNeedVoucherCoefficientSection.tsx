// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext, useRef, useState } from 'react'
import styled from 'styled-components'

import { ChildContext, ChildState } from 'employee-frontend/state/child'
import { UIContext } from 'employee-frontend/state/ui'
import { AssistanceNeedVoucherCoefficient } from 'lib-common/generated/api-types/assistanceneed'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { scrollToRef } from 'lib-common/utils/scrolling'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Title from 'lib-components/atoms/Title'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody } from 'lib-components/layout/Table'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faQuestion } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import AssistanceNeedVoucherCoefficientForm from './assistance-need/voucher-coefficient/AssistanceNeedVoucherCoefficientForm'
import AssistanceNeedVoucherCoefficientRow from './assistance-need/voucher-coefficient/AssistanceNeedVoucherCoefficientRow'
import {
  deleteAssistanceNeedVoucherCoefficientMutation,
  getAssistanceNeedVoucherCoefficientsQuery
} from './queries'

const TitleRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: ${defaultMargins.m};
`

export interface Props {
  childId: UUID
}

export default React.memo(function AssistanceNeedVoucherCoefficientSection({
  childId
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext<ChildState>(ChildContext)

  const refSectionTop = useRef(null)

  const { uiMode, toggleUiMode, clearUiMode } = useContext(UIContext)

  const coefficients = useQueryResult(
    getAssistanceNeedVoucherCoefficientsQuery({ childId })
  )

  const [activeCoefficient, setActiveCoefficient] =
    useState<AssistanceNeedVoucherCoefficient>()

  return (
    <div ref={refSectionTop}>
      {uiMode === 'delete-assistance-need-voucher-coefficient' &&
        activeCoefficient && (
          <DeleteAssistanceNeedVoucherCoefficientModal
            childId={childId}
            coefficientId={activeCoefficient.id}
            onClose={() => {
              clearUiMode()
              setActiveCoefficient(undefined)
            }}
          />
        )}

      <TitleRow>
        <Title size={4}>
          {i18n.childInformation.assistanceNeedVoucherCoefficient.sectionTitle}
        </Title>
        {permittedActions.has('CREATE_ASSISTANCE_NEED_VOUCHER_COEFFICIENT') && (
          <AddButton
            flipped
            text={i18n.childInformation.assistanceNeedVoucherCoefficient.create}
            onClick={() => {
              toggleUiMode('create-new-assistance-need-voucher-coefficient')
              scrollToRef(refSectionTop)
            }}
            disabled={
              uiMode === 'create-new-assistance-need-voucher-coefficient'
            }
            data-qa="assistance-need-voucher-coefficient-create-btn"
          />
        )}
      </TitleRow>
      {renderResult(coefficients, (coefficients) => (
        <>
          {uiMode === 'create-new-assistance-need-voucher-coefficient' && (
            <>
              <HorizontalLine slim />
              <Gap size="s" />
              <div data-qa="create-new-assistance-need-voucher-coefficient">
                <AssistanceNeedVoucherCoefficientForm
                  onSuccess={() => undefined}
                  childId={childId}
                  coefficients={coefficients.map(
                    ({ voucherCoefficient }) => voucherCoefficient
                  )}
                />
              </div>
              <Gap size="m" />
            </>
          )}
          {coefficients.length === 0 ? null : (
            <Table data-qa="table-of-assistance-need-voucher-coefficients">
              <Tbody>
                {orderBy(
                  coefficients,
                  (c) => c.voucherCoefficient.validityPeriod.start,
                  ['desc']
                ).map(({ voucherCoefficient, permittedActions }) => (
                  <AssistanceNeedVoucherCoefficientRow
                    childId={childId}
                    key={voucherCoefficient.id}
                    coefficients={coefficients.map(
                      ({ voucherCoefficient }) => voucherCoefficient
                    )}
                    voucherCoefficient={voucherCoefficient}
                    activeCoefficient={activeCoefficient}
                    setActiveCoefficient={setActiveCoefficient}
                    permittedActions={permittedActions}
                  />
                ))}
              </Tbody>
            </Table>
          )}
        </>
      ))}
    </div>
  )
})

const DeleteAssistanceNeedVoucherCoefficientModal = React.memo(
  function DeleteAssistanceNeedVoucherCoefficientModal({
    childId,
    coefficientId,
    onClose
  }: {
    childId: UUID
    coefficientId: UUID
    onClose: () => void
  }) {
    const { mutateAsync: deleteAssistanceNeedVoucherCoefficient } =
      useMutationResult(deleteAssistanceNeedVoucherCoefficientMutation)
    const { i18n } = useTranslation()
    return (
      <InfoModal
        type="warning"
        title={
          i18n.childInformation.assistanceNeedVoucherCoefficient.deleteModal
            .title
        }
        text={
          i18n.childInformation.assistanceNeedVoucherCoefficient.deleteModal
            .description
        }
        icon={faQuestion}
        reject={{
          action: () => onClose(),
          label: i18n.common.cancel
        }}
        resolve={{
          async action() {
            await deleteAssistanceNeedVoucherCoefficient({
              childId,
              id: coefficientId
            })
            onClose()
          },
          label:
            i18n.childInformation.assistanceNeedVoucherCoefficient.deleteModal
              .delete
        }}
      />
    )
  }
)
