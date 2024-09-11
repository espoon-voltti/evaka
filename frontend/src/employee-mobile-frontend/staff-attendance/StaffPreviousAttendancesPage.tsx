// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useNavigate } from 'react-router-dom'

import { combine } from 'lib-common/api'
import {
  GroupInfo,
  StaffMemberAttendance
} from 'lib-common/generated/api-types/attendance'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import useRouteParams from 'lib-common/useRouteParams'
import { Button } from 'lib-components/atoms/buttons/Button'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H3, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPen } from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { useTranslation } from '../common/i18n'
import { UnitOrGroup } from '../common/unit-or-group'
import { unitInfoQuery } from '../units/queries'

import { EmployeeCardBackground } from './components/EmployeeCardBackground'
import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import { staffAttendancesByEmployeeQuery } from './queries'
import { toStaff } from './utils'

export default React.memo(function StaffPreviousAttendancesPage({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const { employeeId } = useRouteParams(['employeeId'])
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const unitId = unitOrGroup.unitId

  const unitResponse = useQueryResult(unitInfoQuery({ unitId }))
  const attendanceResponse = useQueryResult(
    staffAttendancesByEmployeeQuery({
      unitId,
      employeeId,
      from: LocalDate.todayInSystemTz().subDays(7),
      to: LocalDate.todayInSystemTz()
    })
  )

  return renderResult(
    combine(unitResponse, attendanceResponse),
    ([unitInfo, staffMember]) => {
      return (
        <StaffMemberPageContainer
          back={routes.staffAttendances(unitOrGroup, 'present').value}
        >
          <EmployeeCardBackground staff={toStaff(staffMember)} />
          <Gap />
          <FixedSpaceColumn alignItems="center">
            <H3>{i18n.attendances.staff.previousDays}</H3>
            <FixedSpaceColumn spacing="m">
              {[0, 1, 2, 3, 4, 5, 6, 7].map((sinceDays) => {
                const date = LocalDate.todayInHelsinkiTz().subDays(sinceDays)
                return (
                  <DateAttendances
                    key={`attendances-${date.formatIso()}`}
                    date={date}
                    attendances={staffMember.attendances.filter((a) =>
                      a.arrived.toLocalDate().isEqual(date)
                    )}
                    groups={unitInfo.groups}
                    onEdit={() =>
                      navigate(
                        routes.staffAttendanceEdit(
                          unitOrGroup,
                          employeeId,
                          date
                        ).value
                      )
                    }
                  />
                )
              })}
            </FixedSpaceColumn>
          </FixedSpaceColumn>
        </StaffMemberPageContainer>
      )
    }
  )
})

const DateAttendances = React.memo(function DateAttendances({
  date,
  attendances,
  groups,
  onEdit
}: {
  date: LocalDate
  attendances: StaffMemberAttendance[]
  groups: GroupInfo[]
  onEdit: () => void
}) {
  const { i18n, lang } = useTranslation()

  return (
    <FixedSpaceColumn spacing="xs" alignItems="flex-start">
      <H4 noMargin>{date.format('EEEEEE dd.MM.yyyy', lang)}</H4>
      {attendances.map((a) => (
        <FixedSpaceRow key={a.id}>
          <div>
            {a.groupId
              ? groups.find((g) => g.id === a.groupId)?.name
              : i18n.attendances.staffTypes[a.type]}
          </div>
          <div>
            {a.arrived.toLocalTime().format()} -{' '}
            {a.departed?.toLocalTime()?.format()}
          </div>
        </FixedSpaceRow>
      ))}
      {attendances.length === 0 && <div>{i18n.attendances.staff.absent}</div>}
      <Button
        appearance="inline"
        text={i18n.common.edit}
        icon={faPen}
        onClick={onEdit}
      />
    </FixedSpaceColumn>
  )
})
