// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isAfter } from 'date-fns'
import React, { useMemo, useState } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import {
  AttendanceChild,
  ChildAttendanceStatusResponse
} from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalTime from 'lib-common/local-time'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import { faArrowLeft } from 'lib-icons'

import { routes } from '../../App'
import { renderResult } from '../../async-rendering'
import ChildNameBackButton from '../../common/ChildNameBackButton'
import {
  Actions,
  BackButtonInline,
  TimeWrapper,
  WideMutateButton
} from '../../common/components'
import { useTranslation } from '../../common/i18n'
import { TallContentArea } from '../../pairing/components'
import ChildNotesSummary from '../ChildNotesSummary'
import {
  attendanceStatusesQuery,
  childrenQuery,
  createArrivalMutation,
  returnToPresentMutation
} from '../queries'
import { childAttendanceStatus } from '../utils'

const MarkPresentInner = React.memo(function MarkPresentInner({
  unitId,
  childList
}: {
  unitId: UUID
  childList: {
    child: AttendanceChild
    attendanceStatus: ChildAttendanceStatusResponse
  }[]
}) {
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const [time, setTime] = useState(() => LocalTime.nowInHelsinkiTz().format())

  const { mutateAsync: createArrival } = useMutationResult(
    createArrivalMutation
  )

  const isValidTime = useMemo(() => {
    const parsedTime = LocalTime.tryParse(time)
    if (!parsedTime) return false

    return childList.every(({ attendanceStatus }) => {
      const childLatestDeparture =
        attendanceStatus.attendances.length > 0
          ? attendanceStatus.attendances[0].departed
          : null
      if (childLatestDeparture === null) return true
      return isAfter(
        HelsinkiDateTime.now().withTime(parsedTime).toSystemTzDate(),
        childLatestDeparture.toSystemTzDate()
      )
    })
  }, [childList, time])

  const singeDepartedChild =
    childList.length === 1 &&
    childList[0].attendanceStatus.status === 'DEPARTED'

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      <div>
        {childList.length === 1 ? (
          <ChildNameBackButton
            child={childList[0].child}
            onClick={() => navigate(-1)}
          />
        ) : (
          <BackButtonInline
            icon={faArrowLeft}
            text={i18n.common.return}
            onClick={() => navigate(-1)}
          />
        )}
      </div>
      <ContentArea
        shadow
        opaque={true}
        paddingHorizontal="s"
        paddingVertical="m"
      >
        <TimeWrapper>
          <TitleNoMargin size={2}>
            {singeDepartedChild
              ? i18n.attendances.actions.returnToPresent
              : i18n.attendances.actions.markPresent(childList.length)}
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
              disabled={!isValidTime}
              onClick={() =>
                createArrival({
                  unitId,
                  body: childList.reduce(
                    (acc, { child }) => ({
                      ...acc,
                      [child.id]: {
                        arrived: time
                      }
                    }),
                    {}
                  )
                })
              }
              onSuccess={() => {
                navigate(-2)
              }}
              data-qa="mark-present-btn"
            />
          </FixedSpaceRow>
        </Actions>
        {singeDepartedChild && (
          <>
            <Title centered size={2}>
              {i18n.attendances.actions.or}
            </Title>
            <JustifyContainer>
              <WideMutateButton
                text={i18n.attendances.actions.returnToPresentNoTimeNeeded}
                mutation={returnToPresentMutation}
                onClick={() => ({ unitId, childId: childList[0].child.id })}
                onSuccess={() => navigate(-2)}
                data-qa="return-to-present-btn"
              />
            </JustifyContainer>
          </>
        )}
      </ContentArea>
      <Gap size="s" />
      <FixedSpaceColumn>
        {childList.map(({ child }) => (
          <ChildNotesSummary child={child} key={child.id} />
        ))}
      </FixedSpaceColumn>
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
  const children = useQueryResult(childrenQuery(unitId)).map((children) =>
    children.filter((child) => childIds.includes(child.id))
  )

  const attendanceStatuses = useQueryResult(attendanceStatusesQuery({ unitId }))

  return renderResult(
    combine(children, attendanceStatuses),
    ([children, attendanceStatuses]) => (
      <MarkPresentInner
        unitId={unitId}
        childList={children.map((child) => ({
          child,
          attendanceStatus: childAttendanceStatus(child, attendanceStatuses)
        }))}
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
