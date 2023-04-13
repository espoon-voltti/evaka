// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback } from 'react'

import { boolean, localDate, localDateRange } from 'lib-common/form/fields'
import { object, required, validated } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { StateOf } from 'lib-common/form/types'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { useMutationResult } from 'lib-common/query'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { H1, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

import {
  createHolidayPeriodMutation,
  updateHolidayPeriodMutation
} from './queries'

const holidayPeriodForm = object({
  period: required(localDateRange),
  reservationDeadline: required(localDate),
  confirm: validated(boolean(), (value) => (value ? undefined : 'required'))
})

const emptyFormState: StateOf<typeof holidayPeriodForm> = {
  period: {
    startDate: null,
    endDate: null
  },
  reservationDeadline: null,
  confirm: false
}

function initialFormState(p: HolidayPeriod): StateOf<typeof holidayPeriodForm> {
  return {
    period: {
      startDate: p.period.start,
      endDate: p.period.end
    },
    reservationDeadline: p.reservationDeadline,
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
    holidayPeriodForm,
    () =>
      holidayPeriod !== undefined
        ? initialFormState(holidayPeriod)
        : emptyFormState,
    i18n.validationErrors
  )

  const { mutateAsync: createHolidayPeriod } = useMutationResult(
    createHolidayPeriodMutation
  )
  const { mutateAsync: updateHolidayPeriod } = useMutationResult(
    updateHolidayPeriodMutation
  )

  const onSubmit = useCallback(() => {
    return holidayPeriod !== undefined
      ? updateHolidayPeriod({ id: holidayPeriod.id, data: form.value() })
      : createHolidayPeriod(form.value())
  }, [form, holidayPeriod, createHolidayPeriod, updateHolidayPeriod])

  const hideErrorsBeforeTouched = holidayPeriod === undefined

  const { period, reservationDeadline, confirm } = useFormFields(form)
  const { startDate, endDate } = useFormFields(period)

  return (
    <>
      <H1>{i18n.titles.holidayPeriod}</H1>
      <ListGrid>
        <Label>{i18n.holidayPeriods.period} *</Label>
        <FixedSpaceRow alignItems="center">
          <DatePickerF
            bind={startDate}
            locale={lang}
            required
            maxDate={endDate.state ?? undefined}
            hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            data-qa="input-start"
          />
          <span>-</span>
          <DatePickerF
            bind={endDate}
            locale={lang}
            required
            minDate={startDate.state ?? undefined}
            hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            data-qa="input-end"
          />
        </FixedSpaceRow>

        <Label>{i18n.holidayPeriods.reservationDeadline} *</Label>
        <DatePickerF
          bind={reservationDeadline}
          locale={lang}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
          maxDate={startDate.state ?? undefined}
          data-qa="input-reservation-deadline"
        />
      </ListGrid>

      {holidayPeriod === undefined ? (
        <>
          <AlertBox message="Kuntalaisten jo tekemät varaukset pyyhitään valitulta aikaväliltä" />
          <CheckboxF
            label="Ymmärrän, että tehdyt varaukset poistetaan välittömästi, eikä tätä voi enää perua."
            bind={confirm}
          />
        </>
      ) : null}

      <Gap />
      <ButtonContainer>
        <AsyncButton
          primary
          disabled={!form.isValid()}
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
