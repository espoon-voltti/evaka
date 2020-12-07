// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import MetaTags from 'react-meta-tags'

import { ContentArea } from '~components/shared/layout/Container'
import { UUID } from '~types'
import RoundIcon from '~components/shared/atoms/RoundIcon'
import { faChevronLeft, farUser } from '~icon-set'
import Colors from '~components/shared/Colors'
import { Gap } from '~components/shared/layout/white-space'
import { useTranslation } from '~state/i18n'
import Loader from '~components/shared/atoms/Loader'
import AttendanceChildComing from './AttendanceChildComing'
import AttendanceChildPresent from './AttendanceChildPresent'
import AttendanceChildDeparted from './AttendanceChildDeparted'
import IconButton from '~components/shared/atoms/buttons/IconButton'
import { AttendanceChild, getDaycareAttendances, Group } from '~api/attendances'
import { AttendanceUIContext } from '~state/attendance-ui'
import { FixedSpaceRow } from '~components/shared/layout/flex-helpers'
import { FlexColumn } from './components'
import AttendanceChildAbsent from './AttendanceChildAbsent'

const FullHeightContentArea = styled(ContentArea)`
  min-height: 100vh;
  display: flex;
  flex-direction: column;
`

const Titles = styled.div``

const ChildStatus = styled.div`
  color: ${Colors.greyscale.medium};
`

const TallContentArea = styled(ContentArea)`
  height: 100%;
  display: flex;
  flex-direction: column;
  flex: 1;
`

const BackButton = styled(IconButton)`
  color: ${Colors.greyscale.medium};
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
  color: ${Colors.greyscale.dark};
`

export default React.memo(function AttendanceChildPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { unitId, groupId: groupIdOrAll, childId } = useParams<{
    unitId: UUID
    groupId: UUID | 'all'
    childId: UUID
  }>()

  const [child, setChild] = useState<AttendanceChild | undefined>(undefined)
  const [group, setGroup] = useState<Group | undefined>(undefined)
  const { attendanceResponse, filterAndSetAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  useEffect(() => {
    void getDaycareAttendances(unitId).then((res) =>
      filterAndSetAttendanceResponse(res, groupIdOrAll)
    )
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
      <MetaTags>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
      </MetaTags>

      <FullHeightContentArea opaque={false} paddingHorozontal={'s'}>
        <TallContentArea opaque paddingHorozontal={'s'}>
          <BackButton onClick={() => history.goBack()} icon={faChevronLeft} />
          {child && group && !loading ? (
            <Fragment>
              <FixedSpaceRow>
                <RoundIcon
                  content={farUser}
                  color={
                    child.status === 'ABSENT'
                      ? Colors.greyscale.dark
                      : child.status === 'DEPARTED'
                      ? Colors.blues.medium
                      : child.status === 'PRESENT'
                      ? Colors.accents.green
                      : child.status === 'COMING'
                      ? Colors.accents.water
                      : Colors.blues.medium
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
                    groupId={groupIdOrAll}
                  />
                )}
                {child.status === 'PRESENT' && (
                  <AttendanceChildPresent
                    child={child}
                    unitId={unitId}
                    groupId={groupIdOrAll}
                  />
                )}
                {child.status === 'DEPARTED' && (
                  <AttendanceChildDeparted
                    child={child}
                    unitId={unitId}
                    groupId={groupIdOrAll}
                  />
                )}
                {child.status === 'ABSENT' && (
                  <AttendanceChildAbsent
                    child={child}
                    unitId={unitId}
                    groupId={groupIdOrAll}
                  />
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
