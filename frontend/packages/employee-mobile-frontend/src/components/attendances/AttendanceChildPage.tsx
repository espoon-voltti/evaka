// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'
import { faArrowLeft, farUser } from '@evaka/lib-icons'
import colors from '@evaka/lib-components/src/colors'
import { Gap } from '@evaka/lib-components/src/white-space'
import { useTranslation } from '../../state/i18n'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import { StaticChip } from '@evaka/lib-components/src/atoms/Chip'

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

export default React.memo(function AttendanceChildPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { unitId, childId, groupId: groupIdOrAll } = useParams<{
    unitId: string
    groupId: string | 'all'
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
            </FixedSpaceColumn>

            <Gap size={'s'} />
            <FlexColumn>
              {child.status === 'COMING' && (
                <AttendanceChildComing
                  unitId={unitId}
                  child={child}
                  groupIdOrAll={groupIdOrAll}
                />
              )}
              {child.status === 'PRESENT' && (
                <AttendanceChildPresent child={child} unitId={unitId} />
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
