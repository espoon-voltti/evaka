// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'

import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'
import { faChevronLeft, farUser } from '@evaka/lib-icons'
import colors from '@evaka/lib-components/src/colors'
import { Gap } from '@evaka/lib-components/src/white-space'
import { useTranslation } from '../../state/i18n'
import Loader from '@evaka/lib-components/src/atoms/Loader'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import IconButton from '@evaka/lib-components/src/atoms/buttons/IconButton'
import { FixedSpaceRow } from '@evaka/lib-components/src/layout/flex-helpers'

import AttendanceChildComing from './AttendanceChildComing'
import AttendanceChildPresent from './AttendanceChildPresent'
import AttendanceChildDeparted from './AttendanceChildDeparted'
import {
  AttendanceChild,
  getDaycareAttendances,
  Group
} from '../../api/attendances'
import { AttendanceUIContext } from '../../state/attendance-ui'
import { FlexColumn } from './components'
import AttendanceChildAbsent from './AttendanceChildAbsent'

const FullHeightContentArea = styled(ContentArea)`
  min-height: 100%;
  display: flex;
  flex-direction: column;
`

const Titles = styled.div``

const ChildStatus = styled.div`
  color: ${colors.greyscale.medium};
`

const TallContentArea = styled(ContentArea)`
  height: 100%;
  display: flex;
  flex-direction: column;
  flex: 1;
`

const BackButton = styled(IconButton)`
  color: ${colors.greyscale.medium};
`

const CustomTitle = styled.h1`
  font-style: normal;
  font-weight: 600;
  font-size: 20px;
  line-height: 30px;
  margin-block-start: 0;
  margin-block-end: 0;
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

  const { unitId, childId } = useParams<{
    unitId: string
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
      <FullHeightContentArea opaque={false} paddingHorizontal={'s'}>
        <TallContentArea opaque paddingHorizontal={'s'}>
          <BackButton onClick={() => history.goBack()} icon={faChevronLeft} />
          {child && group && !loading ? (
            <Fragment>
              <FixedSpaceRow>
                <RoundIcon
                  content={farUser}
                  color={
                    child.status === 'ABSENT'
                      ? colors.greyscale.dark
                      : child.status === 'DEPARTED'
                      ? colors.blues.medium
                      : child.status === 'PRESENT'
                      ? colors.accents.green
                      : child.status === 'COMING'
                      ? colors.accents.water
                      : colors.blues.medium
                  }
                  size="XXL"
                />
                <Titles>
                  <CustomTitle>
                    {child.firstName} {child.lastName}
                  </CustomTitle>
                  <GroupName>{group.name}</GroupName>
                  <Gap size={'s'} />
                  <ChildStatus>
                    {i18n.attendances.types[child.status]}
                  </ChildStatus>
                </Titles>
              </FixedSpaceRow>

              <Gap size={'s'} />
              <FlexColumn>
                {child.status === 'COMING' && (
                  <AttendanceChildComing
                    unitId={unitId}
                    child={child}
                    group={group}
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
      </FullHeightContentArea>
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
