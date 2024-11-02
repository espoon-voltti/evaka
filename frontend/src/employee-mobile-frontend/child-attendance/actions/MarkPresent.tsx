// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isAfter } from 'date-fns'
import React, { useCallback, useMemo, useState } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import {
  AttendanceChild,
  ChildAttendanceStatusResponse
} from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalTime from 'lib-common/local-time'
import {
  constantQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'

import { routes } from '../../App'
import { renderResult } from '../../async-rendering'
import { groupNotesQuery } from '../../child-notes/queries'
import ChildNameBackButton from '../../common/ChildNameBackButton'
import { Actions, TimeWrapper, WideMutateButton } from '../../common/components'
import { useTranslation } from '../../common/i18n'
import { TallContentArea } from '../../pairing/components'
import ChildNotesSummary from '../ChildNotesSummary'
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

  const [time, setTime] = useState(() => LocalTime.nowInHelsinkiTz().format())

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
    child.groupId
      ? groupNotesQuery({ groupId: child.groupId })
      : constantQuery([])
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
                <LegacyButton
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
          <ChildNotesSummary child={child} groupNotes={groupNotes} />
        </>
      ))}
    </TallContentArea>
  )
})

const TitleNoMargin = styled(Title)`
  margin-top: 0;
  margin-bottom: 0;
`

const JustifyContainer = styled.div`
  display: flex;
  justify-content: center;
`

const MarkPresentWithChildIds = React.memo(function MarkPresentWithChildIds({
  unitId,
  childIds
}: {
  unitId: UUID
  childIds: UUID[]
}) {
  // TODO: using first child for now, need to implement multi-child support
  const childId = childIds[0]

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

export default React.memo(function MarkPresent({ unitId }: { unitId: UUID }) {
  const [searchParams] = useSearchParams()
  const children = searchParams.get('children')
  if (children === null)
    return <Navigate replace to={routes.unit(unitId).value} />
  const childIds = children.split(',').filter((id) => id.length > 0)
  if (childIds.length === 0)
    return <Navigate replace to={routes.unit(unitId).value} />

  return <MarkPresentWithChildIds unitId={unitId} childIds={childIds} />
})
