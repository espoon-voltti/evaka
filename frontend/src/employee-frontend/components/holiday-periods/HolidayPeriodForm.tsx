// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState } from 'react'
import FiniteDateRange from 'lib-common/finite-date-range'
import { UpdateStateFn } from 'lib-common/form-state'
import {
  ErrorKey,
  httpUrl,
  required,
  validate,
  validateIf
} from 'lib-common/form-validation'
import {
  HolidayPeriod,
  HolidayPeriodBody
} from 'lib-common/generated/api-types/holidayperiod'
import LocalDate from 'lib-common/local-date'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import TextArea from 'lib-components/atoms/form/TextArea'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H1, H3, InformationText, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { useTranslation } from '../../state/i18n'
import { errorToInputInfo } from '../../utils/validation/input-info-helper'
import { createHolidayPeriod, updateHolidayPeriod } from './api'

interface FormState {
  descriptionFi: string
  descriptionSv: string
  descriptionEn: string
  descriptionLinkFi: string
  descriptionLinkSv: string
  descriptionLinkEn: string
  start: string
  end: string
  reservationDeadline: string
  showReservationBannerFrom: string

  useFreeAbsencePeriod: boolean
  freePeriodDeadline: string
  freePeriodQuestionLabelFi: string
  freePeriodQuestionLabelSv: string
  freePeriodQuestionLabelEn: string
  freePeriodOptions: string
  freePeriodOptionLabelFi: string
  freePeriodOptionLabelSv: string
  freePeriodOptionLabelEn: string
}

const parseFiniteDateRange = (range: string): FiniteDateRange | null => {
  const [start, end] = range
    .split('-')
    .map((s) => s.trim())
    .map((s) => LocalDate.parseFiOrNull(s))
  if (!(start && end && start.isEqualOrBefore(end))) return null
  return new FiniteDateRange(start, end)
}

const parseDateRanges = (s: string) =>
  s
    .split(/[,\n]/)
    .map((s) => s.trim())
    .filter(Boolean)
    .map(parseFiniteDateRange)
    .filter(Boolean) as FiniteDateRange[]

const formToHolidayPeriodBody = (
  s: FormState
): HolidayPeriodBody | undefined => {
  const parsedDeadline = LocalDate.parseFiOrNull(s.reservationDeadline)
  const parsedBannerDate = LocalDate.parseFiOrNull(s.showReservationBannerFrom)
  const parsedStart = LocalDate.parseFiOrNull(s.start)
  const parsedEnd = LocalDate.parseFiOrNull(s.end)
  if (!(parsedStart && parsedEnd && parsedBannerDate && parsedDeadline)) {
    return undefined
  }

  const parsedFreePeriodDeadline = LocalDate.parseFiOrNull(s.freePeriodDeadline)
  if (s.useFreeAbsencePeriod && !parsedFreePeriodDeadline) {
    return undefined
  }
  return {
    description: {
      fi: s.descriptionFi,
      sv: s.descriptionSv,
      en: s.descriptionEn
    },
    descriptionLink: {
      fi: s.descriptionLinkFi,
      sv: s.descriptionLinkSv,
      en: s.descriptionLinkEn
    },
    period: new FiniteDateRange(parsedStart, parsedEnd),
    reservationDeadline: parsedDeadline,
    showReservationBannerFrom: parsedBannerDate,
    freePeriod:
      s.useFreeAbsencePeriod && parsedFreePeriodDeadline
        ? {
            deadline: parsedFreePeriodDeadline,
            periodOptionLabel: {
              fi: s.freePeriodOptionLabelFi,
              sv: s.freePeriodOptionLabelSv,
              en: s.freePeriodOptionLabelEn
            },
            questionLabel: {
              fi: s.freePeriodQuestionLabelFi,
              sv: s.freePeriodQuestionLabelSv,
              en: s.freePeriodQuestionLabelEn
            },
            periodOptions: parseDateRanges(s.freePeriodOptions)
          }
        : null
  }
}

const emptyFreePeriod = () => ({
  freePeriodDeadline: '',
  freePeriodOptions: '',
  freePeriodOptionLabelFi: '',
  freePeriodOptionLabelSv: '',
  freePeriodOptionLabelEn: '',
  freePeriodQuestionLabelFi: '',
  freePeriodQuestionLabelSv: '',
  freePeriodQuestionLabelEn: ''
})

const emptyFormState = (): FormState => ({
  descriptionFi: '',
  descriptionSv: '',
  descriptionEn: '',
  descriptionLinkFi: '',
  descriptionLinkSv: '',
  descriptionLinkEn: '',
  start: '',
  end: '',
  reservationDeadline: '',
  showReservationBannerFrom: '',
  useFreeAbsencePeriod: false,
  ...emptyFreePeriod()
})

