// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { faArrowLeft, farStickyNote } from 'lib-icons'
import colors from 'lib-customizations/common'
import { formatTime, isValidTime } from 'lib-common/date'
import Loader from 'lib-components/atoms/Loader'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Gap } from 'lib-components/white-space'
import InputField from 'lib-components/atoms/form/InputField'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { Result, Loading } from 'lib-common/api'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { fontWeights } from 'lib-components/typography'

import { TallContentArea } from '../../mobile/components'
import { AttendanceUIContext } from '../../../state/attendance-ui'
import {
  childDeparts,
  DepartureInfoResponse,
  getChildDeparture,
  getDaycareAttendances,
  postDeparture
} from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import DailyNote from '../notes/DailyNote'
import { isBefore, parse } from 'date-fns'
import AbsenceSelector from '../AbsenceSelector'
import {
  AbsentFrom,
  Actions,
  BackButtonInline,
  CustomTitle,
  DailyNotes
} from '../components'
import { AbsenceType } from '../../../types'

export default React.memo(function MarkDeparted() {
  const history = useHistory()
  const { i18n } = useTranslation()

  const { attendanceResponse, setAttendanceResponse } =
    useContext(AttendanceUIContext)

  const [time, setTime] = useState<string>(formatTime(new Date()))
  const [childDepartureInfo, setChildDepartureInfo] = useState<
    Result<DepartureInfoResponse>
  >(Loading.of())
  const [selectedAbsenceType, setSelectedAbsenceType] = useState<
    AbsenceType | undefined
  >(undefined)

  const { childId, unitId, groupId } = useParams<{
    unitId: string
    childId: string
    groupId: string
  }>()

  const child = attendanceResponse
    .map((response) => response.children.find((ac) => ac.id === childId))
    .getOrElse(undefined)

  const groupNote =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.unit.groups.find((g) => g.id === groupId)
      ?.dailyNote

  const loadDaycareAttendances = useRestApi(
    getDaycareAttendances,
    setAttendanceResponse
  )

  useEffect(() => {
    loadDaycareAttendances(unitId)
  }, [unitId, loadDaycareAttendances])

  function markDeparted() {
    return childDeparts(unitId, childId, time)
  }

  function markDepartedWithAbsence(absenceType: AbsenceType) {
    return postDeparture(unitId, childId, absenceType, time)
  }

  function validateTime() {
    if (!child?.attendance) return

    if (!isValidTime(time)) {
      return i18n.attendances.timeError
    }

    try {
      const parsedTime = parse(time, 'HH:mm', new Date())

      if (isBefore(parsedTime, child.attendance.arrived)) {
        return `${i18n.attendances.arrived} ${formatTime(
          child.attendance.arrived
        )}`
      }
    } catch (e) {
      return i18n.attendances.timeError
    }

    return
  }

  const timeError = validateTime()

  useEffect(() => {
    void getChildDeparture(unitId, childId).then(setChildDepartureInfo)
  }, [unitId, childId, setChildDepartureInfo])

  const absentFrom = childDepartureInfo.map((thresholds) =>
    thresholds
      .filter((threshold) => time <= threshold.time)
      .map(({ type }) => type)
  )

  return (
    <>
      {attendanceResponse.isLoading && <Loader />}
      {attendanceResponse.isFailure && <ErrorSegment />}
      {attendanceResponse.isSuccess && (
        <TallContentArea
          opaque={false}
          paddingHorizontal={'zero'}
          paddingVertical={'zero'}
        >
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
              <CustomTitle>{i18n.attendances.actions.markDeparted}</CustomTitle>
              <InputField
                onChange={setTime}
                value={time}
                width="s"
                type="time"
                data-qa="set-time"
                info={
                  timeError ? { text: timeError, status: 'warning' } : undefined
                }
              />
            </TimeWrapper>
            {child && absentFrom.isSuccess && absentFrom.value.length > 0 ? (
              <FixedSpaceColumn>
                <AbsentFrom child={child} absentFrom={absentFrom.value} />
                <AbsenceSelector
                  selectedAbsenceType={selectedAbsenceType}
                  setSelectedAbsenceType={setSelectedAbsenceType}
                />
                <Actions>
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
                        onSuccess={() => history.goBack()}
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
              <Actions>
                <FixedSpaceRow fullWidth>
                  <Button
                    text={i18n.common.cancel}
                    onClick={() => history.goBack()}
                  />
                  <AsyncButton
                    primary
                    text={i18n.common.confirm}
                    onClick={() => markDeparted()}
                    onSuccess={() => history.go(-2)}
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
        </TallContentArea>
      )}
    </>
  )
})

const TimeWrapper = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 20px;
  color: ${colors.blues.dark};
  font-weight: ${fontWeights.semibold};
  margin-bottom: 26px;

  input {
    font-size: 60px;
    width: 100%;
    color: ${colors.blues.dark};
    font-family: Montserrat, sans-serif;
    font-weight: ${fontWeights.light};
    border-bottom: none;
    padding: 0;
  }
`
