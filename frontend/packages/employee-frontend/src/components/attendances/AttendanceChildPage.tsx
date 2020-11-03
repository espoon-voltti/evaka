// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useEffect, useState } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import MetaTags from 'react-meta-tags'

import LocalDate from '@evaka/lib-common/src/local-date'
import { isLoading, isSuccess, Loading, Result } from '~api'
import {
  ChildInGroup,
  getChildrenInGroup,
  getUnitData,
  UnitData
} from '~api/unit'
import { ContentArea } from '~components/shared/layout/Container'
import { DaycareGroup } from '~types/unit'
import { UUID } from '~types'
import RoundIcon from '~components/shared/atoms/RoundIcon'
import { faChevronLeft, farUser } from '~icon-set'
import Colors from '~components/shared/Colors'
import { DefaultMargins, Gap } from '~components/shared/layout/white-space'
import Title from '~components/shared/atoms/Title'
import { useTranslation } from '~state/i18n'
import Loader from '~components/shared/atoms/Loader'
import { FlexColumn } from './AttendanceGroupSelectorPage'
import { Label } from '~components/shared/Typography'
import AttendanceChildComing from './AttendanceChildComing'
import AttendanceChildPresent from './AttendanceChildPresent'
import AttendanceChildDeparted from './AttendanceChildDeparted'
import IconButton from '~components/shared/atoms/buttons/IconButton'

const FullHeightContentArea = styled(ContentArea)`
  min-height: 100vh;
  display: flex;
  flex-direction: column;
`

const Centered = styled.div`
  display: flex;
  justify-content: center;
`

const Titles = styled.div`
  h1,
  h2 {
    font-weight: 600;
  }

  h2 {
    font-family: 'Open Sans', 'Arial', sans-serif;
    text-transform: uppercase;
  }
`

export const FlexLabel = styled(Label)`
  display: flex;
  align-items: center;

  span {
    margin-right: ${DefaultMargins.m};
  }
`

const BlackTitle = styled(Title)`
  color: ${Colors.greyscale.darkest};
`

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

export default React.memo(function AttendanceChildPage() {
  const { i18n } = useTranslation()
  const history = useHistory()

  const { id, groupid, childid } = useParams<{
    id: UUID
    groupid: UUID | 'all'
    childid: UUID
  }>()

  const [child, setChild] = useState<ChildInGroup | undefined>(undefined)
  const [group, setGroup] = useState<DaycareGroup | undefined>(undefined)
  const [unitData, setUnitData] = useState<Result<UnitData>>(Loading())
  const [groupAttendances, setGoupAttendances] = useState<
    Result<ChildInGroup[]>
  >(Loading())

  useEffect(() => {
    void getUnitData(id, LocalDate.today(), LocalDate.today()).then(setUnitData)
    void getChildrenInGroup(groupid).then(setGoupAttendances)
  }, [])

  useEffect(() => {
    if (isSuccess(groupAttendances))
      setChild(groupAttendances.data.find((elem) => elem.childId === childid))
    if (isSuccess(unitData))
      setGroup(
        unitData.data.groups.find((elem: DaycareGroup) => elem.id === groupid)
      )
  }, [groupAttendances, unitData])

  const loading = isLoading(groupAttendances)

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
              <Centered>
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
              </Centered>
              <Gap size={'s'} />
              <Titles>
                <BlackTitle size={1} centered smaller>
                  {child.firstName} {child.lastName}
                </BlackTitle>
                <Title size={2} centered smaller>
                  {group.name}
                </Title>
              </Titles>
              <Centered>
                <ChildStatus>
                  {i18n.attendances.types[child.status]}
                </ChildStatus>
              </Centered>
              <Gap size={'s'} />
              <FlexColumn>
                {child.status === 'COMING' && (
                  <AttendanceChildComing
                    unitId={id}
                    child={child}
                    group={group}
                    groupId={groupid}
                  />
                )}
                {child.status === 'PRESENT' && (
                  <AttendanceChildPresent
                    child={child}
                    id={id}
                    groupid={groupid}
                  />
                )}
                {child.status === 'DEPARTED' && (
                  <AttendanceChildDeparted
                    child={child}
                    id={id}
                    groupid={groupid}
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
