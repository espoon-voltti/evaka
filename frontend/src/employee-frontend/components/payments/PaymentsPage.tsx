// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faEnvelope } from '@fortawesome/free-solid-svg-icons'
import React, { useState } from 'react'
import styled from 'styled-components'

import { Result } from 'lib-common/api'
import { PaymentStatus } from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

import Actions from './Actions'
import PaymentFilters from './PaymentFilters'
import Payments from './Payments'
import { PaymentsActions, usePaymentsState } from './payments-state'

export const selectablePaymentStatuses: PaymentStatus[] = ['DRAFT', 'CONFIRMED']

export default React.memo(function PaymentsPage() {
  const {
    actions,
    state: {
      page,
      pages,
      total,
      payments,
      sortBy,
      sortDirection,
      checkedPayments,
      showModal
    },
    searchFilters,
    createPayments,
    reloadPayments,
    sendPayments,
    onSendSuccess
  } = usePaymentsState()

  return (
    <Container data-qa="payments-page">
      <ContentArea opaque>
        <PaymentFilters />
      </ContentArea>
      <Gap size="XL" />
      <ContentArea opaque>
        <Payments
          actions={actions}
          payments={payments}
          createPayments={createPayments}
          total={total}
          pages={pages}
          currentPage={page}
          sortBy={sortBy}
          sortDirection={sortDirection}
          showCheckboxes={selectablePaymentStatuses.includes(
            searchFilters.status
          )}
          checked={checkedPayments}
        />
      </ContentArea>
      <Actions
        actions={actions}
        reloadPayments={reloadPayments}
        status={searchFilters.status}
        checkedPayments={checkedPayments}
      />
      {showModal ? (
        <Modal
          actions={actions}
          onSendDone={onSendSuccess}
          sendPayments={sendPayments}
        />
      ) : null}
    </Container>
  )
})

const Modal = React.memo(function Modal({
  actions,
  onSendDone,
  sendPayments
}: {
  actions: PaymentsActions
  onSendDone: () => void
  sendPayments: (args: {
    paymentDate: LocalDate
    dueDate: LocalDate
  }) => Promise<Result<void>>
}) {
  const { i18n } = useTranslation()
  const [paymentDate, setPaymentDate] = useState(LocalDate.todayInHelsinkiTz())
  const [dueDate, setDueDate] = useState(
    LocalDate.todayInHelsinkiTz().addBusinessDays(10)
  )

  return (
    <AsyncFormModal
      type="info"
      title={i18n.payments.sendModal.title}
      icon={faEnvelope}
      resolveAction={() => sendPayments({ paymentDate, dueDate })}
      resolveLabel={i18n.common.confirm}
      onSuccess={onSendDone}
      rejectAction={actions.closeModal}
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
    </AsyncFormModal>
  )
})

const ModalContent = styled.div`
  align-self: flex-start;
  margin-left: 4rem;
  margin-right: 4rem;
`
