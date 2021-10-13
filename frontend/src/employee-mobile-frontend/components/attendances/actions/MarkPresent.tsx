// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { faArrowLeft, farStickyNote } from 'lib-icons'
import colors from 'lib-customizations/common'
import Loader from 'lib-components/atoms/Loader'
import { formatTime, isValidTime } from 'lib-common/date'
import { Gap } from 'lib-components/white-space'
import InputField from 'lib-components/atoms/form/InputField'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import Title from 'lib-components/atoms/Title'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { ContentArea } from 'lib-components/layout/Container'
import { fontWeights } from 'lib-components/typography'

import { TallContentArea } from '../../mobile/components'
import { ChildAttendanceContext } from '../../../state/child-attendance'
import { childArrivesPOST } from '../../../api/attendances'
import { useTranslation } from '../../../state/i18n'
import DailyNote from '../notes/DailyNote'
import { Actions, BackButtonInline } from '../components'

export default React.memo(function MarkPresent() {
  const history = useHistory()
  const { i18n } = useTranslation()

  const { attendanceResponse, reloadAttendances } = useContext(
    ChildAttendanceContext
  )

  useEffect(() => reloadAttendances(true), [reloadAttendances])

  const [time, setTime] = useState<string>(formatTime(new Date()))

  const { childId, unitId, groupId } = useParams<{
    unitId: string
    childId: string
    groupId: string
  }>()

  function childArrives() {
    return childArrivesPOST(unitId, childId, time)
  }

  const child =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.children.find((ac) => ac.id === childId)

  const groupNote =
    attendanceResponse.isSuccess &&
    attendanceResponse.value.groupNotes.find((g) => g.groupId === groupId)
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
                  disabled={!isValidTime(time)}
                  onClick={() => childArrives()}
                  onSuccess={() => {
                    reloadAttendances()
                    history.go(-2)
                  }}
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

const TimeWrapper = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 20px;
  color: ${colors.blues.dark};
  font-weight: ${fontWeights.semibold};

  input {
    font-size: 60px;
    width: 100%;
    color: ${colors.blues.dark};
    font-family: Montserrat, sans-serif;
    font-weight: ${fontWeights.light};
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
