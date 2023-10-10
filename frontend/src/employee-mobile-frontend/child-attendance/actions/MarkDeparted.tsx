// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { combine } from 'lib-common/api'
import { formatTime, isValidTime } from 'lib-common/date'
import { AttendanceTimes } from 'lib-common/generated/api-types/attendance'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import {
  queryOrDefault,
  useMutationResult,
  useQuery,
  useQueryResult
} from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { mockNow } from 'lib-common/utils/helpers'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employeeMobile'
import { farStickyNote } from 'lib-icons'

import { renderResult } from '../../async-rendering'
import { groupNotesQuery } from '../../child-notes/queries'
import ChildNameBackButton from '../../common/ChildNameBackButton'
import {
  Actions,
  CustomTitle,
  DailyNotes,
  TimeWrapper
} from '../../common/components'
import { Translations, useTranslation } from '../../common/i18n'
import { TallContentArea } from '../../pairing/components'
import DailyNote from '../DailyNote'
import {
  attendanceStatusesQuery,
  childDepartureQuery,
  childrenQuery,
  createDepartureMutation
} from '../queries'
import { childAttendanceStatus, useChild } from '../utils'

import AbsenceSelector, { AbsenceTypeWithNoAbsence } from './AbsenceSelector'
import { AbsentFrom } from './AbsentFrom'

function validateTime(
  i18n: Translations,
  time: string,
  attendances: AttendanceTimes[] | undefined
): string | undefined {
  if (!attendances || attendances.length === 0) return undefined

  if (!isValidTime(time)) {
    return i18n.attendances.timeError
  }

  try {
    const latestArrival = attendances.sort((l, r) =>
      l.arrived > r.arrived ? -1 : 1
    )[0]

    const parsedTime = LocalTime.parse(time)
    const parsedTimestamp =
      LocalDate.todayInSystemTz().toHelsinkiDateTime(parsedTime)
    if (!parsedTimestamp.isAfter(latestArrival.arrived)) {
      return `${i18n.attendances.arrived} ${latestArrival.arrived
        .toLocalTime()
        .format()}`
    }
  } catch (e) {
    return i18n.attendances.timeError
  }

  return undefined
}

export default React.memo(function MarkDeparted() {
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const { childId, unitId } = useNonNullableParams<{
    unitId: string
    childId: string
  }>()
  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)

  const [time, setTime] = useState(() => formatTime(mockNow() ?? new Date()))
  const childDepartureInfo = useQueryResult(
    childDepartureQuery({ unitId, childId })
  )

  const [selectedAbsenceType, setSelectedAbsenceType] = useState<
    AbsenceTypeWithNoAbsence | undefined
  >(undefined)

  const groupId = child.map(({ groupId }) => groupId).getOrElse(null)
  const groupNotes = useQueryResult(
    queryOrDefault(groupNotesQuery, [])(groupId)
  )

  const absentFrom = useMemo(
    () =>
      childDepartureInfo.map((thresholds) =>
        thresholds
          .filter((threshold) => time <= threshold.time)
          .map(({ category }) => category)
      ),
    [childDepartureInfo, time]
  )

  const { data: attendanceStatuses, isError } = useQuery(
    attendanceStatusesQuery(unitId)
  )
  const timeError = useMemo(() => {
    if (attendanceStatuses && child.isSuccess) {
      return validateTime(
        i18n,
        time,
        childAttendanceStatus(child.value, attendanceStatuses).attendances
      )
    }
    if (isError) return i18n.common.loadingFailed
    return undefined
  }, [attendanceStatuses, child, i18n, isError, time])

  const { mutateAsync: createDeparture } = useMutationResult(
    createDepartureMutation
  )

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      {renderResult(
        combine(child, groupNotes, absentFrom),
        ([child, groupNotes, absentFrom]) => (
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
                <CustomTitle>
                  {i18n.attendances.actions.markDeparted}
                </CustomTitle>
                <TimeInput
                  onChange={setTime}
                  value={time}
                  data-qa="set-time"
                  info={
                    timeError
                      ? { text: timeError, status: 'warning' }
                      : undefined
                  }
                />
              </TimeWrapper>
              <Gap size="xs" />
              {child && absentFrom.length > 0 ? (
                <FixedSpaceColumn>
                  <AbsentFrom
                    placementType={child.placementType}
                    absentFrom={absentFrom}
                  />
                  <AbsenceSelector
                    absenceTypes={[
                      'OTHER_ABSENCE',
                      'SICKLEAVE',
                      'UNKNOWN_ABSENCE',
                      'PLANNED_ABSENCE',
                      ...(featureFlags.noAbsenceType
                        ? (['NO_ABSENCE'] as const)
                        : ([] as const))
                    ]}
                    selectedAbsenceType={selectedAbsenceType}
                    setSelectedAbsenceType={setSelectedAbsenceType}
                  />
                  <Actions data-qa="absence-actions">
                    <FixedSpaceRow fullWidth>
                      <Button
                        text={i18n.common.cancel}
                        onClick={() => navigate(-1)}
                      />
                      {selectedAbsenceType !== undefined && !timeError ? (
                        <AsyncButton
                          primary
                          text={i18n.common.confirm}
                          onClick={() =>
                            createDeparture({
                              unitId,
                              childId,
                              absenceType:
                                selectedAbsenceType === 'NO_ABSENCE'
                                  ? null
                                  : selectedAbsenceType,
                              departed: time
                            })
                          }
                          onSuccess={() => {
                            navigate(-1)
                          }}
                          data-qa="mark-departed-with-absence-btn"
                        />
                      ) : (
                        <Button
                          primary
                          text={i18n.common.confirm}
                          disabled={true}
                        />
                      )}
                    </FixedSpaceRow>
                  </Actions>
                </FixedSpaceColumn>
              ) : (
                <Actions data-qa="non-absence-actions">
                  <FixedSpaceRow fullWidth>
                    <Button
                      text={i18n.common.cancel}
                      onClick={() => navigate(-1)}
                    />
                    <AsyncButton
                      primary
                      text={i18n.common.confirm}
                      onClick={() =>
                        createDeparture({
                          unitId,
                          childId,
                          absenceType: null,
                          departed: time
                        })
                      }
                      onSuccess={() => {
                        history.go(-2)
                      }}
                      data-qa="mark-departed-btn"
                      disabled={timeError !== undefined}
                    />
                  </FixedSpaceRow>
                </Actions>
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
        )
      )}
    </TallContentArea>
  )
})
