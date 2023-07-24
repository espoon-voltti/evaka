// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isAfter } from 'date-fns'
import React, { useCallback, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { formatTime } from 'lib-common/date'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalTime from 'lib-common/local-time'
import { useMutationResult, useQuery, useQueryResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
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

export default React.memo(function MarkPresent() {
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const { childId, unitId, groupId } = useNonNullableParams<{
    unitId: string
    childId: string
    groupId: string
  }>()

  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)

  const [time, setTime] = useState(() => formatTime(mockNow() ?? new Date()))

  const { mutateAsync: createArrival } = useMutationResult(
    createArrivalMutation
  )

  const { data: attendanceStatuses } = useQuery(attendanceStatusesQuery(unitId))
  const childLatestDeparture = useMemo(() => {
    if (!attendanceStatuses) return null
    const attendances = childAttendanceStatus(
      attendanceStatuses,
      childId
    ).attendances
    return attendances.length > 0 ? attendances[0].departed : null
  }, [attendanceStatuses, childId])

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

  const groupNotes = useQueryResult(groupNotesQuery(groupId))

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      {renderResult(combine(child, groupNotes), ([child, groupNotes]) => (
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
                {childLatestDeparture
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
                    createArrival({ unitId, childId, arrived: time })
                  }
                  onSuccess={() => {
                    navigate(-2)
                  }}
                  data-qa="mark-present-btn"
                />
              </FixedSpaceRow>
            </Actions>
            {childLatestDeparture && (
              <>
                <Title centered size={2}>
                  {i18n.attendances.actions.or}
                </Title>
                <JustifyContainer>
                  <WideMutateButton
                    text={i18n.attendances.actions.returnToPresentNoTimeNeeded}
                    mutation={returnToPresentMutation}
                    onClick={() => ({ unitId, childId })}
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
