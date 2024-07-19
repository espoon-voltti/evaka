// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
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
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
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
import { faQuestion } from 'lib-icons'

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

  const { title, description, attendees } = useFormFields(form)

  const isBasicInfoValid = useMemo(
    () => title.isValid() && description.isValid() && attendees.isValid(),
    [title, attendees, description]
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
              info={title.inputInfo()}
              placeholder={t.discussionReservation.surveySubjectPlaceholder}
              maxLength={30}
              data-qa="survey-title-input"
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
              info={description.inputInfo()}
              placeholder={t.discussionReservation.surveySummaryPlaceholder}
              data-qa="survey-description-input"
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
              data-qa="survey-attendees-select"
              placeholder={
                i18n.unit.calendar.events.create.attendeesPlaceholder
              }
            />
          </WidthLimiter>
        </SurveyFormFieldGroup>
      </SurveyFormSectionGroup>

      <FixedSpaceRow justifyContent="flex-start" alignItems="center">
        <LegacyButton
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
            onClick={() =>
              isBasicInfoValid
                ? {
                    id: eventData.id,
                    body: {
                      description: description.value(),
                      title: title.value(),
                      times: null,
                      unitId,
                      period: eventData.period,
                      tree: getTreeSelectionAsRecord(attendees.value())
                    }
                  }
                : cancelMutation
            }
            data-qa="survey-editor-submit-button"
          />
        ) : (
          <MutateButton
            primary
            mutation={createCalendarEventMutation}
            text={t.discussionReservation.createSurveyButton}
            disabled={!form.isValid()}
            onSuccess={(newId) =>
              navigate(
                `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys/${newId}`
              )
            }
            onClick={() => {
              if (form.isValid()) {
                const values = form.value()
                return {
                  body: {
                    title: values.title,
                    description: values.description,
                    times: values.times,
                    eventType: 'DISCUSSION_SURVEY' as CalendarEventType,
                    tree: getTreeSelectionAsRecord(values.attendees),
                    period: getPeriodFromDatesOrToday(
                      values.times.map((t) => t.date)
                    ),
                    unitId
                  }
                }
              } else return cancelMutation
            }}
            data-qa="survey-editor-submit-button"
          />
        )}
      </FixedSpaceRow>
    </>
  )
})
