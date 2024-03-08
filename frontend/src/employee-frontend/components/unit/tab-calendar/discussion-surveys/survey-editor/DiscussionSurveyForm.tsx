// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faQuestion } from 'Icons'
import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { mapped } from 'lib-common/form/form'
import {
  BoundForm,
  useForm,
  useFormElems,
  useFormFields
} from 'lib-common/form/hooks'
import {
  CalendarEvent,
  IndividualChild
} from 'lib-common/generated/api-types/calendarevent'
import { UnitGroupDetails } from 'lib-common/generated/api-types/daycare'
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

import { TreeNodeInfo, attendeeForm, surveyForm } from './form'

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
  unitId,
  period,
  groupResult
}: {
  form: BoundForm<typeof surveyForm>
  eventData: CalendarEvent | null
  groupId: UUID
  unitId: UUID
  period: FiniteDateRange
  groupResult: UnitGroupDetails
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events

  const [cancelConfirmModalVisible, setCancelConfirmModalVisible] =
    useState(false)

  const navigate = useNavigate()

  const invitedAttendees = useMemo(
    () => ({
      individualChildren: eventData?.individualChildren ?? [],
      groups: eventData?.groups ?? []
    }),
    [eventData]
  )

  const isChildSelected = (childId: string, selections: IndividualChild[]) =>
    selections.some((s) => s.id === childId)

  const attendeeTree: TreeNodeInfo[] = useMemo(() => {
    const { groups, placements } = groupResult
    const currentGroup = groups.filter((g) => g.id === groupId)
    return currentGroup.map((g) => {
      // individualChildren contains childSelections that are not a part of a full group selection
      // -> any children there means their full group is not selected
      const individualChildrenOfSelectedGroup =
        invitedAttendees.individualChildren.filter((c) => c.groupId === groupId)
      const groupChildren = placements.filter((p) =>
        p.groupPlacements.some(
          (gp) =>
            gp.groupId === g.id &&
            period.overlaps(new DateRange(gp.startDate, gp.endDate))
        )
      )

      const sortedGroupChildren = orderBy(groupChildren, [
        ({ child }) => child.lastName,
        ({ child }) => child.firstName
      ]).map(({ child: c }) => ({
        key: c.id,
        text: `${c.firstName} ${c.lastName}`,
        checked:
          individualChildrenOfSelectedGroup.length === 0 ||
          isChildSelected(c.id, individualChildrenOfSelectedGroup),
        children: [],
        firstName: c.firstName,
        lastName: c.lastName
      }))

      // group is always selected:
      // - the full group is selected by default for a new survey
      // - existing survey has to have at least one attendee for the group -> tree node selected
      return {
        text: g.name,
        key: g.id,
        checked: true,
        children: sortedGroupChildren,
        firstName: '',
        lastName: ''
      }
    })
  }, [invitedAttendees, groupResult, groupId, period])

  const mappedAttendeeForm = mapped(attendeeForm, (output) => ({
    ...output,
    tree: getTreeSelectionAsRecord(output.attendees),
    unitId: unitId
  }))

  const initializedAttendeeForm = useForm(
    mappedAttendeeForm,
    () => ({
      attendees: attendeeTree
    }),
    i18n.validationErrors
  )

  const { attendees } = useFormFields(initializedAttendeeForm)

  const { times, title, description } = useFormFields(form)
  const timeElems = useFormElems(times)

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
            const values = form.value()
            const attendeeValue = attendees.value()
            const baseForm = {
              ...values,
              unitId,
              period,
              tree: getTreeSelectionAsRecord(attendeeValue)
            }
            if (eventData) {
              return isBasicInfoValid
                ? {
                    eventId: eventData.id,
                    form: {
                      ...baseForm,
                      times: null
                    }
                  }
                : cancelMutation
            } else {
              return isFullyValid
                ? {
                    eventId: '',
                    form: {
                      ...baseForm,
                      times: times.value().map((t) => ({
                        ...t,
                        startTime: t.timeRange.start,
                        endTime: t.timeRange.end
                      }))
                    }
                  }
                : cancelMutation
            }
          }}
          data-qa="save-button"
        />
      </FixedSpaceRow>
    </>
  )
})
