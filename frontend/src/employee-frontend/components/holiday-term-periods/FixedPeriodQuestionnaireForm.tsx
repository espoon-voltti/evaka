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
  FixedPeriodQuestionnaire,
  FixedPeriodQuestionnaireBody
} from 'lib-common/generated/api-types/holidayperiod'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import { SelectionChip } from 'lib-components/atoms/Chip'
import { LegacyAsyncButton } from 'lib-components/atoms/buttons/LegacyAsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import TextArea from 'lib-components/atoms/form/TextArea'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H1, InformationText, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { errorToInputInfo } from '../../utils/validation/input-info-helper'

import {
  createFixedPeriodQuestionnaireMutation,
  updateFixedPeriodQuestionnaireMutation
} from './queries'

interface FormState {
  requiresStrongAuth: boolean
  absenceType: 'FREE_ABSENCE'
  titleFi: string
  titleSv: string
  titleEn: string
  descriptionFi: string
  descriptionSv: string
  descriptionEn: string
  descriptionLinkFi: string
  descriptionLinkSv: string
  descriptionLinkEn: string
  start: LocalDate | null
  end: LocalDate | null
  periodOptions: string
  periodOptionLabelFi: string
  periodOptionLabelSv: string
  periodOptionLabelEn: string
  conditionContinuousPlacementStart: LocalDate | null
  conditionContinuousPlacementEnd: LocalDate | null
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

const formToQuestionnaireBody = (
  s: FormState
): FixedPeriodQuestionnaireBody | undefined => {
  if (!s.start || !s.end) {
    return undefined
  }

  return {
    ...s,
    title: {
      fi: s.titleFi,
      sv: s.titleSv,
      en: s.titleEn
    },
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
    active: new FiniteDateRange(s.start, s.end),
    periodOptionLabel: {
      fi: s.periodOptionLabelFi,
      sv: s.periodOptionLabelSv,
      en: s.periodOptionLabelEn
    },
    periodOptions: parseDateRanges(s.periodOptions),
    conditions: {
      continuousPlacement:
        s.conditionContinuousPlacementStart && s.conditionContinuousPlacementEnd
          ? new FiniteDateRange(
              s.conditionContinuousPlacementStart,
              s.conditionContinuousPlacementEnd
            )
          : null
    }
  }
}

const emptyFormState: FormState = {
  absenceType: 'FREE_ABSENCE',
  requiresStrongAuth: false,
  titleFi: '',
  titleSv: '',
  titleEn: '',
  descriptionFi: '',
  descriptionSv: '',
  descriptionEn: '',
  descriptionLinkFi: '',
  descriptionLinkSv: '',
  descriptionLinkEn: '',
  start: null,
  end: null,
  periodOptions: '',
  periodOptionLabelFi: '',
  periodOptionLabelSv: '',
  periodOptionLabelEn: '',
  conditionContinuousPlacementStart: null,
  conditionContinuousPlacementEnd: null
}

const toFormState = (p: FixedPeriodQuestionnaire | undefined): FormState =>
  p
    ? {
        absenceType: 'FREE_ABSENCE',
        requiresStrongAuth: p.requiresStrongAuth,
        titleFi: p.title.fi,
        titleSv: p.title.sv,
        titleEn: p.title.en,
        descriptionFi: p.description.fi,
        descriptionSv: p.description.sv,
        descriptionEn: p.description.en,
        descriptionLinkFi: p.descriptionLink.fi,
        descriptionLinkSv: p.descriptionLink.sv,
        descriptionLinkEn: p.descriptionLink.en,
        start: p.active.start,
        end: p.active.end,
        periodOptions: p.periodOptions.map((r) => r.format()).join(', '),
        periodOptionLabelFi: p.periodOptionLabel.fi,
        periodOptionLabelSv: p.periodOptionLabel.sv,
        periodOptionLabelEn: p.periodOptionLabel.en,
        conditionContinuousPlacementStart:
          p.conditions.continuousPlacement?.start ?? null,
        conditionContinuousPlacementEnd:
          p.conditions.continuousPlacement?.end ?? null
      }
    : emptyFormState

interface Props {
  onSuccess: () => void
  onCancel: () => void
  questionnaire?: FixedPeriodQuestionnaire
}

export default React.memo(function FixedPeriodQuestionnaireForm({
  onCancel,
  onSuccess,
  questionnaire
}: Props) {
  const { i18n, lang } = useTranslation()

  const [form, setForm] = useState<FormState>(() => toFormState(questionnaire))
  const update = useCallback<UpdateStateFn<FormState>>(
    (p) => setForm((old) => ({ ...old, ...p })),
    []
  )

  const [
    errors,
    isValid,
    parsedStart,
    parsedEnd,
    parsedConditionContinuousPlacementStart,
    parsedConditionContinuousPlacementEnd
  ] = useMemo(() => {
    const parsedStart = form.start
    const parsedEnd = form.end
    const parsedConditionContinuousPlacementStart =
      form.conditionContinuousPlacementStart
    const parsedConditionContinuousPlacementEnd =
      form.conditionContinuousPlacementEnd

    const errors: Record<keyof typeof form, ErrorKey | undefined> = {
      absenceType: undefined,
      requiresStrongAuth: undefined,
      titleFi: validate(form.titleFi, required),
      titleSv: undefined,
      titleEn: undefined,
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
      periodOptionLabelFi: validate(form.periodOptionLabelFi, required),
      periodOptionLabelSv: undefined,
      periodOptionLabelEn: undefined,
      periodOptions:
        parseDateRanges(form.periodOptions).length === 0
          ? 'required'
          : undefined,
      conditionContinuousPlacementStart:
        !form.conditionContinuousPlacementStart &&
        !form.conditionContinuousPlacementEnd
          ? undefined
          : !parsedConditionContinuousPlacementStart
            ? 'validDate'
            : parsedConditionContinuousPlacementEnd &&
                parsedConditionContinuousPlacementStart.isAfter(
                  parsedConditionContinuousPlacementEnd
                )
              ? 'dateTooLate'
              : undefined,
      conditionContinuousPlacementEnd:
        !form.conditionContinuousPlacementStart &&
        !form.conditionContinuousPlacementEnd
          ? undefined
          : !parsedConditionContinuousPlacementEnd
            ? 'validDate'
            : parsedConditionContinuousPlacementStart &&
                parsedConditionContinuousPlacementEnd.isBefore(
                  parsedConditionContinuousPlacementStart
                )
              ? 'dateTooEarly'
              : undefined
    }
    const isValid = Object.values(errors).every((err) => !err)
    return [
      errors,
      isValid,
      parsedStart,
      parsedEnd,
      parsedConditionContinuousPlacementStart,
      parsedConditionContinuousPlacementEnd
    ]
  }, [form])

  const { mutateAsync: createFixedPeriodQuestionnaire } = useMutationResult(
    createFixedPeriodQuestionnaireMutation
  )
  const { mutateAsync: updateFixedPeriodQuestionnaire } = useMutationResult(
    updateFixedPeriodQuestionnaireMutation
  )

  const onSubmit = useCallback(() => {
    const body = isValid && formToQuestionnaireBody(form)
    if (!body) return
    return questionnaire
      ? updateFixedPeriodQuestionnaire({ id: questionnaire.id, body })
      : createFixedPeriodQuestionnaire({ body })
  }, [
    createFixedPeriodQuestionnaire,
    updateFixedPeriodQuestionnaire,
    form,
    questionnaire,
    isValid
  ])

  const hideErrorsBeforeTouched = !questionnaire

  return (
    <>
      <H1>
        {i18n.holidayQuestionnaires.questionnaires}:{' '}
        {i18n.holidayQuestionnaires.types.FIXED_PERIOD}
      </H1>
      <ListGrid>
        <Label inputRow>{i18n.holidayQuestionnaires.absenceType} *</Label>
        <FixedSpaceRow>
          <SelectionChip
            text={i18n.absences.absenceTypes.FREE_ABSENCE}
            selected={form.absenceType === 'FREE_ABSENCE'}
            onChange={(selected) =>
              selected ? update({ absenceType: 'FREE_ABSENCE' }) : null
            }
            showIcon={false}
          />
        </FixedSpaceRow>

        <Label inputRow>{i18n.holidayQuestionnaires.active} *</Label>
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
          />
        </FixedSpaceRow>

        <Label inputRow>{i18n.holidayQuestionnaires.requiresStrongAuth}</Label>
        <FixedSpaceRow>
          <Radio
            label={i18n.common.yes}
            checked={form.requiresStrongAuth}
            onChange={() => update({ requiresStrongAuth: true })}
          />
          <Radio
            label={i18n.common.no}
            checked={!form.requiresStrongAuth}
            onChange={() => update({ requiresStrongAuth: false })}
          />
        </FixedSpaceRow>

        <Label inputRow>{i18n.holidayQuestionnaires.title} *</Label>
        <TextArea
          value={form.titleFi}
          onChange={(titleFi) => update({ titleFi })}
          data-qa="input-title-fi"
          info={useMemo(
            () => errorToInputInfo(errors.titleFi, i18n.validationErrors),
            [errors.titleFi, i18n]
          )}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Label inputRow>{i18n.holidayQuestionnaires.title} SV</Label>
        <TextArea
          value={form.titleSv}
          onChange={(titleSv) => update({ titleSv })}
          data-qa="input-title-sv"
          info={useMemo(
            () => errorToInputInfo(errors.titleSv, i18n.validationErrors),
            [errors.titleSv, i18n]
          )}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Label inputRow>{i18n.holidayQuestionnaires.title} EN</Label>
        <TextArea
          value={form.titleEn}
          onChange={(titleEn) => update({ titleEn })}
          data-qa="input-title-en"
          info={useMemo(
            () => errorToInputInfo(errors.titleEn, i18n.validationErrors),
            [errors.titleEn, i18n]
          )}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Label inputRow>{i18n.holidayQuestionnaires.description} *</Label>
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

        <Label inputRow>{i18n.holidayQuestionnaires.description} SV</Label>
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

        <Label inputRow>{i18n.holidayQuestionnaires.description} EN</Label>
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

        <Label inputRow>{i18n.holidayQuestionnaires.descriptionLink}</Label>
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

        <Label inputRow>{i18n.holidayQuestionnaires.descriptionLink} SV</Label>
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

        <Label inputRow>{i18n.holidayQuestionnaires.descriptionLink} EN</Label>
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

        <Label inputRow>
          {i18n.holidayQuestionnaires.fixedPeriodOptions} *
        </Label>
        <TextArea
          required
          value={form.periodOptions}
          onChange={(fixedPeriodOptions) =>
            update({ periodOptions: fixedPeriodOptions })
          }
          placeholder={i18n.holidayQuestionnaires.fixedPeriodOptionsPlaceholder}
          data-qa="input-fixed-period-options"
          info={errorToInputInfo(errors.periodOptions, i18n.validationErrors)}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Gap horizontal />
        <InformationText>
          {parseDateRanges(form.periodOptions)
            .map((r) => r.formatCompact())
            .join(', ')}
        </InformationText>

        <Label inputRow>
          {i18n.holidayQuestionnaires.fixedPeriodOptionLabel} *
        </Label>
        <TextArea
          required
          value={form.periodOptionLabelFi}
          onChange={(fixedPeriodOptionLabelFi) =>
            update({ periodOptionLabelFi: fixedPeriodOptionLabelFi })
          }
          placeholder={
            i18n.holidayQuestionnaires.fixedPeriodOptionLabelPlaceholder
          }
          data-qa="input-fixed-period-option-label-fi"
          info={errorToInputInfo(
            errors.periodOptionLabelFi,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Label inputRow>
          {i18n.holidayQuestionnaires.fixedPeriodOptionLabel} SV
        </Label>
        <TextArea
          value={form.periodOptionLabelSv}
          onChange={(fixedPeriodOptionLabelSv) =>
            update({ periodOptionLabelSv: fixedPeriodOptionLabelSv })
          }
          data-qa="input-fixed-period-option-label-sv"
          info={errorToInputInfo(
            errors.periodOptionLabelSv,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Label inputRow>
          {i18n.holidayQuestionnaires.fixedPeriodOptionLabel} EN
        </Label>
        <TextArea
          value={form.periodOptionLabelEn}
          onChange={(fixedPeriodOptionLabelEn) =>
            update({ periodOptionLabelEn: fixedPeriodOptionLabelEn })
          }
          data-qa="input-fixed-period-option-label-en"
          info={errorToInputInfo(
            errors.periodOptionLabelEn,
            i18n.validationErrors
          )}
          hideErrorsBeforeTouched={hideErrorsBeforeTouched}
        />

        <Label inputRow>
          {i18n.holidayQuestionnaires.conditionContinuousPlacement}
        </Label>
        <FixedSpaceRow alignItems="center">
          <DatePicker
            info={useMemo(
              () =>
                errorToInputInfo(
                  errors.conditionContinuousPlacementStart,
                  i18n.validationErrors
                ),
              [errors.conditionContinuousPlacementStart, i18n.validationErrors]
            )}
            date={form.conditionContinuousPlacementStart}
            locale={lang}
            required
            maxDate={parsedConditionContinuousPlacementEnd ?? undefined}
            onChange={(conditionContinuousPlacementStart) =>
              update({ conditionContinuousPlacementStart })
            }
            hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            data-qa="continuous-placement-start"
          />
          <span>-</span>
          <DatePicker
            info={useMemo(
              () =>
                errorToInputInfo(
                  errors.conditionContinuousPlacementEnd,
                  i18n.validationErrors
                ),
              [errors.conditionContinuousPlacementEnd, i18n.validationErrors]
            )}
            date={form.conditionContinuousPlacementEnd}
            locale={lang}
            required
            minDate={parsedConditionContinuousPlacementStart ?? undefined}
            onChange={(conditionContinuousPlacementEnd) =>
              update({ conditionContinuousPlacementEnd })
            }
            hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            data-qa="continuous-placement-end"
          />
        </FixedSpaceRow>
      </ListGrid>

      <Gap />
      <ButtonContainer>
        <LegacyAsyncButton
          primary
          disabled={!isValid}
          text={i18n.common.save}
          onClick={onSubmit}
          onSuccess={onSuccess}
          data-qa="save-btn"
        />
        <LegacyButton onClick={onCancel} text={i18n.common.goBack} />
      </ButtonContainer>
    </>
  )
})
