// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { faArrowLeft, farStickyNote } from '@evaka/lib-icons'
import InlineButton from '@evaka/lib-components/atoms/buttons/InlineButton'
import colors from '@evaka/lib-components/colors'
import Loader from '@evaka/lib-components/atoms/Loader'
import { useRestApi } from '@evaka/lib-common/utils/useRestApi'
import { defaultMargins, Gap } from '@evaka/lib-components/white-space'
import InputField from '@evaka/lib-components/atoms/form/InputField'
import AsyncButton from '@evaka/lib-components/atoms/buttons/AsyncButton'
import Button from '@evaka/lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from '@evaka/lib-components/layout/flex-helpers'
import Title from '@evaka/lib-components/atoms/Title'
import RoundIcon from '@evaka/lib-components/atoms/RoundIcon'

import {
  ContentAreaWithShadow,
  TallContentArea
} from '../../../components/mobile/components'
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

  const { childId, unitId } = useParams<{
    unitId: string
    childId: string
  }>()

  const loadDaycareAttendances = useRestApi(
    getDaycareAttendances,
    setAttendanceResponse
  )

  useEffect(() => {
    loadDaycareAttendances(unitId)
  }, [])

  function childArrives() {
    return childArrivesPOST(unitId, childId, time)
  }

  const child =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.children.find((ac) => ac.id === childId)

  return (
    <>
      {attendanceResponse.isLoading && <Loader />}
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
          <ContentAreaWithShadow
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
                  data-qa="mark-present"
                />
              </FixedSpaceRow>
            </Actions>
          </ContentAreaWithShadow>
          <Gap size={'s'} />
          <ContentAreaWithShadow
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
              <DailyNote child={child ? child : undefined} />
            </DailyNotes>
          </ContentAreaWithShadow>
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
