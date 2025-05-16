// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled from 'styled-components'

import type { EmployeeId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H3, LabelLike } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { useTranslation } from '../common/i18n'
import type { UnitOrGroup } from '../common/unit-or-group'

import { EmployeeCardBackground } from './components/EmployeeCardBackground'
import { StaffMemberPageContainer } from './components/StaffMemberPageContainer'
import { staffAttendancesByEmployeeQuery } from './queries'
import { toStaff } from './utils'

export default React.memo(function StaffMemberPlannedAttendancesPage({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const employeeId = useIdRouteParam<EmployeeId>('employeeId')
  const { i18n } = useTranslation()

  const unitId = unitOrGroup.unitId

  const attendanceResponse = useQueryResult(
    staffAttendancesByEmployeeQuery({
      unitId,
      employeeId,
      from: LocalDate.todayInSystemTz().addDays(1),
      to: LocalDate.todayInSystemTz().addDays(7)
    })
  )

  const days = useMemo(() => {
    const today = LocalDate.todayInHelsinkiTz()
    const startDate = today.addDays(1)
    const endDate = today.addWeeks(1).endOfWeek()
    const result = []
    for (
      let date = startDate;
      date.isEqualOrBefore(endDate);
      date = date.addDays(1)
    ) {
      result.push(date)
    }
    return result
  }, [])

  return renderResult(attendanceResponse, (staffMember) => {
    return (
      <StaffMemberPageContainer
        back={routes.staffAttendancesToday(unitOrGroup, 'present').value}
      >
        <EmployeeCardBackground staff={toStaff(staffMember)} />
        <Gap />
        <DayPlansContainer>
          <ExpandingInfo info={i18n.attendances.staff.staffMemberPlanInfo}>
            <H3 noMargin>{i18n.attendances.staff.nextDays}</H3>
          </ExpandingInfo>
          {staffMember.unitIds.length > 1 && (
            <div>
              <AlertBox
                message={i18n.attendances.staff.staffMemberMultipleUnits}
                thin
              />
            </div>
          )}
          <FixedSpaceColumn spacing="s">
            {days.map((date) => {
              const attendances = staffMember.plannedAttendances
                .filter(
                  (a) =>
                    a.start.toLocalDate().isEqual(date) ||
                    a.end.toLocalDate().isEqual(date)
                )
                .map((a) => ({
                  start: a.start.toLocalDate().isEqual(date)
                    ? a.start.toLocalTime().format()
                    : '→',
                  end: a.end.toLocalDate().isEqual(date)
                    ? a.end.toLocalTime().format()
                    : '→',
                  type: i18n.attendances.staffTypes[a.type],
                  description: a.description
                }))
              return (
                <DayPlan
                  key={date.formatIso()}
                  data-qa={`day-plan-${date.formatIso()}`}
                >
                  <LabelLike>{date.formatExotic('EEEEEE d.M.')}</LabelLike>
                  {attendances.length > 0 ? (
                    <FixedSpaceColumn spacing="xs">
                      {attendances.map((a, i) => (
                        <FixedSpaceRow key={i}>
                          <DayPlanTypeCol>{a.type}</DayPlanTypeCol>
                          <div>
                            <div>
                              {a.start} - {a.end}
                            </div>
                            {a.description ? <i>({a.description})</i> : null}
                          </div>
                        </FixedSpaceRow>
                      ))}
                    </FixedSpaceColumn>
                  ) : (
                    <div>{i18n.attendances.staff.noPlan}</div>
                  )}
                </DayPlan>
              )
            })}
          </FixedSpaceColumn>
        </DayPlansContainer>
      </StaffMemberPageContainer>
    )
  })
})

const DayPlansContainer = styled(FixedSpaceColumn).attrs({
  alignItems: 'stretch'
})`
  padding: 0 32px;
`

const DayPlan = styled(FixedSpaceColumn).attrs({ spacing: 'xxs' })``

const DayPlanTypeCol = styled.div`
  width: 30%;
`
