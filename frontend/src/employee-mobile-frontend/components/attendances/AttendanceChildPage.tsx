// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useState } from 'react'
import { Link, useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import RoundIcon from '@evaka/lib-components/atoms/RoundIcon'
import {
  faArrowLeft,
  faComments,
  faInfo,
  farStickyNote,
  farUser
} from '@evaka/lib-icons'
import colors from '@evaka/lib-components/colors'
import { useTranslation } from '../../state/i18n'
import Loader from '@evaka/lib-components/atoms/Loader'
import { useRestApi } from '@evaka/lib-common/utils/useRestApi'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from '@evaka/lib-components/layout/flex-helpers'
import { StaticChip } from '@evaka/lib-components/atoms/Chip'

import AttendanceChildComing from './AttendanceChildComing'
import AttendanceChildPresent from './AttendanceChildPresent'
import AttendanceChildDeparted from './AttendanceChildDeparted'
import {
  AttendanceChild,
  AttendanceStatus,
  getDaycareAttendances,
  Group
} from '../../api/attendances'
import { AttendanceUIContext } from '../../state/attendance-ui'
import { FlexColumn } from './components'
import AttendanceChildAbsent from './AttendanceChildAbsent'
import { BackButton, TallContentArea } from '../../components/mobile/components'

const ChildStatus = styled.div`
  color: ${colors.greyscale.medium};
`

const CustomTitle = styled.h1`
  font-style: normal;
  font-weight: 600;
  font-size: 20px;
  line-height: 30px;
  margin-top: 0;
  color: ${colors.blues.dark};
  text-align: center;
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

const IconWrapper = styled.div`
  display: flex;
  justify-content: center;
  margin-top: 16px;
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
  }, [])

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
  }, [attendanceResponse])

  const loading = attendanceResponse.isLoading

  return (
    <Fragment>
      <TallContentArea opaque paddingHorizontal={'s'} spaced>
        <BackButton onClick={() => history.goBack()} icon={faArrowLeft} />
        {child && group && !loading ? (
          <Fragment>
            <FixedSpaceColumn alignItems={'center'}>
              <RoundIcon
                content={farUser}
                color={getColorByStatus(child.status)}
                size="XXL"
              />
              <CustomTitle>
                {child.firstName} {child.lastName}
              </CustomTitle>
              <GroupName>{group.name}</GroupName>
              <ChildStatus>
                <StaticChip color={getColorByStatus(child.status)}>
                  {i18n.attendances.types[child.status]}
                </StaticChip>
              </ChildStatus>
              <IconWrapper>
                <FixedSpaceRow spacing={'m'}>
                  <RoundIcon
                    content={faComments}
                    color={colors.brandEspoo.espooTurquoise}
                    size="L"
                    active={false}
                  />
                  <Link
                    to={`/units/${unitId}/groups/${groupId}/childattendance/${child.id}/note`}
                    data-qa={'link-child-daycare-daily-note'}
                  >
                    <RoundIcon
                      content={farStickyNote}
                      color={colors.accents.petrol}
                      size="L"
                      active={child && child.dailyNote ? true : false}
                    />
                  </Link>
                  <RoundIcon
                    content={faInfo}
                    color={colors.blues.medium}
                    size="L"
                    active={false}
                  />
                </FixedSpaceRow>
              </IconWrapper>
            </FixedSpaceColumn>

            <FlexColumn>
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
      </TallContentArea>
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
