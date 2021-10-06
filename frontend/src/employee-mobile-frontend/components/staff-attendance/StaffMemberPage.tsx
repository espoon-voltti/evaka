// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import styled from 'styled-components'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { faArrowLeft, farUser } from 'lib-icons'
import { fontWeights } from 'lib-components/typography'
import { StaticChip } from 'lib-components/atoms/Chip'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { renderResult } from '../async-rendering'
import {
  BackButton,
  TallContentArea,
  WideLinkButton
} from '../mobile/components'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { IconBox } from './StaffListItem'
import { StaffAttendanceContext } from '../../state/staff-attendance'

const EmployeeStatus = styled.div`
  color: ${colors.greyscale.medium};
  top: 10px;
  position: relative;
`

const CustomTitle = styled.h2`
  font-family: Montserrat, 'Arial', sans-serif;
  font-style: normal;
  font-weight: ${fontWeights.semibold};
  font-size: 20px;
  line-height: 30px;
  margin-top: 0;
  color: ${colors.blues.dark};
  text-align: center;
  margin-bottom: ${defaultMargins.xs};
`

const Zindex = styled.div`
  z-index: 1;
  margin-left: -8%;
  margin-right: -8%;
`

const EmployeeBackground = styled.div<{ present: boolean }>`
  background: ${(p) => getBackgroundColorByStatus(p.present)};
  display: flex;
  flex-direction: column;
  align-items: center;
  border-radius: 0 0 50% 50%;
  padding-top: ${defaultMargins.s};
`

const BackButtonMargin = styled(BackButton)`
  margin-left: 8px;
  margin-top: 8px;
  z-index: 2;
`

export const TallContentAreaNoOverflow = styled(TallContentArea)`
  overflow-x: hidden;
`

const Center = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  max-width: 100vw;
`

const Shadow = styled.div`
  box-shadow: 0 4px 4px 0 ${colors.greyscale.lighter};
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: calc(100vh - 74px);
`

export default React.memo(function AttendanceChildPage() {
  const history = useHistory()

  const { unitId, groupId, employeeId } = useParams<{
    unitId: string
    groupId: string
    employeeId: string
  }>()

  const { staffAttendanceResponse, reloadStaffAttendance } = useContext(
    StaffAttendanceContext
  )
  useEffect(reloadStaffAttendance, [reloadStaffAttendance])

  const staffMember = staffAttendanceResponse.map((res) =>
    res.staff.find((s) => s.employeeId === employeeId)
  )

  return (
    <TallContentAreaNoOverflow
      opaque
      paddingHorizontal={'0px'}
      paddingVertical={'0px'}
      shadow
    >
      <BackButtonMargin
        onClick={() => history.goBack()}
        icon={faArrowLeft}
        data-qa="back-btn"
      />
      {renderResult(staffMember, (staffMember) =>
        staffMember === undefined ? (
          <ErrorSegment title={'Työntekijää ei löytynyt'} />
        ) : (
          <Shadow>
            <Zindex>
              <EmployeeBackground present={!!staffMember.present}>
                <Center>
                  <IconBox present={!!staffMember.present}>
                    <RoundIcon
                      content={farUser}
                      color={getColorByStatus(!!staffMember.present)}
                      size="XXL"
                    />
                  </IconBox>

                  <Gap size={'s'} />

                  <CustomTitle data-qa={'employee-name'}>
                    {staffMember.firstName} {staffMember.lastName}
                  </CustomTitle>

                  <EmployeeStatus>
                    <StaticChip
                      color={getColorByStatus(!!staffMember.present)}
                      data-qa="employee-status"
                    >
                      {staffMember.present ? 'Läsnä' : 'Poissa'}
                    </StaticChip>
                  </EmployeeStatus>
                </Center>
              </EmployeeBackground>
            </Zindex>
            {staffMember.present ? (
              <WideLinkButton
                $primary
                data-qa="mark-departed-link"
                to={`/units/${unitId}/groups/${groupId}/staff-attendance/${staffMember.employeeId}/mark-departed`}
              >
                Kirjaudu poissaolevaksi
              </WideLinkButton>
            ) : (
              <WideLinkButton
                $primary
                data-qa="mark-arrived-link"
                to={`/units/${unitId}/groups/${groupId}/staff-attendance/${staffMember.employeeId}/mark-arrived`}
              >
                Kirjaudu läsnäolevaksi
              </WideLinkButton>
            )}
          </Shadow>
        )
      )}
    </TallContentAreaNoOverflow>
  )
})

function getColorByStatus(present: boolean) {
  return present ? colors.accents.green : colors.accents.water
}

function getBackgroundColorByStatus(present: boolean) {
  return present ? '#EEF4B3' : '#E2ECF2'
}
