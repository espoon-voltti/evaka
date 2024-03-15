// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faQuestion } from 'Icons'
import React, { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { BoundForm, useFormFields } from 'lib-common/form/hooks'
import {
  CalendarEvent,
  CalendarEventType
} from 'lib-common/generated/api-types/calendarevent'
import { cancelMutation } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Button from 'lib-components/atoms/buttons/Button'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import TreeDropdown, {
  TreeNode,
  hasUncheckedChildren
} from 'lib-components/atoms/dropdowns/TreeDropdown'
import { TextAreaF } from 'lib-components/atoms/form/TextArea'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2, H3, Label } from 'lib-components/typography'

import {
  createCalendarEventMutation,
  updateCalendarEventMutation
} from '../../queries'

import { getPeriodFromDatesOrToday } from './DiscussionTimesForm'
import { surveyForm } from './form'

const SurveyFormFieldGroup = styled(FixedSpaceColumn).attrs({ spacing: 'S' })``
const SurveyFormSectionGroup = styled(FixedSpaceColumn).attrs({ spacing: 'L' })`
  margin-bottom: 60px;
`
export const WidthLimiter = styled.div`
  max-width: 400px;
`
export type DiscussionSurveyEditMode = 'create' | 'reserve'

const getTreeSelectionAsRecord = (tree: TreeNode[]) =>
  Object.fromEntries(
    tree
      ?.filter((group) => group.checked)
      .map((group) => [
        group.key,
        hasUncheckedChildren(group)
          ? group.children
              ?.filter((child) => child.checked)
              .map((child) => child.key) ?? []
          : null
      ]) ?? []
  )

export default React.memo(function DiscussionSurveyForm({
  form,
  eventData,
  groupId,
  unitId
}: {
  form: BoundForm<typeof surveyForm>
  eventData: CalendarEvent | null
  groupId: UUID
  unitId: UUID
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events

  const [cancelConfirmModalVisible, setCancelConfirmModalVisible] =
    useState(false)

  const navigate = useNavigate()

  const { attendees } = useFormFields(form)

  const { times, title, description } = useFormFields(form)

  const basicInfoValidationErrors = useMemo(
    () => ({
      title:
        title.state && title.state.length > 0
          ? undefined
          : ('required' as const),
      description:
        description.state && description.state.trim().length > 0
          ? undefined
          : ('required' as const),
      attendees:
        attendees.state.filter((a) => a.checked).length > 0
          ? undefined
          : ('required' as const)
    }),
    [attendees.state, title.state, description.state]
  )

  const timesValidationErrors = useMemo(
    () => ({
      times:
        times.state && times.state.length > 0 && times.isValid()
          ? undefined
          : ('required' as const)
    }),
    [times]
  )

  const isBasicInfoValid = useMemo(
    () =>
      Object.values(basicInfoValidationErrors).every((e) => e === undefined),
    [basicInfoValidationErrors]
  )

  const isFullyValid = useMemo(
    () =>
      isBasicInfoValid &&
      Object.values(timesValidationErrors).every((e) => e === undefined),
    [isBasicInfoValid, timesValidationErrors]
  )

  const info = useCallback(
    (key: keyof typeof basicInfoValidationErrors) => {
      const error = basicInfoValidationErrors[key]
      if (!error) return undefined

      return {
        status: 'warning' as const,
        text: i18n.validationErrors[error]
      }
    },
    [basicInfoValidationErrors, i18n]
  )

  return (
    <>
      {cancelConfirmModalVisible && (
        <InfoModal
          type="warning"
          title={t.discussionReservation.cancelConfirmation.title}
          icon={faQuestion}
          reject={{
            action: () => setCancelConfirmModalVisible(false),
            label: t.discussionReservation.cancelConfirmation.cancelButton
          }}
          resolve={{
            action: () => {
              setCancelConfirmModalVisible(false)
              if (eventData) {
                navigate(
                  `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys/${eventData.id}`,
                  { replace: true }
                )
              } else {
                navigate(
                  `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys`,
                  { replace: true }
                )
              }
            },
            label: t.discussionReservation.cancelConfirmation.continueButton
          }}
          text={t.discussionReservation.cancelConfirmation.text}
        />
      )}

      {eventData ? (
        <H2>{title.state}</H2>
      ) : (
        <H2>{t.discussionReservation.surveyCreate}</H2>
      )}
      <H3>{t.discussionReservation.surveyBasicsTitle}</H3>

      <SurveyFormSectionGroup>
        <SurveyFormFieldGroup>
          <Label>{t.discussionReservation.surveySubject}</Label>
          <WidthLimiter>
            <TextAreaF
              bind={title}
              info={info('title')}
              placeholder={t.discussionReservation.surveySubjectPlaceholder}
              maxLength={30}
            />
          </WidthLimiter>
        </SurveyFormFieldGroup>

        <SurveyFormFieldGroup>
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
        </SurveyFormFieldGroup>
      </SurveyFormSectionGroup>
      <SurveyFormSectionGroup>
        <H3>{t.discussionReservation.surveyInviteeTitle}</H3>
        <SurveyFormFieldGroup>
          <WidthLimiter>
            <Label>{t.discussionReservation.surveyInvitees}</Label>
            <TreeDropdown
              tree={attendees.state}
              onChange={attendees.set}
              data-qa="attendees-select"
              placeholder={
                i18n.unit.calendar.events.create.attendeesPlaceholder
              }
            />
          </WidthLimiter>
        </SurveyFormFieldGroup>
      </SurveyFormSectionGroup>

      <FixedSpaceRow justifyContent="flex-start" alignItems="center">
        <Button
          text={t.discussionReservation.cancelButton}
          onClick={() => setCancelConfirmModalVisible(true)}
          data-qa="cancel-button"
        />
        {eventData ? (
          <MutateButton
            primary
            mutation={updateCalendarEventMutation}
            text={t.discussionReservation.saveSurveyButton}
            disabled={!isBasicInfoValid}
            onSuccess={() =>
              navigate(
                `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys/${eventData.id}`
              )
            }
            onClick={() => {
              const values = form.value()
              const attendeeValue = attendees.value()
              const baseFormValues = {
                ...values,
                unitId,
                period: getPeriodFromDatesOrToday(
                  values.times.map((t) => t.date)
                ),
                tree: getTreeSelectionAsRecord(attendeeValue)
              }

              return isBasicInfoValid
                ? {
                    id: eventData.id,
                    body: {
                      ...baseFormValues,
                      times: null
                    }
                  }
                : cancelMutation
            }}
            data-qa="save-button"
          />
        ) : (
          <MutateButton
            primary
            mutation={createCalendarEventMutation}
            text={t.discussionReservation.createSurveyButton}
            disabled={!isFullyValid}
            onSuccess={(newId) =>
              navigate(
                `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys/${newId}`
              )
            }
            onClick={() => {
              const values = form.value()
              const attendeeValue = attendees.value()
              const eventType: CalendarEventType = 'DISCUSSION_SURVEY'
              const baseFormValues = {
                ...values,
                unitId,
                period: getPeriodFromDatesOrToday(
                  values.times.map((t) => t.date)
                ),
                tree: getTreeSelectionAsRecord(attendeeValue),
                eventType
              }

              return isFullyValid
                ? {
                    body: {
                      ...baseFormValues,
                      times: times.value()
                    }
                  }
                : cancelMutation
            }}
            data-qa="save-button"
          />
        )}
      </FixedSpaceRow>
    </>
  )
})
