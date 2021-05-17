// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { faArrowLeft, farStickyNote } from 'lib-icons'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import colors from 'lib-customizations/common'
import Loader from 'lib-components/atoms/Loader'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { defaultMargins, Gap } from 'lib-components/white-space'
import InputField from 'lib-components/atoms/form/InputField'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import Title from 'lib-components/atoms/Title'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'

import { TallContentArea } from '../../../components/mobile/components'
import { AttendanceUIContext } from '../../../state/attendance-ui'
import {
  childArrivesPOST,
  getDaycareAttendances
} from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import { getCurrentTime } from '../AttendanceChildPage'
import DailyNote from '../notes/DailyNote'
import { Actions } from '../components'

export default React.memo(function MarkPresent() {
  const history = useHistory()
  const { i18n } = useTranslation()

  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  const [time, setTime] = useState<string>(getCurrentTime())

  const { childId, unitId, groupId } = useParams<{
    unitId: string
    childId: string
    groupId: string
  }>()

  const loadDaycareAttendances = useRestApi(
    getDaycareAttendances,
    setAttendanceResponse
  )

  useEffect(() => {
    loadDaycareAttendances(unitId)
  }, [loadDaycareAttendances, unitId])

  function childArrives() {
    return childArrivesPOST(unitId, childId, time)
  }

  const child =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.children.find((ac) => ac.id === childId)

  const groupNote =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.unit.groups.find((g) => g.id === groupId)
      ?.dailyNote

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
          <BackButton
            onClick={() => history.goBack()}
            icon={faArrowLeft}
            text={
              child ? `${child.firstName} ${child.lastName}` : i18n.common.back
            }
          />
          <ContentArea
            shadow
            opaque={true}
            paddingHorizontal={'s'}
            paddingVertical={'m'}
          >
            <TimeWrapper>
              <CustomTitle>{i18n.attendances.actions.markPresent}</CustomTitle>
              <InputField
                onChange={setTime}
                value={time}
                width="s"
                type="time"
                data-qa="set-time"
              />
            </TimeWrapper>
            <Gap size={'xs'} />
            <Actions>
              <FixedSpaceRow fullWidth>
                <Button
                  text={i18n.common.cancel}
                  onClick={() => history.goBack()}
                />
                <AsyncButton
                  primary
                  text={i18n.common.confirm}
                  onClick={() => childArrives()}
                  onSuccess={() => history.go(-2)}
                  data-qa="mark-present-btn"
                />
              </FixedSpaceRow>
            </Actions>
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

const BackButton = styled(InlineButton)`
  color: ${colors.blues.dark};
  margin-top: ${defaultMargins.s};
  margin-left: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.s};
`

const TimeWrapper = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 20px;
  color: ${colors.blues.dark};
  font-weight: 600;

  input {
    font-size: 60px;
    width: 100%;
    color: ${colors.blues.dark};
    font-family: Montserrat, sans-serif;
    font-weight: 300;
    border-bottom: none;
  }
`

const CustomTitle = styled(Title)`
  margin-top: 0;
  margin-bottom: 0;
`

const DailyNotes = styled.div`
  display: flex;
`
