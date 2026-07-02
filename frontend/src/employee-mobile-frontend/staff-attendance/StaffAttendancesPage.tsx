// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { useSpring } from '@react-spring/web'
import sortBy from 'lodash/sortBy'
import React, { Fragment, useCallback, useMemo, useState } from 'react'
import styled, { useTheme } from 'styled-components'
import { useLocation } from 'wouter'

import type { Result } from 'lib-common/api'
import type {
  GroupInfo,
  StaffAttendanceType
} from 'lib-common/generated/api-types/attendance'
import type { EmployeeId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import type LocalTime from 'lib-common/local-time'
import { useQueryResult } from 'lib-common/query'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { Button } from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { PersonName } from 'lib-components/molecules/PersonNames'
import type { TabLink } from 'lib-components/molecules/Tabs'
import { TabLinks } from 'lib-components/molecules/Tabs'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { fasExclamationTriangle } from 'lib-icons'
import { faChevronDown, faChevronRight, faChevronUp } from 'lib-icons'

import { routes } from '../App'
import { renderResult } from '../async-rendering'
import { bottomNavBarHeight } from '../common/BottomNavbar'
import FreeTextSearch, { SearchContainer } from '../common/FreeTextSearch'
import { PageWithNavigation } from '../common/PageWithNavigation'
import { useTranslation } from '../common/i18n'
import type { UnitOrGroup } from '../common/unit-or-group'
import { isUnitView, toUnitOrGroup } from '../common/unit-or-group'
import { unitInfoQuery } from '../units/queries'

import StaffListItem from './StaffListItem'
import { staffAttendanceQuery } from './queries'
import { toStaff } from './utils'

type PrimaryTab = 'today' | 'planned'

type Props = {
  unitOrGroup: UnitOrGroup
  primaryTab: PrimaryTab
}

export default React.memo(function StaffAttendancesPage(props: Props) {
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()
  const unitOrGroup = props.unitOrGroup
  const unitId = unitOrGroup.unitId
  const unitInfoResponse = useQueryResult(unitInfoQuery({ unitId }))

  const [showSearch, setShowSearch] = useState(false)
  const toggleSearch = useCallback(() => setShowSearch((show) => !show), [])

  const changeGroup = useCallback(
    (group: GroupInfo | undefined) => {
      navigate(
        props.primaryTab === 'today'
          ? routes.staffAttendancesToday(toUnitOrGroup(unitId, group?.id)).value
          : routes.staffAttendancesPlanned(toUnitOrGroup(unitId, group?.id))
              .value
      )
    },
    [navigate, props, unitId]
  )

  const selectedGroup = useMemo(
    () =>
      unitInfoResponse
        .map(({ groups }) =>
          isUnitView(unitOrGroup)
            ? undefined
            : groups.find((g) => g.id === unitOrGroup.id)
        )
        .getOrElse(undefined),
    [unitOrGroup, unitInfoResponse]
  )

  const tabs = useMemo(
    (): TabLink[] => [
      {
        id: 'today',
        link: '/today',
        label: i18n.attendances.views.TODAY
      },
      {
        id: 'planned',
        link: '/planned',
        label: i18n.attendances.views.NEXT_DAYS
      }
    ],
    [i18n]
  )

  return (
    <>
      {props.primaryTab === 'today' && (
        <StaffSearch
          unitOrGroup={unitOrGroup}
          show={showSearch}
          toggleShow={toggleSearch}
        />
      )}
      <PageWithNavigation
        unitOrGroup={unitOrGroup}
        selected="staff"
        selectedGroup={selectedGroup}
        onChangeGroup={changeGroup}
        toggleSearch={props.primaryTab === 'today' ? toggleSearch : undefined}
      >
        <TabLinks tabs={tabs} mobile />
        {props.primaryTab === 'today' ? (
          <StaffAttendancesToday unitOrGroup={unitOrGroup} />
        ) : (
          <StaffAttendancesPlanned unitOrGroup={unitOrGroup} />
        )}
      </PageWithNavigation>
    </>
  )
})

const StaffAttendancesToday = React.memo(function StaffAttendancesToday({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()

  const staffAttendanceResponse = useQueryResult(
    staffAttendanceQuery({ unitId: unitOrGroup.unitId })
  )

  const [presentOpen, setPresentOpen] = useState(true)
  const [absentOpen, setAbsentOpen] = useState(true)

  const navigateToExternalMemberArrival = useCallback(
    () => navigate(routes.externalStaffAttendances(unitOrGroup).value),
    [unitOrGroup, navigate]
  )

  const groupedStaff = useMemo(
    () =>
      staffAttendanceResponse.map((res) => {
        const present = isUnitView(unitOrGroup)
          ? [
              ...res.staff.filter((s) => s.present !== null),
              ...res.extraAttendances
            ]
          : [
              ...res.staff.filter((s) => s.present === unitOrGroup.id),
              ...res.extraAttendances.filter(
                (s) => s.groupId === unitOrGroup.id
              )
            ]
        const absent = res.staff.filter(
          (s) =>
            s.present === null &&
            (isUnitView(unitOrGroup) || s.groupIds.includes(unitOrGroup.id))
        )
        return { present, absent }
      }),
    [unitOrGroup, staffAttendanceResponse]
  )

  return (
    <>
      {renderResult(groupedStaff, ({ present, absent }) => (
        <StaffListSpacer $spacing="xs">
          <StaffSection
            dataQa="present"
            title={i18n.attendances.types.PRESENT}
            count={present.length}
            open={presentOpen}
            toggleOpen={() => setPresentOpen((prev) => !prev)}
          >
            {present.map((staffMember) => {
              const s = toStaff(staffMember)
              return (
                <StaffListItem {...s} key={s.id} unitOrGroup={unitOrGroup} />
              )
            })}
          </StaffSection>
          <StaffSection
            dataQa="absent"
            title={i18n.attendances.types.ABSENT}
            count={absent.length}
            open={absentOpen}
            toggleOpen={() => setAbsentOpen((prev) => !prev)}
          >
            {absent.map((staffMember) => {
              const s = toStaff(staffMember)
              return (
                <StaffListItem {...s} key={s.id} unitOrGroup={unitOrGroup} />
              )
            })}
          </StaffSection>
        </StaffListSpacer>
      ))}
      <AddExternalMemberContainer>
        <span>{i18n.attendances.staff.externalPersonCantFindYourName}</span>
        <Button
          data-qa="add-external-member-btn"
          appearance="link"
          text={i18n.attendances.staff.markExternalPerson}
          onClick={navigateToExternalMemberArrival}
        />
      </AddExternalMemberContainer>
    </>
  )
})

const StaffSearch = React.memo(function StaffSearch({
  unitOrGroup,
  show,
  toggleShow
}: {
  unitOrGroup: UnitOrGroup
  show: boolean
  toggleShow: () => void
}) {
  const { i18n } = useTranslation()
  const containerSpring = useSpring<{ x: number }>({ x: show ? 1 : 0 })
  const [freeText, setFreeText] = useState('')

  const staffAttendanceResponse = useQueryResult(
    staffAttendanceQuery({ unitId: unitOrGroup.unitId })
  )

  const staff = useMemo(
    () =>
      staffAttendanceResponse
        .map((res) => [...res.staff, ...res.extraAttendances])
        .getOrElse([]),
    [staffAttendanceResponse]
  )

  const searchResults = useMemo(
    () =>
      freeText === ''
        ? []
        : staff.filter((s) => {
            const text = freeText.toLowerCase()
            return 'employeeId' in s
              ? s.firstName.toLowerCase().includes(text) ||
                  s.lastName.toLowerCase().includes(text)
              : s.name.toLowerCase().includes(text)
          }),
    [freeText, staff]
  )

  return (
    <SearchContainer
      style={{ height: containerSpring.x.to((x) => `${100 * x}%`) }}
    >
      <ContentArea
        $opaque={false}
        $paddingVertical="zero"
        $paddingHorizontal="zero"
      >
        <FreeTextSearch
          value={freeText}
          setValue={setFreeText}
          placeholder={i18n.attendances.staff.searchPlaceholder}
          background={colors.grayscale.g0}
          setShowSearch={toggleShow}
          resultCount={searchResults.length}
        />
        <FixedSpaceColumn $spacing="zero">
          {searchResults.map((staffMember) => {
            const s = toStaff(staffMember)
            return <StaffListItem {...s} key={s.id} unitOrGroup={unitOrGroup} />
          })}
        </FixedSpaceColumn>
      </ContentArea>
    </SearchContainer>
  )
})

interface StaffMemberDay {
  employeeId: EmployeeId
  firstName: string
  lastName: string
  occupancyEffect: boolean
  plans: {
    start: LocalTime | null // null if started on previous day
    end: LocalTime | null // null if ends on the next day
    type: StaffAttendanceType
    description: string | null
  }[]
  confidence: 'full' | 'maybeInOtherGroup' | 'maybeInOtherUnit'
}

interface StaffMembersByDate {
  date: LocalDate
  staff: StaffMemberDay[]
}

const StaffAttendancesPlanned = React.memo(function StaffAttendancesPlanned({
  unitOrGroup
}: {
  unitOrGroup: UnitOrGroup
}) {
  const { i18n, lang } = useTranslation()
  const theme = useTheme()
  const today = LocalDate.todayInHelsinkiTz()

  const [expandedDate, setExpandedDate] = useState<LocalDate | null>(null)

  const staffAttendanceResponse = useQueryResult(
    staffAttendanceQuery({
      unitId: unitOrGroup.unitId,
      startDate: today.addDays(1),
      endDate: today.addDays(7)
    })
  )

  const staffMemberDays: Result<StaffMembersByDate[]> = useMemo(
    () =>
      staffAttendanceResponse.map((res) =>
        res.operationalDays
          .filter((_, index) => index < 5)
          .map((date) => ({
            date,
            staff: res.staff
              .filter(
                (s) =>
                  unitOrGroup.type !== 'group' ||
                  s.groupIds.includes(unitOrGroup.id)
              )
              .map((s) => ({
                employeeId: s.employeeId,
                firstName: s.firstName,
                lastName: s.lastName,
                occupancyEffect: s.occupancyEffect,
                plans: s.plannedAttendances
                  .filter(
                    (p) =>
                      p.start.toLocalDate().isEqual(date) ||
                      p.end.toLocalDate().isEqual(date)
                  )
                  .map((p) => ({
                    start: p.start.toLocalDate().isEqual(date)
                      ? p.start.toLocalTime()
                      : null,
                    end: p.end.toLocalDate().isEqual(date)
                      ? p.end.toLocalTime()
                      : null,
                    type: p.type,
                    description: p.description
                  })),
                confidence:
                  s.unitIds.length > 1
                    ? 'maybeInOtherUnit'
                    : s.groupIds.length > 1
                      ? 'maybeInOtherGroup'
                      : 'full'
              }))
          }))
      ),
    [unitOrGroup, staffAttendanceResponse]
  )

  return renderResult(staffMemberDays, (days) => (
    <FixedSpaceColumn $spacing="xxs">
      <DayRow $open={false}>
        <InfoBox message={i18n.attendances.staff.plansInfo} noMargin thin />
      </DayRow>
      {days.map(({ date, staff }) => (
        <Fragment key={date.formatIso()}>
          <DayRow
            data-qa={`date-row-${date.formatIso()}`}
            onClick={() =>
              setExpandedDate(expandedDate?.isEqual(date) ? null : date)
            }
            $justifyContent="space-between"
            $alignItems="center"
            $open={expandedDate?.isEqual(date) ?? false}
          >
            <div>{date.formatExotic('EEEEEE d.M.', lang)}</div>
            <div>
              <FontAwesomeIcon
                icon={expandedDate?.isEqual(date) ? faChevronUp : faChevronDown}
                color={theme.colors.main.m2}
                style={{ fontSize: '24px' }}
              />
            </div>
          </DayRow>
          {expandedDate?.isEqual(date) && (
            <ExpandedStaff
              $spacing="L"
              data-qa={`expanded-date-${date.formatIso()}`}
            >
              {sortBy(
                staff.filter((s) => s.plans.length > 0),
                (s) => s.firstName,
                (s) => s.lastName
              ).map((s) => (
                <FixedSpaceColumn
                  key={s.employeeId}
                  $spacing="xs"
                  data-qa={`present-employee-${s.employeeId}`}
                >
                  <FixedSpaceRow $spacing="zero" $alignItems="center">
                    <IconWrapper>
                      {s.occupancyEffect && (
                        <RoundIcon
                          content="K"
                          active={true}
                          color={theme.colors.accents.a3emerald}
                          size="s"
                        />
                      )}
                    </IconWrapper>
                    <StaffName>
                      <PersonName person={s} format="First Last" />
                    </StaffName>
                  </FixedSpaceRow>

                  {s.confidence !== 'full' && (
                    <FixedSpaceRow $spacing="zero" $alignItems="center">
                      <IconWrapper>
                        <FontAwesomeIcon
                          icon={fasExclamationTriangle}
                          color={theme.colors.status.warning}
                        />
                      </IconWrapper>
                      <span data-qa="confidence-warning">
                        {i18n.attendances.staff.planWarnings[s.confidence]}
                      </span>
                    </FixedSpaceRow>
                  )}

                  {s.plans.map((p, i) => (
                    <DetailsRow key={i} $alignItems="start">
                      <PlanType>{i18n.attendances.staffTypes[p.type]}</PlanType>
                      <div>
                        <div>{`${p.start?.format() ?? '→'} - ${p.end?.format() ?? '→'}`}</div>
                        {p.description ? <i>({p.description})</i> : null}
                      </div>
                    </DetailsRow>
                  ))}
                </FixedSpaceColumn>
              ))}

              {staff.some((s) => s.plans.length > 0) &&
                staff.some((s) => s.plans.length === 0) && <NoPlansSeparator />}

              {sortBy(
                staff.filter((s) => s.plans.length === 0),
                (s) => s.firstName,
                (s) => s.lastName
              ).map((s) => (
                <AbsentStaff
                  key={s.employeeId}
                  data-qa={`absent-employee-${s.employeeId}`}
                >
                  <FixedSpaceColumn $spacing="xxs">
                    <FixedSpaceRow $spacing="zero" $alignItems="center">
                      <IconWrapper>
                        {s.occupancyEffect && (
                          <RoundIcon
                            content="K"
                            active={true}
                            color={theme.colors.accents.a3emerald}
                            size="s"
                          />
                        )}
                      </IconWrapper>
                      <StaffName>
                        <PersonName person={s} format="First Last" />
                      </StaffName>
                    </FixedSpaceRow>
                    <DetailsRow>{i18n.attendances.staff.noPlan}</DetailsRow>
                  </FixedSpaceColumn>
                </AbsentStaff>
              ))}
            </ExpandedStaff>
          )}
        </Fragment>
      ))}
    </FixedSpaceColumn>
  ))
})

const DayRow = styled(FixedSpaceRow)<{ $open: boolean }>`
  border-left: 4px solid
    ${(p) => (p.$open ? p.theme.colors.main.m2 : 'transparent')};
  padding: 16px;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`

const ExpandedStaff = styled(FixedSpaceColumn)`
  padding: 16px 8px;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`

const AbsentStaff = styled.div`
  color: ${(p) => p.theme.colors.grayscale.g70};
`

const StaffName = styled.div`
  font-weight: ${fontWeights.semibold};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`

const PlanType = styled.div`
  width: 30%;
`

const IconWrapper = styled.div`
  min-width: 40px;
  display: flex;
  justify-content: center;
  align-items: center;
`

const DetailsRow = styled(FixedSpaceRow)`
  padding-left: 40px;
`

const NoPlansSeparator = styled(HorizontalLine)`
  margin-block-start: 8px;
  margin-block-end: 16px;
`

const AddExternalMemberContainer = styled.div`
  width: 100%;
  background-color: ${(p) => p.theme.colors.grayscale.g0};
  box-shadow: 0 0 4px 0 #0f0f0f15;
  position: fixed;
  bottom: ${bottomNavBarHeight}px;
  padding: ${defaultMargins.xs};
  text-align: center;
  font-weight: ${fontWeights.semibold};
  display: flex;
  justify-content: center;
  align-items: center;
  gap: ${defaultMargins.xs};
  font-size: 14px;
`

const StaffListSpacer = styled(FixedSpaceColumn)`
  margin-bottom: 35px;
`

const StaffSection = React.memo(function StaffSection({
  dataQa,
  title,
  count,
  open,
  toggleOpen,
  children
}: {
  dataQa: string
  title: string
  count: number
  open: boolean
  toggleOpen: () => void
  children: React.ReactNode
}) {
  const theme = useTheme()
  return (
    <div data-qa={`${dataQa}-section`}>
      <SectionHeading
        onClick={toggleOpen}
        data-qa={`${dataQa}-section-header`}
        aria-expanded={open}
      >
        <FontAwesomeIcon
          icon={open ? faChevronDown : faChevronRight}
          color={theme.colors.main.m2}
        />
        <SectionTitle data-qa={`${dataQa}-heading`}>
          {title} <SectionCount>{count}</SectionCount>
        </SectionTitle>
      </SectionHeading>
      {open && <FixedSpaceColumn $spacing="zero">{children}</FixedSpaceColumn>}
    </div>
  )
})

const SectionHeading = styled.button`
  display: flex;
  align-items: center;
  gap: ${defaultMargins.s};
  width: 100%;
  padding: ${defaultMargins.s};
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  font-size: 16px;
  color: ${(p) => p.theme.colors.main.m2};
  border-bottom: 1px solid ${(p) => p.theme.colors.grayscale.g15};
`

const SectionTitle = styled.span`
  font-weight: ${fontWeights.medium};
`

const SectionCount = styled.span`
  margin-left: ${defaultMargins.xs};
  font-weight: ${fontWeights.bold};
`