const toFormState = (p: HolidayPeriod | undefined): FormState => {
  if (!p) {
    return emptyFormState()
  }
  return {
    descriptionFi: p.description.fi,
    descriptionSv: p.description.sv,
    descriptionEn: p.description.en,
    descriptionLinkFi: p.descriptionLink.fi,
    descriptionLinkSv: p.descriptionLink.sv,
    descriptionLinkEn: p.descriptionLink.en,
    start: p.period.start.format(),
    end: p.period.end.format(),
    reservationDeadline: p.reservationDeadline.format(),
    showReservationBannerFrom: p.showReservationBannerFrom.format(),
    useFreeAbsencePeriod: !!p.freePeriod,
    ...(p.freePeriod
      ? {
          freePeriodDeadline: p.freePeriod.deadline.format(),
          freePeriodOptions: p.freePeriod.periodOptions
            .map((r) => r.format())
            .join(', '),
          freePeriodOptionLabelFi: p.freePeriod.periodOptionLabel.fi,
          freePeriodOptionLabelSv: p.freePeriod.periodOptionLabel.sv,
          freePeriodOptionLabelEn: p.freePeriod.periodOptionLabel.en,
          freePeriodQuestionLabelFi: p.freePeriod.questionLabel.fi,
          freePeriodQuestionLabelSv: p.freePeriod.questionLabel.sv,
          freePeriodQuestionLabelEn: p.freePeriod.questionLabel.en
        }
      : emptyFreePeriod())
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

  const [form, setForm] = useState<FormState>(() => toFormState(holidayPeriod))
  const update = useCallback<UpdateStateFn<FormState>>(
    (p) => setForm((old) => ({ ...old, ...p })),
    []
  )

  const [errors, isValid, parsedStart, parsedEnd] = useMemo(() => {
    const parsedBannerDate = LocalDate.parseFiOrNull(
      form.showReservationBannerFrom
    )
    const parsedDeadline = LocalDate.parseFiOrNull(form.reservationDeadline)
    const parsedStart = LocalDate.parseFiOrNull(form.start)
    const parsedEnd = LocalDate.parseFiOrNull(form.end)
    const parsedFreePeriodDeadline = LocalDate.parseFiOrNull(
      form.freePeriodDeadline
    )

    const errors: Record<keyof typeof form, ErrorKey | undefined> = {
      showReservationBannerFrom: !parsedBannerDate
        ? 'required'
        : parsedStart && parsedBannerDate.isAfter(parsedStart)
        ? 'dateTooLate'
        : undefined,
      reservationDeadline: !parsedDeadline
        ? 'required'
        : parsedStart && parsedDeadline.isAfter(parsedStart)
        ? 'dateTooLate'
        : undefined,
      descriptionFi: validate(form.descriptionFi, required),
      descriptionSv: undefined,
      descriptionEn: undefined,
      descriptionLinkFi: validateIf(
        form.descriptionLinkFi.length > 0,
        form.descriptionLinkFi,
        httpUrl
      ),
      descriptionLinkSv: undefined,
      descriptionLinkEn: undefined,
      start: !parsedStart
        ? 'validDate'
        : parsedEnd && parsedStart.isAfter(parsedEnd)
        ? 'dateTooLate'
        : undefined,
      end: !parsedEnd
        ? 'validDate'
        : parsedStart && parsedEnd.isBefore(parsedStart)
        ? 'dateTooEarly'
        : undefined,
      useFreeAbsencePeriod: undefined,
      freePeriodDeadline:
        form.useFreeAbsencePeriod && !parsedFreePeriodDeadline
          ? 'validDate'
          : undefined,
      freePeriodOptionLabelFi: validateIf(
        form.useFreeAbsencePeriod,
        form.freePeriodOptionLabelFi,
        required
      ),
      freePeriodOptionLabelSv: undefined,
      freePeriodOptionLabelEn: undefined,
      freePeriodOptions:
        form.useFreeAbsencePeriod &&
        parseDateRanges(form.freePeriodOptions).length === 0
          ? 'required'
          : undefined,
      freePeriodQuestionLabelFi: validateIf(
        form.useFreeAbsencePeriod,
        form.freePeriodQuestionLabelFi,
        required
      ),
      freePeriodQuestionLabelEn: undefined,
      freePeriodQuestionLabelSv: undefined
    }
    const isValid = Object.values(errors).every((err) => !err)
    return [errors, isValid, parsedStart, parsedEnd]
  }, [form])

  const onSubmit = useCallback(() => {
    const validForm = isValid && formToHolidayPeriodBody(form)
    if (!validForm) {
      return Promise.reject()
    }
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
            isValidDate={(d) => !parsedEnd || d.isEqualOrBefore(parsedEnd)}
            onChange={(start) => update({ start })}
            hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            data-qa="input-start"
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
            isValidDate={(d) => !parsedStart || d.isEqualOrAfter(parsedStart)}
            onChange={(end) => update({ end })}
            hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            data-qa="input-end"
          />
        </FixedSpaceRow>

        <Label>{i18n.holidayPeriods.reservationDeadline} *</Label>
        <DatePicker
          locale={lang}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
          required
          info={useMemo(
            () =>
              errorToInputInfo(
                errors.reservationDeadline,
                i18n.validationErrors
              ),
            [errors.reservationDeadline, i18n.validationErrors]
          )}
          isValidDate={(d) => !parsedStart || d.isEqualOrBefore(parsedStart)}
          date={form.reservationDeadline}
          onChange={(reservationDeadline) => update({ reservationDeadline })}
          data-qa="input-reservation-deadline"
        />

        <Label>{i18n.holidayPeriods.showReservationBannerFrom} *</Label>
        <DatePicker
          locale={lang}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
          required
          info={useMemo(
            () =>
              errorToInputInfo(
                errors.showReservationBannerFrom,
                i18n.validationErrors
              ),
            [errors.showReservationBannerFrom, i18n.validationErrors]
          )}
          isValidDate={(d) => !parsedStart || d.isEqualOrBefore(parsedStart)}
          date={form.showReservationBannerFrom}
          onChange={(v) => update({ showReservationBannerFrom: v })}
          data-qa="input-show-banner-from"
        />

        <Label>{i18n.holidayPeriods.description} *</Label>
        <TextArea
          value={form.descriptionFi}
          onChange={(descriptionFi) => update({ descriptionFi })}
          data-qa="input-description-fi"
          info={useMemo(
            () => errorToInputInfo(errors.descriptionFi, i18n.validationErrors),
            [errors.descriptionFi, i18n]
          )}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Label>{i18n.holidayPeriods.description} SV</Label>
        <TextArea
          value={form.descriptionSv}
          onChange={(descriptionSv) => update({ descriptionSv })}
          data-qa="input-description-sv"
          info={useMemo(
            () => errorToInputInfo(errors.descriptionSv, i18n.validationErrors),
            [errors.descriptionSv, i18n]
          )}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Label>{i18n.holidayPeriods.description} EN</Label>
        <TextArea
          value={form.descriptionEn}
          onChange={(descriptionEn) => update({ descriptionEn })}
          data-qa="input-description-en"
          info={useMemo(
            () => errorToInputInfo(errors.descriptionEn, i18n.validationErrors),
            [errors.descriptionEn, i18n]
          )}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Label>{i18n.holidayPeriods.descriptionLink}</Label>
        <InputField
          width="L"
          placeholder="https://example.com"
          value={form.descriptionLinkFi}
          onChange={(descriptionLinkFi) => update({ descriptionLinkFi })}
          data-qa="input-description-link-fi"
          info={useMemo(
            () =>
              errorToInputInfo(errors.descriptionLinkFi, i18n.validationErrors),
            [errors.descriptionLinkFi, i18n]
          )}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Label>{i18n.holidayPeriods.descriptionLink} SV</Label>
        <InputField
          width="L"
          value={form.descriptionLinkSv}
          onChange={(descriptionLinkSv) => update({ descriptionLinkSv })}
          data-qa="input-description-link-sv"
          info={useMemo(
            () =>
              errorToInputInfo(errors.descriptionLinkSv, i18n.validationErrors),
            [errors.descriptionLinkSv, i18n]
          )}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Label>{i18n.holidayPeriods.descriptionLink} EN</Label>
        <InputField
          width="L"
          value={form.descriptionLinkEn}
          onChange={(descriptionLinkEn) => update({ descriptionLinkEn })}
          data-qa="input-description-link-en"
          info={useMemo(
            () =>
              errorToInputInfo(errors.descriptionLinkEn, i18n.validationErrors),
            [errors.descriptionLinkEn, i18n]
          )}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Label>Kysy maksuton kausi</Label>
        <Checkbox
          checked={form.useFreeAbsencePeriod}
          data-qa="input-use-free-absence-period"
          label=""
          hiddenLabel
          onChange={(useFreeAbsencePeriod) => update({ useFreeAbsencePeriod })}
        />
      </ListGrid>

      {form.useFreeAbsencePeriod && (
        <>
          <H3>Maksuton kausi</H3>
          <ListGrid>
            <Label>{i18n.holidayPeriods.reservationDeadline} *</Label>
            <DatePicker
              date={form.freePeriodDeadline}
              locale={lang}
              required
              onChange={(freePeriodDeadline) => update({ freePeriodDeadline })}
              data-qa="input-free-period-deadline"
            />

            <Label>{i18n.holidayPeriods.freePeriodQuestionLabel} *</Label>
            <TextArea
              value={form.freePeriodQuestionLabelFi}
              onChange={(freePeriodQuestionLabelFi) =>
                update({ freePeriodQuestionLabelFi })
              }
              placeholder={
                i18n.holidayPeriods.freePeriodQuestionLabelPlaceholder
              }
              data-qa="input-free-period-question-label-fi"
              info={errorToInputInfo(
                errors.freePeriodQuestionLabelFi,
                i18n.validationErrors
              )}
              hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            />

            <Label>{i18n.holidayPeriods.freePeriodQuestionLabel} SV</Label>
            <TextArea
              value={form.freePeriodQuestionLabelSv}
              onChange={(freePeriodQuestionLabelSv) =>
                update({ freePeriodQuestionLabelSv })
              }
              data-qa="input-free-period-question-label-sv"
              info={errorToInputInfo(
                errors.freePeriodQuestionLabelSv,
                i18n.validationErrors
              )}
              hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            />

            <Label>{i18n.holidayPeriods.freePeriodQuestionLabel} EN</Label>
            <TextArea
              value={form.freePeriodQuestionLabelEn}
              onChange={(freePeriodQuestionLabelEn) =>
                update({ freePeriodQuestionLabelEn })
              }
              data-qa="input-free-period-question-label-en"
              info={errorToInputInfo(
                errors.freePeriodQuestionLabelEn,
                i18n.validationErrors
              )}
              hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            />

            <Label>{i18n.holidayPeriods.freePeriodOptions} *</Label>
            <TextArea
              required
              value={form.freePeriodOptions}
              onChange={(freePeriodOptions) => update({ freePeriodOptions })}
              placeholder={i18n.holidayPeriods.freePeriodOptionsPlaceholder}
              data-qa="input-free-period-options"
              info={errorToInputInfo(
                errors.freePeriodOptions,
                i18n.validationErrors
              )}
              hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            />

            <Gap horizontal />
            <InformationText>
              {parseDateRanges(form.freePeriodOptions)
                .map((r) => r.formatCompact())
                .join(', ')}
            </InformationText>

            <Label>{i18n.holidayPeriods.freePeriodOptionLabel} *</Label>
            <TextArea
              required
              value={form.freePeriodOptionLabelFi}
              onChange={(freePeriodOptionLabelFi) =>
                update({ freePeriodOptionLabelFi })
              }
              placeholder={i18n.holidayPeriods.freePeriodOptionLabelPlaceholder}
              data-qa="input-free-period-option-label-fi"
              info={errorToInputInfo(
                errors.freePeriodOptionLabelFi,
                i18n.validationErrors
              )}
              hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            />

            <Label>{i18n.holidayPeriods.freePeriodOptionLabel} SV</Label>
            <TextArea
              value={form.freePeriodOptionLabelSv}
              onChange={(freePeriodOptionLabelSv) =>
                update({ freePeriodOptionLabelSv })
              }
              data-qa="input-free-period-option-label-sv"
              info={errorToInputInfo(
                errors.freePeriodOptionLabelSv,
                i18n.validationErrors
              )}
              hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            />

            <Label>{i18n.holidayPeriods.freePeriodOptionLabel} EN</Label>
            <TextArea
              value={form.freePeriodOptionLabelEn}
              onChange={(freePeriodOptionLabelEn) =>
                update({ freePeriodOptionLabelEn })
              }
              data-qa="input-free-period-option-label-en"
              info={errorToInputInfo(
                errors.freePeriodOptionLabelEn,
                i18n.validationErrors
              )}
              hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            />
          </ListGrid>
        </>
      )}

      <Gap />
      <ButtonContainer>
        <AsyncButton
          primary
          disabled={!isValid}
          text={i18n.common.save}
          onSuccess={onSuccess}
          onClick={onSubmit}
          data-qa="save-holiday-period-btn"
        />
        <Button onClick={onCancel} text={i18n.common.goBack} />
      </ButtonContainer>
    </>
  )
})
