// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import RoundIcon from 'lib-components/atoms/RoundIcon'
import Title from 'lib-components/atoms/Title'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { fontWeights } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags } from 'lib-customizations/employee'
import {
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
  faUsers
} from 'lib-icons'

import { useTranslation } from '../state/i18n'
import { UserContext } from '../state/user'

import { AssistanceNeedDecisionReportContext } from './reports/AssistanceNeedDecisionReportContext'

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

export default React.memo(function Reports() {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)
  const { assistanceNeedDecisionCounts } = useContext(
    AssistanceNeedDecisionReportContext
  )

  return (
    <Container>
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.title}</Title>
        <ReportItems>
          {user?.permittedGlobalActions.has('READ_DUPLICATE_PEOPLE_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.status.warning}
                  content={faCopy}
                />
                <LinkTitle to="/reports/duplicate-people">
                  {i18n.reports.duplicatePeople.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.duplicatePeople.description}
              </Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has('READ_FAMILY_CONFLICT_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.status.warning}
                  content={faUsers}
                />
                <LinkTitle to="/reports/family-conflicts">
                  {i18n.reports.familyConflicts.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.familyConflicts.description}
              </Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has(
            'READ_MISSING_HEAD_OF_FAMILY_REPORT'
          ) && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.status.warning}
                  content={faUserAltSlash}
                />
                <LinkTitle to="/reports/missing-head-of-family">
                  {i18n.reports.missingHeadOfFamily.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.missingHeadOfFamily.description}
              </Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has(
            'READ_MISSING_SERVICE_NEED_REPORT'
          ) && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.status.warning}
                  content={faClock}
                />
                <LinkTitle to="/reports/missing-service-need">
                  {i18n.reports.missingServiceNeed.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.missingServiceNeed.description}
              </Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has(
            'READ_PARTNERS_IN_DIFFERENT_ADDRESS_REPORT'
          ) && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.status.warning}
                  content={faHome}
                />
                <LinkTitle to="/reports/partners-in-different-address">
                  {i18n.reports.partnersInDifferentAddress.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.partnersInDifferentAddress.description}
              </Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has(
            'READ_CHILD_IN_DIFFERENT_ADDRESS_REPORT'
          ) && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.accents.a5orangeLight}
                  content={faHome}
                />
                <LinkTitle to="/reports/children-in-different-address">
                  {i18n.reports.childrenInDifferentAddress.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.childrenInDifferentAddress.description}
              </Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has('READ_APPLICATIONS_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.main.m2}
                  content={faFileAlt}
                />
                <LinkTitle
                  data-qa="report-applications"
                  to="/reports/applications"
                >
                  {i18n.reports.applications.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.applications.description}</Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has('READ_DECISIONS_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon size="L" color={colors.main.m2} content={faGavel} />
                <LinkTitle data-qa="report-decisions" to="/reports/decisions">
                  {i18n.reports.decisions.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.decisions.description}</Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has('READ_OCCUPANCY_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.main.m2}
                  content={faPercentage}
                />
                <LinkTitle to="/reports/occupancies">
                  {i18n.reports.occupancies.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.occupancies.description}</Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has(
            'READ_CHILD_AGE_AND_LANGUAGE_REPORT'
          ) && (
            <ReportItem>
              <TitleRow>
                <RoundIcon size="L" color={colors.main.m2} content={faChild} />
                <LinkTitle to="/reports/child-age-language">
                  {i18n.reports.childAgeLanguage.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.childAgeLanguage.description}
              </Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has('READ_SERVICE_NEED_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon size="L" color={colors.main.m2} content={faChild} />
                <LinkTitle to="/reports/service-needs">
                  {i18n.reports.serviceNeeds.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.serviceNeeds.description}</Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has(
            'READ_ASSISTANCE_NEEDS_AND_ACTIONS_REPORT'
          ) && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.main.m2}
                  content={faHandHolding}
                />
                <LinkTitle to="/reports/assistance-needs-and-actions">
                  {i18n.reports.assistanceNeedsAndActions.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.assistanceNeedsAndActions.description}
              </Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has('READ_INVOICE_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.main.m2}
                  content={faEuroSign}
                />
                <LinkTitle to="/reports/invoices">
                  {i18n.reports.invoices.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.invoices.description}</Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has(
            'READ_STARTING_PLACEMENTS_REPORT'
          ) && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.main.m2}
                  content={faHourglassStart}
                />
                <LinkTitle to="/reports/starting-placements">
                  {i18n.reports.startingPlacements.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.startingPlacements.description}
              </Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has('READ_ENDED_PLACEMENTS_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.main.m2}
                  content={faHourglassEnd}
                />
                <LinkTitle to="/reports/ended-placements">
                  {i18n.reports.endedPlacements.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.endedPlacements.description}
              </Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has('READ_PRESENCE_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.main.m2}
                  content={faDiagnoses}
                />
                <LinkTitle to="/reports/presences">
                  {i18n.reports.presence.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.presence.description}</Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has('READ_SERVICE_VOUCHER_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.main.m2}
                  content={faMoneyBillWave}
                />
                <LinkTitle
                  data-qa="report-voucher-service-providers"
                  to="/reports/voucher-service-providers"
                >
                  {i18n.reports.voucherServiceProviders.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.voucherServiceProviders.description}
              </Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has('READ_SEXTET_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.main.m2}
                  content={faDiagnoses}
                />
                <LinkTitle data-qa="report-sextet" to="/reports/sextet">
                  {i18n.reports.sextet.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.sextet.description}</Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has('READ_RAW_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.main.m2}
                  content={faDatabase}
                />
                <LinkTitle to="/reports/raw">
                  {i18n.reports.raw.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.raw.description}</Description>
            </ReportItem>
          )}
          {featureFlags.experimental?.assistanceNeedDecisions &&
            user?.permittedGlobalActions.has(
              'READ_ASSISTANCE_NEED_DECISIONS_REPORT'
            ) && (
              <ReportItem>
                <TitleRow>
                  <RoundIcon
                    size="L"
                    color={colors.main.m2}
                    content={faHandHolding}
                  />
                  <LinkTitle to="/reports/assistance-need-decisions">
                    {i18n.reports.assistanceNeedDecisions.title}
                  </LinkTitle>
                  {assistanceNeedDecisionCounts
                    .map(
                      (unread) =>
                        unread > 0 && <UnreadCount>{unread}</UnreadCount>
                    )
                    .getOrElse(null)}
                </TitleRow>
                <Description>
                  {i18n.reports.assistanceNeedDecisions.description}
                </Description>
              </ReportItem>
            )}
          {user?.permittedGlobalActions.has(
            'READ_PLACEMENT_SKETCHING_REPORT'
          ) && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.status.warning}
                  content={faUsers}
                />
                <LinkTitle
                  to="/reports/placement-sketching"
                  data-qa="report-placement-sketching"
                >
                  {i18n.reports.placementSketching.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.placementSketching.description}
              </Description>
            </ReportItem>
          )}
          {user?.permittedGlobalActions.has('READ_VARDA_REPORT') && (
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.status.warning}
                  content={faDiagnoses}
                />
                <LinkTitle
                  to="/reports/varda-errors"
                  data-qa="report-varda-errors"
                >
                  {i18n.reports.vardaErrors.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.vardaErrors.description}</Description>
            </ReportItem>
          )}
        </ReportItems>
      </ContentArea>
    </Container>
  )
})
