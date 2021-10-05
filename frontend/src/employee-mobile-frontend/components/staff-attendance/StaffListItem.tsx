// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { fontWeights } from 'lib-components/typography'

import RoundIcon from 'lib-components/atoms/RoundIcon'
import colors from 'lib-customizations/common'
import { defaultMargins } from 'lib-components/white-space'
import { farUser } from 'lib-icons'
import { Link, useParams } from 'react-router-dom'
import { StaffMember } from 'lib-common/generated/api-types/attendance'

const StaffBox = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
  color: ${colors.greyscale.darkest};
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-radius: 2px;
  background-color: ${colors.greyscale.white};

  &:after {
    content: '';
    width: calc(100% - ${defaultMargins.s});
    background: ${colors.greyscale.lighter};
    height: 1px;
    display: block;
    margin: 8px 0 -8px;
  }
`

const AttendanceLinkBox = styled(Link)`
  align-items: center;
  display: flex;
  width: 100%;
`

const imageHeight = '56px'

const StaffBoxInfo = styled.div`
  margin-left: 24px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  min-height: ${imageHeight};
`

const Bold = styled.div`
  font-weight: ${fontWeights.semibold};

  h2,
  h3 {
    font-weight: ${fontWeights.medium};
  }
`

export const IconBox = styled.div<{ present: boolean }>`
  background-color: ${(p) =>
    p.present ? colors.accents.green : colors.accents.water};
  border-radius: 50%;
  box-shadow: ${(p) =>
    p.present
      ? `0 0 0 2px ${colors.accents.green}`
      : `0 0 0 2px ${colors.accents.water}`};
  border: 2px solid ${colors.greyscale.white};
`

const DetailsRow = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  color: ${colors.greyscale.dark};
  font-size: 0.875em;
  width: 100%;
`

interface StaffListItemProps {
  staffMember: StaffMember
}

export default React.memo(function StaffListItem({
  staffMember: { employeeId, firstName, lastName, present }
}: StaffListItemProps) {
  const { unitId, groupId } = useParams<{
    unitId: string
    groupId: string
  }>()

  return (
    <StaffBox data-qa={`staff-${employeeId}`}>
      <AttendanceLinkBox
        to={`/units/${unitId}/groups/${groupId}/staff-attendance/${employeeId}`}
      >
        <IconBox present={!!present}>
          <RoundIcon
            content={farUser}
            color={present ? colors.accents.green : colors.accents.water}
            size="XL"
          />
        </IconBox>
        <StaffBoxInfo>
          <Bold data-qa={'employee-name'}>
            {firstName} {lastName}
          </Bold>
          <DetailsRow>Jotain tietoa?</DetailsRow>
        </StaffBoxInfo>
      </AttendanceLinkBox>
    </StaffBox>
  )
})
