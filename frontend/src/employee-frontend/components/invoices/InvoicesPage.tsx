// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faEnvelope } from '@fortawesome/free-solid-svg-icons'
import React, { useContext, useState } from 'react'
import styled from 'styled-components'

import { UserContext } from 'employee-frontend/state/user'
import { useBoolean } from 'lib-common/form/hooks'
import {
  InvoiceSortParam,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import {
  first,
  second,
  constantQuery,
  useQueryResult,
  useSelectMutation
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { renderResult } from '../async-rendering'

import Actions from './Actions'
import InvoiceFilters from './InvoiceFilters'
import Invoices from './Invoices'
import {
  invoicesQuery,
  sendInvoicesByDateMutation,
  sendInvoicesMutation
} from './queries'

export default React.memo(function InvoicesPage() {
  const { user } = useContext(UserContext)
  const {
    invoices: { searchFilters, debouncedSearchTerms }
  } = useContext(InvoicingUiContext)

  const { startDate, endDate, area, unit, status, distinctiveDetails } =
    searchFilters

  const [page, setPage] = useState(1)
  const [sortBy, setSortBy] = useState<InvoiceSortParam>('HEAD_OF_FAMILY')
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')

  const invoices = useQueryResult(
    !startDate || !endDate || startDate.isEqualOrBefore(endDate)
      ? invoicesQuery({
          body: {
            page,
            sortBy,
            sortDirection,
            area,
            unit: unit ?? null,
            status,
            distinctions: distinctiveDetails,
            searchTerms: debouncedSearchTerms ? debouncedSearchTerms : null,
            periodStart: startDate ?? null,
            periodEnd: endDate ?? null
          }
        })
      : constantQuery({ data: [], pages: 0, total: 0 })
  )

  const [showModal, { off: closeModal, on: openModal }] = useBoolean(false)

  const [fullAreaSelection, { set: setFullAreaSelection }] = useBoolean(false)

  const [checkedInvoices, setCheckedInvoices] = useState<Set<UUID>>(new Set())
  const toggleChecked = (invoiceId: UUID) =>
    setCheckedInvoices((prev) => {
      const next = new Set([...prev])
      if (next.has(invoiceId)) {
        next.delete(invoiceId)
      } else {
        next.add(invoiceId)
      }
      return next
    })
  const clearChecked = () => setCheckedInvoices(new Set())
  const checkAllOnPage = () =>
    setCheckedInvoices((prev) => {
      const next = new Set([...prev])
      invoices
        .map((res) => res.data.map((invoice) => invoice.data.id))
        .getOrElse([])
        .forEach((invoiceId) => next.add(invoiceId))
      return next
    })

  const canSend = invoices
    .map((a) => a.data.some((b) => b.permittedActions.includes('SEND')))
    .getOrElse(false)

  const canDelete = invoices
    .map((a) => a.data.some((b) => b.permittedActions.includes('DELETE')))
    .getOrElse(false)

  return (
    <Container data-qa="invoices-page">
      <ContentArea opaque>
        <InvoiceFilters />
      </ContentArea>
      <Gap size="XL" />
      <ContentArea opaque>
        {renderResult(invoices, (invoices, isReloading) => (
          <Invoices
            user={user}
            pagedInvoices={invoices}
            isLoading={isReloading}
            currentPage={page}
            setCurrentPage={setPage}
            sortBy={sortBy}
            setSortBy={setSortBy}
            sortDirection={sortDirection}
            setSortDirection={setSortDirection}
            showCheckboxes={
              (searchFilters.status === 'DRAFT' ||
                searchFilters.status === 'WAITING_FOR_SENDING') &&
              (canSend || canDelete)
            }
            checked={checkedInvoices}
            checkAll={checkAllOnPage}
            clearChecked={clearChecked}
            toggleChecked={toggleChecked}
            fullAreaSelection={fullAreaSelection}
            setFullAreaSelection={setFullAreaSelection}
            fullAreaSelectionDisabled={searchFilters.area.length < 1}
          />
        ))}
      </ContentArea>
      <Actions
        openModal={openModal}
        status={searchFilters.status}
        canSend={canSend}
        canDelete={canDelete}
        checkedInvoices={checkedInvoices}
        clearCheckedInvoices={clearChecked}
        checkedAreas={searchFilters.area}
        fullAreaSelection={fullAreaSelection}
      />
      {showModal ? (
        <Modal
          onClose={closeModal}
          onSendDone={() => {
            closeModal()
            clearChecked()
          }}
          checkedInvoices={checkedInvoices}
          fullAreaSelection={fullAreaSelection}
        />
      ) : null}
    </Container>
  )
})

const Modal = React.memo(function Modal({
  onClose,
  onSendDone,
  checkedInvoices,
  fullAreaSelection
}: {
  onClose: () => void
  onSendDone: () => void
  checkedInvoices: Set<UUID>
  fullAreaSelection: boolean
}) {
  const { i18n } = useTranslation()
  const {
    invoices: { searchFilters }
  } = useContext(InvoicingUiContext)

  const [invoiceDate, setInvoiceDate] = useState(LocalDate.todayInSystemTz())
  const [dueDate, setDueDate] = useState(
    LocalDate.todayInSystemTz().addBusinessDays(10)
  )

  const [mutation, onClick] = useSelectMutation(
    () => (fullAreaSelection ? first() : second()),
    [
      sendInvoicesByDateMutation,
      () => ({
        body: {
          dueDate,
          invoiceDate,
          areas: searchFilters.area,
          from:
            searchFilters.startDate &&
            searchFilters.useCustomDatesForInvoiceSending
              ? searchFilters.startDate
              : LocalDate.todayInSystemTz().withDate(1),
          to:
            searchFilters.endDate &&
            searchFilters.useCustomDatesForInvoiceSending
              ? searchFilters.endDate
              : LocalDate.todayInSystemTz().lastDayOfMonth()
        }
      })
    ],
    [
      sendInvoicesMutation,
      () => ({
        invoiceDate,
        dueDate,
        body: [...checkedInvoices]
      })
    ]
  )

  return (
    <MutateFormModal
      type="info"
      title={i18n.invoices.sendModal.title}
      icon={faEnvelope}
      resolveMutation={mutation}
      resolveAction={onClick}
      resolveLabel={i18n.common.confirm}
      onSuccess={onSendDone}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
      data-qa="send-invoices-dialog"
    >
      <ModalContent>
        <Label>{i18n.invoices.sendModal.invoiceDate}</Label>
        <div>
          <DatePickerDeprecated
            date={invoiceDate}
            onChange={setInvoiceDate}
            type="full-width"
            data-qa="invoice-date-input"
          />
        </div>
        <Gap size="s" />
        <Label>{i18n.invoices.sendModal.dueDate}</Label>
        <div>
          <DatePickerDeprecated
            date={dueDate}
            onChange={setDueDate}
            type="full-width"
            data-qa="invoice-due-date-input"
          />
        </div>
        {!fullAreaSelection && i18n.invoices.buttons.individualSendAlertText ? (
          <>
            <Gap size="s" />
            <AlertBox
              message={i18n.invoices.buttons.individualSendAlertText}
              thin
            />
          </>
        ) : null}
      </ModalContent>
    </MutateFormModal>
  )
})

const ModalContent = styled.div`
  align-self: flex-start;
  margin-left: 4rem;
  margin-right: 4rem;
`
