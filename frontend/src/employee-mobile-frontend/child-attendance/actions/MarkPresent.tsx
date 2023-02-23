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
import { Actions, TimeWrapper } from '../../common/components'
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
    const parsedTime = LocalTime.tryParse(time, 'HH:mm')
    if (!parsedTime) return false
    else if (childLatestDeparture) {
      return isAfter(
        HelsinkiDateTime.now().withTime(parsedTime).toSystemTzDate(),
        childLatestDeparture.toSystemTzDate()
      )
    } else return true
  }, [childLatestDeparture, time])

  const groupNotes = useQueryResult(groupNotesQuery(groupId))

  const { mutateAsync: returnToPresent } = useMutationResult(
    returnToPresentMutation
  )

  // Prevent the "return to present" AsyncButton from unmounting while the success animation is in progress.
  // It would unmount because childLatestDeparture vanishes after returnToPresent succeeds. The button needs
  // to remain mounted because onSuccess handles navigation back to list.
  const [returningToPresent, setReturningToPresent] = useState(false)

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
              <CustomTitle>{i18n.attendances.actions.markPresent}</CustomTitle>
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
            {(childLatestDeparture || returningToPresent) && (
              <>
                <Gap size="s" />
                <JustifyContainer>
                  <InlineWideAsyncButton
                    text={i18n.attendances.actions.returnToPresentNoTimeNeeded}
                    onClick={() => {
                      setReturningToPresent(true)
                      return returnToPresent({ unitId, childId })
                    }}
                    onSuccess={() => navigate(-2)}
                    onFailure={() => setReturningToPresent(false)}
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
                groupNote={groupNotes.length > 0 ? groupNotes[0] : undefined}
              />
            </DailyNotes>
          </ContentArea>
        </>
      ))}
    </TallContentArea>
  )
})

const CustomTitle = styled(Title)`
  margin-top: 0;
  margin-bottom: 0;
`

const DailyNotes = styled.div`
  display: flex;
`
const InlineWideAsyncButton = styled(AsyncButton)`
  border: none;
  height: 50px;
`

const JustifyContainer = styled.div`
  display: flex;
  justify-content: center;
`
