// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { Fragment, ReactNode, useContext, useMemo } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import { useApiState } from 'lib-common/utils/useRestApi'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import Title from 'lib-components/atoms/Title'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import {
  faCar,
  faChild,
  faClock,
  faCopy,
  faDatabase,
  faDiagnoses,
  faEuroSign,
  faFileAlt,
  faGavel,
  faHandHolding,
  faHome,
  faHourglassEnd,
  faHourglassStart,
  faMoneyBillWave,
  faPercentage,
  faUserAltSlash,
  faUsers,
  faUtensils
} from 'lib-icons'

import { getPermittedReports } from '../generated/api-clients/reports'
import { useTranslation } from '../state/i18n'

import { renderResult } from './async-rendering'
import { AssistanceNeedDecisionReportContext } from './reports/AssistanceNeedDecisionReportContext'

const getPermittedReportsResult = wrapResult(getPermittedReports)

const ReportItems = styled.div`
  margin: 20px 0;
`

const ReportItem = styled.div`
  margin-bottom: 25px;
`

const TitleRow = styled.div`
  display: flex;
  justify-content: flex-start;
  align-items: center;

  > * {
    margin-right: ${defaultMargins.s};
  }
`

const LinkTitle = styled(Link)`
  text-transform: uppercase;
  margin: 0;
  font-size: 0.9rem;
  font-weight: ${fontWeights.semibold};
`
const Description = styled.p`
  margin-left: calc(34px + ${defaultMargins.s});
  width: 70%;
`

const UnreadCount = styled.span`
  color: ${colors.main.m2};
  font-size: 0.9rem;
  font-weight: ${fontWeights.semibold};
  margin-left: ${defaultMargins.xs};
  border: 1.5px solid ${colors.main.m2};
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-radius: 100%;
  width: ${defaultMargins.m};
  height: ${defaultMargins.m};
`

interface ReportProps {
  path: string
  color: string
  icon: IconDefinition
  i18n: {
    title: string
    description: string
  }
  'data-qa'?: string
  children?: React.ReactNode
}

const Report = React.memo(function Report(props: ReportProps) {
  return (
    <ReportItem>
      <TitleRow>
        <RoundIcon size="L" color={props.color} content={props.icon} />
        <LinkTitle to={props.path} data-qa={props['data-qa']}>
          {props.i18n.title}
        </LinkTitle>
        {props.children}
      </TitleRow>
      <Description>{props.i18n.description}</Description>
    </ReportItem>
  )
})

