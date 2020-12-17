// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { CaretakerAmount } from '~types/caretakers'
import FormModal from '~components/common/FormModal'
import { faPen, faPlus } from '@evaka/lib-icons'
import { useTranslation } from '~state/i18n'
import InputField from '~components/shared/atoms/form/InputField'
import Section from '~components/shared/layout/Section'
import { DatePicker, DatePickerClearable } from '~components/common/DatePicker'
import styled from 'styled-components'
import { UUID } from '~types'
import { postCaretakers, putCaretakers } from '~api/caretakers'
import { Result } from '~api'
import { AlertBox } from '~components/common/MessageBoxes'

const NumberInputContainer = styled.div`
  width: 150px;
`

const numberRegex = /^\d{1,2}((\.|,){1}(\d){1})?$/

interface FormState {
  startDate: LocalDate
  endDate: LocalDate | null
  amount: string
}

interface Props {
  unitId: UUID
  groupId: UUID
  existing: CaretakerAmount | null
  onSuccess: () => undefined | void
  onReject: () => undefined | void
}

function GroupCaretakersModal({
  existing,
  onSuccess,
  onReject,
  unitId,
  groupId
}: Props) {
  const { i18n } = useTranslation()

  const [form, setForm] = useState<FormState>(
    existing
      ? {
          startDate: existing.startDate,
          endDate: existing.endDate,
          amount: existing.amount.toLocaleString()
        }
      : {
          startDate: LocalDate.today(),
          endDate: null,
          amount: '3'
        }
  )
  const [submitting, setSubmitting] = useState<boolean>(false)
  const [conflict, setConflict] = useState<boolean>(false)

  const assignForm = (values: Partial<FormState>) => {
    setForm({
      ...form,
      ...values
    })
  }

  const submit = () => {
    setSubmitting(true)
    setConflict(false)
    ;(existing
      ? putCaretakers(
          unitId,
          groupId,
          existing.id,
          form.startDate,
          form.endDate,
          parseFloat(form.amount)
        )
      : postCaretakers(
          unitId,
          groupId,
          form.startDate,
          form.endDate,
          parseFloat(form.amount)
        )
    )
      .then((res: Result<null>) => {
        if (res.isSuccess) onSuccess()
        if (res.isFailure && res.statusCode === 409) setConflict(true)
      })
      .finally(() => setSubmitting(false))
  }

  const invalidAmount =
    !numberRegex.test(form.amount) || Number(form.amount.replace(',', '.')) < 0

  const hasErrors =
    (form.endDate && form.endDate.isBefore(form.startDate)) || invalidAmount

  const editingHistory =
    existing && existing.endDate && existing.endDate.isBefore(LocalDate.today())
  const editingActive =
    existing &&
    !existing.startDate.isAfter(LocalDate.today()) &&
    !editingHistory

  return (
    <FormModal
      title={existing ? i18n.groupCaretakers.edit : i18n.groupCaretakers.create}
      icon={existing ? faPen : faPlus}
      iconColour={'blue'}
      resolveLabel={i18n.common.confirm}
      resolve={submit}
      resolveDisabled={hasErrors || submitting}
      rejectLabel={i18n.common.cancel}
      reject={onReject}
    >
      <Section>
        <div className="bold">{i18n.common.form.startDate}</div>
        <DatePicker
          date={form.startDate}
          onChange={(startDate) => assignForm({ startDate })}
          type="full-width"
        />
      </Section>
      <Section>
        <div className="bold">{i18n.common.form.endDate}</div>
        <DatePickerClearable
          date={form.endDate}
          onChange={(endDate) => assignForm({ endDate })}
          onCleared={() => assignForm({ endDate: null })}
          type="full-width"
        />
      </Section>
      <Section>
        <div className="bold">{i18n.groupCaretakers.amount}</div>
        <NumberInputContainer>
          <InputField
            value={form.amount.toString()}
            onChange={(value) =>
              assignForm({
                amount: value
              })
            }
            info={
              invalidAmount
                ? { text: 'Virheellinen arvo', status: 'warning' }
                : undefined
            }
            dataQa="input-assistance-need-multiplier"
          />
        </NumberInputContainer>
      </Section>
      {conflict && <AlertBox message={i18n.groupCaretakers.conflict} />}
      {editingHistory && (
        <AlertBox message={i18n.groupCaretakers.editHistoryWarning} />
      )}
      {editingActive && (
        <AlertBox message={i18n.groupCaretakers.editActiveWarning} />
      )}
    </FormModal>
  )
}

export default GroupCaretakersModal
