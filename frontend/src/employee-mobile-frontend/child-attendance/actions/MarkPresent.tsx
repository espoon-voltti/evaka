// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isAfter } from 'date-fns'
import React, { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { formatTime } from 'lib-common/date'
import {
  AttendanceChild,
  ChildAttendanceStatusResponse
} from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalTime from 'lib-common/local-time'
import {
  queryOrDefault,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'
import { mockNow } from 'lib-common/utils/helpers'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { farStickyNote } from 'lib-icons'

import { renderResult } from '../../async-rendering'
import { groupNotesQuery } from '../../child-notes/queries'
import ChildNameBackButton from '../../common/ChildNameBackButton'
import { Actions, TimeWrapper, WideMutateButton } from '../../common/components'
import { useTranslation } from '../../common/i18n'
import { TallContentArea } from '../../pairing/components'
import DailyNote from '../DailyNote'
import {
  attendanceStatusesQuery,
  childrenQuery,
  createArrivalMutation,
  returnToPresentMutation
} from '../queries'
import { childAttendanceStatus, useChild } from '../utils'

const MarkPresentInner = React.memo(function MarkPresentInner({
  unitId,
  child,
  attendanceStatus
}: {
  unitId: UUID
  child: AttendanceChild
  attendanceStatus: ChildAttendanceStatusResponse
}) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const [time, setTime] = useState(() => formatTime(mockNow() ?? new Date()))

  const { mutateAsync: createArrival } = useMutationResult(
    createArrivalMutation
  )

  const childLatestDeparture = useMemo(
    () =>
      attendanceStatus.attendances.length > 0
        ? attendanceStatus.attendances[0].departed
        : null,
    [attendanceStatus]
  )

  const isValidTime = useCallback(() => {
    const parsedTime = LocalTime.tryParse(time)
    if (!parsedTime) return false
    else if (childLatestDeparture) {
      return isAfter(
        HelsinkiDateTime.now().withTime(parsedTime).toSystemTzDate(),
        childLatestDeparture.toSystemTzDate()
      )
    } else return true
  }, [childLatestDeparture, time])

  const groupNotes = useQueryResult(
    queryOrDefault(
      groupNotesQuery,
      []
    )(child.groupId ? { groupId: child.groupId } : null)
  )

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      {renderResult(groupNotes, (groupNotes) => (
        <>
          <div>
            <ChildNameBackButton child={child} onClick={() => navigate(-1)} />
          </div>
          <ContentArea
            shadow
            opaque={true}
            paddingHorizontal="s"
            paddingVertical="m"
          >
            <TimeWrapper>
              <TitleNoMargin size={2}>
                {attendanceStatus.status === 'DEPARTED'
                  ? i18n.attendances.actions.returnToPresent
                  : i18n.attendances.actions.markPresent}
              </TitleNoMargin>
              <TimeInput onChange={setTime} value={time} data-qa="set-time" />
            </TimeWrapper>
            <Gap size="xs" />
            <Actions>
              <FixedSpaceRow fullWidth>
                <Button
                  text={i18n.common.cancel}
                  onClick={() => navigate(-1)}
                />
                <AsyncButton
                  primary
                  text={i18n.common.confirm}
                  disabled={!isValidTime()}
                  onClick={() =>
                    createArrival({
                      unitId,
                      childId: child.id,
                      body: { arrived: time }
                    })
                  }
                  onSuccess={() => {
                    navigate(-2)
                  }}
                  data-qa="mark-present-btn"
                />
              </FixedSpaceRow>
            </Actions>
            {attendanceStatus.status == 'DEPARTED' && (
              <>
                <Title centered size={2}>
                  {i18n.attendances.actions.or}
                </Title>
                <JustifyContainer>
                  <WideMutateButton
                    text={i18n.attendances.actions.returnToPresentNoTimeNeeded}
                    mutation={returnToPresentMutation}
                    onClick={() => ({ unitId, childId: child.id })}
                    onSuccess={() => navigate(-2)}
                    data-qa="return-to-present-btn"
                  />
                </JustifyContainer>
              </>
            )}
          </ContentArea>
          <Gap size="s" />
          <ContentArea
            shadow
            opaque={true}
            paddingHorizontal="s"
            paddingVertical="s"
            blue
          >
            <DailyNotes>
              <span>
                <RoundIcon
                  content={farStickyNote}
                  color={colors.main.m1}
                  size="m"
                />
              </span>
              <DailyNote
                child={child ? child : undefined}
                groupNotes={groupNotes}
              />
            </DailyNotes>
          </ContentArea>
        </>
      ))}
    </TallContentArea>
  )
})

const TitleNoMargin = styled(Title)`
  margin-top: 0;
  margin-bottom: 0;
`

const DailyNotes = styled.div`
  display: flex;
`

const JustifyContainer = styled.div`
  display: flex;
  justify-content: center;
`

export default React.memo(function MarkPresent({ unitId }: { unitId: UUID }) {
  const { childId } = useRouteParams(['childId'])
  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)
  const attendanceStatuses = useQueryResult(attendanceStatusesQuery({ unitId }))

  return renderResult(
    combine(child, attendanceStatuses),
    ([child, attendanceStatuses]) => (
      <MarkPresentInner
        unitId={unitId}
        child={child}
        attendanceStatus={childAttendanceStatus(child, attendanceStatuses)}
      />
    )
  )
})
