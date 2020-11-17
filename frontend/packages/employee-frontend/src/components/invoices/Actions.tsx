// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useEffect } from 'react'
import styled from 'styled-components'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Gap } from '~components/shared/layout/white-space'
import { Label } from '~components/shared/Typography'
import Checkbox from '~components/shared/atoms/form/Checkbox'
import FormModal from '~components/common/FormModal'
import AsyncButton from '~components/shared/atoms/buttons/AsyncButton'
import Button from '~components/shared/atoms/buttons/Button'
import { useTranslation } from '../../state/i18n'
import { AlertBox } from '~components/common/MessageBoxes'
import StickyActionBar from '../common/StickyActionBar'
import {
  deleteInvoices,
  sendInvoicesByDate,
  sendInvoices
} from '../../api/invoicing'
import { InvoiceStatus } from '../../types/invoicing'
import { EspooColours } from '../../utils/colours'
import { faEnvelope } from '@fortawesome/free-solid-svg-icons'
import { DatePicker } from '~components/common/DatePicker'
import { InvoicesAction } from './invoices-state'

const LeftSideContent = styled.div`
  margin-right: auto;
`

const ErrorMessage = styled.div`
  color: ${EspooColours.red};
`

const CheckedRowsInfo = styled.div`
  color: ${EspooColours.grey};
  font-style: italic;
  font-weight: bold;
`

const ModalContent = styled.div`
  align-self: flex-start;
  margin-left: 4rem;
  margin-right: 4rem;
`

type Props = {
  dispatch: React.Dispatch<InvoicesAction>
  status: InvoiceStatus
  checkedInvoices: Record<string, boolean>
  checkedAreas: string[]
  periodStart: LocalDate | undefined
  periodEnd: LocalDate | undefined
  useCustomDatesForInvoiceSending: boolean
}

const Actions = React.memo(function Actions({
  dispatch,
  status,
  checkedInvoices,
  checkedAreas,
  periodStart,
  periodEnd,
  useCustomDatesForInvoiceSending
}: Props) {
  const { i18n } = useTranslation()
  const [error, setError] = useState(false)
  const [showModal, setShowModal] = useState(false)
  const [invoiceDate, setInvoiceDate] = useState(LocalDate.today())
  const [dueDate, setDueDate] = useState(LocalDate.today().addBusinessDays(10))
  const [individualInvoices, setIndividualInvoices] = useState(false)
  const [sendWholeArea, setSendWholeArea] = useState(false)

  const checkedIds = Object.entries(checkedInvoices)
    .filter(([, checked]) => checked)
    .map(([id]) => id)

  useEffect(() => {
    if (checkedIds.length > 0 && sendWholeArea) {
      setSendWholeArea(false)
    }
  }, [checkedIds])

  const closeModal = () => setShowModal(false)

  const send = () => {
    const request = individualInvoices
      ? sendInvoices(checkedIds, invoiceDate, dueDate)
      : sendInvoicesByDate(
          invoiceDate,
          dueDate,
          checkedAreas,
          periodStart,
          periodEnd,
          useCustomDatesForInvoiceSending
        )
    request
      .then(() => {
        dispatch({ type: 'CLEAR_CHECKED' })
        dispatch({ type: 'RELOAD_INVOICES' })
        setError(false)
      })
      .catch(() => void setError(true))
    closeModal()
  }

  const sendIndividualInvoices = () => {
    setIndividualInvoices(true)
    setShowModal(true)
  }

  const sendMultipleInvoices = () => {
    setIndividualInvoices(false)
    setShowModal(true)
  }

  return status === 'DRAFT' ? (
    <StickyActionBar align={'right'}>
      <LeftSideContent>
        <Checkbox
          label={i18n.invoices.buttons.checkAreaInvoices(
            useCustomDatesForInvoiceSending
          )}
          checked={sendWholeArea}
          onChange={() => {
            if (checkedIds.length > 0) {
              dispatch({ type: 'CLEAR_CHECKED' })
            }
            setSendWholeArea(!sendWholeArea)
          }}
        />
      </LeftSideContent>
      {error ? (
        <>
          <ErrorMessage>{i18n.common.error.unknown}</ErrorMessage>
          <Gap size="s" horizontal />
        </>
      ) : null}
      {checkedIds.length > 0 ? (
        <>
          <CheckedRowsInfo>
            {i18n.invoices.buttons.checked(checkedIds.length)}
          </CheckedRowsInfo>
          <Gap size="s" horizontal />
        </>
      ) : null}
      {showModal && (
        <FormModal
          iconColour={'blue'}
          title={i18n.invoices.sendModal.title}
          resolveLabel={i18n.common.confirm}
          rejectLabel={i18n.common.cancel}
          icon={faEnvelope}
          reject={() => closeModal()}
          resolve={send}
          data-qa="send-invoices-dialog"
        >
          <ModalContent>
            <Label>{i18n.invoices.sendModal.invoiceDate}</Label>
            <div>
              <DatePicker
                date={invoiceDate}
                onChange={setInvoiceDate}
                type="full-width"
                dataQa="invoice-date-input"
              />
            </div>
            <Gap size="s" />
            <Label>{i18n.invoices.sendModal.dueDate}</Label>
            <div>
              <DatePicker
                date={dueDate}
                onChange={setDueDate}
                type="full-width"
                dataQa="invoice-due-date-input"
              />
            </div>
            {individualInvoices ? (
              <>
                <Gap size="s" />
                <AlertBox
                  message={i18n.invoices.buttons.individualSendAlertText}
                  thin
                />
              </>
            ) : null}
          </ModalContent>
        </FormModal>
      )}
      <AsyncButton
        text={i18n.invoices.buttons.deleteInvoice(checkedIds.length)}
        disabled={checkedIds.length === 0}
        onClick={() =>
          deleteInvoices(checkedIds)
            .then(() => void setError(false))
            .catch(() => void setError(true))
        }
        onSuccess={() => {
          dispatch({ type: 'CLEAR_CHECKED' })
          dispatch({ type: 'RELOAD_INVOICES' })
        }}
        data-qa="delete-invoices"
      />
      <Gap size="s" horizontal />
      <Button
        primary
        disabled={
          (!sendWholeArea && checkedIds.length === 0) ||
          (sendWholeArea && checkedAreas.length === 0)
        }
        text={i18n.invoices.buttons.sendInvoice(checkedIds.length)}
        onClick={() => {
          sendWholeArea ? sendMultipleInvoices() : sendIndividualInvoices()
        }}
        data-qa="open-send-invoices-dialog"
      />
    </StickyActionBar>
  ) : null
})

export default Actions
