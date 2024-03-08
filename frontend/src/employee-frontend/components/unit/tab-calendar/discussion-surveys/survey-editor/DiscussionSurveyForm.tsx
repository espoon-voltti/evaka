// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faQuestion } from 'Icons'
import orderBy from 'lodash/orderBy'
import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import FiniteDateRange from 'lib-common/finite-date-range'
import { mapped } from 'lib-common/form/form'
import {
  BoundForm,
  useForm,
  useFormElems,
  useFormFields
} from 'lib-common/form/hooks'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
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

import { TreeNodeInfo } from './DiscussionTimesForm'
import { surveyForm, timesForm } from './form'

const SurveyFormFieldGroup = styled(FixedSpaceColumn).attrs({ spacing: 'S' })``
const SurveyFormSectionGroup = styled(FixedSpaceColumn).attrs({ spacing: 'L' })`
  margin-bottom: 60px;
`
export const WidthLimiter = styled.div`
  max-width: 400px;
`
export type DiscussionSurveyEditMode = 'create' | 'reserve'

export default React.memo(function DiscussionSurveyForm({
  timesBind,
  attendeeNodes,
  eventData,
  groupId,
  unitId,
  period
}: {
  timesBind: BoundForm<typeof timesForm>
  attendeeNodes: TreeNodeInfo[]
  eventData: CalendarEvent | null
  groupId: UUID
  unitId: UUID
  period: FiniteDateRange
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events

  const [cancelConfirmModalVisible, setCancelConfirmModalVisible] =
    useState(false)

  const navigate = useNavigate()

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

  const mappedForm = mapped(surveyForm, (output) => ({
    ...output,
    tree: getTreeSelectionAsRecord(output.attendees),
    unitId: unitId
  }))

  const initializedForm = useForm(
    mappedForm,
    () => ({
      title: eventData?.title ?? '',
      description: eventData?.description ?? '',
      attendees: attendeeNodes
    }),
    i18n.validationErrors
  )

  const { title, description, attendees } = useFormFields(initializedForm)
  const { times } = useFormFields(timesBind)
  const timeElems = useFormElems(times)

  useEffect(
    () =>
      attendees.update((prev) => {
        const previousGroupNode = prev[0]
        const newGroupNode = attendeeNodes[0]
        const previousChildren = previousGroupNode?.children ?? []

        if (
          previousChildren.length > 0 &&
          previousChildren.every((pc) => pc.checked)
        ) {
          return attendeeNodes
        } else {
          const newChildren = newGroupNode?.children ?? []
          const addedChildren = newChildren
            .filter((ni) => !previousChildren.some((pi) => pi.key === ni.key))
            .map((c) => ({ ...c, checked: false }))
          const removedChildren = previousChildren.filter(
            (pi) => !newChildren.some((ni) => pi.key === ni.key)
          )
          return [
            {
              ...previousGroupNode,
              children: orderBy(
                [
                  ...previousChildren.filter(
                    (pi) => !removedChildren.some((ri) => ri === pi)
                  ),
                  ...addedChildren
                ],
                [(c) => c.lastName, (c) => c.firstName, (c) => c.key]
              )
            }
          ]
        }
      }),
    [attendeeNodes, attendees]
  )

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
        timeElems && timeElems.length > 0 && times.isValid()
          ? undefined
          : ('required' as const)
    }),
    [timeElems, times]
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
                  `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys}`,
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

        <MutateButton
          primary
          mutation={
            eventData?.id
              ? updateCalendarEventMutation
              : createCalendarEventMutation
          }
          text={
            eventData
              ? t.discussionReservation.saveSurveyButton
              : t.discussionReservation.createSurveyButton
          }
          disabled={eventData ? !isBasicInfoValid : !isFullyValid}
          onSuccess={() =>
            eventData
              ? navigate(
                  `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys/${eventData.id}`
                )
              : navigate(
                  `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys`
                )
          }
          onClick={() => {
            if (eventData) {
              const updateForm = isBasicInfoValid
                ? {
                    eventId: eventData.id,
                    form: {
                      ...initializedForm.value(),
                      times: [],
                      period
                    }
                  }
                : cancelMutation
              return updateForm
            } else {
              const createForm = isFullyValid
                ? {
                    eventId: '',
                    form: {
                      ...initializedForm.value(),
                      times: times.value().map((t) => ({
                        ...t,
                        startTime: t.timeRange.start,
                        endTime: t.timeRange.end
                      })),
                      period
                    }
                  }
                : cancelMutation
              return createForm
            }
          }}
          data-qa="save-button"
        />
      </FixedSpaceRow>
    </>
  )
})
