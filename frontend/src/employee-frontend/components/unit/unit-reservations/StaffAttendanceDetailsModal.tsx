// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import initial from 'lodash/initial'
import last from 'lodash/last'
import orderBy from 'lodash/orderBy'
import uniq from 'lodash/uniq'
import React, { Fragment, useCallback, useId, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'wouter'

import type { Result } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import DateTimeRange from 'lib-common/date-time-range'
import FiniteDateTimeRange from 'lib-common/finite-date-time-range'
import type { ErrorKey } from 'lib-common/form-validation'
import type { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import type { DaycareGroup } from 'lib-common/generated/api-types/daycare'
import type { EmployeeId, GroupId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type { Id } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQueryResult } from 'lib-common/query'
import { presentInGroup } from 'lib-common/staff-attendance'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import Tooltip from 'lib-components/atoms/Tooltip'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import TimeInput from 'lib-components/atoms/form/TimeInput'
import { errorToInputInfo } from 'lib-components/input-info-helper'
import ListGrid from 'lib-components/layout/ListGrid'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import { DatePickerSpacer } from 'lib-components/molecules/date-picker/DateRangePicker'
import {
  ModalCloseButton,
  PlainModal
} from 'lib-components/molecules/modals/BaseModal'
import { fontWeights, H1, H2, H3, LabelLike } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  isStaffAttendanceTypesEnabled,
  staffAttendanceTypes as allowedStaffAttendanceTypes
} from 'lib-customizations/employee'
import {
  faExclamationTriangle,
  faPlus,
  fasExclamationTriangle,
  faTrash
} from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { openAttendanceQuery } from '../queries'

export interface ModalAttendance {
  id: Id<string>
  groupId: GroupId | null
  period: DateTimeRange
  departedAutomatically: boolean
  type: StaffAttendanceType
  occupancyCoefficient: number
}

export interface ModalOtherAttendance {
  unitName: string | null
  groupId: GroupId | null
  period: DateTimeRange
}

export interface ModalPlannedAttendance {
  period: FiniteDateTimeRange
  description: string | null
}

interface Props<T extends { period: DateTimeRange }> {
  date: LocalDate
  name: string
  employeeId?: EmployeeId
  staffOccupancyEffectDefault: boolean
  attendances: ModalAttendance[]
  plannedAttendances: ModalPlannedAttendance[]
  otherAttendances: ModalOtherAttendance[]
  isExternal: boolean
  groups: DaycareGroup[]
  defaultGroupId: GroupId | null
  validate: (a: EditedAttendance[]) => [undefined | T[], ValidationError[]]
  onSave: (body: T[]) => Promise<Result<unknown>> | void
  onSuccess: () => void
  onClose: () => void
  unitId: string
}

export interface EditedAttendance {
  id: Id<string> | null
  groupId: GroupId | null
  arrived: string
  departed: string
  type: StaffAttendanceType
  hasStaffOccupancyEffect: boolean
}

export interface ValidationError {
  arrived?: ErrorKey
  departed?: ErrorKey
  groupId?: ErrorKey
  type?: ErrorKey
  overlappingAttendances?: ModalOtherAttendance[]
}

export default React.memo(
  StaffAttendanceDetailsModal
) as typeof StaffAttendanceDetailsModal

function StaffAttendanceDetailsModal<T extends { period: DateTimeRange }>({
  date,
  name,
  employeeId,
  staffOccupancyEffectDefault,
  attendances,
  plannedAttendances,
  otherAttendances,
  isExternal,
  groups,
  defaultGroupId,
  validate,
  onSave,
  onSuccess,
  onClose,
  unitId
}: Props<T>) {
  const { i18n } = useTranslation()
  const dateId = useId()
  const nameId = useId()

  const sortedAttendances = useMemo(
    () => orderBy(attendances ?? [], ({ period }) => period.start),
    [attendances]
  )

  const continuationAttendance = useMemo(
    () =>
      sortedAttendances.find(
        (a) =>
          a.period.start.toLocalDate().isEqual(date.subDays(1)) &&
          (a.period.end === null || a.period.end.toLocalDate().isEqual(date))
      ),
    [sortedAttendances, date]
  )

  const initialEditState = useMemo(
    () =>
      sortedAttendances
        .filter((a) => a.period.start.toLocalDate().isEqual(date))
        .map(({ id, groupId, period, type, occupancyCoefficient }) => ({
          id,
          groupId,
          arrived: date.isEqual(period.start.toLocalDate())
            ? period.start.toLocalTime().format()
            : '',
          departed: period.end ? period.end.toLocalTime().format() : '',
          type,
          hasStaffOccupancyEffect: occupancyCoefficient > 0
        })),
    [date, sortedAttendances]
  )
  const [{ editState, editStateDate }, setEditState] = useState<{
    editState: EditedAttendance[]
    editStateDate: LocalDate
  }>({ editState: initialEditState, editStateDate: date })

  if (editStateDate !== date) {
    setEditState({
      editState: initialEditState,
      editStateDate: date
    })
  }

  const [overlapSaveError, setOverlapSaveError] = useState(false)

  const updateAttendance = useCallback(
    (index: number, data: Omit<EditedAttendance, 'id'>) => {
      setOverlapSaveError(false)
      setEditState(({ editState, editStateDate }) => ({
        editStateDate,
        editState: editState.map((row, i) =>
          index === i ? { ...row, ...data } : row
        )
      }))
    },
    []
  )
  const removeAttendance = useCallback((index: number) => {
    setOverlapSaveError(false)
    setEditState(({ editState, editStateDate }) => ({
      editStateDate,
      editState: editState.filter((_att, i) => index !== i)
    }))
  }, [])
  const addNewAttendance = useCallback(
    () =>
      setEditState(({ editState, editStateDate }) => ({
        editStateDate,
        editState: [
          ...editState,
          {
            id: null,
            groupId: defaultGroupId,
            arrived: '',
            departed: '',
            type: 'PRESENT',
            hasStaffOccupancyEffect: staffOccupancyEffectDefault ?? true
          }
        ]
      })),
    [defaultGroupId, staffOccupancyEffectDefault]
  )
  const [requestBody, errors] = useMemo(
    () => validate(editState),
    [validate, editState]
  )
  const save = useCallback(() => {
    if (!requestBody) return
    setOverlapSaveError(false)
    return onSave(requestBody)
  }, [onSave, requestBody])
  const onSaveFailure = useCallback(
    ({ errorCode }: { errorCode?: string }) =>
      setOverlapSaveError(errorCode === 'overlappingAttendance'),
    []
  )

  const gaplessAttendances = useMemo(
    () =>
      sortedAttendances
        .reduce<
          {
            period: DateTimeRange
            departedAutomatically: boolean
          }[][]
        >((prev, { period, departedAutomatically }) => {
          if (
            prev.length === 0 ||
            !last(last(prev))?.period.end?.isEqual(period.start)
          ) {
            prev.push([
              {
                period,
                departedAutomatically
              }
            ])
            return prev
          }
          return [
            ...initial(prev),
            [...(last(prev) ?? []), { period, departedAutomatically }]
          ]
        }, [])
        .map((gaplessPeriod) => {
          const lastEntry = last(gaplessPeriod)
          return {
            period: new DateTimeRange(
              gaplessPeriod[0].period.start,
              lastEntry?.period.end ?? null
            ),
            departedAutomatically: lastEntry?.departedAutomatically ?? false
          }
        }),
    [sortedAttendances]
  )

  const getTotalMinutesWithinDate = useCallback(
    (attendances: { period: DateTimeRange }[]) => {
      const wholeDate = FiniteDateTimeRange.ofDate(date)
      const range = date.isToday()
        ? new FiniteDateTimeRange(wholeDate.start, HelsinkiDateTime.now())
        : wholeDate
      return attendances.reduce(
        (total, { period }) =>
          total + (range.intersection(period)?.durationInMinutes() ?? 0),
        0
      )
    },
    [date]
  )

  const totalMinutes = useMemo(
    () =>
      gaplessAttendances.some(
        ({ period }) =>
          !period.start.toLocalDate().isEqual(LocalDate.todayInHelsinkiTz()) &&
          !period.end
      )
        ? 'incalculable'
        : getTotalMinutesWithinDate(gaplessAttendances),
    [gaplessAttendances, getTotalMinutesWithinDate]
  )

  const arrivalWithoutDeparture = useMemo(() => {
    const arrivalWithoutDeparture = gaplessAttendances.find(
      ({ period }) =>
        !period.start.toLocalDate().isEqual(LocalDate.todayInHelsinkiTz()) &&
        !period.end
    )

    return arrivalWithoutDeparture
      ? arrivalWithoutDeparture.period.start.format()
      : undefined
  }, [gaplessAttendances])

  const diffPlannedTotalMinutes = useMemo(
    () =>
      totalMinutes === 'incalculable'
        ? 0
        : totalMinutes - getTotalMinutesWithinDate(plannedAttendances),
    [totalMinutes, plannedAttendances, getTotalMinutesWithinDate]
  )

  const getGapBefore = useCallback(
    (i: number) => {
      if (!requestBody) return undefined

      const previousDeparted =
        i === 0
          ? continuationAttendance?.period.end
          : requestBody[i - 1]?.period.end
      if (!previousDeparted) return undefined
      const gap = FiniteDateTimeRange.tryCreate(
        previousDeparted,
        requestBody[i].period.start
      )
      // gap is so large that it must be a new work day already
      return gap && gap.durationInMinutes() <= 8 * 60 ? gap : undefined
    },
    [requestBody, continuationAttendance]
  )

  const openGroups = useMemo(() => groups.filter(isGroupOpen), [groups])

  const otherUnitAttendancesOnDate = useMemo(() => {
    const day = FiniteDateTimeRange.ofDate(date)
    return orderBy(
      otherAttendances.filter(
        (a) => a.unitName !== null && day.overlaps(a.period)
      ),
      ({ period }) => period.start
    )
  }, [otherAttendances, date])

  const overlappingAttendancesOnOtherDates = useMemo(
    () =>
      orderBy(
        uniq(errors.flatMap((e) => e.overlappingAttendances ?? [])).filter(
          (a) => !otherUnitAttendancesOnDate.includes(a)
        ),
        ({ period }) => period.start
      ),
    [errors, otherUnitAttendancesOnDate]
  )

  const saveDisabled = useMemo(
    () =>
      !requestBody ||
      JSON.stringify(initialEditState) === JSON.stringify(editState),
    [editState, initialEditState, requestBody]
  )

  const openAttendanceResult = useQueryResult(
    employeeId
      ? openAttendanceQuery({ userId: employeeId })
      : constantQuery({ openGroupAttendance: null })
  )
  const openAttendance = openAttendanceResult.isSuccess
    ? openAttendanceResult.value.openGroupAttendance
    : null

  const openAttendanceInAnotherUnit =
    !!openAttendance && openAttendance.unitId !== unitId

  return (
    <PlainModal
      margin="auto"
      data-qa="staff-attendance-details-modal"
      onEscapeKey={onClose}
      aria-labelledby={`${dateId} ${nameId}`}
    >
      <Content>
        <FixedSpaceRow $alignItems="center">
          <H1 $noMargin id={dateId}>
            {date.formatExotic('EEEEEE d.M.yyyy')}
          </H1>
        </FixedSpaceRow>
        <H2 id={nameId}>{name}</H2>
        {!isExternal ? (
          <>
            <H3>{i18n.unit.staffAttendance.summary}</H3>
            <ListGrid $rowGap="s" $labelWidth="auto">
              <LabelLike>{i18n.unit.staffAttendance.plan}</LabelLike>
              <FixedSpaceColumn data-qa="staff-attendance-summary-plan">
                {plannedAttendances.length > 0
                  ? plannedAttendances.map(({ period, description }, i) => (
                      <div key={i}>
                        {formatRange({ date, range: period })}
                        {description ? <i> ({description})</i> : null}
                      </div>
                    ))
                  : '–'}
              </FixedSpaceColumn>
              <LabelLike>{i18n.unit.staffAttendance.realized}</LabelLike>
              <FixedSpaceColumn data-qa="staff-attendance-summary-realized">
                {gaplessAttendances.length > 0
                  ? gaplessAttendances.map(
                      ({ period, departedAutomatically }, i) => (
                        <div key={i}>
                          <div>
                            {formatRange({
                              date,
                              range: period,
                              renderEnd: (value) => (
                                <DepartedTime
                                  $automaticDeparture={departedAutomatically}
                                  data-qa={
                                    departedAutomatically
                                      ? 'departed-automatically-time'
                                      : undefined
                                  }
                                >
                                  {value}
                                </DepartedTime>
                              )
                            })}
                          </div>
                          {departedAutomatically && (
                            <AutomaticDepartureLabel data-qa="departed-automatically-label">
                              <FontAwesomeIcon
                                icon={fasExclamationTriangle}
                                color={colors.status.warning}
                              />
                              <span>
                                {
                                  i18n.unit.staffAttendance
                                    .departedAutomatically
                                }
                              </span>
                            </AutomaticDepartureLabel>
                          )}
                        </div>
                      )
                    )
                  : '–'}
              </FixedSpaceColumn>
              <LabelLike>{i18n.unit.staffAttendance.hours}</LabelLike>
              <div data-qa="staff-attendance-summary-hours">
                {totalMinutes === 'incalculable' ? (
                  <Tooltip tooltip={i18n.unit.staffAttendance.incalculableSum}>
                    <FontAwesomeIcon
                      icon={faExclamationTriangle}
                      color={colors.status.warning}
                    />
                  </Tooltip>
                ) : totalMinutes === 0 ? (
                  '-'
                ) : (
                  `${formatMinutes(totalMinutes)}${
                    plannedAttendances.length > 0
                      ? ` (${
                          Math.sign(diffPlannedTotalMinutes) === 1 ? '+' : '-'
                        }${formatMinutes(Math.abs(diffPlannedTotalMinutes))})`
                      : ''
                  }`
                )}
              </div>
            </ListGrid>

            <HorizontalLine $slim />
          </>
        ) : null}

        <H3>{i18n.unit.staffAttendance.dailyAttendances}</H3>
        {otherUnitAttendancesOnDate.length > 0 && (
          <InfoBox
            wide
            data-qa="attendances-in-other-units"
            message={
              <>
                {i18n.unit.staffAttendance.attendancesInOtherUnits}
                <OtherUnitAttendanceList
                  attendances={otherUnitAttendancesOnDate}
                  groups={groups}
                  date={date}
                />
              </>
            }
          />
        )}
        {!!arrivalWithoutDeparture && (
          <FullGridWidth>
            <FixedSpaceRow
              $justifyContent="left"
              $alignItems="center"
              $spacing="s"
            >
              <FontAwesomeIcon
                icon={faExclamationTriangle}
                color={colors.status.warning}
              />
              <div data-qa="open-attendance-warning">
                {i18n.unit.staffAttendance.openAttendanceWarning(
                  arrivalWithoutDeparture
                )}
              </div>
            </FixedSpaceRow>
            <Gap $size="s" />
          </FullGridWidth>
        )}

        {continuationAttendance && (
          <>
            <ListGrid $rowGap="xxs" $labelWidth="auto">
              <div>
                {groups.find((g) => g.id === continuationAttendance.groupId)
                  ?.name ?? i18n.unit.staffAttendance.noGroup}
              </div>
              <TimesDiv>
                <FixedSpaceRow $justifyContent="space-between">
                  <DateInfoDiv>
                    {continuationAttendance.period.start
                      .toLocalDate()
                      .format('dd.MM.')}
                  </DateInfoDiv>
                  <div />
                  <DateInfoDiv>
                    {continuationAttendance.period.end
                      ?.toLocalDate()
                      ?.format('dd.MM.')}
                  </DateInfoDiv>
                </FixedSpaceRow>
              </TimesDiv>
              <div>
                {i18n.unit.staffAttendance.types[continuationAttendance.type]}
              </div>
              <TimesDiv>
                <FixedSpaceRow
                  data-qa="continuation-attendance"
                  $justifyContent="space-between"
                >
                  <TimeDiv>
                    {continuationAttendance.period.start.toLocalTime().format()}
                  </TimeDiv>
                  <div>–</div>
                  <TimeDiv>
                    {continuationAttendance.period.end?.toLocalTime()?.format()}
                    *
                  </TimeDiv>
                </FixedSpaceRow>
              </TimesDiv>
            </ListGrid>
            <Gap $size="xs" />
            <ContinuationInfo>
              {i18n.unit.staffAttendance.continuationAttendance}
            </ContinuationInfo>
            <Gap />
          </>
        )}

        {!arrivalWithoutDeparture &&
          openAttendanceInAnotherUnit &&
          openAttendance.date.isEqualOrBefore(date) && (
            <FullGridWidth>
              <FixedSpaceRow
                $justifyContent="left"
                $alignItems="center"
                $spacing="s"
              >
                <FontAwesomeIcon
                  icon={faExclamationTriangle}
                  color={colors.status.warning}
                />
                <div data-qa="open-attendance-in-another-unit-warning">
                  {i18n.unit.staffAttendance.openAttendanceInAnotherUnitWarning}
                  <Link
                    to={`/units/${openAttendance.unitId}/calendar?group=${openAttendance.groupId ? openAttendance.groupId : 'staff'}&date=${openAttendance.date.formatIso()}&mode=week`}
                  >
                    {openAttendance.date.formatExotic('EEEEEE d.M.yyyy') +
                      ' - ' +
                      openAttendance.unitName}
                  </Link>
                  {
                    i18n.unit.staffAttendance
                      .openAttendanceInAnotherUnitWarningCont
                  }
                </div>
              </FixedSpaceRow>
              <Gap $size="s" />
            </FullGridWidth>
          )}
        <ListGrid $rowGap="s" $labelWidth="auto">
          {editState.map(
            (
              { arrived, departed, type, groupId, hasStaffOccupancyEffect },
              index
            ) => {
              const gap = getGapBefore(index)

              return (
                <Fragment key={index}>
                  {!!gap && (
                    <FullGridWidth>
                      <FixedSpaceRow
                        $justifyContent="center"
                        $alignItems="center"
                        $spacing="s"
                      >
                        <FontAwesomeIcon
                          icon={faExclamationTriangle}
                          color={colors.status.warning}
                        />
                        <div data-qa={`attendance-gap-warning-${index}`}>
                          {i18n.unit.staffAttendance.gapWarning(
                            `${gap.start.toLocalTime().format()} – ${gap.end.toLocalTime().format()}`
                          )}
                        </div>
                      </FixedSpaceRow>
                    </FullGridWidth>
                  )}
                  <GroupIndicator data-qa="group-indicator">
                    {type !== null && !presentInGroup(type) ? (
                      <span>{i18n.unit.staffAttendance.noGroup}</span>
                    ) : groupId === null ? (
                      <Select
                        items={[null, ...openGroups.map(({ id }) => id)]}
                        selectedItem={groupId}
                        onChange={(value) =>
                          updateAttendance(index, {
                            arrived,
                            departed,
                            type,
                            groupId: value,
                            hasStaffOccupancyEffect
                          })
                        }
                        getItemLabel={(item) =>
                          groups.find(({ id }) => id === item)?.name ??
                          i18n.unit.staffAttendance.noGroup
                        }
                        data-qa="attendance-group-select"
                      />
                    ) : (
                      <Button
                        appearance="inline"
                        text={groups.find((g) => g.id === groupId)?.name ?? '-'}
                        onClick={() =>
                          updateAttendance(index, {
                            arrived,
                            departed,
                            type,
                            groupId: null,
                            hasStaffOccupancyEffect
                          })
                        }
                      />
                    )}
                  </GroupIndicator>
                  {isStaffAttendanceTypesEnabled && !isExternal ? (
                    <Select
                      items={[...allowedStaffAttendanceTypes]}
                      selectedItem={type}
                      onChange={(value) =>
                        value &&
                        updateAttendance(index, {
                          arrived,
                          departed,
                          type: value,
                          groupId: presentInGroup(value) ? groupId : null,
                          hasStaffOccupancyEffect: presentInGroup(value)
                            ? hasStaffOccupancyEffect
                            : false
                        })
                      }
                      getItemLabel={(item) =>
                        i18n.unit.staffAttendance.types[item]
                      }
                      data-qa="attendance-type-select"
                    />
                  ) : null}
                  <InputRow>
                    <TimeInput
                      value={arrived}
                      onChange={(value) =>
                        updateAttendance(index, {
                          arrived: value,
                          departed,
                          type,
                          groupId,
                          hasStaffOccupancyEffect
                        })
                      }
                      info={errorToInputInfo(
                        errors[index].arrived,
                        i18n.validationErrors
                      )}
                      data-qa="arrival-time-input"
                    />
                    <Gap $size="xs" $horizontal />
                    <DatePickerSpacer />
                    <Gap $size="xs" $horizontal />
                    <TimeInput
                      value={departed}
                      onChange={(value) =>
                        updateAttendance(index, {
                          arrived,
                          departed: value,
                          type,
                          groupId,
                          hasStaffOccupancyEffect
                        })
                      }
                      info={errorToInputInfo(
                        errors[index].departed,
                        i18n.validationErrors
                      )}
                      data-qa="departure-time-input"
                    />
                    <Gap $size="xs" $horizontal />
                    <IconOnlyButton
                      icon={faTrash}
                      onClick={() => removeAttendance(index)}
                      aria-label={i18n.common.remove}
                      data-qa="remove-attendance"
                    />
                  </InputRow>
                  <FullGridWidth>
                    <Checkbox
                      label={i18n.unit.staffAttendance.hasStaffOccupancyEffect}
                      checked={hasStaffOccupancyEffect}
                      disabled={type === null || !presentInGroup(type)}
                      onChange={(value) =>
                        updateAttendance(index, {
                          arrived,
                          departed,
                          type,
                          groupId,
                          hasStaffOccupancyEffect: value
                        })
                      }
                      data-qa="has-staff-occupancy-effect"
                    />
                  </FullGridWidth>
                  <Gap $size="xs" />
                </Fragment>
              )
            }
          )}
          {overlappingAttendancesOnOtherDates.length > 0 && (
            <FullGridWidth>
              <AlertBox
                wide
                noMargin
                data-qa="overlapping-attendance-warning"
                message={
                  <>
                    {i18n.unit.staffAttendance.overlappingAttendanceNextDay}
                    <OtherUnitAttendanceList
                      attendances={overlappingAttendancesOnOtherDates}
                      groups={groups}
                    />
                  </>
                }
              />
            </FullGridWidth>
          )}
          <NewAttendance>
            <Button
              appearance="inline"
              icon={faPlus}
              text={i18n.unit.staffAttendance.addNewAttendance}
              onClick={addNewAttendance}
              disabled={arrivalWithoutDeparture !== undefined}
              data-qa="new-attendance"
            />
          </NewAttendance>
        </ListGrid>
        <Gap $size="L" />
        {overlapSaveError && (
          <AlertBox
            wide
            data-qa="overlapping-attendance-save-error"
            message={i18n.unit.staffAttendance.overlappingAttendanceSaveError}
          />
        )}
        <ModalActions>
          <Button text={i18n.common.cancel} onClick={onClose} />
          <AsyncButton
            primary
            text={i18n.unit.staffAttendance.saveChanges}
            onClick={save}
            onSuccess={onSuccess}
            onFailure={onSaveFailure}
            disabled={saveDisabled}
            data-qa="save"
          />
        </ModalActions>
      </Content>
      <ModalCloseButton
        close={onClose}
        closeLabel={i18n.common.closeModal}
        data-qa="close"
      />
    </PlainModal>
  )
}

const OtherUnitAttendanceList = React.memo(function OtherUnitAttendanceList({
  attendances,
  groups,
  date
}: {
  attendances: ModalOtherAttendance[]
  groups: DaycareGroup[]
  date?: LocalDate
}) {
  const { i18n } = useTranslation()
  return (
    <OtherUnitAttendanceUl>
      {attendances.map(({ unitName, groupId, period }, i) => {
        const currentDate = date ?? period.start.toLocalDate()
        return (
          <li key={i}>
            {unitName ??
              groups.find((g) => g.id === groupId)?.name ??
              i18n.unit.staffAttendance.noGroup}
            :{' '}
            <strong>
              {formatRange({
                date: currentDate,
                range: new DateTimeRange(
                  period.start.toLocalDate().isBefore(currentDate)
                    ? currentDate.startOfDay()
                    : period.start,
                  period.end
                )
              })}
            </strong>
          </li>
        )
      })}
    </OtherUnitAttendanceUl>
  )
})

const OtherUnitAttendanceUl = styled.ul`
  margin: ${defaultMargins.xs} 0 0;
  padding-left: ${defaultMargins.m};
`

function formatRange(options: {
  date: LocalDate
  range: DateTimeRange | FiniteDateTimeRange
  renderEnd?: (value: string) => React.ReactNode
}): React.ReactNode {
  const times = options.range.timesOn(options.date)
  const renderEnd = options?.renderEnd ?? ((value) => value)

  return (
    <>
      {times.start?.format() ?? '→'} –{' '}
      {options.range.end === null ? '' : renderEnd(times.end?.format() ?? '→')}
    </>
  )
}

function formatMinutes(minutes: number) {
  return `${Math.floor(minutes / 60)}:${Math.floor(minutes % 60)
    .toString()
    .padStart(2, '0')}`
}

function isGroupOpen(group: DaycareGroup) {
  return new DateRange(group.startDate, group.endDate).includes(
    LocalDate.todayInHelsinkiTz()
  )
}

const Content = styled.div`
  padding: ${defaultMargins.L};
`

const GroupIndicator = styled.div`
  grid-column: 1 / 3;
  margin-bottom: -${defaultMargins.xs};
`

const InputRow = styled.div`
  display: flex;
`

const NewAttendance = styled.div`
  grid-column: 1 / 3;
`

const ModalActions = styled.div`
  display: flex;
  justify-content: space-between;
`

const FullGridWidth = styled.div`
  grid-column: 1 / -1;
`

const TimesDiv = styled.div`
  width: 130px;
`

const TimeDiv = styled.div`
  width: 40px;
`

const DateInfoDiv = styled(TimeDiv)`
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-weight: 600;
  font-size: 14px;
`

const ContinuationInfo = styled.div`
  color: ${(p) => p.theme.colors.grayscale.g70};
  font-style: italic;
`

const DepartedTime = styled.span<{ $automaticDeparture: boolean }>`
  ${(p) =>
    p.$automaticDeparture
      ? `color: ${colors.accents.a2orangeDark}; font-weight: ${fontWeights.semibold};`
      : ''}
`

const AutomaticDepartureLabel = styled.div`
  display: flex;
  align-items: center;
  gap: ${defaultMargins.xs};
  font-weight: ${fontWeights.semibold};
  color: ${colors.accents.a2orangeDark};
`
