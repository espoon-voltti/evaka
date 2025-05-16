// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate } from 'react-router'

import { boolean, localDateRange, string } from 'lib-common/form/fields'
import {
  mapped,
  object,
  required,
  validated,
  value
} from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import { ChildId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useIdRouteParam } from 'lib-common/useRouteParams'
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

const formModel = mapped(
  object({
    childId: required(value<ChildId>()),
    range: required(localDateRange()),
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

export const NewAbsenceApplicationPage = () => {
  const childId = useIdRouteParam<ChildId>('childId')
  const i18n = useTranslation()
  const [lang] = useLang()
  const form = useForm(
    formModel,
    () => ({
      childId,
      range: localDateRange.fromRange(null, {
        minDate: LocalDate.todayInHelsinkiTz()
      }),
      description: '',
      confirmation: false
    }),
    i18n.validationErrors
  )
  const { range, description, confirmation } = useFormFields(form)
  const navigate = useNavigate()

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
                  bind={range}
                  locale={lang}
                  hideErrorsBeforeTouched
                />
              </div>
              <div>
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
