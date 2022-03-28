// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useContext, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import styled from 'styled-components'

import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Td } from 'lib-components/layout/Table'
import { defaultMargins } from 'lib-components/white-space'

import CircularLabel from '../components/common/CircularLabel'
import WarningLabel from '../components/common/WarningLabel'
import PersonApplications from '../components/person-profile/PersonApplications'
import PersonDecisions from '../components/person-profile/PersonDecisions'
import PersonDependants from '../components/person-profile/PersonDependants'
import PersonFeeDecisions from '../components/person-profile/PersonFeeDecisions'
import PersonFridgeChild from '../components/person-profile/PersonFridgeChild'
import PersonFridgeHead from '../components/person-profile/PersonFridgeHead'
import PersonFridgePartner from '../components/person-profile/PersonFridgePartner'
import PersonIncome from '../components/person-profile/PersonIncome'
import PersonInvoices from '../components/person-profile/PersonInvoices'
import { useTranslation } from '../state/i18n'
import { PersonContext, PersonContextProvider } from '../state/person'
import { UserContext } from '../state/user'

import { getLayout, Layouts } from './layouts'
import FamilyOverview from './person-profile/PersonFamilyOverview'
import PersonInvoiceCorrections from './person-profile/PersonInvoiceCorrections'
import PersonVoucherValueDecisions from './person-profile/PersonVoucherValueDecisions'

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
  invoiceCorrections: PersonInvoiceCorrections,
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
    { component: 'income', open: false },
    { component: 'fee-decisions', open: false },
    { component: 'invoices', open: false },
    { component: 'invoiceCorrections', open: false },
    { component: 'voucherValueDecisions', open: false }
  ],
  ['FINANCE_ADMIN']: [
    { component: 'family-overview', open: true },
    { component: 'income', open: true },
    { component: 'fee-decisions', open: false },
    { component: 'invoices', open: false },
    { component: 'invoiceCorrections', open: false },
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
    { component: 'family-overview', open: true },
    { component: 'partners', open: false },
    { component: 'fridge-children', open: false },
    { component: 'dependants', open: false },
    { component: 'decisions', open: false }
  ]
}

const PersonProfile = React.memo(function PersonProfile({ id }: { id: UUID }) {
  const { i18n } = useTranslation()

  const { roles } = useContext(UserContext)
  const { person } = useContext(PersonContext)

  const layout = useMemo(() => getLayout(layouts, roles), [roles])

  return (
    <Container>
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
                  background="black"
                  color="white"
                  data-qa="deceased-label"
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
          <PersonFridgeHead />
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

export default React.memo(function PersonProfileWrapper() {
  const { id } = useParams<{ id: UUID }>()
  return (
    <PersonContextProvider id={id}>
      <PersonProfile id={id} />
    </PersonContextProvider>
  )
})
