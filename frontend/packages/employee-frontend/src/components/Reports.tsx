// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Container, ContentArea } from 'components/shared/layout/Container'
import Title from './shared/atoms/Title'
import { Gap } from 'components/shared/layout/white-space'
import { useTranslation } from '~state/i18n'
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
  faDatabase
} from 'icon-set'
import { EspooColours } from '~utils/colours'
import { RequireRole } from '~utils/roles'
import RoundIcon from 'components/shared/atoms/RoundIcon'
import { DefaultMargins } from 'components/shared/layout/white-space'

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
    margin-right: ${DefaultMargins.s};
  }
`

const LinkTitle = styled(Link)`
  text-transform: uppercase;
  margin: 0;
  font-size: 0.9rem;
  font-weight: 600;
`
const Description = styled.p`
  margin-left: calc(34px + ${DefaultMargins.s});
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
                  color={EspooColours.orange}
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
                  color={EspooColours.orange}
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
                  color={EspooColours.orange}
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
                  color={EspooColours.orange}
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
                  color={EspooColours.orange}
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
                  color={EspooColours.yellow}
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
                  color={EspooColours.bluePrimary}
                  content={faFileAlt}
                />
                <LinkTitle to="/reports/applications">
                  {i18n.reports.applications.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.applications.description}</Description>
            </ReportItem>
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'SERVICE_WORKER', 'DIRECTOR']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={EspooColours.bluePrimary}
                  content={faPercentage}
                />
                <LinkTitle to="/reports/occupancies">
                  {i18n.reports.occupancies.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.occupancies.description}</Description>
            </ReportItem>
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'SERVICE_WORKER', 'DIRECTOR']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={EspooColours.bluePrimary}
                  content={faChild}
                />
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
                <RoundIcon
                  size="L"
                  color={EspooColours.bluePrimary}
                  content={faChild}
                />
                <LinkTitle to="/reports/service-needs">
                  {i18n.reports.serviceNeeds.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.serviceNeeds.description}</Description>
            </ReportItem>
          </RequireRole>
          <RequireRole
            oneOf={['ADMIN', 'SERVICE_WORKER', 'DIRECTOR', 'UNIT_SUPERVISOR']}
          >
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={EspooColours.bluePrimary}
                  content={faHandHolding}
                />
                <LinkTitle to="/reports/assistance-needs">
                  {i18n.reports.assistanceNeeds.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.assistanceNeeds.description}
              </Description>
            </ReportItem>
          </RequireRole>
          <RequireRole
            oneOf={['ADMIN', 'SERVICE_WORKER', 'DIRECTOR', 'UNIT_SUPERVISOR']}
          >
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={EspooColours.bluePrimary}
                  content={faHandHolding}
                />
                <LinkTitle to="/reports/assistance-actions">
                  {i18n.reports.assistanceActions.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.assistanceActions.description}
              </Description>
            </ReportItem>
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'FINANCE_ADMIN', 'DIRECTOR']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={EspooColours.bluePrimary}
                  content={faEuroSign}
                />
                <LinkTitle to="/reports/invoices">
                  {i18n.reports.invoices.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.invoices.description}</Description>
            </ReportItem>
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'FINANCE_ADMIN', 'DIRECTOR']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={EspooColours.bluePrimary}
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
          <RequireRole oneOf={['ADMIN', 'FINANCE_ADMIN', 'DIRECTOR']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={EspooColours.bluePrimary}
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
                  color={EspooColours.bluePrimary}
                  content={faDiagnoses}
                />
                <LinkTitle to="/reports/presences">
                  {i18n.reports.presence.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.presence.description}</Description>
            </ReportItem>
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'DIRECTOR']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={EspooColours.bluePrimary}
                  content={faDiagnoses}
                />
                <LinkTitle to="/reports/voucher-service-providers">
                  {i18n.reports.voucherServiceProviders.title}
                </LinkTitle>
              </TitleRow>
              <Description>
                {i18n.reports.voucherServiceProviders.description}
              </Description>
            </ReportItem>
          </RequireRole>
          <RequireRole oneOf={['ADMIN', 'DIRECTOR']}>
            <ReportItem>
              <TitleRow>
                <RoundIcon
                  size="L"
                  color={EspooColours.bluePrimary}
                  content={faDatabase}
                />
                <LinkTitle to="/reports/raw">
                  {i18n.reports.raw.title}
                </LinkTitle>
              </TitleRow>
              <Description>{i18n.reports.raw.description}</Description>
            </ReportItem>
          </RequireRole>
        </ReportItems>
      </ContentArea>
    </Container>
  )
}

export default Reports
