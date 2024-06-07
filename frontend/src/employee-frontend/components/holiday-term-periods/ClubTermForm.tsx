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
  ClubTerm,
  ClubTermRequest
} from 'lib-common/generated/api-types/daycare'
import { useMutationResult } from 'lib-common/query'
import UnderRowStatusIcon from 'lib-components/atoms/StatusIcon'
import { Button } from 'lib-components/atoms/buttons/Button'
import AsyncButton from 'lib-components/atoms/buttons/LegacyAsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { InputFieldUnderRow } from 'lib-components/atoms/form/InputField'
import ButtonContainer from 'lib-components/layout/ButtonContainer'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { DatePickerF } from 'lib-components/molecules/date-picker/DatePicker'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { H1, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'

import { createClubTermMutation, updateClubTermMutation } from './queries'

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

const clubTermForm = transformed(
  object({
    term: required(localDateRange()),
    applicationPeriodStart: required(localDate()),
    termBreaks: array(required(localDateRange())),
    allTerms: value<ClubTerm[]>()
  }),
  ({ term, applicationPeriodStart, termBreaks, allTerms }) => {
    if (allTerms.some((clubTerm) => clubTerm.term.overlaps(term))) {
      return ValidationError.field('term', 'overlap')
    }

    if (termBreaks.length > 0) {
      if (checkForOverlappingRanges(termBreaks)) {
        return ValidationError.field('termBreaks', 'termBreaksOverlap')
      }
    }

    const success: ClubTermRequest = {
      term: term,
      applicationPeriod: new FiniteDateRange(applicationPeriodStart, term.end),
      termBreaks: termBreaks
    }
    return ValidationSuccess.of(success)
  }
)

function initialFormState(
  allTerms: ClubTerm[],
  clubTerm?: ClubTerm
): StateOf<typeof clubTermForm> {
  return {
    term: clubTerm
      ? localDateRange.fromRange(clubTerm.term)
      : localDateRange.empty(),
    applicationPeriodStart: clubTerm
      ? localDate.fromDate(clubTerm.applicationPeriod.start)
      : localDate.empty(),
    termBreaks: clubTerm
      ? clubTerm.termBreaks.map((tb) => localDateRange.fromRange(tb))
      : [],
    allTerms: clubTerm
      ? allTerms.filter((existing) => existing.id !== clubTerm.id)
      : allTerms
  }
}

interface Props {
  clubTerm?: ClubTerm
  allTerms: ClubTerm[]
  onSuccess: () => void
  onCancel: () => void
}

export default React.memo(function ClubTermForm({
  clubTerm,
  allTerms,
  onCancel,
  onSuccess
}: Props) {
  const { i18n, lang } = useTranslation()

  const form = useForm(
    clubTermForm,
    () => initialFormState(allTerms, clubTerm),
    {
      ...i18n.validationErrors,
      ...i18n.terms.validationErrors
    }
  )

  const { mutateAsync: createClubTerm } = useMutationResult(
    createClubTermMutation
  )

  const { mutateAsync: updateClubTerm } = useMutationResult(
    updateClubTermMutation
  )

  const onSubmit = useCallback(
    () =>
      clubTerm !== undefined
        ? updateClubTerm({ id: clubTerm.id, body: form.value() })
        : createClubTerm({ body: form.value() }),
    [clubTerm, form, createClubTerm, updateClubTerm]
  )

  const { term, applicationPeriodStart, termBreaks } = useFormFields(form)

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
      <H1>{i18n.titles.clubTerm}</H1>
      <ListGrid labelWidth="max-content" rowGap="s" columnGap="L">
        <div>
          <FixedSpaceRow>
            <Label>{i18n.terms.term} *</Label>
          </FixedSpaceRow>
          <DateRangePickerF
            hideErrorsBeforeTouched
            bind={term}
            locale={lang}
            data-qa="term"
            info={term.inputInfo()}
          />
        </div>

        <Gap size="L" />

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

              <Button
                appearance="inline"
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

          <Button
            appearance="inline"
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
        <LegacyButton onClick={onCancel} text={i18n.common.goBack} />
      </ButtonContainer>
    </>
  )
})
