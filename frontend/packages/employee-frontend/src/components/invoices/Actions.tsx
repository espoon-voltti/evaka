// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useEffect, Fragment } from 'react'
import styled from 'styled-components'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Checkbox, Field } from '~components/shared/alpha'
import FormModal from '~components/common/FormModal'
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
import { GapHorizalSmall } from '~components/common/styled/separators'

const LeftSideContent = styled.div`
  margin-right: auto;
`

const ErrorMessage = styled.div`
  color: ${EspooColours.red};
  margin-right: 20px;
`

const CheckedRowsInfo = styled.div`
  color: ${EspooColours.grey};
  font-style: italic;
  font-weight: bold;
  margin: 0 20px;
`

const ModalContent = styled.div`
  align-self: flex-start;
  margin-left: 4rem;
  margin-right: 4rem;
`

type Action = {
  id: string
  label: string
  primary: boolean
  enabled: boolean
  disabled: boolean
  onClick: () => void
}

type Props = {
  status: InvoiceStatus
  checkedIds: string[]
  checkedAreas: string[]
  clearChecked: () => void
  loadInvoices: () => void
  periodStart: LocalDate | undefined
  periodEnd: LocalDate | undefined
  useCustomDatesForInvoiceSending: boolean
}

const Actions = React.memo(function Actions({
  status,
  checkedIds,
  checkedAreas,
  clearChecked,
  loadInvoices,
  periodStart,
  periodEnd,
  useCustomDatesForInvoiceSending
}: Props) {
  const { i18n } = useTranslation()
  const [actionInFlight, setActionInFlight] = useState(false)
  const [error, setError] = useState(false)
  const [showModal, setShowModal] = useState(false)
  const [invoiceDate, setInvoiceDate] = useState(LocalDate.today())
  const [dueDate, setDueDate] = useState(LocalDate.today().addBusinessDays(10))
  const [individualInvoices, setIndividualInvoices] = useState(false)
  const [sendWholeArea, setSendWholeArea] = useState(false)

  useEffect(() => {
    if (checkedIds.length > 0 && sendWholeArea) {
      setSendWholeArea(false)
    }
  }, [checkedIds])

  const closeModal = () => setShowModal(false)

  const send = () => {
    setActionInFlight(true)
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
      .then(() => clearChecked())
      .then(() => loadInvoices())
      .then(() => void setError(false))
      .catch(() => void setError(true))
      .finally(() => void setActionInFlight(false))
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

  const actions: Action[] = [
    {
      id: 'delete-invoices',
      label: i18n.invoices.buttons.deleteInvoice(checkedIds.length),
      primary: false,
      enabled: status === 'DRAFT',
      disabled: actionInFlight || checkedIds.length === 0,
      onClick: () => {
        setActionInFlight(true)
        deleteInvoices(checkedIds)
          .then(() => clearChecked())
          .then(() => loadInvoices())
          .then(() => void setError(false))
          .catch(() => void setError(true))
          .finally(() => void setActionInFlight(false))
      }
    },
    {
      id: 'open-send-invoices-dialog',
      label: i18n.invoices.buttons.sendInvoice(checkedIds.length),
      primary: true,
      enabled: status === 'DRAFT',
      disabled:
        actionInFlight ||
        (!sendWholeArea && checkedIds.length === 0) ||
        (sendWholeArea && checkedAreas.length === 0),
      onClick: () => {
        sendWholeArea ? sendMultipleInvoices() : sendIndividualInvoices()
      }
    }
  ].filter(({ enabled }) => enabled)

  return actions.length > 0 ? (
    <StickyActionBar align={'right'}>
      {status === 'DRAFT' ? (
        <LeftSideContent>
          <Checkbox
            name={'send-whole-area'}
            label={i18n.invoices.buttons.checkAreaInvoices(
              useCustomDatesForInvoiceSending
            )}
            checked={sendWholeArea}
            onChange={() => {
              if (checkedIds.length > 0) {
                clearChecked()
              }
              setSendWholeArea(!sendWholeArea)
            }}
          />
        </LeftSideContent>
      ) : null}
      {error ? <ErrorMessage>{i18n.common.error.unknown}</ErrorMessage> : null}
      {checkedIds.length > 0 ? (
        <CheckedRowsInfo>
          {i18n.invoices.buttons.checked(checkedIds.length)}
        </CheckedRowsInfo>
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
            <Field label={i18n.invoices.sendModal.invoiceDate}>
              <DatePicker
                date={invoiceDate}
                onChange={setInvoiceDate}
                type="full-width"
                dataQa="invoice-date-input"
              />
            </Field>
            <Field label={i18n.invoices.sendModal.dueDate}>
              <DatePicker
                date={dueDate}
                onChange={setDueDate}
                type="full-width"
                dataQa="invoice-due-date-input"
              />
            </Field>
            {individualInvoices ? (
              <AlertBox
                message={i18n.invoices.buttons.individualSendAlertText}
                thin
              />
            ) : null}
          </ModalContent>
        </FormModal>
      )}
      {actions.map(({ id, label, primary, disabled, onClick }) => (
        <Fragment key={id}>
          <Button
            key={id}
            primary={primary}
            disabled={disabled}
            text={label}
            onClick={onClick}
            dataQa={id}
          />
          <GapHorizalSmall />
        </Fragment>
      ))}
    </StickyActionBar>
  ) : null
})

export default Actions
