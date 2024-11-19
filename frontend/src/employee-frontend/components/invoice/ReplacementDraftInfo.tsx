// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import { string } from 'lib-common/form/fields'
import { object, oneOf, required } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import {
  InvoiceDetailedResponse,
  InvoiceReplacementReason,
  invoiceReplacementReasons
} from 'lib-common/generated/api-types/invoicing'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { SelectF } from 'lib-components/atoms/dropdowns/Select'
import { TextAreaF } from 'lib-components/atoms/form/TextArea'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { H3, Label, P } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { markReplacementDraftSentMutation } from '../invoices/queries'

const replacementDraftForm = object({
  reason: required(oneOf<InvoiceReplacementReason>()),
  notes: string()
})

export function ReplacementDraftForm({
  invoiceResponse
}: {
  invoiceResponse: InvoiceDetailedResponse
}) {
  const { i18n } = useTranslation()

  const form = useForm(
    replacementDraftForm,
    () => ({
      reason: {
        domValue: '',
        options: invoiceReplacementReasons.map((r) => ({
          domValue: r,
          value: r,
          label: i18n.invoice.form.replacement.reasons[r]
        }))
      },
      notes: ''
    }),
    i18n.validationErrors
  )
  const { reason, notes } = useFormFields(form)

  return (
    <FixedSpaceColumn data-qa="replacement-draft-form">
      <H3>{i18n.invoice.form.replacement.title}</H3>
      <P>{i18n.invoice.form.replacement.info}</P>
      <FixedSpaceRow>
        <FixedSpaceColumn>
          <Label>{i18n.invoice.form.replacement.reason} *</Label>
          <SelectF
            bind={reason}
            placeholder={i18n.common.select}
            hideErrorsBeforeTouched
            data-qa="replacement-reason"
          />
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{i18n.invoice.form.replacement.notes}</Label>
          <TextAreaWrapper>
            <TextAreaF bind={notes} data-qa="replacement-notes" />
          </TextAreaWrapper>
        </FixedSpaceColumn>
      </FixedSpaceRow>
      <FixedSpaceRow justifyContent="flex-end">
        <InfoBox message={i18n.invoice.form.replacement.sendInfo} />
      </FixedSpaceRow>
      <FixedSpaceRow justifyContent="flex-end">
        <MutateButton
          primary
          mutation={markReplacementDraftSentMutation}
          onClick={() => ({
            invoiceId: invoiceResponse.invoice.id,
            body: form.value()
          })}
          text={i18n.invoice.form.replacement.send}
          disabled={!form.isValid()}
          data-qa="mark-sent"
        />
      </FixedSpaceRow>
    </FixedSpaceColumn>
  )
}

export function ReplacementInfo({
  invoiceResponse
}: {
  invoiceResponse: InvoiceDetailedResponse
}) {
  const { i18n } = useTranslation()
  const { invoice } = invoiceResponse

  if (invoice.replacementReason === null) return null

  return (
    <FixedSpaceColumn data-qa="replacement-info">
      <H3>{i18n.invoice.form.replacement.title}</H3>
      <P>{i18n.invoice.form.replacement.info}</P>
      <FixedSpaceRow spacing="L">
        <FixedSpaceColumn>
          <Label>{i18n.invoice.form.replacement.reason}</Label>
          <div data-qa="replacement-reason">
            {i18n.invoice.form.replacement.reasons[invoice.replacementReason]}
          </div>
        </FixedSpaceColumn>
        <FixedSpaceColumn>
          <Label>{i18n.invoice.form.replacement.notes}</Label>
          <NotesWrapper data-qa="replacement-notes">
            {invoice.replacementNotes}
          </NotesWrapper>
        </FixedSpaceColumn>
      </FixedSpaceRow>
      <div>
        <Label>{i18n.invoice.form.replacement.markedAsSent}</Label>
        <div>
          <span data-qa="sent-at">{invoice.sentAt?.format()}</span>
          {' ('}
          <span data-qa="sent-by">{invoice.sentBy?.name}</span>)
        </div>
      </div>
    </FixedSpaceColumn>
  )
}

const TextAreaWrapper = styled.div`
  min-width: 400px;
`

const NotesWrapper = styled.div`
  white-space: pre-line;
`
