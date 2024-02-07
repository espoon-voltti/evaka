// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { BoundForm, useFormFields } from 'lib-common/form/hooks'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import { TextAreaF } from 'lib-components/atoms/form/TextArea'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { H2, H3, Label } from 'lib-components/typography'

import { WidthLimiter, basicInfoForm } from './DiscussionSurveyEditor'

const FormFieldGroup = styled(FixedSpaceColumn).attrs({ spacing: 'S' })``
const FormSectionGroup = styled(FixedSpaceColumn).attrs({ spacing: 'L' })`
  margin-bottom: 60px;
`

export default React.memo(function BasicInfoSection({
  basicInfo,
  eventData
}: {
  basicInfo: BoundForm<typeof basicInfoForm>
  eventData: CalendarEvent | null
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events
  const { title, description } = useFormFields(basicInfo)

  const validationErrors = useMemo(
    () => ({
      title:
        basicInfo.state.title && basicInfo.state.title.trim().length > 0
          ? undefined
          : ('required' as const),
      description:
        basicInfo.state.description &&
        basicInfo.state.description.trim().length > 0
          ? undefined
          : ('required' as const)
    }),
    [basicInfo.state.title, basicInfo.state.description]
  )

  const info = useCallback(
    (key: keyof typeof validationErrors) => {
      const error = validationErrors[key]
      if (!error) return undefined

      return {
        status: 'warning' as const,
        text: i18n.validationErrors[error]
      }
    },
    [validationErrors, i18n]
  )

  return (
    <>
      {eventData ? (
        <H2>{title.state}</H2>
      ) : (
        <H2>{t.discussionReservation.surveyCreate}</H2>
      )}
      <H3>{t.discussionReservation.surveyBasicsTitle}</H3>

      <FormSectionGroup>
        <FormFieldGroup>
          <Label>{t.discussionReservation.surveySubject}</Label>
          <WidthLimiter>
            <TextAreaF
              bind={title}
              info={info('title')}
              placeholder={t.discussionReservation.surveySubjectPlaceholder}
              maxLength={30}
            />
          </WidthLimiter>
        </FormFieldGroup>

        <FormFieldGroup>
          <ExpandingInfo info={t.discussionReservation.surveySummaryInfo}>
            <Label>{t.discussionReservation.surveySummary}</Label>
          </ExpandingInfo>
          <WidthLimiter>
            <TextAreaF
              bind={description}
              info={info('description')}
              placeholder={t.discussionReservation.surveySummaryPlaceholder}
            />
          </WidthLimiter>
        </FormFieldGroup>
      </FormSectionGroup>
    </>
  )
})
