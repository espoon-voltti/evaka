// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faPlus, faTrash } from 'Icons'
import React, { useCallback } from 'react'

import FiniteDateRange from 'lib-common/finite-date-range'
import { localDate, localDateRange } from 'lib-common/form/fields'
import {
  array,
  object,
  required,
  transformed,
  value
} from 'lib-common/form/form'
import { useForm, useFormElems, useFormFields } from 'lib-common/form/hooks'
import {
  StateOf,
  ValidationError,
  ValidationSuccess
} from 'lib-common/form/types'
import {
  PreschoolTerm,
  PreschoolTermRequest
} from 'lib-common/generated/api-types/daycare'
import { useMutationResult } from 'lib-common/query'
import UnderRowStatusIcon from 'lib-components/atoms/StatusIcon'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { H1, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employeeMobile'

import { useTranslation } from '../../state/i18n'

import {
  createPreschoolTermMutation,
  updatePreschoolTermMutation
} from './queries'

function checkForOverlappingRanges(ranges: FiniteDateRange[]): boolean {
  const rangesProcessed: FiniteDateRange[] = []

  for (const value of ranges) {
    if (rangesProcessed.some((range) => value.overlaps(range))) {
      return true
    }
    rangesProcessed.push(value)
  }
  return false
}

const preschoolTermForm = transformed(
  object({
    finnishPreschool: required(localDateRange()),
    extendedTermStart: localDate(),
    applicationPeriodStart: required(localDate()),
    termBreaks: array(required(localDateRange())),
    allTerms: value<PreschoolTerm[]>()
  }),
  ({
    finnishPreschool,
    extendedTermStart,
    applicationPeriodStart,
    termBreaks,
    allTerms
  }) => {
    if (
      allTerms.some((term) => term.finnishPreschool.overlaps(finnishPreschool))
    ) {
      return ValidationError.field('finnishPreschool', 'overlap')
    }

    if (featureFlags.extendedPreschoolTerm) {
      if (!extendedTermStart) {
        return ValidationError.field('extendedTermStart', 'required')
      }
      if (extendedTermStart.isAfter(finnishPreschool.start)) {
        return ValidationError.field(
          'extendedTermStart',
          'extendedTermStartAfter'
        )
      }
    }

    const extendedTerm = new FiniteDateRange(
      featureFlags.extendedPreschoolTerm && extendedTermStart
        ? extendedTermStart
        : finnishPreschool.start,
      finnishPreschool.end
    )

    if (allTerms.some((term) => term.extendedTerm.overlaps(extendedTerm))) {
      return ValidationError.field('extendedTermStart', 'extendedTermOverlap')
    }

    if (termBreaks.length > 0) {
      if (checkForOverlappingRanges(termBreaks)) {
        return ValidationError.field('termBreaks', 'termBreaksOverlap')
      }
    }

    const success: PreschoolTermRequest = {
      finnishPreschool: finnishPreschool,
      swedishPreschool: finnishPreschool,
      applicationPeriod: new FiniteDateRange(
        applicationPeriodStart,
        finnishPreschool.end
      ),
      extendedTerm: extendedTerm,
      termBreaks: termBreaks
    }
    return ValidationSuccess.of(success)
  }
)

function initialFormState(
  allTerms: PreschoolTerm[],
  term?: PreschoolTerm
): StateOf<typeof preschoolTermForm> {
  return {
    finnishPreschool: term
      ? localDateRange.fromRange(term.finnishPreschool)
      : localDateRange.empty(),
    applicationPeriodStart: term
      ? localDate.fromDate(term.applicationPeriod.start)
      : localDate.empty(),
    extendedTermStart: term
      ? localDate.fromDate(term.extendedTerm.start)
      : localDate.empty(),
    termBreaks: term
      ? term.termBreaks.map((tb) => localDateRange.fromRange(tb))
      : [],
    allTerms: term
      ? allTerms.filter((existing) => existing.id !== term.id)
      : allTerms
  }
}

interface Props {
  term?: PreschoolTerm
  allTerms: PreschoolTerm[]
  onSuccess: () => void
  onCancel: () => void
}

export default React.memo(function PreschoolTermForm({
  term,
  allTerms,
  onCancel,
  onSuccess
}: Props) {
  const { i18n, lang } = useTranslation()

  const form = useForm(
    preschoolTermForm,
    () => initialFormState(allTerms, term),
    {
      ...i18n.validationErrors,
      ...i18n.terms.validationErrors
    }
  )

  const { mutateAsync: createPreschoolTerm } = useMutationResult(
    createPreschoolTermMutation
  )

  const { mutateAsync: updatePreschoolTerm } = useMutationResult(
    updatePreschoolTermMutation
  )

  const onSubmit = useCallback(
    () =>
      term !== undefined
        ? updatePreschoolTerm({ id: term.id, body: form.value() })
        : createPreschoolTerm({ body: form.value() }),
    [term, form, createPreschoolTerm, updatePreschoolTerm]
  )

  const {
    finnishPreschool,
    extendedTermStart,
    applicationPeriodStart,
    termBreaks
  } = useFormFields(form)

  const termBreakElems = useFormElems(termBreaks)

  const addTermBreakEntry = useCallback(() => {
    termBreaks.update((prev) => [...prev, localDateRange.empty()])
  }, [termBreaks])

  const removeTermBreakEntry = useCallback(
    (index: number) => {
      termBreaks.update((prev) => [
        ...prev.slice(0, index),
        ...prev.slice(index + 1)
      ])
    },
    [termBreaks]
  )

  return (
    <>
      <H1>{i18n.titles.preschoolTerm}</H1>
      <ListGrid labelWidth="max-content" rowGap="s" columnGap="L">
        <div>
          <FixedSpaceRow>
            <Label>{i18n.terms.finnishPreschool} *</Label>
          </FixedSpaceRow>
          <DateRangePickerF
            hideErrorsBeforeTouched
            bind={finnishPreschool}
            locale={lang}
            data-qa="finnish-preschool"
            info={finnishPreschool.inputInfo()}
          />
        </div>

        <Gap size="L" />

        {featureFlags.extendedPreschoolTerm && (
          <>
            <div>
              <ExpandingInfo info={i18n.terms.extendedTermStartInfo}>
                <Label>{i18n.terms.extendedTermStart} *</Label>
              </ExpandingInfo>
              <DatePickerF
                hideErrorsBeforeTouched
                bind={extendedTermStart}
                locale={lang}
                data-qa="input-extended-term-start"
                info={extendedTermStart.inputInfo()}
              />
            </div>
            <Gap size="L" />
          </>
        )}

        <div>
          <FixedSpaceRow>
            <Label>{i18n.terms.applicationPeriodStart} *</Label>
          </FixedSpaceRow>
          <DatePickerF
            hideErrorsBeforeTouched
            bind={applicationPeriodStart}
            locale={lang}
            data-qa="input-application-period-start"
          />
        </div>

        <Gap size="L" />

        <div>
          <ExpandingInfo info={i18n.terms.termBreaksInfo}>
            <Label>{i18n.terms.termBreaks}</Label>
          </ExpandingInfo>

          {termBreakElems.map((range, i) => (
            <FixedSpaceRow
              key={`tb-${i}`}
              data-qa="term-break"
              alignItems="center"
              spacing="m"
            >
              <div className="bold">{`${i + 1}.`}</div>
              <DateRangePickerF
                hideErrorsBeforeTouched
                bind={range}
                locale={lang}
                data-qa="term-break-input"
              />

              <InlineButton
                text={i18n.common.remove}
                icon={faTrash}
                data-qa="remove-term-break-button"
                onClick={() => removeTermBreakEntry(i)}
              />
            </FixedSpaceRow>
          ))}

          {!termBreaks.isValid() && termBreaks.inputInfo() ? (
            <InputFieldUnderRow className="warning">
              <span data-qa="remove-term-break-info">
                {termBreaks.inputInfo()?.text}
              </span>
              <UnderRowStatusIcon status="warning" />
            </InputFieldUnderRow>
          ) : undefined}

          <Gap size="L" />

          <InlineButton
            text={i18n.terms.addTermBreak}
            icon={faPlus}
            data-qa="add-term-break-button"
            onClick={addTermBreakEntry}
          />
        </div>
      </ListGrid>

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
