// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ReactNode } from 'react'
import React from 'react'
import { useLocation } from 'wouter'

import type FiniteDateRange from 'lib-common/finite-date-range'
import { boolean, localDateRange, string } from 'lib-common/form/fields'
import {
  mapped,
  object,
  required,
  transformed,
  validated,
  value
} from 'lib-common/form/form'
import { useForm, useFormField, useFormFields } from 'lib-common/form/hooks'
import { ValidationError, ValidationSuccess } from 'lib-common/form/types'
import { nonBlank } from 'lib-common/form/validators'
import type { ChildId, PersonId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import Main from 'lib-components/atoms/Main'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { H1, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import Footer from '../../../Footer'
import { useLang, useTranslation } from '../../../localization'

import { postAbsenceApplicationMutation } from './queries'

const validatedAbsenceDateRange = () =>
  transformed(
    object({
      range: localDateRange(),
      allowedDateRanges: value<FiniteDateRange[]>()
    }),
    (output) => {
      if (output.range === undefined) return ValidationSuccess.of(undefined)
      if (
        !output.allowedDateRanges.some(
          (dateRange) =>
            dateRange.includes(output.range!.start) ||
            dateRange.includes(output.range!.end)
        )
      )
        return ValidationError.of('invalidDateRange')
      return ValidationSuccess.of(output.range)
    }
  )

const formModel = mapped(
  object({
    childId: required(value<ChildId>()),
    range: required(validatedAbsenceDateRange()),
    description: validated(string(), nonBlank),
    confirmation: validated(boolean(), (value) =>
      !value ? 'required' : undefined
    )
  }),
  (output) => ({
    childId: output.childId,
    startDate: output.range.start,
    endDate: output.range.end,
    description: output.description
  })
)

export const NewAbsenceApplicationForm = ({
  childId,
  absenceApplicationDateRanges
}: {
  childId: PersonId
  absenceApplicationDateRanges: FiniteDateRange[]
}): ReactNode => {
  const i18n = useTranslation()
  const [lang] = useLang()
  const form = useForm(
    formModel,
    () => ({
      childId,
      range: {
        range: localDateRange.fromRange(null, {
          minDate: LocalDate.todayInHelsinkiTz()
        }),
        allowedDateRanges: absenceApplicationDateRanges
      },
      description: '',
      confirmation: false
    }),
    {
      ...i18n.validationErrors,
      ...i18n.children.absenceApplication.newPage.validationErrors
    }
  )
  const { range, description, confirmation } = useFormFields(form)
  const innerRange = useFormField(range, 'range')
  const [, navigate] = useLocation()

  return (
    <>
      <Main>
        <Container>
          <ReturnButton label={i18n.common.return} />
          <ContentArea opaque>
            <H1 noMargin>{i18n.children.absenceApplication.newPage.title}</H1>
            <Gap />
            <FixedSpaceColumn>
              <div>
                <Label>{i18n.children.absenceApplication.newPage.range}</Label>
                <DateRangePickerF
                  bind={innerRange}
                  locale={lang}
                  hideErrorsBeforeTouched
                  info={range.inputInfo()}
                />
                <Label>{i18n.children.absenceApplication.description}</Label>
                <InputFieldF
                  bind={description}
                  hideErrorsBeforeTouched
                  data-qa="description"
                />
              </div>
              <div>{i18n.children.absenceApplication.newPage.info}</div>
              <CheckboxF
                bind={confirmation}
                label={i18n.children.absenceApplication.newPage.confirmation}
                data-qa="confirmation"
              />
              <FixedSpaceRow justifyContent="space-between">
                <Button
                  text={i18n.common.cancel}
                  appearance="button"
                  onClick={() => navigate(`/children/${childId}`)}
                />
                <MutateButton
                  text={i18n.children.serviceApplication.send}
                  mutation={postAbsenceApplicationMutation}
                  appearance="button"
                  primary
                  disabled={!form.isValid()}
                  onClick={() => ({
                    body: form.value()
                  })}
                  onSuccess={() => navigate(`/children/${childId}`)}
                  data-qa="create-button"
                />
              </FixedSpaceRow>
            </FixedSpaceColumn>
          </ContentArea>
        </Container>
      </Main>
      <Footer />
    </>
  )
}
