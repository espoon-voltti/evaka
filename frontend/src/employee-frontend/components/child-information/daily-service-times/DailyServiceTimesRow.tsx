// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faChevronDown, faChevronUp, faPen, faTrash } from 'Icons'
import React, { useCallback, useState } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { DailyServiceTimes } from 'lib-common/api-types/child/common'
import { Action } from 'lib-common/generated/action'
import LocalDate from 'lib-common/local-date'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H4, LabelLike } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { TimeBasedStatusChip } from '../TimeBasedStatusChip'

export default React.memo(function DailyServiceTimesRow({
  times,
  permittedActions
}: {
  times: DailyServiceTimes
  permittedActions: Action.DailyServiceTime[]
}) {
  const { i18n } = useTranslation()

  const [isOpen, setIsOpen] = useState(
    times.validityPeriod.includes(LocalDate.todayInHelsinkiTz())
  )

  const toggleOpen = useCallback(() => setIsOpen(!isOpen), [isOpen])

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
            {permittedActions.includes('UPDATE') && (
              <IconButton icon={faPen} data-qa="daily-service-times-row-edit" />
            )}
            {permittedActions.includes('DELETE') && (
              <IconButton
                icon={faTrash}
                data-qa="daily-service-times-row-delete"
              />
            )}
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
            <IconButton
              icon={isOpen ? faChevronUp : faChevronDown}
              onClick={toggleOpen}
              data-qa="daily-service-times-row-opener"
            />
          </FixedSpaceRow>
        </Td>
      </ClickableTr>
      {isOpen && (
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

const DailyServiceTimesReadOnly = React.memo(
  function DailyServiceTimesReadOnly({ times }: { times: DailyServiceTimes }) {
    const { i18n } = useTranslation()

    switch (times.type) {
      case 'REGULAR':
        return (
          <div>
            <Gap size="xs" />
            {i18n.childInformation.dailyServiceTimes.weekdays.monday}–
            {i18n.childInformation.dailyServiceTimes.weekdays.friday}{' '}
            {times.regularTimes.start}–{times.regularTimes.end}
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
                  } ${times[weekday]?.start ?? ''}–${times[weekday]?.end ?? ''}`
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
