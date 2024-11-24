// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'
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
import { formatPreferredName } from 'lib-common/names'
import {
  constantQuery,
  useMutationResult,
  useQueryResult
} from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { fontWeights, H4 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'
import { faArrowLeft } from 'lib-icons'

import { routes } from '../../App'
import { renderResult } from '../../async-rendering'
import ChildNameBackButton from '../../common/ChildNameBackButton'
import {
  Actions,
  BackButtonInline,
  CustomTitle,
  TimeWrapper
} from '../../common/components'
import { Translations, useTranslation } from '../../common/i18n'
import { TallContentArea } from '../../pairing/components'
import { formatCategory } from '../../types'
import ChildNotesSummary from '../ChildNotesSummary'
import {
  attendanceStatusesQuery,
  childrenQuery,
  createDeparturesMutation,
  expectedAbsencesOnDeparturesQuery
} from '../queries'
import { childAttendanceStatus } from '../utils'

import AbsenceSelector, { AbsenceTypeWithNoAbsence } from './AbsenceSelector'

const AbsenceTitle = styled(Title)`
  font-size: 18px;
  font-style: normal;
  font-weight: ${fontWeights.medium};
  line-height: 27px;
  letter-spacing: 0;
  text-align: left;
  margin-top: 0;
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

type ChildAbsenceState = {
  childId: UUID
  nonBillable: AbsenceTypeWithNoAbsence | undefined
  billable: AbsenceTypeWithNoAbsence | undefined
}

const MarkDepartedInner = React.memo(function MarkDepartedWithChild({
  unitId,
  childList,
  multiselect
}: {
  unitId: UUID
  childList: {
    child: AttendanceChild
    attendanceStatus: ChildAttendanceStatusResponse
  }[]
  multiselect: boolean
}) {
  const childIds = childList.map(({ child }) => child.id)

  const navigate = useNavigate()
  const { i18n } = useTranslation()

  const [time, setTime] = useState(() =>
    HelsinkiDateTime.now().toLocalTime().format()
  )

  const [childAbsenceStates, setChildAbsenceStates] = useState<
    ChildAbsenceState[]
  >(
    childList.map(({ child, attendanceStatus }) => ({
      childId: child.id,
      nonBillable: attendanceStatus.absences.find(
        (a) => a.category === 'NONBILLABLE'
      )?.type,
      billable: attendanceStatus.absences.find((a) => a.category === 'BILLABLE')
        ?.type
    }))
  )

  const setChildNonBillableAbsenceType = (
    childId: UUID,
    absenceType: AbsenceTypeWithNoAbsence | undefined
  ) => {
    setChildAbsenceStates((prev) =>
      prev.map((s) =>
        s.childId === childId ? { ...s, nonBillable: absenceType } : s
      )
    )
  }

  const setChildBillableAbsenceType = (
    childId: UUID,
    absenceType: AbsenceTypeWithNoAbsence | undefined
  ) => {
    setChildAbsenceStates((prev) =>
      prev.map((s) =>
        s.childId === childId ? { ...s, billable: absenceType } : s
      )
    )
  }

  const timeError = useMemo(() => {
    for (const child of childList) {
      const error = validateTime(i18n, time, child.attendanceStatus.attendances)
      if (error) return error
    }
    return undefined
  }, [i18n, time, childList])

  const expectedAbsences = useQueryResult(
    timeError === undefined
      ? expectedAbsencesOnDeparturesQuery({
          unitId,
          body: { departed: LocalTime.parse(time), childIds }
        })
      : constantQuery(null)
  )

  const { mutateAsync: createDepartures } = useMutationResult(
    createDeparturesMutation
  )

  const formIsValid =
    !timeError &&
    expectedAbsences.isSuccess &&
    childIds.every((childId) => {
      const childExpectedAbsences =
        expectedAbsences.value?.categoriesByChild[childId]
      const childAbsenceState = childAbsenceStates.find(
        (s) => s.childId === childId
      )

      return (
        !!childAbsenceState &&
        (childExpectedAbsences?.includes('NONBILLABLE') !== true ||
          childAbsenceState.nonBillable !== undefined) &&
        (childExpectedAbsences?.includes('BILLABLE') !== true ||
          childAbsenceState.billable !== undefined)
      )
    })

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
      <div>
        {childList.length === 1 ? (
          <ChildNameBackButton
            child={childList[0].child}
            onClick={() => navigate(-1)}
          />
        ) : (
          <BackButtonInline
            icon={faArrowLeft}
            text={i18n.common.return}
            onClick={() => navigate(-1)}
          />
        )}
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

        {renderResult(expectedAbsences, (expectedAbsences) => {
          if (
            !expectedAbsences ||
            !Object.values(expectedAbsences.categoriesByChild).some(
              (categories) => categories && categories.length > 0
            )
          )
            return null

          return (
            <FixedSpaceColumn>
              <AbsenceTitle size={2}>
                {i18n.attendances.absenceTitle}
              </AbsenceTitle>

              {Object.entries(expectedAbsences.categoriesByChild).map(
                ([childId, categories]) => {
                  if (!categories || categories.length === 0) return null
                  const childData = childList.find(
                    ({ child }) => child.id === childId
                  )
                  if (!childData) return null

                  const { child, attendanceStatus } = childData

                  return (
                    <FixedSpaceColumn key={childId}>
                      <H4
                        noMargin
                      >{`${formatPreferredName({ firstName: child.firstName, preferredName: child.preferredName })} ${child.lastName}`}</H4>
                      <FixedSpaceColumn>
                        {categories.includes('NONBILLABLE') && (
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
                              selectedAbsenceType={
                                childAbsenceStates.find(
                                  (s) => s.childId === childId
                                )?.nonBillable
                              }
                              setSelectedAbsenceType={(type) =>
                                setChildNonBillableAbsenceType(childId, type)
                              }
                            />
                          </FixedSpaceColumn>
                        )}
                        {categories.includes('BILLABLE') && (
                          <FixedSpaceColumn
                            spacing="xs"
                            data-qa="absence-BILLABLE"
                          >
                            <div>
                              {formatCategory(
                                'BILLABLE',
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
                                      a.category === 'BILLABLE' &&
                                      !basicAbsenceTypes.includes(a.type)
                                  )
                                  .map((a) => a.type),
                                ...(featureFlags.noAbsenceType
                                  ? (['NO_ABSENCE'] as const)
                                  : ([] as const))
                              ]}
                              selectedAbsenceType={
                                childAbsenceStates.find(
                                  (s) => s.childId === childId
                                )?.billable
                              }
                              setSelectedAbsenceType={(type) =>
                                setChildBillableAbsenceType(childId, type)
                              }
                            />
                          </FixedSpaceColumn>
                        )}
                      </FixedSpaceColumn>
                    </FixedSpaceColumn>
                  )
                }
              )}
            </FixedSpaceColumn>
          )
        })}

        <Gap size="s" />

        <Actions>
          <FixedSpaceRow fullWidth>
            <LegacyButton
              text={i18n.common.cancel}
              onClick={() => navigate(-1)}
            />
            <AsyncButton
              primary
              text={i18n.common.confirm}
              disabled={!formIsValid}
              onClick={() => {
                if (!expectedAbsences.isSuccess) return undefined

                return createDepartures({
                  unitId,
                  body: {
                    departed: LocalTime.parse(time),
                    departures: childIds.map((childId) => {
                      const expectedCategories =
                        expectedAbsences.value?.categoriesByChild[childId] ?? []
                      const absences = childAbsenceStates.find(
                        (s) => s.childId === childId
                      )

                      return {
                        childId,
                        absenceTypeNonbillable:
                          expectedCategories?.includes('NONBILLABLE') &&
                          absences?.nonBillable !== 'NO_ABSENCE'
                            ? absences?.nonBillable ?? null
                            : null,
                        absenceTypeBillable:
                          expectedCategories.includes('BILLABLE') &&
                          absences?.billable !== 'NO_ABSENCE'
                            ? absences?.billable ?? null
                            : null
                      }
                    })
                  }
                })
              }}
              onSuccess={() => {
                navigate(multiselect ? -1 : -2)
              }}
              data-qa="mark-departed-btn"
            />
          </FixedSpaceRow>
        </Actions>
      </ContentArea>
      <Gap size="s" />
      <FixedSpaceColumn>
        {childList.map(({ child }) => (
          <ChildNotesSummary child={child} key={child.id} />
        ))}
      </FixedSpaceColumn>
    </TallContentArea>
  )
})

const MarkDepartedWithParams = React.memo(function MarkPresentWithParams({
  unitId,
  childIds,
  multiselect
}: {
  unitId: UUID
  childIds: UUID[]
  multiselect: boolean
}) {
  const children = useQueryResult(childrenQuery(unitId)).map((children) =>
    children.filter((child) => childIds.includes(child.id))
  )

  const attendanceStatuses = useQueryResult(attendanceStatusesQuery({ unitId }))

  return renderResult(
    combine(children, attendanceStatuses),
    ([children, attendanceStatuses]) => (
      <MarkDepartedInner
        unitId={unitId}
        childList={children.map((child) => ({
          child,
          attendanceStatus: childAttendanceStatus(child, attendanceStatuses)
        }))}
        multiselect={multiselect}
      />
    )
  )
})

export default React.memo(function MarkDeparted({ unitId }: { unitId: UUID }) {
  const [searchParams] = useSearchParams()
  const children = searchParams.get('children')
  const multiselect = searchParams.get('multiselect') === 'true'
  if (children === null)
    return <Navigate replace to={routes.unit(unitId).value} />
  const childIds = children.split(',').filter((id) => id.length > 0)
  if (childIds.length === 0)
    return <Navigate replace to={routes.unit(unitId).value} />

  return (
    <MarkDepartedWithParams
      unitId={unitId}
      childIds={childIds}
      multiselect={multiselect}
    />
  )
})
