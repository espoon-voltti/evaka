// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import max from 'lodash/max'
import React from 'react'
import styled from 'styled-components'

import { localDate } from 'lib-common/form/fields'
import { object, required, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import type { Daycare } from 'lib-common/generated/api-types/daycare'
import type LocalDate from 'lib-common/local-date'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'

import { useTranslation } from '../../../state/i18n'
import { updateUnitClosingDateMutation } from '../queries'

import { closingDateIsBeforeLastPlacementDate } from './utils'

interface UnitClosingDateModalProps {
  unit: Daycare
  lastPlacementDate: LocalDate | null
  onClose: () => void
}

const form = validated(
  object({
    openingDate: localDate(),
    closingDate: required(localDate()),
    lastPlacementDate: localDate()
  }),
  ({ openingDate, closingDate, lastPlacementDate }) =>
    (openingDate !== undefined && closingDate.isBefore(openingDate)) ||
    closingDateIsBeforeLastPlacementDate(closingDate, lastPlacementDate)
      ? { closingDate: 'dateTooEarly' }
      : undefined
)

export const UnitClosingDateModal = React.memo(function UnitClosingDateModal({
  unit,
  lastPlacementDate,
  onClose
}: UnitClosingDateModalProps) {
  const { i18n, lang } = useTranslation()
  const boundForm = useForm(
    form,
    () => ({
      openingDate: localDate.fromDate(unit.openingDate),
      closingDate: localDate.fromDate(unit.closingDate, {
        minDate: max([unit.openingDate, lastPlacementDate]) ?? undefined
      }),
      lastPlacementDate: localDate.fromDate(lastPlacementDate)
    }),
    i18n.validationErrors
  )
  const { closingDate } = useFormFields(boundForm)

  return (
    <MutateFormModal
      title={i18n.unitEditor.closingDateModal}
      resolveLabel={i18n.common.save}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!boundForm.isValid()}
      resolveMutation={updateUnitClosingDateMutation}
      resolveAction={() => ({
        unitId: unit.id,
        closingDate: closingDate.value()
      })}
      rejectAction={onClose}
      onSuccess={onClose}
      data-qa="unit-closing-date-modal"
    >
      <Center>
        <DatePickerF
          bind={closingDate}
          locale={lang}
          hideErrorsBeforeTouched
          data-qa="closing-date"
        />
      </Center>
    </MutateFormModal>
  )
})

const Center = styled.div`
  display: flex;
  justify-content: center;
`
