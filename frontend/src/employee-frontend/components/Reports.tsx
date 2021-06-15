// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Container, ContentArea } from 'lib-components/layout/Container'
import Title from 'lib-components/atoms/Title'
import { Gap } from 'lib-components/white-space'
import { useTranslation } from '../state/i18n'
import { Link } from 'react-router-dom'
import styled from 'styled-components'
import {
  faChild,
  faClock,
  faHomeAlt,
  faHourglassEnd,
  faHourglassStart,
  faPercentage,
  faUserAltSlash,
  faUsers,
  faEuroSign,
  faHandHolding,
  faCopy,
  faFileAlt,
  faDiagnoses,
  faDatabase,
  faMoneyBillWave,
  faGavel
} from 'lib-icons'
import colors from 'lib-customizations/common'
import { RequireRole } from '../utils/roles'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { defaultMargins } from 'lib-components/white-space'
import { featureFlags } from '../config'

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
  font-weight: 600;
`
const Description = styled.p`
  margin-left: calc(34px + ${defaultMargins.s});
  width: 70%;
`

function Reports() {
  const { i18n } = useTranslation()

  return (
    <Container>
      <Gap size={'L'} />
      <ContentArea opaque>
        <Title size={1}>{i18n.reports.title}</Title>
        <ReportItems>
          <RequireRole oneOf={['ADMIN']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.accents.orange}
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
          </RequireRole>
          <RequireRole
            oneOf={[
              'ADMIN',
              'SERVICE_WORKER',
              'FINANCE_ADMIN',
              'UNIT_SUPERVISOR'
            ]}
          >
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.accents.orange}
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
          </RequireRole>
          <RequireRole
            oneOf={[
              'ADMIN',
              'SERVICE_WORKER',
              'FINANCE_ADMIN',
              'UNIT_SUPERVISOR'
            ]}
          >
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.accents.orange}
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
          </RequireRole>
          <RequireRole
            oneOf={[
              'ADMIN',
              'SERVICE_WORKER',
              'FINANCE_ADMIN',
              'UNIT_SUPERVISOR'
            ]}
          >
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.accents.orange}
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
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'SERVICE_WORKER', 'FINANCE_ADMIN']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.accents.orange}
                  content={faHomeAlt}
                />
                <LinkTitle to="/reports/partners-in-different-address">
                  {i18n.reports.partnersInDifferentAddress.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.partnersInDifferentAddress.description}
              </Description>
            </ReportItem>
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'SERVICE_WORKER', 'FINANCE_ADMIN']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.accents.yellow}
                  content={faHomeAlt}
                />
                <LinkTitle to="/reports/children-in-different-address">
                  {i18n.reports.childrenInDifferentAddress.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.childrenInDifferentAddress.description}
              </Description>
            </ReportItem>
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'SERVICE_WORKER', 'DIRECTOR']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.primary}
                  content={faFileAlt}
                />
                <LinkTitle
                  data-qa={'report-applications'}
                  to="/reports/applications"
                >
                  {i18n.reports.applications.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.applications.description}</Description>
            </ReportItem>
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'SERVICE_WORKER', 'DIRECTOR']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon size="L" color={colors.primary} content={faGavel} />
                <LinkTitle data-qa={'report-decisions'} to="/reports/decisions">
                  {i18n.reports.decisions.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.decisions.description}</Description>
            </ReportItem>
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'SERVICE_WORKER', 'DIRECTOR']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.primary}
                  content={faPercentage}
                />
                <LinkTitle to="/reports/occupancies">
                  {i18n.reports.occupancies.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.occupancies.description}</Description>
            </ReportItem>
          </RequireRole>
          <RequireRole
            oneOf={[
              'ADMIN',
              'SERVICE_WORKER',
              'DIRECTOR',
              'SPECIAL_EDUCATION_TEACHER'
            ]}
          >
            <ReportItem>
              <TitleRow>
                <RoundIcon size="L" color={colors.primary} content={faChild} />
                <LinkTitle to="/reports/child-age-language">
                  {i18n.reports.childAgeLanguage.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.childAgeLanguage.description}
              </Description>
            </ReportItem>
          </RequireRole>
          <RequireRole
            oneOf={['ADMIN', 'SERVICE_WORKER', 'DIRECTOR', 'UNIT_SUPERVISOR']}
          >
            <ReportItem>
              <TitleRow>
                <RoundIcon size="L" color={colors.primary} content={faChild} />
                <LinkTitle to="/reports/service-needs">
                  {i18n.reports.serviceNeeds.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.serviceNeeds.description}</Description>
            </ReportItem>
          </RequireRole>
          <RequireRole
            oneOf={[
              'ADMIN',
              'SERVICE_WORKER',
              'DIRECTOR',
              'UNIT_SUPERVISOR',
              'SPECIAL_EDUCATION_TEACHER'
            ]}
          >
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.primary}
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
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'FINANCE_ADMIN']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.primary}
                  content={faEuroSign}
                />
                <LinkTitle to="/reports/invoices">
                  {i18n.reports.invoices.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.invoices.description}</Description>
            </ReportItem>
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'FINANCE_ADMIN']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.primary}
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
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'FINANCE_ADMIN']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.primary}
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
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'DIRECTOR']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.primary}
                  content={faDiagnoses}
                />
                <LinkTitle to="/reports/presences">
                  {i18n.reports.presence.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.presence.description}</Description>
            </ReportItem>
          </RequireRole>
          {featureFlags.voucherValueDecisionsPage && (
            <RequireRole
              oneOf={['ADMIN', 'FINANCE_ADMIN', 'DIRECTOR', 'UNIT_SUPERVISOR']}
            >
              <ReportItem>
                <TitleRow>
                  <RoundIcon
                    size="L"
                    color={colors.primary}
                    content={faMoneyBillWave}
                  />
                  <LinkTitle
                    data-qa={'report-voucher-service-providers'}
                    to="/reports/voucher-service-providers"
                  >
                    {i18n.reports.voucherServiceProviders.title}
                  </LinkTitle>
                </TitleRow>
                <Description>
                  {i18n.reports.voucherServiceProviders.description}
                </Description>
              </ReportItem>
            </RequireRole>
          )}
          <RequireRole oneOf={['ADMIN', 'DIRECTOR']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.primary}
                  content={faDatabase}
                />
                <LinkTitle to="/reports/raw">
                  {i18n.reports.raw.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.raw.description}</Description>
            </ReportItem>
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'SERVICE_WORKER']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={colors.accents.orange}
                  content={faUsers}
                />
                <LinkTitle
                  to="/reports/placement-sketching"
                  data-qa={'report-placement-sketching'}
                >
                  {i18n.reports.placementSketching.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.placementSketching.description}
              </Description>
            </ReportItem>
          </RequireRole>
        </ReportItems>
      </ContentArea>
    </Container>
  )
}

export default Reports
