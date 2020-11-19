// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'
import MetaTags from 'react-meta-tags'

import { isFailure, isLoading, isSuccess } from '~api'
import Loader from '~components/shared/atoms/Loader'
import Title from '~components/shared/atoms/Title'
import { ContentArea } from '~components/shared/layout/Container'
import { DefaultMargins } from '~components/shared/layout/white-space'
import { useTranslation } from '~state/i18n'
import { AttendanceUIContext } from '~state/attendance-ui'
import { getDaycareAttendances, Group } from '~api/attendances'
import { Flex, CustomButton } from './components'

const AllCapsTitle = styled(Title)`
  text-transform: uppercase;
  font-size: 15px;
  font-family: 'Open Sans';
`

const Padding = styled.div`
  padding: 0 ${DefaultMargins.s};
`

export default React.memo(function AttendanceGroupSelectorPage() {
  const { i18n } = useTranslation()
  const { unitId } = useParams<{ unitId: string }>()

  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  const loading = isLoading(attendanceResponse)

  useEffect(() => {
    void getDaycareAttendances(unitId).then(setAttendanceResponse)
  }, [])

  return (
    <Fragment>
      <MetaTags>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
      </MetaTags>

      {loading && <Loader />}
      {isFailure(attendanceResponse) && <div>{i18n.common.loadingFailed}</div>}
      {isSuccess(attendanceResponse) && (
        <Fragment>
          <Title size={1} centered smaller bold>
            {attendanceResponse.data.unit.name}
          </Title>
          <Padding>
            <ContentArea paddingHorozontal={'s'} opaque>
              <AllCapsTitle size={2} centered bold>
                {i18n.attendances.chooseGroup}
              </AllCapsTitle>
              <Flex>
                <a href={`attendance/all/coming`}>
                  <CustomButton primary text={i18n.common.all} />
                </a>
                {attendanceResponse.data.unit.groups.map((group: Group) => (
                  <a key={group.id} href={`attendance/${group.id}/coming`}>
                    <CustomButton primary key={group.id} text={group.name} />
                  </a>
                ))}
              </Flex>
            </ContentArea>
          </Padding>
        </Fragment>
      )}
    </Fragment>
  )
})
