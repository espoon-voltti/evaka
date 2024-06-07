// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import groupBy from 'lodash/groupBy'
import omitBy from 'lodash/omitBy'
import sortBy from 'lodash/sortBy'
import uniqBy from 'lodash/uniqBy'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { combine, wrapResult } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  CalendarEvent,
  GroupInfo,
  IndividualChild
} from 'lib-common/generated/api-types/calendarevent'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import { LegacyAsyncButton } from 'lib-components/atoms/buttons/LegacyAsyncButton'
import TreeDropdown, {
  hasUncheckedChildren,
  TreeNode
} from 'lib-components/atoms/dropdowns/TreeDropdown'
import InputField from 'lib-components/atoms/form/InputField'
import TextArea from 'lib-components/atoms/form/TextArea'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DateRangePicker from 'lib-components/molecules/date-picker/DateRangePicker'
import BaseModal from 'lib-components/molecules/modals/BaseModal'
import { AsyncFormModal } from 'lib-components/molecules/modals/FormModal'
import { Bold, fontWeights, H4, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'
import { faCalendarPlus, faQuestion, faTrash } from 'lib-icons'

import {
  createCalendarEvent,
  deleteCalendarEvent,
  getUnitCalendarEvents,
  modifyCalendarEvent
} from '../../../generated/api-clients/calendarevent'
import { useTranslation } from '../../../state/i18n'
import { UnitContext } from '../../../state/unit'
import { DayOfWeek } from '../../../types'
import { renderResult } from '../../async-rendering'
import { FlexRow } from '../../common/styled/containers'
import { unitGroupDetailsQuery } from '../queries'

const createCalendarEventResult = wrapResult(createCalendarEvent)
const getUnitCalendarEventsResult = wrapResult(getUnitCalendarEvents)
const modifyCalendarEventResult = wrapResult(modifyCalendarEvent)
const deleteCalendarEventResult = wrapResult(deleteCalendarEvent)

const EventsWeekContainer = styled.div`
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(0, 1fr);
  min-height: 175px;
  word-wrap: break-word;
`

export const EventDay = styled.div<{
  $isToday: boolean
  $isOtherMonth: boolean
}>`
  height: 100%;
  border: ${(p) => `1px solid ${p.theme.colors.grayscale.g15}`};
  padding: ${defaultMargins.s} 0;

  ${(p) =>
    p.$isToday
      ? `
    z-index: 5;
    border-top: 4px solid ${p.theme.colors.status.success};
  `
      : ''}
  &:not(:first-child) {
    margin-left: -1px;
  }

  &:first-child {
    border-radius: 4px 0 0 4px;
  }

  &:last-child {
    border-radius: 0 4px 4px 0;
  }

  h4 {
    padding: 0 ${defaultMargins.s};
  }

  ${(p) =>
    p.$isToday
      ? `
      h4 {
        color: ${p.theme.colors.main.m1};
        font-weight: ${fontWeights.medium};
      }
  `
      : ''}

  ${(p) =>
    p.$isOtherMonth
      ? `
      h4 {
        color: ${p.theme.colors.grayscale.g35};
        font-weight: ${fontWeights.medium};
      }
  `
      : ''}
`

const EventLinkContainer = styled.div`
  border-left: 6px solid transparent;
  padding: ${defaultMargins.xxs} calc(${defaultMargins.s} - 6px);

  &.type-daycare {
    border-color: ${(p) => p.theme.colors.accents.a4violet};
  }

  &.type-group {
    border-color: ${(p) => p.theme.colors.accents.a6turquoise};
  }

  &.type-partial-group {
    border-color: ${(p) => p.theme.colors.accents.a9pink};
  }
`

const DiscussionLink = styled(Link)`
  font-weight: ${fontWeights.semibold};
`

type SpecifierType = 'daycare' | 'group' | 'partial-group'

const getShortAttendees = (
  daycareName: string,
  groups: GroupInfo[],
  individualChildren: IndividualChild[],
  groupId: UUID | null
): {
  type: SpecifierType
  matchers: string[]
  text: string
} => {
  if (groups.length === 0) {
    return { type: 'daycare', matchers: [], text: daycareName }
  }

  if (!individualChildren.some((i) => i.groupId === groupId)) {
    return {
      type: 'group',
      matchers: groups.map(({ id }) => id),
      text: groups
        .filter(({ id }) => id === groupId)
        .map(({ name }) => name)
        .join(', ')
    }
  }

  const childGroupIds = new Set(
    individualChildren.map(({ groupId }) => groupId)
  )
  return {
    type: 'partial-group',
    matchers: groups.map(({ id }) => id),
    text: groups
      .filter(({ id }) => groupId === id)
      .map(({ name, id }) => (childGroupIds.has(id) ? 'Osa ryhmästä' : name))
      .join(', ')
  }
}

export default React.memo(function CalendarEventsSection({
  unitId,
  selectedDate,
  dateRange,
  operationalDays,
  groupId
}: {
  unitId: UUID
  selectedDate: LocalDate
  dateRange: FiniteDateRange
  operationalDays: DayOfWeek[]
  groupId: UUID | null // null means all groups
}) {
  const [events, reloadEvents] = useApiState(
    () =>
      getUnitCalendarEventsResult({
        unitId,
        start: dateRange.start,
        end: dateRange.end
      }),
    [unitId, dateRange]
  )

  const { calendarEventId } = useParams<{ calendarEventId: UUID }>()
  const navigate = useNavigate()

  const { i18n } = useTranslation()

  const [createEventModalVisible, setCreateEventModalVisible] = useState(false)

  const { unitInformation } = useContext(UnitContext)

  const editingEvent = useMemo(() => {
    if (!calendarEventId) return undefined

    const event = events.getOrElse([]).find(({ id }) => id === calendarEventId)

    if (!event && events.isSuccess) {
      navigate(`/units/${unitId}/calendar`)
      return undefined
    }

    return event
  }, [calendarEventId, events, navigate, unitId])

  const datesByWeeks: LocalDate[][] = useMemo(
    () =>
      Array.from(dateRange.dates()).reduce((acc, date) => {
        if (!operationalDays.includes(date.getIsoDayOfWeek() as DayOfWeek)) {
          return acc
        }
        const lastWeek = acc.length > 0 ? acc[acc.length - 1] : undefined
        const lastDate = lastWeek ? lastWeek[lastWeek.length - 1] : undefined
        if (
          lastWeek &&
          lastDate &&
          date.getIsoWeek() === lastDate.getIsoWeek()
        ) {
          lastWeek.push(date)
        } else {
          acc.push([date])
        }
        return acc
      }, [] as LocalDate[][]),
    [dateRange, operationalDays]
  )

  return (
    <div>
      {editingEvent && (
        <EditEventModal
          event={editingEvent}
          onClose={(shouldRefresh) => {
            if (shouldRefresh) {
              void reloadEvents()
            }
            navigate(`/units/${unitId}/calendar`)
          }}
        />
      )}

      {createEventModalVisible && (
        <CreateEventModal
          unitId={unitId}
          onClose={(shouldRefresh) => {
            setCreateEventModalVisible(false)
            if (shouldRefresh) {
              void reloadEvents()
            }
          }}
          groupId={groupId}
        />
      )}

      <FlexRow justifyContent={groupId ? 'space-between' : 'flex-end'}>
        {featureFlags.discussionReservations && !!groupId && (
          <DiscussionLink
            data-qa="discussion-survey-page-button"
            to={`/units/${unitId}/groups/${groupId}/discussion-reservation-surveys`}
          >
            {
              i18n.unit.calendar.events.discussionReservation
                .discussionPageTitle
            }
          </DiscussionLink>
        )}
        <AddButton
          flipped
          text={i18n.unit.calendar.events.createEvent}
          onClick={() => setCreateEventModalVisible(true)}
          data-qa="create-new-event-btn"
        />
      </FlexRow>
      <Gap size="s" />
      {renderResult(events, (events) => (
        <div>
          {datesByWeeks.map((datesInWeek, i) => (
            <EventsWeekContainer key={i}>
              {datesInWeek.map((day) => (
                <EventDay
                  key={day.formatIso()}
                  $isToday={LocalDate.todayInHelsinkiTz().isEqual(day)}
                  $isOtherMonth={day.month !== selectedDate.month}
                  data-qa={`calendar-event-day-${day.formatIso()}`}
                >
                  <H4 noMargin>
                    {
                      i18n.common.datetime.weekdaysShort[
                        day.getIsoDayOfWeek() - 1
                      ]
                    }{' '}
                    {day.format('d.M.')}
                  </H4>
                  <Gap size="xs" />
                  <FixedSpaceColumn spacing="s">
                    {sortBy(
                      events
                        .filter(({ period }) => period.includes(day))
                        .map((event) => ({
                          event,
                          specifier: getShortAttendees(
                            unitInformation
                              .map(({ daycare }) => daycare.name)
                              .getOrElse(''),
                            event.groups,
                            event.individualChildren,
                            groupId
                          )
                        }))
                        .filter(({ specifier }) =>
                          // if all groups selected: only show events applicable for daycare, NOT group events
                          // if specific group selected: show daycare-wide events and group's events
                          groupId === null
                            ? specifier.type === 'daycare'
                            : specifier.type === 'daycare' ||
                              ((specifier.type === 'group' ||
                                specifier.type === 'partial-group') &&
                                specifier.matchers.some((id) => id === groupId))
                        ),
                      ({ specifier }) =>
                        ['unit', 'group', 'partial-group'].indexOf(
                          specifier.type
                        )
                    ).map(({ event, specifier }) => (
                      <EventLinkContainer
                        className={`type-${specifier.type}`}
                        key={event.id}
                      >
                        <Link
                          to={`/units/${unitId}/calendar/events/${event.id}`}
                          data-qa="event"
                        >
                          <Bold>{specifier.text}:</Bold> {event.title}
                        </Link>
                      </EventLinkContainer>
                    ))}
                  </FixedSpaceColumn>
                </EventDay>
              ))}
            </EventsWeekContainer>
          ))}
        </div>
      ))}
    </div>
  )
})

interface CreationForm {
  attendees: TreeNode[]
  period: FiniteDateRange
  title: string
  description: string
}

const getFormTree = (tree: TreeNode[]) => {
  if (tree.length !== 1) {
    throw Error(
      'Calendar event tree select should have one initial parent (the current unit)'
    )
  }

  if (!hasUncheckedChildren(tree[0])) {
    return null
  }

  return Object.fromEntries(
    tree[0].children
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
}

const CreateEventModal = React.memo(function CreateEventModal({
  unitId,
  onClose,
  groupId
}: {
  unitId: UUID
  onClose: (shouldRefresh: boolean) => void
  groupId: UUID | null
}) {
  const { i18n, lang } = useTranslation()

  const { unitInformation } = useContext(UnitContext)

  const [form, setForm] = useState<CreationForm>({
    attendees: [],
    period: new FiniteDateRange(
      LocalDate.todayInHelsinkiTz(),
      LocalDate.todayInHelsinkiTz()
    ),
    title: '',
    description: ''
  })

  const groupData = useQueryResult(
    unitGroupDetailsQuery({
      unitId,
      from: form.period.start,
      to: form.period.end
    })
  )

  const updateForm = useCallback(
    <T extends keyof CreationForm>(key: T, value: CreationForm[T]) =>
      setForm((form) => ({
        ...form,
        [key]: value
      })),
    []
  )

  useEffect(() => {
    combine(groupData, unitInformation).map(
      ([{ groups, placements, backupCares }, { daycare }]) => {
        setForm(({ attendees, ...rest }) => {
          const useDefault = attendees.length === 0

          const selectedChildrenByGroup = Object.fromEntries(
            attendees.flatMap((unit) =>
              unit.children.map((group) => {
                const groupChildren = group.children ?? []
                const selectedChildren = groupChildren
                  .filter(({ checked }) => checked)
                  .map(({ key, checked }) => [key, checked] as const)

                return [
                  group.key,
                  {
                    allChildrenAreSelected:
                      groupChildren.length > 0 &&
                      groupChildren.length === selectedChildren.length,
                    selectedChildren: Object.fromEntries(selectedChildren)
                  }
                ]
              })
            )
          )

          const attendeeGroups = groups.map((group) => {
            const groupChildren = uniqBy(
              placements
                .filter((placement) =>
                  placement.groupPlacements.some(
                    (groupPlacement) =>
                      groupPlacement.groupId === group.id &&
                      groupPlacement.endDate.isEqualOrAfter(
                        form.period.start
                      ) &&
                      groupPlacement.startDate.isEqualOrBefore(form.period.end)
                  )
                )
                .map(({ child: { firstName, lastName, id } }) => ({
                  firstName,
                  lastName,
                  id
                }))
                .concat(
                  backupCares
                    .filter(
                      (bc) =>
                        bc.group?.id === group.id &&
                        bc.period.overlaps(form.period)
                    )
                    .map(({ child: { firstName, lastName, id } }) => ({
                      firstName,
                      lastName,
                      id
                    }))
                ),
              (child) => child.id
            ).map<TreeNode>((child) => ({
              text: `${child.firstName ?? ''} ${child.lastName ?? ''}`,
              key: child.id,
              checked: useDefault
                ? groupId === group.id || groupId === null
                : selectedChildrenByGroup[group.id]?.allChildrenAreSelected ||
                  selectedChildrenByGroup[group.id]?.selectedChildren[child.id],
              children: []
            }))

            return {
              text: group.name,
              key: group.id,
              checked: groupChildren.some(({ checked }) => checked),
              children: groupChildren
            }
          })

          return {
            ...rest,
            attendees: [
              {
                text: daycare.name,
                key: daycare.id,
                checked: attendeeGroups.some(({ checked }) => checked),
                children: attendeeGroups
              }
            ]
          }
        })
      }
    )
  }, [form.period, groupId, groupData, unitInformation, updateForm])

  const childrenWithMissingPlacement = useMemo(() => {
    const specificChildSelections = omitBy(
      groupBy(
        form.attendees[0]?.children?.flatMap((groupNode) => {
          // While group-wide attendee selections shouldn't make the warning show up,
          // the group-wide attendee selection may supplement gaps in between, e.g. if
          // a children has group placements like: group 1 = mon-tue, group 2 = wed;
          // an event for mon-wed won't show a warning if, e.g., a selection is made for
          // child-specific group 1 and whole group 2.
          const isSupplemental = !hasUncheckedChildren(groupNode)

          return (
            groupNode.children
              ?.filter(({ checked }) => checked)
              .map((childNode) => ({
                childId: childNode.key,
                groupId: groupNode.key,
                isSupplemental
              })) ?? []
          )
        }),
        ({ childId }) => childId
      ),
      (value) => value.every(({ isSupplemental }) => isSupplemental)
    )

    return groupData
      .map(({ placements, backupCares }) =>
        Object.entries(specificChildSelections)
          .map(([childId, groups]) => {
            const groupIds: string[] = groups.map((g) => g.groupId)
            const childsGroupPlacements = placements
              .filter((placement) => placement.child.id === childId)
              .flatMap((placement) => placement.groupPlacements)

            const child =
              placements.find(({ child }) => child.id === childId)?.child ??
              backupCares.find(({ child }) => child.id === childId)?.child

            const childsBackupCares = backupCares.filter(
              (bc) => bc.child.id === childId
            )

            return {
              childId,
              childName: child
                ? `${child.firstName ?? ''} ${child.lastName ?? ''}`
                : '',
              missingPlacementDates: [...form.period.dates()].filter(
                (date) =>
                  !childsGroupPlacements.some(
                    (gp) =>
                      gp.groupId &&
                      groupIds.includes(gp.groupId) &&
                      new FiniteDateRange(gp.startDate, gp.endDate).includes(
                        date
                      )
                  ) &&
                  !childsBackupCares.some(
                    (bc) =>
                      bc.group &&
                      groupIds.includes(bc.group.id) &&
                      bc.period.includes(date)
                  )
              )
            }
          })
          .filter(
            ({ missingPlacementDates }) => missingPlacementDates.length > 0
          )
      )
      .getOrElse([])
  }, [form.attendees, form.period, groupData])

  const anyTreeNodeChecked = useMemo(() => {
    const stack: TreeNode[] = [...form.attendees]

    while (stack.length > 0) {
      const node = stack.pop()!
      if (node.checked) {
        return true
      }
      if (node.children && node.children.length > 0) {
        stack.push(...node.children)
      }
    }

    return false
  }, [form.attendees])

  const formIsValid = useMemo(
    () => !!form.title && anyTreeNodeChecked,
    [form, anyTreeNodeChecked]
  )

  return (
    <AsyncFormModal
      title={i18n.unit.calendar.events.create.title}
      text={i18n.unit.calendar.events.create.text}
      icon={faCalendarPlus}
      type="info"
      resolveAction={() =>
        createCalendarEventResult({
          body: {
            unitId,
            title: form.title,
            description: form.description,
            period: form.period,
            tree: getFormTree(form.attendees),
            eventType: 'DAYCARE_EVENT',
            times: []
          }
        })
      }
      resolveLabel={i18n.unit.calendar.events.create.add}
      onSuccess={() => onClose(true)}
      rejectAction={() => onClose(false)}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!formIsValid}
    >
      <Label>{i18n.unit.calendar.events.create.attendees}</Label>
      <Gap size="xs" />
      <TreeDropdown
        tree={form.attendees}
        onChange={(tree) => updateForm('attendees', tree)}
        data-qa="attendees"
        placeholder={i18n.unit.calendar.events.create.attendeesPlaceholder}
      />

      <Gap size="s" />

      <Label>{i18n.unit.calendar.events.create.eventTitle}</Label>
      <InputField
        value={form.title}
        onChange={(val) => updateForm('title', val)}
        placeholder={i18n.unit.calendar.events.create.eventTitlePlaceholder}
        maxLength={30}
        width="full"
        data-qa="title-input"
      />

      <Gap size="s" />

      <Label id="event-create-period">
        {i18n.unit.calendar.events.create.period}
      </Label>
      <DateRangePicker
        start={form.period.start}
        end={form.period.end}
        onChange={(start, end) =>
          start && end && updateForm('period', new FiniteDateRange(start, end))
        }
        locale={lang}
        required={true}
      />

      {childrenWithMissingPlacement.length > 0 && (
        <AlertBox
          message={
            <>
              <P noMargin>
                {i18n.unit.calendar.events.create.missingPlacementsWarning}
              </P>
              <ul>
                {childrenWithMissingPlacement.map((child) => (
                  <li key={child.childId}>
                    {child.childName}:{' '}
                    {child.missingPlacementDates
                      .map((d) => d.format())
                      .join(', ')}
                  </li>
                ))}
              </ul>
            </>
          }
        />
      )}

      <Gap size="s" />

      <Label>{i18n.unit.calendar.events.create.description}</Label>
      <TextArea
        value={form.description}
        onChange={(val) => updateForm('description', val)}
        placeholder={i18n.unit.calendar.events.create.descriptionPlaceholder}
        data-qa="description-input"
      />
    </AsyncFormModal>
  )
})

const getLongAttendees = (
  daycareName: string,
  groups: GroupInfo[],
  individualChildren: IndividualChild[]
): string => {
  if (groups.length === 0) {
    return daycareName
  }

  if (individualChildren.length === 0) {
    return groups.map(({ name }) => name).join(', ')
  }

  const childGroupIds = groupBy(individualChildren, (child) => child.groupId)

  return groups
    .flatMap(
      ({ name, id }) =>
        childGroupIds[id]?.map(
          (child) => `${name}/${child.lastName} ${child.firstName}`
        ) ?? [name]
    )
    .join(', ')
}

const EditEventModal = React.memo(function EditEventModal({
  event,
  onClose
}: {
  event: CalendarEvent
  onClose: (shouldRefresh: boolean) => void
}) {
  const { i18n } = useTranslation()

  const [form, setForm] = useState<{
    title: string
    description: string
    tree: Record<string, UUID[] | null> | null
  }>({
    title: event.title,
    description: event.description,
    tree: null
  })

  const updateForm = useCallback(
    <T extends keyof CreationForm>(key: T, value: CreationForm[T]) =>
      setForm((form) => ({
        ...form,
        [key]: value
      })),
    []
  )

  const { unitInformation } = useContext(UnitContext)

  const [showDeletionModal, setShowDeletionModal] = useState(false)

  return (
    <>
      {!showDeletionModal && (
        <BaseModal
          title={i18n.unit.calendar.events.edit.title}
          type="info"
          width="wide"
          close={() => onClose(false)}
          closeLabel={i18n.common.closeModal}
          padding="L"
        >
          <Label>{i18n.unit.calendar.events.create.eventTitle}</Label>
          <InputField
            value={form.title}
            onChange={(val) => updateForm('title', val)}
            placeholder={i18n.unit.calendar.events.create.eventTitlePlaceholder}
            maxLength={30}
            width="full"
            data-qa="title-input"
          />

          <Gap size="s" />

          <Label>{i18n.unit.calendar.events.create.period}</Label>
          <Gap size="xs" />
          <div data-qa="period">
            {event.period.start.isEqual(event.period.end)
              ? event.period.start.format()
              : `${event.period.start.format()}–${event.period.end.format()}`}
          </div>

          <Gap size="s" />

          <Label>{i18n.unit.calendar.events.create.attendees}</Label>
          <Gap size="xs" />
          <div data-qa="attendee-list">
            {getLongAttendees(
              unitInformation.map(({ daycare }) => daycare.name).getOrElse(''),
              event.groups,
              event.individualChildren
            )}
          </div>

          <Gap size="s" />

          <Label>{i18n.unit.calendar.events.create.description}</Label>
          <TextArea
            value={form.description}
            onChange={(val) => updateForm('description', val)}
            placeholder={
              i18n.unit.calendar.events.create.descriptionPlaceholder
            }
            data-qa="description-input"
          />

          <Gap size="L" />
          <FixedSpaceRow justifyContent="space-between">
            <Button
              appearance="inline"
              icon={faTrash}
              text={i18n.unit.calendar.events.edit.delete}
              onClick={() => {
                setShowDeletionModal(true)
              }}
              data-qa="delete"
            />
            <LegacyAsyncButton
              primary
              onClick={() =>
                modifyCalendarEventResult({ id: event.id, body: form })
              }
              onSuccess={() => onClose(true)}
              text={i18n.unit.calendar.events.edit.saveChanges}
              data-qa="save"
            />
          </FixedSpaceRow>
          <Gap size="L" />
        </BaseModal>
      )}

      {showDeletionModal && (
        <AsyncFormModal
          icon={faQuestion}
          title="Haluatko varmasti poistaa tapahtuman?"
          text="Tapahtuma poistetaan sekä henkilökunnan että huoltajien kalenterista."
          type="warning"
          resolveAction={() => deleteCalendarEventResult({ id: event.id })}
          resolveLabel="Poista tapahtuma"
          onSuccess={() => {
            setShowDeletionModal(false)
            onClose(true)
          }}
          rejectAction={() => setShowDeletionModal(false)}
          rejectLabel="Älä poista"
          data-qa="deletion-modal"
        />
      )}
    </>
  )
})