export default React.memo(function Reports() {
  const { i18n } = useTranslation()
  const { assistanceNeedDecisionCounts } = useContext(
    AssistanceNeedDecisionReportContext
  )

  const [permittedReports] = useApiState(getPermittedReportsResult, [])
  const permittedReportsSet = useMemo(
    () => permittedReports.map((reports) => new Set(reports)),
    [permittedReports]
  )

  return (
    <Container>
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.title}</Title>
        {renderResult(permittedReportsSet, (reports) => {
          type NamedItem = {
            name: string
            item: ReactNode
          }

          const reportItems: (NamedItem | null)[] = [
            reports.has('CHILDREN_IN_DIFFERENT_ADDRESS')
              ? {
                  name: i18n.reports.childrenInDifferentAddress.title,
                  item: (
                    <Report
                      path="/reports/children-in-different-address"
                      color={colors.accents.a5orangeLight}
                      icon={faHome}
                      i18n={i18n.reports.childrenInDifferentAddress}
                    />
                  )
                }
              : null,
            reports.has('DUPLICATE_PEOPLE')
              ? {
                  name: i18n.reports.duplicatePeople.title,
                  item: (
                    <Report
                      path="/reports/duplicate-people"
                      color={colors.status.warning}
                      icon={faCopy}
                      i18n={i18n.reports.duplicatePeople}
                    />
                  )
                }
              : null,
            reports.has('FAMILY_CONFLICT')
              ? {
                  name: i18n.reports.familyConflicts.title,
                  item: (
                    <Report
                      path="/reports/family-conflicts"
                      color={colors.status.warning}
                      icon={faUsers}
                      i18n={i18n.reports.familyConflicts}
                    />
                  )
                }
              : null,
            reports.has('PARTNERS_IN_DIFFERENT_ADDRESS')
              ? {
                  name: i18n.reports.partnersInDifferentAddress.title,
                  item: (
                    <Report
                      path="/reports/partners-in-different-address"
                      color={colors.status.warning}
                      icon={faHome}
                      i18n={i18n.reports.partnersInDifferentAddress}
                    />
                  )
                }
              : null,
            reports.has('MISSING_SERVICE_NEED')
              ? {
                  name: i18n.reports.missingServiceNeed.title,
                  item: (
                    <Report
                      path="/reports/missing-service-need"
                      color={colors.status.warning}
                      icon={faClock}
                      i18n={i18n.reports.missingServiceNeed}
                    />
                  )
                }
              : null,
            reports.has('MISSING_HEAD_OF_FAMILY')
              ? {
                  name: i18n.reports.missingHeadOfFamily.title,
                  item: (
                    <Report
                      path="/reports/missing-head-of-family"
                      color={colors.status.warning}
                      icon={faUserAltSlash}
                      i18n={i18n.reports.missingHeadOfFamily}
                      data-qa="report-missing-head-of-family"
                    />
                  )
                }
              : null,
            reports.has('UNITS')
              ? {
                  name: i18n.reports.units.title,
                  item: (
                    <Report
                      path="/reports/units"
                      color={colors.main.m2}
                      icon={faHome}
                      i18n={i18n.reports.units}
                      data-qa="report-units"
                    />
                  )
                }
              : null,
            reports.has('APPLICATIONS')
              ? {
                  name: i18n.reports.applications.title,
                  item: (
                    <Report
                      path="/reports/applications"
                      color={colors.main.m2}
                      icon={faFileAlt}
                      i18n={i18n.reports.applications}
                      data-qa="report-applications"
                    />
                  )
                }
              : null,
            reports.has('DECISIONS')
              ? {
                  name: i18n.reports.decisions.title,
                  item: (
                    <Report
                      path="/reports/decisions"
                      color={colors.main.m2}
                      icon={faGavel}
                      i18n={i18n.reports.decisions}
                      data-qa="report-decisions"
                    />
                  )
                }
              : null,
            reports.has('OCCUPANCY')
              ? {
                  name: i18n.reports.occupancies.title,
                  item: (
                    <Report
                      path="/reports/occupancies"
                      color={colors.main.m2}
                      icon={faPercentage}
                      i18n={i18n.reports.occupancies}
                    />
                  )
                }
              : null,
            reports.has('CHILD_AGE_LANGUAGE')
              ? {
                  name: i18n.reports.childAgeLanguage.title,
                  item: (
                    <Report
                      path="/reports/child-age-language"
                      color={colors.main.m2}
                      icon={faChild}
                      i18n={i18n.reports.childAgeLanguage}
                    />
                  )
                }
              : null,
            reports.has('SERVICE_NEED')
              ? {
                  name: i18n.reports.serviceNeeds.title,
                  item: (
                    <Report
                      path="/reports/service-needs"
                      color={colors.main.m2}
                      icon={faChild}
                      i18n={i18n.reports.serviceNeeds}
                    />
                  )
                }
              : null,
            reports.has('EXCEEDED_SERVICE_NEEDS')
              ? {
                  name: i18n.reports.exceededServiceNeed.title,
                  item: (
                    <Report
                      path="/reports/exceeded-service-needs"
                      color={colors.main.m2}
                      icon={faChild}
                      i18n={i18n.reports.exceededServiceNeed}
                    />
                  )
                }
              : null,
            reports.has('ASSISTANCE_NEEDS_AND_ACTIONS')
              ? {
                  name: i18n.reports.assistanceNeedsAndActions.title,
                  item: (
                    <Report
                      path="/reports/assistance-needs-and-actions"
                      color={colors.main.m2}
                      icon={faHandHolding}
                      i18n={i18n.reports.assistanceNeedsAndActions}
                    />
                  )
                }
              : null,
            reports.has('INVOICE')
              ? {
                  name: i18n.reports.invoices.title,
                  item: (
                    <Report
                      path="/reports/invoices"
                      color={colors.main.m2}
                      icon={faEuroSign}
                      i18n={i18n.reports.invoices}
                    />
                  )
                }
              : null,
            reports.has('CUSTOMER_FEES')
              ? {
                  name: i18n.reports.customerFees.title,
                  item: (
                    <Report
                      path="/reports/customer-fees"
                      color={colors.main.m2}
                      icon={faEuroSign}
                      i18n={i18n.reports.customerFees}
                    />
                  )
                }
              : null,
            reports.has('STARTING_PLACEMENTS')
              ? {
                  name: i18n.reports.startingPlacements.title,
                  item: (
                    <Report
                      path="/reports/starting-placements"
                      color={colors.main.m2}
                      icon={faHourglassStart}
                      i18n={i18n.reports.startingPlacements}
                      data-qa="report-starting-placements"
                    />
                  )
                }
              : null,
            reports.has('ENDED_PLACEMENTS')
              ? {
                  name: i18n.reports.endedPlacements.title,
                  item: (
                    <Report
                      path="/reports/ended-placements"
                      color={colors.main.m2}
                      icon={faHourglassEnd}
                      i18n={i18n.reports.endedPlacements}
                      data-qa="report-ended-placements"
                    />
                  )
                }
              : null,
            reports.has('PRESENCE')
              ? {
                  name: i18n.reports.presence.title,
                  item: (
                    <Report
                      path="/reports/presences"
                      color={colors.main.m2}
                      icon={faDiagnoses}
                      i18n={i18n.reports.presence}
                    />
                  )
                }
              : null,
            reports.has('SERVICE_VOUCHER_VALUE')
              ? {
                  name: i18n.reports.voucherServiceProviders.title,
                  item: (
                    <Report
                      path="/reports/voucher-service-providers"
                      color={colors.main.m2}
                      icon={faMoneyBillWave}
                      i18n={i18n.reports.voucherServiceProviders}
                      data-qa="report-voucher-service-providers"
                    />
                  )
                }
              : null,
            reports.has('ATTENDANCE_RESERVATION')
              ? {
                  name: i18n.reports.attendanceReservation.title,
                  item: (
                    <Report
                      path="/reports/attendance-reservation"
                      color={colors.main.m2}
                      icon={faCar}
                      i18n={i18n.reports.attendanceReservation}
                      data-qa="report-attendance-reservation"
                    />
                  )
                }
              : null,
            reports.has('ATTENDANCE_RESERVATION')
              ? {
                  name: i18n.reports.attendanceReservationByChild.title,
                  item: (
                    <Report
                      path="/reports/attendance-reservation-by-child"
                      color={colors.main.m2}
                      icon={faCar}
                      i18n={i18n.reports.attendanceReservationByChild}
                      data-qa="report-attendance-reservation-by-child"
                    />
                  )
                }
              : null,
            featureFlags.jamixIntegration && reports.has('MEALS')
              ? {
                  name: i18n.reports.meals.title,
                  item: (
                    <Report
                      path="/reports/meals"
                      color={colors.main.m2}
                      icon={faUtensils}
                      i18n={i18n.reports.meals}
                      data-qa="report-meals"
                    />
                  )
                }
              : null,
            reports.has('SEXTET')
              ? {
                  name: i18n.reports.sextet.title,
                  item: (
                    <Report
                      path="/reports/sextet"
                      color={colors.main.m2}
                      icon={faDiagnoses}
                      i18n={i18n.reports.sextet}
                      data-qa="report-sextet"
                    />
                  )
                }
              : null,
            reports.has('RAW')
              ? {
                  name: i18n.reports.raw.title,
                  item: (
                    <Report
                      path="/reports/raw"
                      color={colors.main.m2}
                      icon={faDatabase}
                      i18n={i18n.reports.raw}
                    />
                  )
                }
              : null,
            reports.has('ASSISTANCE_NEED_DECISIONS')
              ? {
                  name: i18n.reports.assistanceNeedDecisions.title,
                  item: (
                    <Report
                      path="/reports/assistance-need-decisions"
                      color={colors.main.m2}
                      icon={faHandHolding}
                      i18n={i18n.reports.assistanceNeedDecisions}
                    >
                      {assistanceNeedDecisionCounts
                        .map(
                          (unread) =>
                            unread > 0 && (
                              <UnreadCount key="unread">{unread}</UnreadCount>
                            )
                        )
                        .getOrElse(null)}
                    </Report>
                  )
                }
              : null,
            reports.has('MANUAL_DUPLICATION')
              ? {
                  name: i18n.reports.manualDuplication.title,
                  item: (
                    <Report
                      path="/reports/manual-duplication"
                      color={colors.status.warning}
                      icon={faChild}
                      i18n={i18n.reports.manualDuplication}
                      data-qa="report-manual-duplication"
                    />
                  )
                }
              : null,
            reports.has('NON_SSN_CHILDREN')
              ? {
                  name: i18n.reports.nonSsnChildren.title,
                  item: (
                    <Report
                      path="/reports/non-ssn-children"
                      color={colors.main.m2}
                      icon={faChild}
                      i18n={i18n.reports.nonSsnChildren}
                      data-qa="report-non-ssn-children"
                    />
                  )
                }
              : null,
            reports.has('PLACEMENT_COUNT')
              ? {
                  name: i18n.reports.placementCount.title,
                  item: (
                    <Report
                      path="/reports/placement-count"
                      color={colors.main.m2}
                      icon={faChild}
                      i18n={i18n.reports.placementCount}
                      data-qa="report-placement-count"
                    />
                  )
                }
              : null,
            featureFlags.placementGuarantee &&
            reports.has('PLACEMENT_GUARANTEE')
              ? {
                  name: i18n.reports.placementGuarantee.title,
                  item: (
                    <Report
                      path="/reports/placement-guarantee"
                      color={colors.main.m2}
                      icon={faChild}
                      i18n={i18n.reports.placementGuarantee}
                      data-qa="report-placement-guarantee"
                    />
                  )
                }
              : null,
            reports.has('PLACEMENT_SKETCHING')
              ? {
                  name: i18n.reports.placementSketching.title,
                  item: (
                    <Report
                      path="/reports/placement-sketching"
                      color={colors.status.warning}
                      icon={faUsers}
                      i18n={i18n.reports.placementSketching}
                      data-qa="report-placement-sketching"
                    />
                  )
                }
              : null,
            reports.has('PRESCHOOL_ABSENCES')
              ? {
                  name: i18n.reports.preschoolAbsences.title,
                  item: (
                    <Report
                      data-qa="report-preschool-absence"
                      path="/reports/preschool-absence"
                      color={colors.main.m2}
                      icon={faChild}
                      i18n={i18n.reports.preschoolAbsences}
                    />
                  )
                }
              : null,
            reports.has('PRESCHOOL_APPLICATIONS')
              ? {
                  name: i18n.reports.preschoolApplications.title,
                  item: (
                    <Report
                      data-qa="report-preschool-application"
                      path="/reports/preschool-application"
                      color={colors.main.m2}
                      icon={faChild}
                      i18n={i18n.reports.preschoolApplications}
                    />
                  )
                }
              : null,
            reports.has('FAMILY_DAYCARE_MEAL_REPORT')
              ? {
                  name: i18n.reports.familyDaycareMealCount.title,
                  item: (
                    <Report
                      data-qa="family-daycare-meal-report"
                      path="/reports/family-daycare-meal-count"
                      color={colors.main.m2}
                      icon={faMoneyBillWave}
                      i18n={i18n.reports.familyDaycareMealCount}
                    />
                  )
                }
              : null,
            reports.has('FUTURE_PRESCHOOLERS')
              ? {
                  name: i18n.reports.futurePreschoolers.title,
                  item: (
                    <Report
                      path="/reports/future-preschoolers"
                      color={colors.main.m2}
                      icon={faChild}
                      i18n={i18n.reports.futurePreschoolers}
                    />
                  )
                }
              : null,
            reports.has('VARDA_ERRORS')
              ? {
                  name: i18n.reports.vardaChildErrors.title,
                  item: (
                    <Report
                      data-qa="report-varda-child-errors"
                      path="/reports/varda-child-errors"
                      color={colors.status.warning}
                      icon={faDiagnoses}
                      i18n={i18n.reports.vardaChildErrors}
                    />
                  )
                }
              : null,
            reports.has('VARDA_ERRORS')
              ? {
                  name: i18n.reports.vardaUnitErrors.title,
                  item: (
                    <Report
                      data-qa="report-varda-unit-errors"
                      path="/reports/varda-unit-errors"
                      color={colors.status.warning}
                      icon={faDiagnoses}
                      i18n={i18n.reports.vardaUnitErrors}
                    />
                  )
                }
              : null,
            reports.has('HOLIDAY_PERIOD_ATTENDANCE')
              ? {
                  name: i18n.reports.holidayPeriodAttendance.title,
                  item: (
                    <Report
                      path="/reports/holiday-period-attendance"
                      color={colors.main.m2}
                      icon={faChild}
                      i18n={i18n.reports.holidayPeriodAttendance}
                      data-qa="report-holiday-period-attendance"
                    />
                  )
                }
              : null,
            reports.has('TITANIA_ERRORS')
              ? {
                  name: i18n.reports.titaniaErrors.title,
                  item: (
                    <Report
                      data-qa="report-titania-errors"
                      path="/reports/titania-errors"
                      color={colors.status.warning}
                      icon={faDiagnoses}
                      i18n={i18n.reports.titaniaErrors}
                    />
                  )
                }
              : null
          ]

          return (
            <ReportItems>
              {sortBy(
                reportItems.filter((r): r is NamedItem => r !== null),
                (i) => i.name
              ).map(({ item, name }) => (
                <Fragment key={name}>{item}</Fragment>
              ))}
            </ReportItems>
          )
        })}
      </ContentArea>
    </Container>
  )
})
