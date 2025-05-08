// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faEnvelope, faExclamation } from '@fortawesome/free-solid-svg-icons'
import React, { useCallback, useContext, useState } from 'react'
import styled from 'styled-components'

import { UserContext } from 'employee-frontend/state/user'
import { useBoolean } from 'lib-common/form/hooks'
import { required, validate } from 'lib-common/form-validation'
import {
  InvoiceSortParam,
  InvoiceStatus,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import { InvoiceId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import {
  first,
  second,
  constantQuery,
  useQueryResult,
  useSelectMutation
} from 'lib-common/query'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { ModalType } from 'lib-components/molecules/modals/BaseModal'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { fasExclamationTriangle } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { InvoicingUiContext } from '../../state/invoicing-ui'
import { errorToInputInfo } from '../../utils/validation/input-info-helper'
import { renderResult } from '../async-rendering'

import Actions from './Actions'
import InvoiceFilters from './InvoiceFilters'
import Invoices from './Invoices'
import {
  invoicesQuery,
  resendInvoicesByDateMutation,
  resendInvoicesMutation,
  sendInvoicesByDateMutation,
  sendInvoicesMutation
} from './queries'

type SendModalType = Extract<InvoiceStatus, 'DRAFT' | 'SENT'> | null
type ResultModalType = Extract<ModalType, 'danger' | 'info'> | null

export default React.memo(function InvoicesPage() {
  const { user } = useContext(UserContext)
  const {
    invoices: { confirmedSearchFilters: searchFilters, page }
  } = useContext(InvoicingUiContext)

  const { i18n } = useTranslation()

  const [sortBy, setSortBy] = useState<InvoiceSortParam>('HEAD_OF_FAMILY')
  const [sortDirection, setSortDirection] = useState<SortDirection>('ASC')

  const invoices = useQueryResult(
    searchFilters &&
      (!searchFilters.startDate ||
        !searchFilters.endDate ||
        searchFilters.startDate.isEqualOrBefore(searchFilters.endDate))
      ? invoicesQuery({
          body: {
            page,
            sortBy,
            sortDirection,
            area: searchFilters.area,
            unit: searchFilters.unit ?? null,
            status: searchFilters.status,
            distinctions: searchFilters.distinctiveDetails,
            searchTerms: searchFilters.searchTerms
              ? searchFilters.searchTerms
              : null,
            periodStart: searchFilters.startDate ?? null,
            periodEnd: searchFilters.endDate ?? null
          }
        })
      : constantQuery({ data: [], pages: 0, total: 0 })
  )

  const [sendModalType, setSendModalType] = useState<SendModalType>(null)
  const [resultModalType, setResultModalType] = useState<ResultModalType>(null)
  const openSuccessModal = useCallback(() => {
    setResultModalType('info')
  }, [])
  const openErrorModal = useCallback(() => {
    setResultModalType('danger')
  }, [])
  const closeResultModal = useCallback(() => {
    setResultModalType(null)
  }, [])
  const closeSendModal = useCallback(() => {
    setSendModalType(null)
  }, [])

  const [fullAreaSelection, { set: setFullAreaSelection }] = useBoolean(false)

  const [checkedInvoices, setCheckedInvoices] = useState<Set<InvoiceId>>(
    new Set()
  )
  const toggleChecked = (invoiceId: InvoiceId) =>
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

  const canResend = invoices
    .map((a) => a.data.some((b) => b.permittedActions.includes('RESEND')))
    .getOrElse(false)

  return (
    <Container data-qa="invoices-page">
      <ContentArea opaque>
        <InvoiceFilters clearPreviousResults={clearChecked} />
      </ContentArea>
      <Gap size="XL" />
      {searchFilters &&
        renderResult(invoices, (invoices, isReloading) => (
          <>
            <ContentArea opaque>
              <Invoices
                user={user}
                pagedInvoices={invoices}
                isLoading={isReloading}
                sortBy={sortBy}
                setSortBy={setSortBy}
                sortDirection={sortDirection}
                setSortDirection={setSortDirection}
                showCheckboxes={
                  (searchFilters.status === 'DRAFT' ||
                    searchFilters.status === 'SENT' ||
                    searchFilters.status === 'WAITING_FOR_SENDING') &&
                  (canSend || canDelete || canResend)
                }
                checked={checkedInvoices}
                checkAll={checkAllOnPage}
                clearChecked={clearChecked}
                toggleChecked={toggleChecked}
                fullAreaSelection={fullAreaSelection}
                setFullAreaSelection={setFullAreaSelection}
                fullAreaSelectionDisabled={searchFilters.area.length < 1}
              />
            </ContentArea>
            <Actions
              openModal={() => setSendModalType('DRAFT')}
              openResendModal={() => setSendModalType('SENT')}
              status={searchFilters.status}
              canSend={canSend}
              canDelete={canDelete}
              canResend={canResend}
              checkedInvoices={checkedInvoices}
              clearCheckedInvoices={clearChecked}
              checkedAreas={searchFilters.area}
              fullAreaSelection={fullAreaSelection}
            />
          </>
        ))}
      {sendModalType && (
        <SendModal
          onClose={closeSendModal}
          onSendDone={() => {
            closeSendModal()
            clearChecked()
            openSuccessModal()
          }}
          onSendFailure={() => {
            closeSendModal()
            clearChecked()
            openErrorModal()
          }}
          checkedInvoices={checkedInvoices}
          fullAreaSelection={fullAreaSelection}
          sendType={sendModalType}
        />
      )}
      {resultModalType && (
        <InfoModal
          type={resultModalType}
          title={
            resultModalType === 'info'
              ? i18n.invoices.sendSuccess
              : i18n.invoices.sendFailure
          }
          icon={resultModalType === 'info' ? faEnvelope : faExclamation}
          resolve={{
            action: closeResultModal,
            label: i18n.common.ok
          }}
          close={closeResultModal}
          closeLabel={i18n.common.closeModal}
        />
      )}
    </Container>
  )
})

const SendModal = React.memo(function SendModal({
  onClose,
  onSendDone,
  onSendFailure,
  checkedInvoices,
  fullAreaSelection,
  sendType
}: {
  onClose: () => void
  onSendDone: () => void
  onSendFailure: () => void
  checkedInvoices: Set<InvoiceId>
  fullAreaSelection: boolean
  sendType: SendModalType
}) {
  const { i18n } = useTranslation()
  const {
    invoices: { searchFilters }
  } = useContext(InvoicingUiContext)

  const [invoiceDate, setInvoiceDate] = useState<LocalDate | null>(
    LocalDate.todayInSystemTz()
  )
  const [dueDate, setDueDate] = useState<LocalDate | null>(
    LocalDate.todayInSystemTz().addBusinessDays(10)
  )

  const [mutation, onClick] = useSelectMutation(
    () => (fullAreaSelection ? first() : second()),
    [
      sendType === 'DRAFT'
        ? sendInvoicesByDateMutation
        : resendInvoicesByDateMutation,
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
      sendType === 'DRAFT' ? sendInvoicesMutation : resendInvoicesMutation,
      sendType === 'DRAFT'
        ? () => ({
            invoiceDate,
            dueDate,
            body: [...checkedInvoices]
          })
        : () => ({
            body: [...checkedInvoices]
          })
    ]
  )

  const [confirmResend, setConfirmResend] = useState(false)

  return sendType === 'DRAFT' ? (
    <MutateFormModal
      type="info"
      title={i18n.invoices.sendModal.title}
      icon={faEnvelope}
      resolveMutation={mutation}
      resolveAction={onClick}
      resolveLabel={i18n.common.confirm}
      resolveDisabled={invoiceDate === null || dueDate === null}
      onSuccess={onSendDone}
      onFailure={onSendFailure}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
      data-qa="send-invoices-dialog"
    >
      <ModalContent>
        <Label>{i18n.invoices.sendModal.invoiceDate}</Label>
        <div>
          <DatePicker
            date={invoiceDate}
            onChange={setInvoiceDate}
            info={errorToInputInfo(
              validate(invoiceDate, required),
              i18n.validationErrors
            )}
            hideErrorsBeforeTouched
            locale="fi"
            data-qa="invoice-date-input"
          />
        </div>
        <Gap size="s" />
        <Label>{i18n.invoices.sendModal.dueDate}</Label>
        <div>
          <DatePicker
            date={dueDate}
            onChange={setDueDate}
            info={errorToInputInfo(
              validate(dueDate, required),
              i18n.validationErrors
            )}
            hideErrorsBeforeTouched
            locale="fi"
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
  ) : (
    <MutateFormModal
      type="danger"
      title={i18n.invoices.resendModal.title}
      text={i18n.invoices.resendModal.text}
      icon={fasExclamationTriangle}
      resolveMutation={mutation}
      resolveAction={onClick}
      resolveLabel={i18n.common.confirm}
      resolveDisabled={!confirmResend}
      onSuccess={onSendDone}
      onFailure={onSendFailure}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
      data-qa="resend-invoices-dialog"
    >
      <Checkbox
        label={i18n.invoices.resendModal.confirm}
        checked={confirmResend}
        onChange={setConfirmResend}
        data-qa="resend-invoices-confirm-checkbox"
      />
    </MutateFormModal>
  )
})

const ModalContent = styled.div`
  align-self: flex-start;
  margin-left: 4rem;
  margin-right: 4rem;
`
