// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useMemo } from 'react'
import { RouteComponentProps } from 'react-router'
import { UUID } from '../types'
import { Container, ContentArea } from 'lib-components/layout/Container'
import Title from 'lib-components/atoms/Title'
import { Td } from 'lib-components/layout/Table'
import PersonFridgePartner from '../components/person-profile/PersonFridgePartner'
import PersonFridgeChild from '../components/person-profile/PersonFridgeChild'
import PersonFridgeHead from '../components/person-profile/PersonFridgeHead'
import PersonIncome from '../components/person-profile/PersonIncome'
import PersonApplications from '../components/person-profile/PersonApplications'
import PersonDependants from '../components/person-profile/PersonDependants'
import PersonDecisions from '../components/person-profile/PersonDecisions'
import PersonVoucherValueDecisions from './person-profile/PersonVoucherValueDecisions'
import WarningLabel from '../components/common/WarningLabel'
import { getLayout, Layouts } from './layouts'
import { UserContext } from '../state/user'
import { PersonContext } from '../state/person'
import PersonFeeDecisions from '../components/person-profile/PersonFeeDecisions'
import PersonInvoices from '../components/person-profile/PersonInvoices'
import styled from 'styled-components'
import FamilyOverview from './person-profile/PersonFamilyOverview'
import { useTranslation } from '../state/i18n'
import CircularLabel from '../components/common/CircularLabel'
import { Gap, defaultMargins } from 'lib-components/white-space'

export const NameTd = styled(Td)`
  width: 30%;
`

export const DateTd = styled(Td)`
  width: 12%;
`

export const ButtonsTd = styled(Td)`
  width: 13%;
`

export const StatusTd = styled(Td)`
  width: 13%;
`

export const HeaderRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
`

export const InfoLabelContainer = styled.div`
  display: flex;
  > div:not(:last-child) {
    margin-right: ${defaultMargins.xs};
  }
`

const components = {
  'family-overview': FamilyOverview,
  income: PersonIncome,
  'fee-decisions': PersonFeeDecisions,
  invoices: PersonInvoices,
  voucherValueDecisions: PersonVoucherValueDecisions,
  partners: PersonFridgePartner,
  'fridge-children': PersonFridgeChild,
  dependants: PersonDependants,
  applications: PersonApplications,
  decisions: PersonDecisions
}

const layouts: Layouts<typeof components> = {
  ['ADMIN']: [
    { component: 'family-overview', open: true },
    { component: 'partners', open: false },
    { component: 'fridge-children', open: false },
    { component: 'dependants', open: false },
    { component: 'applications', open: false },
    { component: 'decisions', open: false },
    { component: 'income', open: true },
    { component: 'fee-decisions', open: false },
    { component: 'invoices', open: false },
    { component: 'voucherValueDecisions', open: false }
  ],
  ['FINANCE_ADMIN']: [
    { component: 'family-overview', open: true },
    { component: 'income', open: true },
    { component: 'fee-decisions', open: false },
    { component: 'invoices', open: false },
    { component: 'voucherValueDecisions', open: false },
    { component: 'partners', open: false },
    { component: 'fridge-children', open: false },
    { component: 'dependants', open: false }
  ],
  ['SERVICE_WORKER']: [
    { component: 'partners', open: false },
    { component: 'fridge-children', open: false },
    { component: 'dependants', open: false },
    { component: 'applications', open: false },
    { component: 'decisions', open: false }
  ],
  ['UNIT_SUPERVISOR']: [
    { component: 'partners', open: false },
    { component: 'fridge-children', open: false },
    { component: 'dependants', open: false },
    { component: 'decisions', open: false }
  ]
}

const PersonProfile = React.memo(function PersonProfile({
  match
}: RouteComponentProps<{ id: UUID }>) {
  const { id } = match.params
  const { i18n } = useTranslation()

  const { roles } = useContext(UserContext)
  const { person } = useContext(PersonContext)

  const layout = useMemo(() => getLayout(layouts, roles), [roles])

  return (
    <Container>
      <Gap size={'L'} />
      <ContentArea opaque>
        <div className="person-profile-wrapper" data-person-id={id}>
          <HeaderRow>
            <Title size={1}> {i18n.personProfile.title}</Title>
            <InfoLabelContainer>
              {person.isSuccess && person.value.dateOfDeath && (
                <CircularLabel
                  text={`${i18n.common.form.dateOfDeath}: ${
                    person.value.dateOfDeath?.format() ?? ''
                  }`}
                  background={`black`}
                  color={`white`}
                  data-qa="deceaced-label"
                />
              )}
              {person.isSuccess && person.value.restrictedDetailsEnabled && (
                <WarningLabel
                  text={i18n.personProfile.restrictedDetails}
                  data-qa="restriction-details-enabled-label"
                />
              )}
            </InfoLabelContainer>
          </HeaderRow>
          <PersonFridgeHead id={id} />
          {layout.map(({ component, open }) => {
            const Component = components[component]
            return (
              <Fragment key={component}>
                <div className="separator-gap-small" />
                <Component id={id} open={open} />
              </Fragment>
            )
          })}
        </div>
      </ContentArea>
    </Container>
  )
})

export default PersonProfile
