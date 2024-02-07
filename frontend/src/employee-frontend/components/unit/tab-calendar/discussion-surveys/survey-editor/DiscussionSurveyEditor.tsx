// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faQuestion } from 'Icons'
import orderBy from 'lodash/orderBy'
import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { boolean, requiredLocalTimeRange, string } from 'lib-common/form/fields'
import {
  array,
  mapped,
  object,
  recursive,
  required,
  value
} from 'lib-common/form/form'
import {
  BoundForm,
  useForm,
  useFormElems,
  useFormFields
} from 'lib-common/form/hooks'
import { Form } from 'lib-common/form/types'
import {
  CalendarEvent,
  GroupInfo,
  IndividualChild
} from 'lib-common/generated/api-types/calendarevent'
import { UnitGroupDetails } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { cancelMutation } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Button from 'lib-components/atoms/buttons/Button'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import TreeDropdown, {
  TreeNode,
  hasUncheckedChildren
} from 'lib-components/atoms/dropdowns/TreeDropdown'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H3, Label } from 'lib-components/typography'

import {
  createCalendarEventMutation,
  updateCalendarEventMutation
} from '../../queries'

import BasicInfoSection from './BasicInfoSection'
import DiscussionTimesForm from './DiscussionTimesForm'

const SurveyFormFieldGroup = styled(FixedSpaceColumn).attrs({ spacing: 'S' })``
const SurveyFormSectionGroup = styled(FixedSpaceColumn).attrs({ spacing: 'L' })`
  margin-bottom: 60px;
`
export const WidthLimiter = styled.div`
  max-width: 400px;
`
export type DiscussionSurveyEditMode = 'create' | 'reserve'

export const basicInfoForm = object({
  title: required(string()),
  description: required(string())
})

export const calendarEventTimeForm = object({
  id: value<UUID | null>(),
  childId: value<UUID | null>(),
  date: required(value<LocalDate>()),
  timeRange: required(requiredLocalTimeRange())
})

export const timesForm = object({
  times: array(calendarEventTimeForm)
})

export const DiscussionSurveyForm = React.memo(function DiscussionSurveyForm({
  times,
  basicInfo,
  period,
  eventData,
  possibleAttendees,
  invitedAttendees,
  groupId,
  unitId
}: {
  times: BoundForm<typeof timesForm>
  basicInfo: BoundForm<typeof basicInfoForm>
  period: FiniteDateRange
  eventData: CalendarEvent | null
  possibleAttendees: UnitGroupDetails
  invitedAttendees: {
    individualChildren: IndividualChild[]
    groups: GroupInfo[]
    isNewSurvey: boolean
  }
  groupId: UUID
  unitId: UUID
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events

  const [cancelConfirmModalVisible, setCancelConfirmModalVisible] =
    useState(false)
  const navigate = useNavigate()
  const isChildSelected = (childId: string, selections: IndividualChild[]) =>
    selections.some((s) => s.id === childId)
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

  const { title, description } = useFormFields(basicInfo)
  const { times: timesField } = useFormFields(times)
  const timeElems = useFormElems(timesField)

  const attendeeTree: TreeNode[] = useMemo(() => {
    const { groups, placements } = possibleAttendees
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
        children: []
      }))

      // group is always selected:
      // - the full group is selected by default for a new survey
      // - existing survey has to have at least one attendee for the group -> tree node selected
      return {
        text: g.name,
        key: g.id,
        checked: true,
        children: sortedGroupChildren
      }
    })
  }, [invitedAttendees, possibleAttendees, groupId, period])

  const treeNode = (): Form<TreeNode, never, TreeNode, unknown> =>
    object({
      text: string(),
      key: string(),
      checked: boolean(),
      children: array(recursive(treeNode))
    })

  const form = mapped(
    object({
      attendees: array(recursive(treeNode))
    }),
    (output) => ({
      ...output,
      tree: getTreeSelectionAsRecord(output.attendees),
      unitId: unitId
    })
  )

  const bind = useForm(
    form,
    () => ({
      attendees: attendeeTree
    }),
    i18n.validationErrors
  )

  const { attendees } = useFormFields(bind)

  const validationErrors = useMemo(
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
          : ('required' as const),
      times:
        timeElems && timeElems.length > 0 && timesField.isValid()
          ? undefined
          : ('required' as const)
    }),
    [attendees.state, title.state, description.state, timeElems, timesField]
  )

  const isValid = useMemo(
    () => Object.values(validationErrors).every((e) => e === undefined),
    [validationErrors]
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
              navigate(
                `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys`,
                { replace: true }
              )
            },
            label: t.discussionReservation.cancelConfirmation.continueButton
          }}
          text={t.discussionReservation.cancelConfirmation.text}
        />
      )}
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
          disabled={false}
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
          disabled={!isValid}
          onSuccess={() =>
            navigate(
              `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys`
            )
          }
          onClick={() =>
            isValid
              ? {
                  eventId: eventData?.id ?? '',
                  form: {
                    ...bind.value(),
                    ...basicInfo.value(),
                    times: timesField.value().map((t) => ({
                      ...t,
                      startTime: t.timeRange.start,
                      endTime: t.timeRange.end
                    })),
                    period
                  }
                }
              : cancelMutation
          }
          data-qa="save-button"
        />
      </FixedSpaceRow>
    </>
  )
})

export default React.memo(function DiscussionSurveyEditor({
  unitId,
  groupId,
  eventData
}: {
  unitId: UUID
  groupId: UUID
  eventData: CalendarEvent | null
}) {
  const { i18n } = useTranslation()

  const form = mapped(basicInfoForm, (output) => ({
    ...output
  }))

  const basicInfo = useForm(
    form,
    () => ({
      title: eventData?.title ?? '',
      description: eventData?.description ?? ''
    }),
    i18n.validationErrors
  )

  return (
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        <BasicInfoSection eventData={eventData} basicInfo={basicInfo} />
        <DiscussionTimesForm
          eventData={eventData}
          basicInfo={basicInfo}
          unitId={unitId}
          groupId={groupId}
          editMode="reserve"
        />
      </ContentArea>
    </Container>
  )
})

export const CreateDiscussionSurveyEditor = React.memo(
  function CreateDiscussionSurveyEditor({
    unitId,
    groupId
  }: {
    unitId: UUID
    groupId: UUID
  }) {
    const { i18n } = useTranslation()

    const form = mapped(basicInfoForm, (output) => ({
      ...output
    }))

    const basicInfo = useForm(
      form,
      () => ({
        title: '',
        description: ''
      }),
      i18n.validationErrors
    )

    return (
      <Container>
        <ReturnButton label={i18n.common.goBack} />
        <ContentArea opaque>
          <BasicInfoSection eventData={null} basicInfo={basicInfo} />
          <DiscussionTimesForm
            eventData={null}
            basicInfo={basicInfo}
            unitId={unitId}
            groupId={groupId}
            editMode="create"
          />
        </ContentArea>
      </Container>
    )
  }
)
