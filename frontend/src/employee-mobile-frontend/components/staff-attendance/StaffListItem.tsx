// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import RoundIcon from 'lib-components/atoms/RoundIcon'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { farUser } from 'lib-icons'
import React from 'react'
import { Link, useParams } from 'react-router-dom'
import styled from 'styled-components'
import { Staff } from './staff'

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
    p.present ? colors.accents.successGreen : colors.accents.turquoise};
  border-radius: 50%;
  box-shadow: ${(p) =>
    p.present
      ? `0 0 0 2px ${colors.accents.successGreen}`
      : `0 0 0 2px ${colors.accents.turquoise}`};
  border: 2px solid ${colors.greyscale.white};
`

export default React.memo(function StaffListItem({
  name,
  id,
  present,
  type
}: Staff) {
  const { unitId, groupId } = useParams<{
    unitId: string
    groupId: string
  }>()

  const base = `/units/${unitId}/groups/${groupId}/staff-attendance`
  const link = type === 'external' ? `${base}/external/${id}` : `${base}/${id}`

  return (
    <StaffBox data-qa={`staff-${id}`} key={id}>
      <AttendanceLinkBox to={link}>
        <IconBox present={present}>
          <RoundIcon
            content={farUser}
            color={
              present ? colors.accents.successGreen : colors.accents.turquoise
            }
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
