// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { combine } from 'lib-common/api'
import { isValidTime } from 'lib-common/date'
import { AbsenceType } from 'lib-common/generated/api-types/absence'
import {
  AttendanceChild,
  AttendanceTimes,
  ChildAttendanceStatusResponse
} from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import {
  queryOrDefault,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useRequiredParams from 'lib-common/useRequiredParams'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Button from 'lib-components/atoms/buttons/Button'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import { farStickyNote } from 'lib-icons'

import { renderResult } from '../../async-rendering'
import { groupNotesQuery } from '../../child-notes/queries'
import ChildNameBackButton from '../../common/ChildNameBackButton'
import {
  Actions,
  CustomTitle,
  DailyNotes,
  TimeWrapper
} from '../../common/components'
import { Translations, useTranslation } from '../../common/i18n'
import { TallContentArea } from '../../pairing/components'
import { formatCategory } from '../../types'
import DailyNote from '../DailyNote'
import {
  attendanceStatusesQuery,
  childExpectedAbsencesOnDepartureQuery,
  childrenQuery,
  createDepartureMutation
} from '../queries'
import { childAttendanceStatus, useChild } from '../utils'

import AbsenceSelector, { AbsenceTypeWithNoAbsence } from './AbsenceSelector'

const AbsenceTitle = styled(Title)`
  font-size: 18px;
  font-style: normal;
  font-weight: ${fontWeights.medium};
  line-height: 27px;
  letter-spacing: 0;
  text-align: left;
  margin-top: 0;
  margin-bottom: 0;
`

function validateTime(
  i18n: Translations,
  time: string,
  attendances: AttendanceTimes[]
): string | undefined {
  if (!isValidTime(time)) {
    return i18n.attendances.timeError
  }

  const arrived = attendances.find((a) => a.departed === null)?.arrived

  if (!arrived) return undefined

  try {
    const parsedTime = LocalTime.parse(time)
    const parsedTimestamp =
      LocalDate.todayInSystemTz().toHelsinkiDateTime(parsedTime)
    if (!parsedTimestamp.isAfter(arrived)) {
      return `${i18n.attendances.arrived} ${arrived.toLocalTime().format()}`
    }
  } catch (e) {
    return i18n.attendances.timeError
  }

  return undefined
}

const MarkDepartedInner = React.memo(function MarkDepartedWithChild({
  unitId,
  child,
  attendanceStatus
}: {
  unitId: UUID
  child: AttendanceChild
  attendanceStatus: ChildAttendanceStatusResponse
}) {
  const childId = child.id
  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const groupId = child.groupId
  const groupNotes = useQueryResult(
    queryOrDefault(groupNotesQuery, [])(groupId)
  )

  const [time, setTime] = useState(() =>
    HelsinkiDateTime.now().toLocalTime().format()
  )

  const [selectedAbsenceTypeNonbillable, setSelectedAbsenceTypeNonbillable] =
    useState<AbsenceTypeWithNoAbsence | undefined>(
      attendanceStatus.absences.find((a) => a.category === 'NONBILLABLE')?.type
    )
  const [selectedAbsenceTypeBillable, setSelectedAbsenceTypeBillable] =
    useState<AbsenceTypeWithNoAbsence | undefined>(
      attendanceStatus.absences.find((a) => a.category === 'BILLABLE')?.type
    )

  const timeError = useMemo(
    () => validateTime(i18n, time, attendanceStatus.attendances),
    [i18n, time, attendanceStatus]
  )

  const expectedAbsences = useQueryResult(
    queryOrDefault(
      (departed: LocalTime) =>
        childExpectedAbsencesOnDepartureQuery({ unitId, childId, departed }),
      null
    )(timeError === undefined ? LocalTime.tryParse(time) : null)
  )

  const { mutateAsync: createDeparture } = useMutationResult(
    createDepartureMutation
  )

  const formIsValid =
    !timeError &&
    expectedAbsences.isSuccess &&
    (expectedAbsences.value?.includes('NONBILLABLE') !== true ||
      selectedAbsenceTypeNonbillable !== undefined) &&
    (expectedAbsences.value?.includes('BILLABLE') !== true ||
      selectedAbsenceTypeBillable !== undefined)

  const basicAbsenceTypes: AbsenceType[] = [
    'OTHER_ABSENCE',
    'SICKLEAVE',
    'UNKNOWN_ABSENCE'
  ]

  return (
    <TallContentArea
      opaque={false}
      paddingHorizontal="zero"
      paddingVertical="zero"
    >
      {renderResult(groupNotes, (groupNotes) => (
        <>
          <div>
            <ChildNameBackButton child={child} onClick={() => navigate(-1)} />
          </div>

          <ContentArea
            shadow
            opaque={true}
            paddingHorizontal="s"
            paddingVertical="m"
          >
            <TimeWrapper>
              <CustomTitle>{i18n.attendances.actions.markDeparted}</CustomTitle>
              <TimeInput
                onChange={setTime}
                value={time}
                data-qa="set-time"
                info={
                  timeError ? { text: timeError, status: 'warning' } : undefined
                }
              />
            </TimeWrapper>

            <Gap size="xs" />

            {renderResult(expectedAbsences, (expectedAbsences) =>
              expectedAbsences && expectedAbsences.length > 0 ? (
                <FixedSpaceColumn>
                  <AbsenceTitle size={2}>
                    {i18n.attendances.absenceTitle}
                  </AbsenceTitle>
                  {expectedAbsences.includes('NONBILLABLE') && (
                    <FixedSpaceColumn
                      spacing="xs"
                      data-qa="absence-NONBILLABLE"
                    >
                      <div>
                        {formatCategory(
                          'NONBILLABLE',
                          child.placementType,
                          i18n
                        )}
                      </div>
                      <AbsenceSelector
                        absenceTypes={[
                          ...basicAbsenceTypes,
                          ...attendanceStatus.absences
                            .filter(
                              (a) =>
                                a.category === 'NONBILLABLE' &&
                                !basicAbsenceTypes.includes(a.type)
                            )
                            .map((a) => a.type),
                          ...(featureFlags.noAbsenceType
                            ? (['NO_ABSENCE'] as const)
                            : ([] as const))
                        ]}
                        selectedAbsenceType={selectedAbsenceTypeNonbillable}
                        setSelectedAbsenceType={
                          setSelectedAbsenceTypeNonbillable
                        }
                      />
                    </FixedSpaceColumn>
                  )}
                  {expectedAbsences.includes('BILLABLE') && (
                    <FixedSpaceColumn spacing="xs" data-qa="absence-BILLABLE">
                      <div>
                        {formatCategory('BILLABLE', child.placementType, i18n)}
                      </div>
                      <AbsenceSelector
                        absenceTypes={[
                          ...basicAbsenceTypes,
                          ...attendanceStatus.absences
                            .filter(
                              (a) =>
                                a.category === 'BILLABLE' &&
                                !basicAbsenceTypes.includes(a.type)
                            )
                            .map((a) => a.type),
                          ...(featureFlags.noAbsenceType
                            ? (['NO_ABSENCE'] as const)
                            : ([] as const))
                        ]}
                        selectedAbsenceType={selectedAbsenceTypeBillable}
                        setSelectedAbsenceType={setSelectedAbsenceTypeBillable}
                      />
                    </FixedSpaceColumn>
                  )}
                </FixedSpaceColumn>
              ) : null
            )}

            <Gap size="s" />

            <Actions>
              <FixedSpaceRow fullWidth>
                <Button
                  text={i18n.common.cancel}
                  onClick={() => navigate(-1)}
                />
                <AsyncButton
                  primary
                  text={i18n.common.confirm}
                  disabled={!formIsValid}
                  onClick={() => {
                    if (!expectedAbsences.isSuccess) return undefined

                    return createDeparture({
                      unitId,
                      childId,
                      absenceTypeNonbillable:
                        expectedAbsences.value?.includes('NONBILLABLE') &&
                        selectedAbsenceTypeNonbillable !== 'NO_ABSENCE'
                          ? selectedAbsenceTypeNonbillable ?? null
                          : null,
                      absenceTypeBillable:
                        expectedAbsences.value?.includes('BILLABLE') &&
                        selectedAbsenceTypeBillable !== 'NO_ABSENCE'
                          ? selectedAbsenceTypeBillable ?? null
                          : null,
                      departed: LocalTime.parse(time)
                    })
                  }}
                  onSuccess={() => {
                    const absenceMarked = expectedAbsences
                      .map((exp) => exp && exp.length > 0)
                      .getOrElse(false)
                    navigate(absenceMarked ? -1 : -2)
                  }}
                  data-qa="mark-departed-btn"
                />
              </FixedSpaceRow>
            </Actions>
          </ContentArea>

          <Gap size="s" />

          <ContentArea
            shadow
            opaque={true}
            paddingHorizontal="s"
            paddingVertical="s"
            blue
          >
            <DailyNotes>
              <span>
                <RoundIcon
                  content={farStickyNote}
                  color={colors.main.m1}
                  size="m"
                />
              </span>
              <DailyNote
                child={child ? child : undefined}
                groupNotes={groupNotes}
              />
            </DailyNotes>
          </ContentArea>
        </>
      ))}
    </TallContentArea>
  )
})

export default React.memo(function MarkDeparted() {
  const { childId, unitId } = useRequiredParams('childId', 'unitId')
  const child = useChild(useQueryResult(childrenQuery(unitId)), childId)
  const attendanceStatuses = useQueryResult(attendanceStatusesQuery(unitId))

  return renderResult(
    combine(child, attendanceStatuses),
    ([child, attendanceStatuses]) => (
      <MarkDepartedInner
        unitId={unitId}
        child={child}
        attendanceStatus={childAttendanceStatus(child, attendanceStatuses)}
      />
    )
  )
})
