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

import { client } from 'employee-frontend/api/client'
import { getUnitData } from 'employee-frontend/api/unit'
import { renderResult } from 'employee-frontend/components/async-rendering'
import { FlexRow } from 'employee-frontend/components/common/styled/containers'
import { useTranslation } from 'employee-frontend/state/i18n'
import { UnitContext } from 'employee-frontend/state/unit'
import { combine, Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  CalendarEvent,
  CalendarEventForm,
  CalendarEventUpdateForm,
  GroupInfo,
  IndividualChild
} from 'lib-common/generated/api-types/calendarevent'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
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
import { faCalendarPlus, faQuestion, faTrash } from 'lib-icons'

import { AttendanceGroupFilter } from '../TabCalendar'

async function getCalendarEvents(
  unitId: UUID,
  range: FiniteDateRange
): Promise<Result<CalendarEvent[]>> {
  try {
    const { data } = await client.get<JsonOf<CalendarEvent[]>>(
      `/units/${unitId}/calendar-events?start=${range.start.toJSON()}&end=${range.end.toJSON()}`
    )
    return Success.of(
      data.map((event) => ({
        ...event,
        period: FiniteDateRange.parseJson(event.period)
      }))
    )
  } catch (e) {
    return Failure.fromError(e)
  }
}

async function createCalendarEvent(
  data: CalendarEventForm
): Promise<Result<void>> {
  try {
    await client.post('/calendar-event', data)
    return Success.of()
  } catch (e) {
    return Failure.fromError(e)
  }
}

async function updateCalendarEvent(
  id: UUID,
  data: CalendarEventUpdateForm
): Promise<Result<void>> {
  try {
    await client.patch(`/calendar-event/${id}`, data)
    return Success.of()
  } catch (e) {
    return Failure.fromError(e)
  }
}

async function deleteCalendarEvent(id: UUID): Promise<Result<void>> {
  try {
    await client.delete(`/calendar-event/${id}`)
    return Success.of()
  } catch (e) {
    return Failure.fromError(e)
  }
}

const EventsContainer = styled.div`
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(0, 1fr);
  min-height: 175px;
  word-wrap: break-word;
`

const EventDay = styled.div<{ isToday: boolean }>`
  height: 100%;
  border: ${(p) => `1px solid ${p.theme.colors.grayscale.g15}`};
  padding: ${defaultMargins.s} 0;
  ${(p) =>
    p.isToday
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
    p.isToday
      ? `
      h4 {
        color: ${p.theme.colors.main.m1};
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

type SpecifierType = 'daycare' | 'group' | 'partial-group'

const getShortAttendees = (
  daycareName: string,
  groups: GroupInfo[],
  individualChildren: IndividualChild[],
  groupId: AttendanceGroupFilter
): {
  type: SpecifierType
  matchers: string[]
  text: string
} => {
  if (groups.length === 0) {
    return { type: 'daycare', matchers: [], text: daycareName }
  }

  if (individualChildren.filter((i) => i.groupId === groupId).length === 0) {
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
  weekDateRange,
  selectedGroupId
}: {
  unitId: UUID
  weekDateRange: FiniteDateRange
  selectedGroupId: AttendanceGroupFilter
}) {
  const [events, reloadEvents] = useApiState(
    () => getCalendarEvents(unitId, weekDateRange),
    [unitId, weekDateRange]
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
          selectedGroupId={selectedGroupId}
        />
      )}

      <FlexRow justifyContent="flex-end">
        <AddButton
          flipped
          text={i18n.unit.calendar.events.createEvent}
          onClick={() => setCreateEventModalVisible(true)}
          data-qa="create-new-event-btn"
        />
      </FlexRow>
      <Gap size="s" />
      {renderResult(events, (events) => (
        <EventsContainer>
          {Array.from(weekDateRange.dates()).map((day) => (
            <EventDay
              key={day.formatIso()}
              isToday={LocalDate.todayInHelsinkiTz().isEqual(day)}
              data-qa={`calendar-event-day-${day.formatIso()}`}
            >
              <H4 noMargin>
                {i18n.common.datetime.weekdaysShort[day.getIsoDayOfWeek() - 1]}{' '}
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
                        selectedGroupId
                      )
                    }))
                    .filter(({ specifier }) =>
                      // if all groups selected: only show events applicable for daycare, NOT group events
                      // if specific group selected: show daycare-wide events and group's events
                      selectedGroupId === 'all'
                        ? specifier.type === 'daycare'
                        : specifier.type === 'daycare' ||
                          ((specifier.type === 'group' ||
                            specifier.type === 'partial-group') &&
                            specifier.matchers.some(
                              (groupId) => groupId === selectedGroupId
                            ))
                    ),
                  ({ specifier }) =>
                    ['unit', 'group', 'partial-group'].indexOf(specifier.type)
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
        </EventsContainer>
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
  selectedGroupId
}: {
  unitId: UUID
  onClose: (shouldRefresh: boolean) => void
  selectedGroupId: AttendanceGroupFilter
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

  const [unitData] = useApiState(
    () => getUnitData(unitId, form.period.start, form.period.end),
    [unitId, form.period]
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
    combine(unitData, unitInformation).map(
      ([{ groups, placements, backupCares }, { daycare }]) => {
        setForm(({ attendees, ...rest }) => {
          const useDefault = attendees.length === 0

          const selectedChildrenByGroup = Object.fromEntries(
            attendees.flatMap(
              (unit) =>
                unit.children?.map((group) => {
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
                }) ?? []
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
                ? selectedGroupId === group.id || selectedGroupId === 'all'
                : selectedChildrenByGroup[group.id]?.allChildrenAreSelected ||
                  selectedChildrenByGroup[group.id]?.selectedChildren[child.id]
            }))

            return {
              text: group.name,
              key: group.id,
              checked: groupChildren.some(({ checked }) => checked),
              children: groupChildren.length === 0 ? undefined : groupChildren
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
  }, [form.period, selectedGroupId, unitData, unitInformation, updateForm])

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

    return unitData
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
  }, [form.attendees, form.period, unitData])

  return (
    <AsyncFormModal
      title={i18n.unit.calendar.events.create.title}
      text={i18n.unit.calendar.events.create.text}
      icon={faCalendarPlus}
      type="info"
      resolveAction={() =>
        createCalendarEvent({
          unitId,
          title: form.title,
          description: form.description,
          period: form.period,
          tree: getFormTree(form.attendees)
        })
      }
      resolveLabel={i18n.unit.calendar.events.create.add}
      onSuccess={() => onClose(true)}
      rejectAction={() => onClose(false)}
      rejectLabel={i18n.common.cancel}
    >
      <Label>{i18n.unit.calendar.events.create.attendees}</Label>
      <Gap size="xs" />
      <TreeDropdown
        tree={form.attendees}
        onChange={(tree) => updateForm('attendees', tree)}
        data-qa="attendees"
        labels={i18n.common.treeDropdown}
        placeholder={i18n.common.treeDropdown.placeholder}
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
        errorTexts={i18n.validationErrors}
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
        childGroupIds[id]?.map((child) => `${name}/${child.name}`) ?? [name]
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

  const [form, setForm] = useState<{ title: string; description: string }>({
    title: event.title,
    description: event.description
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
          wide
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
            <InlineButton
              icon={faTrash}
              text={i18n.unit.calendar.events.edit.delete}
              onClick={() => {
                setShowDeletionModal(true)
              }}
              data-qa="delete"
            />
            <AsyncButton
              primary
              onClick={() => updateCalendarEvent(event.id, form)}
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
          resolveAction={() => deleteCalendarEvent(event.id)}
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
