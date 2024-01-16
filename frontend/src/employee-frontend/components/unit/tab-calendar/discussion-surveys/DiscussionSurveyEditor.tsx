import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { useTranslation } from 'employee-frontend/state/i18n'
import DateRange from 'lib-common/date-range'
import { boolean, localDateRange, string } from 'lib-common/form/fields'
import {
  array,
  mapped,
  object,
  recursive,
  required
} from 'lib-common/form/form'
import { BoundForm, useForm, useFormFields } from 'lib-common/form/hooks'
import { Form } from 'lib-common/form/types'
import {
  CalendarEvent,
  GroupInfo,
  IndividualChild
} from 'lib-common/generated/api-types/calendarevent'
import { UnitGroupDetails } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { cancelMutation, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Button from 'lib-components/atoms/buttons/Button'
import MutateButton from 'lib-components/atoms/buttons/MutateButton'
import ReturnButton from 'lib-components/atoms/buttons/ReturnButton'
import TreeDropdown, {
  TreeNode,
  hasUncheckedChildren
} from 'lib-components/atoms/dropdowns/TreeDropdown'
import { TextAreaF } from 'lib-components/atoms/form/TextArea'
import Container, { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { DateRangePickerF } from 'lib-components/molecules/date-picker/DateRangePicker'
import { H2, H3, Label } from 'lib-components/typography'

import { unitGroupDetailsQuery } from '../../queries'
import {
  createCalendarEventMutation,
  updateCalendarEventMutation
} from '../queries'

const FormFieldGroup = styled(FixedSpaceColumn).attrs({ spacing: 'S' })``
const FormSectionGroup = styled(FixedSpaceColumn).attrs({ spacing: 'L' })`
  margin-bottom: 60px;
`
const WidthLimiter = styled.div`
  max-width: 400px;
`

const basicInfoForm = object({
  period: required(localDateRange()),
  title: required(string()),
  description: required(string())
})

const DiscussionSurveyForm = React.memo(function DiscussionSurveyForm({
  basicInfo,
  eventData,
  possibleAttendees,
  invitedAttendees,
  groupId,
  unitId
}: {
  basicInfo: BoundForm<typeof basicInfoForm>
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

  const { title, description, period } = useFormFields(basicInfo)

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
            period.value().overlaps(new DateRange(gp.startDate, gp.endDate))
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
  }, [invitedAttendees, possibleAttendees, period, groupId])

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
      times: [],
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
      period:
        period.state.start && period.state.end
          ? undefined
          : ('required' as const),
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
    [attendees, title, description, period]
  )

  const isValid = useMemo(
    () => Object.values(validationErrors).every((e) => e === undefined),
    [validationErrors]
  )

  return (
    <>
      <FormSectionGroup>
        <H3>{t.discussionReservation.surveyInviteeTitle}</H3>
        <FormFieldGroup>
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
        </FormFieldGroup>
      </FormSectionGroup>
      <FormSectionGroup>
        <H3>{t.discussionReservation.surveyDiscussionTimesTitle}</H3>
        <HorizontalLine />

        <HorizontalLine />
      </FormSectionGroup>

      <FixedSpaceRow justifyContent="flex-start" alignItems="center">
        <Button
          text={t.discussionReservation.cancelButton}
          disabled={false}
          onClick={() =>
            navigate(
              `/units/${unitId}/groups/${groupId}/discussion-reservation-surveys`
            )
          }
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
                    //FIXME: force test times
                    times: [
                      {
                        date: period.value().start,
                        startTime: LocalTime.of(8, 0),
                        endTime: LocalTime.of(8, 30)
                      },
                      {
                        date: period.value().start,
                        startTime: LocalTime.of(9, 0),
                        endTime: LocalTime.of(9, 30)
                      }
                    ]
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

const DiscussionSurveyFormWrapper = React.memo(
  function DiscussionSurveyFormWrapper({
    basicInfo,
    unitId,
    groupId,
    eventData
  }: {
    basicInfo: BoundForm<typeof basicInfoForm>
    unitId: UUID
    groupId: UUID
    eventData: CalendarEvent | null
  }) {
    const { period } = useFormFields(basicInfo)
    const groupData = useQueryResult(
      unitGroupDetailsQuery(unitId, period.value().start, period.value().end)
    )

    return (
      <>
        {renderResult(groupData, (groupResult) => {
          const { individualChildren, groups, isNewSurvey } = eventData
            ? { ...eventData, isNewSurvey: false }
            : { individualChildren: [], groups: [], isNewSurvey: true }

          return (
            <DiscussionSurveyForm
              basicInfo={basicInfo}
              eventData={eventData}
              unitId={unitId}
              invitedAttendees={{ individualChildren, groups, isNewSurvey }}
              possibleAttendees={groupResult}
              groupId={groupId}
            />
          )
        })}
      </>
    )
  }
)

export default React.memo(function DiscussionSurveyEditor({
  unitId,
  groupId,
  eventData
}: {
  unitId: UUID
  groupId: UUID
  eventData: CalendarEvent | null
}) {
  const { i18n, lang } = useTranslation()
  const t = i18n.unit.calendar.events
  const today = LocalDate.todayInHelsinkiTz()

  const form = mapped(basicInfoForm, (output) => ({
    ...output
  }))

  const basicInfo = useForm(
    form,
    () => ({
      period: eventData
        ? {
            start: eventData.period.start.format(),
            end: eventData.period.end.format(),
            config: { minDate: eventData.period.start }
          }
        : {
            start: today.format(),
            end: today.addMonths(1).format(),
            config: { minDate: today }
          },
      title: eventData?.title ?? '',
      description: eventData?.description ?? ''
    }),
    i18n.validationErrors
  )

  const { title, description, period } = useFormFields(basicInfo)

  const validationErrors = useMemo(
    () => ({
      period:
        basicInfo.state.period.start && basicInfo.state.period.end
          ? undefined
          : ('required' as const),
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
    [basicInfo.state]
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
    <Container>
      <ReturnButton label={i18n.common.goBack} />
      <ContentArea opaque>
        {eventData ? (
          <H2>{title.state}</H2>
        ) : (
          <H2>{t.discussionReservation.surveyCreate}</H2>
        )}
        <H3>{t.discussionReservation.surveyBasicsTitle}</H3>

        <FormSectionGroup>
          <FormFieldGroup>
            <Label>{t.discussionReservation.surveyPeriod}</Label>
            <DateRangePickerF
              locale={lang}
              bind={period}
              info={info('period')}
              data-qa="survey-period"
            />
          </FormFieldGroup>

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

        {period.isValid() && (
          <DiscussionSurveyFormWrapper
            eventData={eventData}
            basicInfo={basicInfo}
            unitId={unitId}
            groupId={groupId}
          />
        )}
      </ContentArea>
    </Container>
  )
})
