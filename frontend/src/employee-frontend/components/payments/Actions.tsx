// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faEnvelope } from '@fortawesome/free-solid-svg-icons'
import React, { useState } from 'react'
import styled from 'styled-components'

import { useBoolean } from 'lib-common/form/hooks'
import { PaymentStatus } from 'lib-common/generated/api-types/invoicing'
import { PaymentId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { fontWeights, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { getPaymentsDueDate } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'
import StickyActionBar from '../common/StickyActionBar'

import { selectablePaymentStatuses } from './PaymentsPage'
import {
  confirmDraftPaymentsMutation,
  deleteDraftPaymentsMutation,
  revertPaymentsToDraftsMutation,
  sendPaymentsMutation
} from './queries'

const CheckedRowsInfo = styled.div`
  color: ${colors.grayscale.g35};
  font-style: italic;
  font-weight: ${fontWeights.bold};
`

type Props = {
  status: PaymentStatus
  checkedIds: PaymentId[]
  clearChecked: () => void
}

const Actions = React.memo(function Actions({
  status,
  checkedIds,
  clearChecked
}: Props) {
  const { i18n } = useTranslation()
  const [sendModalOpen, { on: showSendModal, off: closeSendModal }] =
    useBoolean(false)

  return (
    <>
      {sendModalOpen && (
        <SendPaymentsModal
          paymentIds={checkedIds}
          onSuccess={() => {
            clearChecked()
            closeSendModal()
          }}
          onCancel={closeSendModal}
        />
      )}
      {selectablePaymentStatuses.includes(status) && (
        <StickyActionBar align="right">
          {checkedIds.length > 0 ? (
            <>
              <CheckedRowsInfo>
                {i18n.payments.buttons.checked(checkedIds.length)}
              </CheckedRowsInfo>
              <Gap size="s" horizontal />
            </>
          ) : null}
          {status === 'DRAFT' ? (
            <>
              <MutateButton
                text={i18n.payments.buttons.deletePayment(checkedIds.length)}
                mutation={deleteDraftPaymentsMutation}
                disabled={checkedIds.length === 0}
                onClick={() => ({ body: checkedIds })}
                onSuccess={clearChecked}
                data-qa="delete-payments"
              />
              <Gap size="s" horizontal />
              <MutateButton
                text={i18n.payments.buttons.confirmPayments(checkedIds.length)}
                mutation={confirmDraftPaymentsMutation}
                disabled={checkedIds.length === 0}
                onClick={() => ({ body: checkedIds })}
                onSuccess={clearChecked}
                data-qa="confirm-payments"
              />
            </>
          ) : status === 'CONFIRMED' ? (
            <>
              <MutateButton
                text={i18n.payments.buttons.revertPayments(checkedIds.length)}
                mutation={revertPaymentsToDraftsMutation}
                disabled={checkedIds.length === 0}
                onClick={() => ({ body: checkedIds })}
                onSuccess={clearChecked}
                data-qa="revert-payments"
              />
              <Gap size="s" horizontal />
              <Button
                primary
                disabled={checkedIds.length === 0}
                text={i18n.payments.buttons.sendPayments(checkedIds.length)}
                onClick={showSendModal}
                data-qa="open-send-payments-dialog"
              />
            </>
          ) : null}
        </StickyActionBar>
      )}
    </>
  )
})

const SendPaymentsModal = React.memo(function Modal({
  paymentIds,
  onSuccess,
  onCancel
}: {
  paymentIds: PaymentId[]
  onSuccess: () => void
  onCancel: () => void
}) {
  const { i18n } = useTranslation()
  const [paymentDate, setPaymentDate] = useState(LocalDate.todayInHelsinkiTz())
  const [dueDate, setDueDate] = useState(
    getPaymentsDueDate ?? LocalDate.todayInHelsinkiTz().addBusinessDays(10)
  )

  return (
    <MutateFormModal
      type="info"
      title={i18n.payments.sendModal.title}
      icon={faEnvelope}
      resolveMutation={sendPaymentsMutation}
      resolveAction={() => ({ body: { paymentDate, dueDate, paymentIds } })}
      resolveLabel={i18n.common.confirm}
      onSuccess={onSuccess}
      rejectAction={onCancel}
      rejectLabel={i18n.common.cancel}
      data-qa="send-payments-modal"
    >
      <ModalContent>
        <Label>{i18n.payments.sendModal.paymentDate}</Label>
        <div>
          <DatePickerDeprecated
            date={paymentDate}
            onChange={setPaymentDate}
            type="full-width"
          />
        </div>
        <Gap size="s" />
        <Label>{i18n.payments.sendModal.dueDate}</Label>
        <div>
          <DatePickerDeprecated
            date={dueDate}
            onChange={setDueDate}
            type="full-width"
          />
        </div>
      </ModalContent>
    </MutateFormModal>
  )
})

const ModalContent = styled.div`
  align-self: flex-start;
  margin-left: 4rem;
  margin-right: 4rem;
`

export default Actions
