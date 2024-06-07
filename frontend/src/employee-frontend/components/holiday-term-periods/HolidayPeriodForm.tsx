// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { boolean, localDate, localDateRange } from 'lib-common/form/fields'
import { object, required, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import { mockToday } from 'lib-common/utils/helpers'
import { LegacyAsyncButton } from 'lib-components/atoms/buttons/LegacyAsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { H1, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

import {
  createHolidayPeriodMutation,
  updateHolidayPeriodMutation
} from './queries'

const minStartDate = (mockToday() ?? LocalDate.todayInSystemTz()).addWeeks(4)
const maxPeriod = 15 * 7 * 24 * 60 * 60 * 1000 // 15 weeks

function makeHolidayPeriodForm(mode: 'create' | 'update') {
  return object({
    period: validated(required(localDateRange()), (output) =>
      // Extra validations when creating a new holiday period
      mode === 'create'
        ? output.start.isBefore(minStartDate)
          ? 'tooSoon'
          : output.end.toSystemTzDate().valueOf() -
                output.start.toSystemTzDate().valueOf() >
              maxPeriod
            ? 'tooLong'
            : undefined
        : undefined
    ),
    reservationDeadline: required(localDate()),
    confirm: validated(boolean(), (value) => (value ? undefined : 'required'))
  })
}

const createHolidayPeriodForm = makeHolidayPeriodForm('create')
const updateHolidayPeriodForm = makeHolidayPeriodForm('update')

type HolidayPeriodFormState = StateOf<typeof createHolidayPeriodForm>

const emptyFormState: HolidayPeriodFormState = {
  period: localDateRange.empty(),
  reservationDeadline: localDate.empty(),
  confirm: false
}

function initialFormState(p: HolidayPeriod): HolidayPeriodFormState {
  return {
    period: localDateRange.fromRange(p.period),
    reservationDeadline: localDate.fromDate(p.reservationDeadline, {
      maxDate: p.period.start
    }),
    confirm: true
  }
}

interface Props {
  onSuccess: () => void
  onCancel: () => void
  holidayPeriod?: HolidayPeriod
}

export default React.memo(function HolidayPeriodForm({
  onCancel,
  onSuccess,
  holidayPeriod
}: Props) {
  const { i18n, lang } = useTranslation()

  const form = useForm(
    holidayPeriod !== undefined
      ? updateHolidayPeriodForm
      : createHolidayPeriodForm,
    () =>
      holidayPeriod !== undefined
        ? initialFormState(holidayPeriod)
        : emptyFormState,
    {
      ...i18n.validationErrors,
      ...i18n.holidayPeriods.validationErrors
    },
    {
      onUpdate(_, nextState, form) {
        const period = form.shape().period.validate(nextState.period)
        if (period.isValid) {
          return {
            ...nextState,
            reservationDeadline: {
              ...nextState.reservationDeadline,
              config: { maxDate: period.value.start } // reservation deadline must be before the start of the period
            }
          }
        }
        return nextState
      }
    }
  )

  const { mutateAsync: createHolidayPeriod } = useMutationResult(
    createHolidayPeriodMutation
  )
  const { mutateAsync: updateHolidayPeriod } = useMutationResult(
    updateHolidayPeriodMutation
  )

  const onSubmit = useCallback(
    () =>
      holidayPeriod !== undefined
        ? updateHolidayPeriod({ id: holidayPeriod.id, body: form.value() })
        : createHolidayPeriod({ body: form.value() }),
    [form, holidayPeriod, createHolidayPeriod, updateHolidayPeriod]
  )

  const hideErrorsBeforeTouched = holidayPeriod === undefined

  const { period, reservationDeadline, confirm } = useFormFields(form)
  return (
    <>
      <H1>{i18n.titles.holidayPeriod}</H1>
      <ListGrid>
        <Label>{i18n.holidayPeriods.period} *</Label>
        <FixedSpaceRow alignItems="center">
          <DateRangePickerF bind={period} locale={lang} data-qa="period" />
        </FixedSpaceRow>

        <Label>{i18n.holidayPeriods.reservationDeadline} *</Label>
        <DatePickerF
          bind={reservationDeadline}
          locale={lang}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
          data-qa="input-reservation-deadline"
        />
      </ListGrid>

      {holidayPeriod === undefined ? (
        <>
          <AlertBox message={i18n.holidayPeriods.clearingAlert} />
          <CheckboxF
            label={i18n.holidayPeriods.confirmLabel}
            bind={confirm}
            data-qa="confirm-checkbox"
          />
        </>
      ) : null}

      <Gap />
      <ButtonContainer>
        <LegacyAsyncButton
          primary
          disabled={!form.isValid()}
          text={i18n.common.save}
          onSuccess={onSuccess}
          onClick={() => onSubmit().then((res) => res.map(() => undefined))}
          data-qa="save-btn"
        />
        <LegacyButton onClick={onCancel} text={i18n.common.goBack} />
      </ButtonContainer>
    </>
  )
})
