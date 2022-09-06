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
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H1, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { errorToInputInfo } from '../../utils/validation/input-info-helper'

import { createHolidayPeriod, updateHolidayPeriod } from './api'

interface FormState {
  start: LocalDate | null
  end: LocalDate | null
  reservationDeadline: LocalDate | null
}

const formToHolidayPeriodBody = (
  s: FormState
): HolidayPeriodBody | undefined => {
  if (!s.start || !s.end) {
    return undefined
  }

  return {
    period: new FiniteDateRange(s.start, s.end),
    reservationDeadline: s.reservationDeadline
  }
}

const emptyFormState = (): FormState => ({
  start: null,
  end: null,
  reservationDeadline: null
})

const toFormState = (p: HolidayPeriod | undefined): FormState =>
  p
    ? {
        start: p.period.start,
        end: p.period.end,
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

  const [errors, isValid, parsedStart, parsedEnd] = useMemo(() => {
    const parsedDeadline = form.reservationDeadline
    const parsedStart = form.start
    const parsedEnd = form.end

    const errors: Record<keyof typeof form, ErrorKey | undefined> = {
      reservationDeadline:
        parsedDeadline && parsedStart && parsedDeadline.isAfter(parsedStart)
          ? 'dateTooLate'
          : undefined,
      start: !parsedStart
        ? 'validDate'
        : parsedEnd && parsedStart.isAfter(parsedEnd)
        ? 'dateTooLate'
        : undefined,
      end: !parsedEnd
        ? 'validDate'
        : parsedStart && parsedEnd.isBefore(parsedStart)
        ? 'dateTooEarly'
        : undefined
    }
    const isValid = Object.values(errors).every((err) => !err)
    return [errors, isValid, parsedStart, parsedEnd]
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

  return (
    <>
      <H1>{i18n.titles.holidayPeriod}</H1>
      <ListGrid>
        <Label>{i18n.holidayPeriods.period} *</Label>
        <FixedSpaceRow alignItems="center">
          <DatePicker
            info={useMemo(
              () => errorToInputInfo(errors.start, i18n.validationErrors),
              [errors.start, i18n.validationErrors]
            )}
            date={form.start}
            locale={lang}
            required
            maxDate={parsedEnd ?? undefined}
            onChange={(start) => update({ start })}
            hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            data-qa="input-start"
            errorTexts={i18n.validationErrors}
          />
          <span>-</span>
          <DatePicker
            info={useMemo(
              () => errorToInputInfo(errors.end, i18n.validationErrors),
              [errors.end, i18n.validationErrors]
            )}
            date={form.end}
            locale={lang}
            required
            minDate={parsedStart ?? undefined}
            onChange={(end) => update({ end })}
            hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            data-qa="input-end"
            errorTexts={i18n.validationErrors}
          />
        </FixedSpaceRow>

        <Label>{i18n.holidayPeriods.reservationDeadline}</Label>
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
          maxDate={parsedStart ?? undefined}
          date={form.reservationDeadline}
          onChange={(reservationDeadline) => update({ reservationDeadline })}
          data-qa="input-reservation-deadline"
          errorTexts={i18n.validationErrors}
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
