// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useMemo, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { faArrowLeft, farStickyNote } from 'lib-icons'
import colors from 'lib-customizations/common'
import { formatTime, isValidTime } from 'lib-common/date'
import { Gap } from 'lib-components/white-space'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { combine } from 'lib-common/api'
import { ContentArea } from 'lib-components/layout/Container'
import { AttendanceTimes } from 'lib-common/generated/api-types/attendance'
import { TallContentArea } from '../../mobile/components'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import {
  childDeparts,
  getChildDeparture,
  postDeparture
} from '../../../api/attendances'
import { Translations, useTranslation } from '../../../state/i18n'
import { AbsentFrom } from '../AbsentFrom'
import DailyNote from '../notes/DailyNote'
import { isBefore, parse } from 'date-fns'
import AbsenceSelector from '../AbsenceSelector'
import {
  Actions,
  BackButtonInline,
  CustomTitle,
  DailyNotes,
  TimeWrapper
} from '../components'
import { AbsenceType } from '../../../types'
import { useApiState } from 'lib-common/utils/useRestApi'
import { renderResult } from '../../async-rendering'

function validateTime(
  i18n: Translations,
  time: string,
  attendance: AttendanceTimes | null | undefined
): string | undefined {
  if (!attendance) return undefined

  if (!isValidTime(time)) {
    return i18n.attendances.timeError
  }

  try {
    const parsedTime = parse(time, 'HH:mm', new Date())

    if (isBefore(parsedTime, attendance.arrived)) {
      return `${i18n.attendances.arrived} ${formatTime(attendance.arrived)}`
    }
  } catch (e) {
    return i18n.attendances.timeError
  }

  return undefined
}

export default React.memo(function MarkDeparted() {
  const history = useHistory()
  const { i18n } = useTranslation()

  const { attendanceResponse, reloadAttendances } = useContext(
    ChildAttendanceContext
  )

  const { childId, unitId, groupId } = useParams<{
    unitId: string
    childId: string
    groupId: string
  }>()

  const [time, setTime] = useState<string>(formatTime(new Date()))
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
          .map(({ type }) => type)
      ),
    [childDepartureInfo, time]
  )

  const timeError = useMemo(
    () => child.map((child) => validateTime(i18n, time, child?.attendance)),
    [child, i18n, time]
  )

  const markDeparted = useCallback(
    () => childDeparts(unitId, childId, time),
    [unitId, childId, time]
  )

  const markDepartedWithAbsence = useCallback(
    (absenceType: AbsenceType) =>
      postDeparture(unitId, childId, absenceType, time),
    [unitId, childId, time]
  )

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal={'zero'}
      paddingVertical={'zero'}
    >
      {renderResult(
        combine(child, groupNote, absentFrom, timeError),
        ([child, groupNote, absentFrom, timeError]) => (
          <>
            <div>
              <BackButtonInline
                onClick={() => history.goBack()}
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
              paddingHorizontal={'s'}
              paddingVertical={'m'}
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
              {child && absentFrom.length > 0 ? (
                <FixedSpaceColumn>
                  <AbsentFrom child={child} absentFrom={absentFrom} />
                  <AbsenceSelector
                    selectedAbsenceType={selectedAbsenceType}
                    setSelectedAbsenceType={setSelectedAbsenceType}
                  />
                  <Actions data-qa={'absence-actions'}>
                    <FixedSpaceRow fullWidth>
                      <Button
                        text={i18n.common.cancel}
                        onClick={() => history.goBack()}
                      />
                      {selectedAbsenceType && !timeError ? (
                        <AsyncButton
                          primary
                          text={i18n.common.confirm}
                          onClick={() =>
                            markDepartedWithAbsence(selectedAbsenceType)
                          }
                          onSuccess={() => {
                            reloadAttendances()
                            history.goBack()
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
                <Actions data-qa={'non-absence-actions'}>
                  <FixedSpaceRow fullWidth>
                    <Button
                      text={i18n.common.cancel}
                      onClick={() => history.goBack()}
                    />
                    <AsyncButton
                      primary
                      text={i18n.common.confirm}
                      onClick={() => markDeparted()}
                      onSuccess={() => {
                        reloadAttendances()
                        history.go(-2)
                      }}
                      data-qa="mark-departed-btn"
                      disabled={timeError !== undefined}
                    />
                  </FixedSpaceRow>
                </Actions>
              )}
            </ContentArea>
            <Gap size={'s'} />
            <ContentArea
              shadow
              opaque={true}
              paddingHorizontal={'s'}
              paddingVertical={'s'}
              blue
            >
              <DailyNotes>
                <span>
                  <RoundIcon
                    content={farStickyNote}
                    color={colors.blues.medium}
                    size={'m'}
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
