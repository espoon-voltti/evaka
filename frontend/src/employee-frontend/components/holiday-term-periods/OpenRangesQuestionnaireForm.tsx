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
  HolidayQuestionnaire,
  QuestionnaireBody
} from 'lib-common/generated/api-types/holidayperiod'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import { SelectionChip } from 'lib-components/atoms/Chip'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import TextArea from 'lib-components/atoms/form/TextArea'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { H1, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { errorToInputInfo } from '../../utils/validation/input-info-helper'

import {
  createQuestionnaireMutation,
  updateQuestionnaireMutation
} from './queries'

interface FormState {
  type: 'OPEN_RANGES'
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
  periodStart: LocalDate | null
  periodEnd: LocalDate | null
  absenceTypeThreshold: number
  conditionContinuousPlacementStart: LocalDate | null
  conditionContinuousPlacementEnd: LocalDate | null
}

const formToQuestionnaireBody = (
  s: FormState
  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
): QuestionnaireBody.OpenRangesQuestionnaireBody | undefined => {
  if (!s.start || !s.end || !s.periodStart || !s.periodEnd) {
    return undefined
  }

  return {
    ...s,
    type: 'OPEN_RANGES',
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
    period: new FiniteDateRange(s.periodStart, s.periodEnd),
    absenceTypeThreshold: s.absenceTypeThreshold,
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
  type: 'OPEN_RANGES',
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
  periodStart: null,
  periodEnd: null,
  absenceTypeThreshold: 0,
  conditionContinuousPlacementStart: null,
  conditionContinuousPlacementEnd: null
}

const toFormState = (
  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
  p: HolidayQuestionnaire.OpenRangesQuestionnaire | undefined
): FormState =>
  p
    ? {
        type: 'OPEN_RANGES',
        absenceType: 'FREE_ABSENCE',
        requiresStrongAuth: p.requiresStrongAuth, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        titleFi: p.title.fi, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        titleSv: p.title.sv, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        titleEn: p.title.en, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        descriptionFi: p.description.fi, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        descriptionSv: p.description.sv, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        descriptionEn: p.description.en, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        descriptionLinkFi: p.descriptionLink.fi, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        descriptionLinkSv: p.descriptionLink.sv, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        descriptionLinkEn: p.descriptionLink.en, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        start: p.active.start, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        end: p.active.end, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        periodStart: p.period.start, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        periodEnd: p.period.end, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        absenceTypeThreshold: p.absenceTypeThreshold, // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
        // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
        conditionContinuousPlacementStart:
          // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
          p.conditions.continuousPlacement?.start ?? null,
        // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
        conditionContinuousPlacementEnd:
          // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
          p.conditions.continuousPlacement?.end ?? null
      }
    : emptyFormState

interface Props {
  onSuccess: () => void
  onCancel: () => void
  questionnaire: HolidayQuestionnaire.OpenRangesQuestionnaire
}

export default React.memo(function OpenRangesQuestionnaireForm({
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
    parsedPeriodStart,
    parsedPeriodEnd,
    parsedConditionContinuousPlacementStart,
    parsedConditionContinuousPlacementEnd
  ] = useMemo(() => {
    const parsedStart = form.start
    const parsedEnd = form.end
    const parsedPeriodStart = form.periodStart
    const parsedPeriodEnd = form.periodEnd
    const parsedConditionContinuousPlacementStart =
      form.conditionContinuousPlacementStart
    const parsedConditionContinuousPlacementEnd =
      form.conditionContinuousPlacementEnd

    const errors: Record<keyof typeof form, ErrorKey | undefined> = {
      type: undefined,
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
      periodStart: !parsedPeriodStart
        ? 'validDate'
        : parsedPeriodEnd && parsedPeriodStart.isAfter(parsedPeriodEnd)
          ? 'dateTooLate'
          : undefined,
      periodEnd: !parsedPeriodEnd
        ? 'validDate'
        : parsedPeriodStart && parsedPeriodEnd.isBefore(parsedPeriodStart)
          ? 'dateTooEarly'
          : undefined,
      absenceTypeThreshold: validate(form.absenceTypeThreshold, required),
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
      parsedPeriodStart,
      parsedPeriodEnd,
      parsedConditionContinuousPlacementStart,
      parsedConditionContinuousPlacementEnd
    ]
  }, [form])

  const { mutateAsync: createOpenRangesQuestionnaire } = useMutationResult(
    createQuestionnaireMutation
  )

  const { mutateAsync: updateOpenRangesQuestionnaire } = useMutationResult(
    updateQuestionnaireMutation
  )

  const onSubmit = useCallback(() => {
    // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
    const body = isValid && formToQuestionnaireBody(form)
    if (!body) return
    return questionnaire
      ? updateOpenRangesQuestionnaire({ id: questionnaire.id, body }) // eslint-disable-line @typescript-eslint/no-unsafe-assignment,@typescript-eslint/no-unsafe-member-access
      : createOpenRangesQuestionnaire({ body }) // eslint-disable-line @typescript-eslint/no-unsafe-assignment
  }, [
    createOpenRangesQuestionnaire,
    updateOpenRangesQuestionnaire,
    form,
    questionnaire,
    isValid
  ])

  const hideErrorsBeforeTouched = !questionnaire

  return (
    <>
      <H1>
        {i18n.holidayQuestionnaires.questionnaires}:{' '}
        {i18n.holidayQuestionnaires.types.OPEN_RANGES}
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
            hideIcon
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

        {/*todo: juttuja*/}
        <Label inputRow>{i18n.holidayQuestionnaires.period}</Label>
        <FixedSpaceRow alignItems="center">
          <DatePicker
            info={useMemo(
              () => errorToInputInfo(errors.periodStart, i18n.validationErrors),
              [errors.periodStart, i18n.validationErrors]
            )}
            date={form.periodStart}
            locale={lang}
            required
            maxDate={parsedPeriodEnd ?? undefined}
            onChange={(periodStart) => update({ periodStart })}
            hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            data-qa="period-start"
          />
          <span>-</span>
          <DatePicker
            info={useMemo(
              () => errorToInputInfo(errors.periodEnd, i18n.validationErrors),
              [errors.periodEnd, i18n.validationErrors]
            )}
            date={form.periodEnd}
            locale={lang}
            required
            minDate={parsedPeriodStart ?? undefined}
            onChange={(periodEnd) => update({ periodEnd })}
            hideErrorsBeforeTouched={hideErrorsBeforeTouched}
            data-qa="period-end"
          />
        </FixedSpaceRow>

        <Label inputRow>
          {i18n.holidayQuestionnaires.absenceTypeThreshold}
        </Label>
        <InputField
          width="L"
          placeholder="0"
          type="number"
          value={form.absenceTypeThreshold.toString()}
          onChange={(absenceTypeThreshold) =>
            update({ absenceTypeThreshold: parseInt(absenceTypeThreshold, 10) })
          }
          data-qa="input-absence-type-threshold"
          info={useMemo(
            () =>
              errorToInputInfo(
                errors.absenceTypeThreshold,
                i18n.validationErrors
              ),
            [errors.absenceTypeThreshold, i18n]
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
        <AsyncButton
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
