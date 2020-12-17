// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import styled from 'styled-components'
import MetaTags from 'react-meta-tags'

import Loader from '@evaka/lib-components/src/atoms/Loader'
import Title from '@evaka/lib-components/src/atoms/Title'
import { ContentArea } from '~components/shared/layout/Container'
import { defaultMargins } from '@evaka/lib-components/src/white-space'
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
  padding: 0 ${defaultMargins.s};
`

export default React.memo(function AttendanceGroupSelectorPage() {
  const { i18n } = useTranslation()
  const { unitId } = useParams<{ unitId: string }>()

  const { attendanceResponse, setAttendanceResponse } = useContext(
    AttendanceUIContext
  )

  useEffect(() => {
    void getDaycareAttendances(unitId).then(setAttendanceResponse)
  }, [])

  return (
    <Fragment>
      <MetaTags>
        <meta name="viewport" content="width=device-width, initial-scale=1" />
      </MetaTags>

      {attendanceResponse.isLoading && <Loader />}
      {attendanceResponse.isFailure && <div>{i18n.common.loadingFailed}</div>}
      {attendanceResponse.isSuccess && (
        <Fragment>
          <Title data-qa="unit-name" size={1} centered smaller bold>
            {attendanceResponse.value.unit.name}
          </Title>
          <Padding>
            <ContentArea paddingHorozontal={'s'} opaque>
              <AllCapsTitle size={2} centered bold>
                {i18n.attendances.chooseGroup}
              </AllCapsTitle>
              <Flex>
                <Link to={`attendance/all/coming`}>
                  <CustomButton primary text={i18n.common.all} />
                </Link>
                {attendanceResponse.value.unit.groups.map((group: Group) => (
                  <Link key={group.id} to={`attendance/${group.id}/coming`}>
                    <CustomButton primary key={group.id} text={group.name} />
                  </Link>
                ))}
              </Flex>
            </ContentArea>
          </Padding>
        </Fragment>
      )}
    </Fragment>
  )
})
