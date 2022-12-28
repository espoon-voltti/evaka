// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { combine } from 'lib-common/api'
import { formatTime, isValidTime } from 'lib-common/date'
import { AttendanceTimes } from 'lib-common/generated/api-types/attendance'
import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { useMutationResult } from 'lib-common/query'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { mockNow } from 'lib-common/utils/helpers'
import { useApiState } from 'lib-common/utils/useRestApi'
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
import { faArrowLeft, farStickyNote } from 'lib-icons'

import { renderResult } from '../../async-rendering'
import {
  Actions,
  BackButtonInline,
  CustomTitle,
  DailyNotes,
  TimeWrapper
} from '../../common/components'
import { Translations, useTranslation } from '../../common/i18n'
import { TallContentArea } from '../../pairing/components'
import DailyNote from '../DailyNote'
import { getChildDeparture } from '../api'
import { createDepartureMutation } from '../queries'
import { ChildAttendanceContext } from '../state'

import AbsenceSelector from './AbsenceSelector'
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
    const parsedTime = LocalTime.parse(time, 'HH:mm')
    const parsedTimestamp =
      LocalDate.todayInSystemTz().toHelsinkiDateTime(parsedTime)
    if (!parsedTimestamp.isAfter(attendances[0].arrived)) {
      return `${i18n.attendances.arrived} ${attendances[0].arrived
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

  const { attendanceResponse } = useContext(ChildAttendanceContext)

  const { childId, unitId, groupId } = useNonNullableParams<{
    unitId: string
    childId: string
    groupId: string
  }>()

  const [time, setTime] = useState<string>(formatTime(mockNow() ?? new Date()))
  const [childDepartureInfo] = useApiState(
    () => getChildDeparture(unitId, childId),
    [childId, unitId]
  )

  const [selectedAbsenceType, setSelectedAbsenceType] = useState<
    AbsenceType | undefined
  >(undefined)

  const child = useMemo(
    () =>
      attendanceResponse.map((response) =>
        response.children.find((ac) => ac.id === childId)
      ),
    [attendanceResponse, childId]
  )

  const groupNote = useMemo(
    () =>
      attendanceResponse.map((response) =>
        response.groupNotes.find((g) => g.groupId === groupId)
      ),
    [attendanceResponse, groupId]
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

  const timeError = useMemo(
    () => child.map((child) => validateTime(i18n, time, child?.attendances)),
    [child, i18n, time]
  )

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
        combine(child, groupNote, absentFrom, timeError),
        ([child, groupNote, absentFrom, timeError]) => (
          <>
            <div>
              <BackButtonInline
                onClick={() => navigate(-1)}
                icon={faArrowLeft}
                text={
                  child
                    ? `${child.firstName} ${child.lastName}`
                    : i18n.common.back
                }
              />
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
                    selectedAbsenceType={selectedAbsenceType}
                    setSelectedAbsenceType={setSelectedAbsenceType}
                  />
                  <Actions data-qa="absence-actions">
                    <FixedSpaceRow fullWidth>
                      <Button
                        text={i18n.common.cancel}
                        onClick={() => navigate(-1)}
                      />
                      {selectedAbsenceType && !timeError ? (
                        <AsyncButton
                          primary
                          text={i18n.common.confirm}
                          onClick={() =>
                            createDeparture({
                              unitId,
                              childId,
                              absenceType: selectedAbsenceType,
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
                  groupNote={groupNote ? groupNote : undefined}
                />
              </DailyNotes>
            </ContentArea>
          </>
        )
      )}
    </TallContentArea>
  )
})
