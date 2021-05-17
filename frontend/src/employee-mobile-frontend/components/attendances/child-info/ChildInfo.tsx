// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useState } from 'react'
import { Link, useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { lighten } from 'polished'

import RoundIcon from 'lib-components/atoms/RoundIcon'
import { faArrowLeft, faCalendarTimes, farUser } from 'lib-icons'
import colors from 'lib-components/colors'
import { useTranslation } from '../../../state/i18n'
import Loader from 'lib-components/atoms/Loader'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { StaticChip } from 'lib-components/atoms/Chip'
import { defaultMargins, Gap } from 'lib-components/white-space'

import AttendanceChildComing from '../AttendanceChildComing'
import AttendanceChildPresent from '../AttendanceChildPresent'
import AttendanceChildDeparted from '../AttendanceChildDeparted'
import AttendanceDailyServiceTimes from '../AttendanceDailyServiceTimes'
import {
  AttendanceChild,
  AttendanceStatus,
  getDaycareAttendances,
  Group
} from '../../../api/attendances'
import { AttendanceUIContext } from '../../../state/attendance-ui'
import { FlexColumn } from '../components'
import AttendanceChildAbsent from '../AttendanceChildAbsent'
import { BackButton, TallContentArea } from '../../mobile/components'
import ChildButtons from './ChildButtons'

const ChildStatus = styled.div`
  color: ${colors.greyscale.medium};
  top: 10px;
  position: relative;
`

const CustomTitle = styled.h1`
  font-style: normal;
  font-weight: 600;
  font-size: 20px;
  line-height: 30px;
  margin-top: 0;
  color: ${colors.blues.dark};
  text-align: center;
  margin-bottom: ${defaultMargins.xs};
`

const GroupName = styled.div`
  font-family: 'Open Sans', 'Arial', sans-serif;
  font-style: normal;
  font-weight: 600;
  font-size: 15px;
  line-height: 22px;
  text-transform: uppercase;
  color: ${colors.greyscale.dark};
`

const Zindex = styled.div`
  z-index: 1;
  margin-left: -8%;
  margin-right: -8%;
`

const ChildBackground = styled.div<{ status: AttendanceStatus }>`
  background: ${(p) => p.status && lighten(0.2, getColorByStatus(p.status))};
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 0% 0% 50% 50%;
  padding-top: ${defaultMargins.s};
`

const BackButtonMargin = styled(BackButton)`
  margin-left: 8px;
  margin-top: 8px;
  z-index: 2;
`

const TallContentAreaNoOverflow = styled(TallContentArea)`
  overflow-x: hidden;
  height: calc(100% - 74px);
`

const Center = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 100vw;
`

const BottonButtonWrapper = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  height: 74px;
`
const LinkButtonWithIcon = styled(Link)``

const LinkButtonText = styled.span`
  color: ${colors.blues.primary};
  margin-left: ${defaultMargins.s};
  font-weight: 600;
  font-size: 16px;
  line-height: 16px;
`

export default React.memo(function AttendanceChildPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { unitId, childId, groupId } = useParams<{
    unitId: string
    groupId: string
    childId: string
  }>()

  const [child, setChild] = useState<AttendanceChild | undefined>(undefined)
  const [group, setGroup] = useState<Group | undefined>(undefined)
  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  const loadDaycareAttendances = useRestApi(
    getDaycareAttendances,
    setAttendanceResponse
  )

  useEffect(() => {
    loadDaycareAttendances(unitId)
  }, [loadDaycareAttendances, unitId])

  useEffect(() => {
    if (attendanceResponse.isSuccess) {
      const child = attendanceResponse.value.children.find(
        (child) => child.id === childId
      )
      setChild(child)
      setGroup(
        attendanceResponse.value.unit.groups.find(
          (group: Group) => group.id === child?.groupId
        )
      )
    }
  }, [attendanceResponse, childId])

  const loading = attendanceResponse.isLoading
  const groupNote = group?.dailyNote

  return (
    <Fragment>
      <TallContentAreaNoOverflow
        opaque
        paddingHorizontal={'0px'}
        paddingVertical={'0px'}
        spaced
        shadow
      >
        <BackButtonMargin
          onClick={() => history.goBack()}
          icon={faArrowLeft}
          data-qa="back-btn"
        />
        {child && group && !loading ? (
          <Fragment>
            <Zindex>
              <ChildBackground status={child.status}>
                <Center>
                  <RoundIcon
                    content={farUser}
                    color={getColorByStatus(child.status)}
                    size="XXL"
                  />

                  <Gap size={'s'} />

                  <CustomTitle data-qa={'child-name'}>
                    {child.firstName} {child.lastName}
                  </CustomTitle>

                  <GroupName>{group.name}</GroupName>

                  <ChildStatus>
                    <StaticChip
                      color={getColorByStatus(child.status)}
                      data-qa="child-status"
                    >
                      {i18n.attendances.types[child.status]}
                    </StaticChip>
                  </ChildStatus>
                </Center>
              </ChildBackground>

              <ChildButtons
                unitId={unitId}
                groupId={groupId}
                child={child}
                groupNote={groupNote}
              />
            </Zindex>

            <FlexColumn paddingHorizontal={'s'}>
              <AttendanceDailyServiceTimes times={child.dailyServiceTimes} />
              {child.status === 'COMING' && (
                <AttendanceChildComing
                  unitId={unitId}
                  child={child}
                  groupIdOrAll={groupId}
                />
              )}
              {child.status === 'PRESENT' && (
                <AttendanceChildPresent
                  child={child}
                  unitId={unitId}
                  groupIdOrAll={groupId}
                />
              )}
              {child.status === 'DEPARTED' && (
                <AttendanceChildDeparted child={child} unitId={unitId} />
              )}
              {child.status === 'ABSENT' && (
                <AttendanceChildAbsent child={child} unitId={unitId} />
              )}
            </FlexColumn>
          </Fragment>
        ) : (
          <Loader />
        )}
      </TallContentAreaNoOverflow>
      <BottonButtonWrapper>
        <LinkButtonWithIcon
          to={`/units/${unitId}/groups/${groupId}/childattendance/${childId}/markabsentbeforhand`}
        >
          <RoundIcon
            size={'L'}
            content={faCalendarTimes}
            color={colors.blues.primary}
          />
          <LinkButtonText>Merkitse tuleva poissaolo</LinkButtonText>
        </LinkButtonWithIcon>
      </BottonButtonWrapper>
    </Fragment>
  )
})

export function getCurrentTime() {
  return getTimeString(new Date())
}

export function getTimeString(date: Date) {
  return date.getHours() < 10
    ? date.getMinutes() < 10
      ? `0${date.getHours()}:0${date.getMinutes()}`
      : `0${date.getHours()}:${date.getMinutes()}`
    : date.getMinutes() < 10
    ? `${date.getHours()}:0${date.getMinutes()}`
    : `${date.getHours()}:${date.getMinutes()}`
}

function getColorByStatus(status: AttendanceStatus) {
  return status === 'ABSENT'
    ? colors.greyscale.dark
    : status === 'DEPARTED'
    ? colors.blues.medium
    : status === 'PRESENT'
    ? colors.accents.green
    : status === 'COMING'
    ? colors.accents.water
    : colors.blues.medium
}
