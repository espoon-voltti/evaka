// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import RoundIcon from 'lib-components/atoms/RoundIcon'
import { Bold } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { farUser } from 'lib-icons'

import { routes } from '../App'
import { UnitOrGroup } from '../common/unit-or-group'

import { Staff } from './utils'

const StaffBox = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
  color: ${colors.grayscale.g100};
  padding: ${defaultMargins.xs} ${defaultMargins.s};
  border-radius: 2px;
  background-color: ${colors.grayscale.g0};

  &:after {
    content: '';
    width: calc(100% - ${defaultMargins.s});
    background: ${colors.grayscale.g15};
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

export const IconBox = styled.div<{ present: boolean }>`
  background-color: ${(p) =>
    p.present ? colors.status.success : colors.accents.a6turquoise};
  border-radius: 50%;
  box-shadow: ${(p) =>
    p.present
      ? `0 0 0 2px ${colors.status.success}`
      : `0 0 0 2px ${colors.accents.a6turquoise}`};
  border: 2px solid ${colors.grayscale.g0};
`

export default React.memo(function StaffListItem({
  unitOrGroup,
  name,
  id,
  present,
  type
}: Staff & { unitOrGroup: UnitOrGroup }) {
  const link = (
    type === 'external'
      ? routes.externalStaffAttendance(unitOrGroup, id)
      : routes.staffAttendance(unitOrGroup, id)
  ).value

  return (
    <StaffBox data-qa={`staff-${id}`} key={id}>
      <AttendanceLinkBox to={link} data-qa="staff-link">
        <IconBox present={present}>
          <RoundIcon
            content={farUser}
            color={present ? colors.status.success : colors.accents.a6turquoise}
            size="XL"
          />
        </IconBox>
        <StaffBoxInfo>
          <Bold data-qa="employee-name">{name}</Bold>
        </StaffBoxInfo>
      </AttendanceLinkBox>
    </StaffBox>
  )
})
