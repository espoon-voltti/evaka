// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'
import { faEnvelope } from '@fortawesome/free-solid-svg-icons'
import LocalDate from '@evaka/lib-common/local-date'
import { Gap } from '@evaka/lib-components/white-space'
import { Container, ContentArea } from '@evaka/lib-components/layout/Container'
import { Label } from '@evaka/lib-components/typography'
import { AsyncFormModal } from '@evaka/lib-components/molecules/modals/FormModal'
import { AlertBox } from '@evaka/lib-components/molecules/MessageBoxes'
import { DatePickerDeprecated } from '@evaka/lib-components/molecules/DatePickerDeprecated'
import Invoices from './Invoices'
import InvoiceFilters from './InvoiceFilters'
import Actions from './Actions'
import { useTranslation } from '../../state/i18n'
import { InvoicesActions, useInvoicesState } from './invoices-state'

export default React.memo(function InvoicesPage() {
  const {
    actions,
    state: {
      page,
      invoices,
      invoiceTotals,
      sortBy,
      sortDirection,
      checkedInvoices,
      allInvoicesToggle,
      showModal
    },
    searchFilters,
    reloadInvoices,
    refreshInvoices,
    sendInvoices,
    onSendSuccess
  } = useInvoicesState()

  return (
    <Container data-qa="invoices-page">
      <ContentArea opaque>
        <InvoiceFilters />
      </ContentArea>
      <Gap size={'XL'} />
      <ContentArea opaque>
        <Invoices
          actions={actions}
          invoices={invoices[page]}
          refreshInvoices={refreshInvoices}
          total={invoiceTotals?.total}
          pages={invoiceTotals?.pages}
          currentPage={page}
          sortBy={sortBy}
          sortDirection={sortDirection}
          showCheckboxes={
            searchFilters.status === 'DRAFT' ||
            searchFilters.status === 'WAITING_FOR_SENDING'
          }
          checked={checkedInvoices}
          allInvoicesToggle={allInvoicesToggle}
          allInvoicesToggleDisabled={searchFilters.area.length < 1}
        />
      </ContentArea>
      <Actions
        actions={actions}
        reloadInvoices={reloadInvoices}
        status={searchFilters.status}
        checkedInvoices={checkedInvoices}
        checkedAreas={searchFilters.area}
        allInvoicesToggle={allInvoicesToggle}
      />
      {showModal ? (
        <Modal
          actions={actions}
          onSendDone={onSendSuccess}
          sendInvoices={sendInvoices}
          allInvoicesToggle={allInvoicesToggle}
        />
      ) : null}
    </Container>
  )
})

const Modal = React.memo(function Modal({
  actions,
  onSendDone,
  sendInvoices,
  allInvoicesToggle
}: {
  actions: InvoicesActions
  onSendDone: () => void
  sendInvoices: (args: {
    invoiceDate: LocalDate
    dueDate: LocalDate
  }) => Promise<void>
  allInvoicesToggle: boolean
}) {
  const { i18n } = useTranslation()
  const [invoiceDate, setInvoiceDate] = useState(LocalDate.today())
  const [dueDate, setDueDate] = useState(LocalDate.today().addBusinessDays(10))

  return (
    <AsyncFormModal
      iconColour={'blue'}
      title={i18n.invoices.sendModal.title}
      icon={faEnvelope}
      resolve={{
        action: () => sendInvoices({ invoiceDate, dueDate }),
        label: i18n.common.confirm,
        onSuccess: onSendDone
      }}
      reject={{
        action: actions.closeModal,
        label: i18n.common.cancel
      }}
      data-qa="send-invoices-dialog"
    >
      <ModalContent>
        <Label>{i18n.invoices.sendModal.invoiceDate}</Label>
        <div>
          <DatePickerDeprecated
            date={invoiceDate}
            onChange={setInvoiceDate}
            type="full-width"
            dataQa="invoice-date-input"
          />
        </div>
        <Gap size="s" />
        <Label>{i18n.invoices.sendModal.dueDate}</Label>
        <div>
          <DatePickerDeprecated
            date={dueDate}
            onChange={setDueDate}
            type="full-width"
            dataQa="invoice-due-date-input"
          />
        </div>
        {!allInvoicesToggle ? (
          <>
            <Gap size="s" />
            <AlertBox
              message={i18n.invoices.buttons.individualSendAlertText}
              thin
            />
          </>
        ) : null}
      </ModalContent>
    </AsyncFormModal>
  )
})

const ModalContent = styled.div`
  align-self: flex-start;
  margin-left: 4rem;
  margin-right: 4rem;
`
