// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faChevronDown, faChevronUp, faPen, faTrash } from 'Icons'
import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { Action } from 'lib-common/generated/action'
import { DailyServiceTimesValue } from 'lib-common/generated/api-types/dailyservicetimes'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H4, LabelLike } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { TimeBasedStatusChip } from '../TimeBasedStatusChip'

import { DailyServiceTimesEditForm } from './DailyServiceTimesForms'

export default React.memo(function DailyServiceTimesRow({
  childId,
  times,
  permittedActions,
  onDelete,
  onEdit,
  isEditing,
  id
}: {
  childId: UUID
  times: DailyServiceTimesValue
  permittedActions: Action.DailyServiceTime[]
  onDelete: () => void
  onEdit: (open: boolean) => void
  isEditing: boolean
  id: UUID
}) {
  const { i18n } = useTranslation()

  const [isOpen, setIsOpen] = useState(
    times.validityPeriod.includes(LocalDate.todayInHelsinkiTz())
  )

  const toggleOpen = useCallback(() => setIsOpen(!isOpen), [isOpen])

  const today = LocalDate.todayInHelsinkiTz()
  const hasStarted = !times.validityPeriod.start.isAfter(today)
  const hasEnded = times.validityPeriod.end?.isBefore(today)

  return (
    <>
      <ClickableTr onClick={toggleOpen} data-qa="daily-service-times-row">
        <Td
          horizontalPadding="zero"
          topBorder
          borderStyle="dashed"
          maximalWidth
        >
          <H4 noMargin data-qa="daily-service-times-row-title">
            {i18n.childInformation.dailyServiceTimes.dailyServiceTime}{' '}
            {times.validityPeriod.start.format()} –{' '}
            {times.validityPeriod.end?.format() ?? ''}
          </H4>
        </Td>
        <Td minimalWidth topBorder borderStyle="dashed" verticalAlign="middle">
          <FixedSpaceRow alignItems="center" spacing="s">
            {!hasEnded && !isEditing && permittedActions.includes('UPDATE') ? (
              <IconOnlyButton
                icon={faPen}
                data-qa="daily-service-times-row-edit"
                onClick={(ev) => {
                  ev.stopPropagation()
                  onEdit(true)
                }}
                aria-label={i18n.common.edit}
              />
            ) : null}
            {!hasStarted &&
            !isEditing &&
            permittedActions.includes('DELETE') ? (
              <IconOnlyButton
                icon={faTrash}
                data-qa="daily-service-times-row-delete"
                onClick={(ev) => {
                  ev.stopPropagation()
                  onDelete()
                }}
                aria-label={i18n.common.remove}
              />
            ) : null}
          </FixedSpaceRow>
        </Td>
        <Td minimalWidth topBorder borderStyle="dashed" verticalAlign="middle">
          <FixedSpaceRow
            justifyContent="flex-end"
            alignItems="center"
            spacing="s"
          >
            <TimeBasedStatusChip
              status={
                times.validityPeriod.start.isAfter(
                  LocalDate.todayInHelsinkiTz()
                )
                  ? 'UPCOMING'
                  : times.validityPeriod.end?.isBefore(
                        LocalDate.todayInHelsinkiTz()
                      )
                    ? 'ENDED'
                    : 'ACTIVE'
              }
              data-qa="status"
            />
            <IconOnlyButton
              icon={isOpen ? faChevronUp : faChevronDown}
              onClick={toggleOpen}
              data-qa="daily-service-times-row-opener"
              aria-label={isOpen ? i18n.common.close : i18n.common.open}
            />
          </FixedSpaceRow>
        </Td>
      </ClickableTr>
      {isOpen && !isEditing && (
        <Tr data-qa="daily-service-times-row-collapsible">
          <Td
            colSpan={3}
            borderStyle="none"
            horizontalPadding="zero"
            verticalPadding="zero"
          >
            <LabelLike>
              {i18n.childInformation.dailyServiceTimes.types[times.type]}
            </LabelLike>
            <DailyServiceTimesReadOnly times={times} />
            <Gap size="s" />
          </Td>
        </Tr>
      )}
      {isEditing && (
        <Tr data-qa="daily-service-times-row-editor">
          <Td
            colSpan={3}
            borderStyle="none"
            horizontalPadding="zero"
            verticalPadding="zero"
          >
            <DailyServiceTimesEditForm
              childId={childId}
              id={id}
              onClose={() => {
                onEdit(false)
              }}
              initialData={times}
            />
            <Gap size="s" />
          </Td>
        </Tr>
      )}
    </>
  )
})

const weekdays = [
  'monday',
  'tuesday',
  'wednesday',
  'thursday',
  'friday',
  'saturday',
  'sunday'
] as const

export const DailyServiceTimesReadOnly = React.memo(
  function DailyServiceTimesReadOnly({
    times
  }: {
    times: DailyServiceTimesValue
  }) {
    const { i18n } = useTranslation()

    switch (times.type) {
      case 'REGULAR':
        return (
          <div>
            <Gap size="xs" />
            {i18n.childInformation.dailyServiceTimes.weekdays.monday}–
            {i18n.childInformation.dailyServiceTimes.weekdays.friday}{' '}
            {times.regularTimes.format()}
          </div>
        )
      case 'IRREGULAR':
        return (
          <div>
            <Gap size="xs" />
            {weekdays
              .filter((weekday) => times[weekday])
              .map(
                (weekday) =>
                  `${
                    i18n.childInformation.dailyServiceTimes.weekdays[weekday]
                  } ${times[weekday]?.formatStart() ?? ''}–${
                    times[weekday]?.formatEnd() ?? ''
                  }`
              )
              .join(', ')}
          </div>
        )
      case 'VARIABLE_TIME':
        return null
    }
  }
)

const ClickableTr = styled(Tr)`
  cursor: pointer;

  &:hover {
    box-shadow: none;
  }
`
