// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { faArrowLeft, farStickyNote } from '@evaka/lib-icons'
import colors from '@evaka/lib-components/src/colors'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { Gap } from '@evaka/lib-components/src/white-space'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import AsyncButton from '@evaka/lib-components/src/atoms/buttons/AsyncButton'
import Button from '@evaka/lib-components/src/atoms/buttons/Button'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/src/layout/flex-helpers'
import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'
import { Result, Loading } from '@evaka/lib-common/src/api'

import {
  ContentAreaWithShadow,
  TallContentArea
} from '~components/mobile/components'
import { AttendanceUIContext } from '~state/attendance-ui'
import {
  childDeparts,
  DepartureInfoResponse,
  getChildDeparture,
  getDaycareAttendances,
  postDeparture
} from '~api/attendances'
import { useTranslation } from '~state/i18n'
import { getCurrentTime } from '../AttendanceChildPage'
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
import { AbsenceType } from '~types'

export default React.memo(function MarkDeparted() {
  const history = useHistory()
  const { i18n } = useTranslation()

  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  const [time, setTime] = useState<string>(getCurrentTime())
  const [timeError, setTimeError] = useState<boolean>(false)
  const [childDepartureInfo, setChildDepartureInfo] = useState<
    Result<DepartureInfoResponse>
  >(Loading.of())
  const [selectedAbsenceType, setSelectedAbsenceType] = useState<
    AbsenceType | undefined
  >(undefined)

  const { childId, unitId } = useParams<{
    unitId: string
    childId: string
  }>()

  const child =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.children.find((ac) => ac.id === childId)

  const loadDaycareAttendances = useRestApi(
    getDaycareAttendances,
    setAttendanceResponse
  )

  useEffect(() => {
    loadDaycareAttendances(unitId)
    void getChildDeparture(unitId, childId, time).then(setChildDepartureInfo)
  }, [])

  function markDeparted() {
    return childDeparts(unitId, childId, time)
  }

  function markDepartedWithAbsence(absenceType: AbsenceType) {
    return postDeparture(unitId, childId, absenceType, time)
  }

  function updateTime(newTime: string) {
    if (child && child.attendance !== null) {
      const newDate = parse(newTime, 'HH:mm', new Date())
      if (isBefore(newDate, child.attendance.arrived)) {
        setTimeError(true)
      } else {
        setTimeError(false)
        void getChildDeparture(unitId, child.id, newTime).then(
          setChildDepartureInfo
        )
      }
    }
    setTime(newTime)
  }

  return (
    <>
      {attendanceResponse.isLoading && <Loader />}
      {attendanceResponse.isSuccess && (
        <TallContentArea
          opaque={false}
          paddingHorizontal={'zero'}
          paddingVertical={'zero'}
        >
          <BackButtonInline
            onClick={() => history.goBack()}
            icon={faArrowLeft}
            text={
              child ? `${child.firstName} ${child.lastName}` : i18n.common.back
            }
          />
          <ContentAreaWithShadow
            opaque={true}
            paddingHorizontal={'s'}
            paddingVertical={'m'}
          >
            <TimeWrapper>
              <CustomTitle>{i18n.attendances.actions.markDeparted}</CustomTitle>
              <InputField
                onChange={updateTime}
                value={time}
                width="s"
                type="time"
                data-qa="set-time"
                info={
                  timeError
                    ? { text: i18n.attendances.timeError, status: 'warning' }
                    : undefined
                }
              />
            </TimeWrapper>
            {childDepartureInfo.isSuccess &&
            childDepartureInfo.value.absentFrom.length > 0 &&
            child ? (
              <FixedSpaceColumn>
                <AbsentFrom
                  child={child}
                  absentFrom={childDepartureInfo.value.absentFrom}
                />
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
                        data-qa="mark-absent"
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
                    onSuccess={() => history.goBack()}
                    data-qa="mark-present"
                    disabled={timeError}
                  />
                </FixedSpaceRow>
              </Actions>
            )}
          </ContentAreaWithShadow>
          <Gap size={'s'} />
          <ContentAreaWithShadow
            opaque={true}
            paddingHorizontal={'s'}
            paddingVertical={'s'}
            active={child && child.dailyNote ? true : false}
          >
            <DailyNotes>
              <span>
                <RoundIcon
                  content={farStickyNote}
                  color={colors.blues.medium}
                  size={'m'}
                />
              </span>
              <DailyNote child={child ? child : undefined} />
            </DailyNotes>
          </ContentAreaWithShadow>
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
  font-weight: 600;
  margin-bottom: 26px;

  input {
    font-size: 60px;
    width: 100%;
    color: ${colors.blues.dark};
    font-family: Montserrat, sans-serif;
    font-weight: 300;
    border-bottom: none;
    padding: 0;
  }
`
