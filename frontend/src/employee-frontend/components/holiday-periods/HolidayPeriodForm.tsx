// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import { UpdateStateFn } from 'lib-common/form-state'
import { ErrorKey } from 'lib-common/form-validation'
import {
  HolidayPeriod,
  HolidayPeriodBody
} from 'lib-common/generated/api-types/holidayperiod'
import LocalDate from 'lib-common/local-date'
import { useUniqueId } from 'lib-common/utils/useUniqueId'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import ListGrid from 'lib-components/layout/ListGrid'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
import { H1, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { errorToInputInfo } from '../../utils/validation/input-info-helper'

import { createHolidayPeriod, updateHolidayPeriod } from './api'

interface FormState {
  range: FiniteDateRange | null
  reservationDeadline: LocalDate | null
}

const formToHolidayPeriodBody = (
  s: FormState
): HolidayPeriodBody | undefined => {
  if (!s.range) {
    return undefined
  }

  return {
    period: s.range,
    reservationDeadline: s.reservationDeadline
  }
}

const emptyFormState = (): FormState => ({
  range: null,
  reservationDeadline: null
})

const toFormState = (p: HolidayPeriod | undefined): FormState =>
  p
    ? {
        range: p.period,
        reservationDeadline: p.reservationDeadline
      }
    : emptyFormState()

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

  const [form, setForm] = useState<FormState>(() => toFormState(holidayPeriod))
  const update = useCallback<UpdateStateFn<FormState>>(
    (p) => setForm((old) => ({ ...old, ...p })),
    []
  )

  const [errors, isValid] = useMemo(() => {
    const parsedDeadline = form.reservationDeadline

    const errors: Record<keyof typeof form, ErrorKey | undefined> = {
      reservationDeadline:
        parsedDeadline && form.range && parsedDeadline.isAfter(form.range.start)
          ? 'dateTooLate'
          : undefined,
      range: !form.range ? 'required' : undefined
    }
    const isValid = Object.values(errors).every((err) => !err)
    return [errors, isValid]
  }, [form])

  const onSubmit = useCallback(() => {
    const validForm = isValid && formToHolidayPeriodBody(form)
    if (!validForm) return

    const apiCall = holidayPeriod
      ? (params: HolidayPeriodBody) =>
          updateHolidayPeriod(holidayPeriod.id, params)
      : createHolidayPeriod
    return apiCall(validForm)
  }, [form, holidayPeriod, isValid])

  const hideErrorsBeforeTouched = !holidayPeriod

  const reservationDeadlineAriaId = useUniqueId()
  const holidayPeriodsAriaId = useUniqueId()

  return (
    <>
      <H1>{i18n.titles.holidayPeriod}</H1>
      <ListGrid>
        <Label id={holidayPeriodsAriaId}>{i18n.holidayPeriods.period} *</Label>
        <DateRangePicker
          info={useMemo(
            () => errorToInputInfo(errors.range, i18n.validationErrors),
            [errors.range, i18n.validationErrors]
          )}
          default={form.range}
          locale={lang}
          required
          onChange={(range) => update({ range })}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
          data-qa="input-range"
          errorTexts={i18n.validationErrors}
          labels={i18n.common.datePicker}
          aria-labelledby={holidayPeriodsAriaId}
        />

        <Label id={reservationDeadlineAriaId}>
          {i18n.holidayPeriods.reservationDeadline}
        </Label>
        <DatePicker
          locale={lang}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
          info={useMemo(
            () =>
              errorToInputInfo(
                errors.reservationDeadline,
                i18n.validationErrors
              ),
            [errors.reservationDeadline, i18n.validationErrors]
          )}
          maxDate={form.range?.start}
          date={form.reservationDeadline}
          onChange={(reservationDeadline) => update({ reservationDeadline })}
          data-qa="input-reservation-deadline"
          errorTexts={i18n.validationErrors}
          aria-labelledby={reservationDeadlineAriaId}
          labels={i18n.common.datePicker}
        />
      </ListGrid>

      <Gap />
      <ButtonContainer>
        <AsyncButton
          primary
          disabled={!isValid}
          text={i18n.common.save}
          onSuccess={onSuccess}
          onClick={onSubmit}
          data-qa="save-btn"
        />
        <Button onClick={onCancel} text={i18n.common.goBack} />
      </ButtonContainer>
    </>
  )
})
