// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faListTimeline } from 'Icons'
import React, { useContext, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import styled from 'styled-components'

import { Action } from 'lib-common/generated/action'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Title from 'lib-components/atoms/Title'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Td } from 'lib-components/layout/Table'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { defaultMargins, Gap } from 'lib-components/white-space'

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
import {
  PersonContext,
  PersonContextProvider,
  PersonState
} from '../state/person'
import { UserContext } from '../state/user'

import { getLayout, Layouts } from './layouts'
import FosterChildren from './person-profile/FosterChildren'
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

interface SectionProps {
  id: UUID
  open: boolean
}

function requireOneOfPermittedActions(
  Component: React.FunctionComponent<SectionProps>,
  ...actions: Action.Person[]
): React.FunctionComponent<SectionProps> {
  return function Section({ id, open }: SectionProps) {
    const { permittedActions } = useContext<PersonState>(PersonContext)
    if (actions.some((action) => permittedActions.has(action))) {
      return <Component id={id} open={open} />
    } else {
      return null
    }
  }
}

const components = {
  'family-overview': requireOneOfPermittedActions(
    FamilyOverview,
    'READ_FAMILY_OVERVIEW'
  ),
  income: requireOneOfPermittedActions(
    PersonIncome,
    'READ_INCOME_STATEMENTS',
    'READ_INCOME'
  ),
  'fee-decisions': requireOneOfPermittedActions(
    PersonFeeDecisions,
    'READ_FEE_DECISIONS'
  ),
  invoices: requireOneOfPermittedActions(PersonInvoices, 'READ_INVOICES'),
  invoiceCorrections: requireOneOfPermittedActions(
    PersonInvoiceCorrections,
    'READ_INVOICE_CORRECTIONS'
  ),
  voucherValueDecisions: requireOneOfPermittedActions(
    PersonVoucherValueDecisions,
    'READ_VOUCHER_VALUE_DECISIONS'
  ),
  partners: requireOneOfPermittedActions(
    PersonFridgePartner,
    'READ_PARTNERSHIPS'
  ),
  'fridge-children': requireOneOfPermittedActions(
    PersonFridgeChild,
    'READ_PARENTSHIPS'
  ),
  dependants: requireOneOfPermittedActions(PersonDependants, 'READ_DEPENDANTS'),
  fosterChildren: requireOneOfPermittedActions(
    FosterChildren,
    'READ_FOSTER_CHILDREN'
  ),
  applications: requireOneOfPermittedActions(
    PersonApplications,
    'READ_APPLICATIONS'
  ),
  decisions: requireOneOfPermittedActions(PersonDecisions, 'READ_DECISIONS')
}

const layouts: Layouts<typeof components> = {
  ['ADMIN']: [
    { component: 'family-overview', open: true },
    { component: 'partners', open: false },
    { component: 'fridge-children', open: false },
    { component: 'dependants', open: false },
    { component: 'fosterChildren', open: false },
    { component: 'applications', open: false },
    { component: 'decisions', open: false },
    { component: 'income', open: false },
    { component: 'fee-decisions', open: false },
    { component: 'invoices', open: false },
    { component: 'invoiceCorrections', open: false },
    { component: 'voucherValueDecisions', open: false }
  ],
  ['DIRECTOR']: [
    { component: 'family-overview', open: true },
    { component: 'partners', open: false },
    { component: 'fridge-children', open: false },
    { component: 'dependants', open: false },
    { component: 'fosterChildren', open: false },
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
    { component: 'dependants', open: false },
    { component: 'fosterChildren', open: false }
  ],
  ['FINANCE_STAFF']: [
    { component: 'family-overview', open: true },
    { component: 'fee-decisions', open: false },
    { component: 'invoices', open: false },
    { component: 'invoiceCorrections', open: false },
    { component: 'partners', open: false }
  ],
  ['SERVICE_WORKER']: [
    { component: 'family-overview', open: true },
    { component: 'partners', open: false },
    { component: 'fridge-children', open: false },
    { component: 'dependants', open: false },
    { component: 'fosterChildren', open: false },
    { component: 'applications', open: false },
    { component: 'decisions', open: false }
  ],
  ['UNIT_SUPERVISOR']: [
    { component: 'family-overview', open: true },
    { component: 'partners', open: false },
    { component: 'fridge-children', open: false },
    { component: 'dependants', open: false },
    { component: 'fosterChildren', open: false },
    { component: 'decisions', open: false }
  ],
  ['EARLY_CHILDHOOD_EDUCATION_SECRETARY']: [
    { component: 'family-overview', open: true },
    { component: 'partners', open: false },
    { component: 'fridge-children', open: false },
    { component: 'dependants', open: false },
    { component: 'fosterChildren', open: false },
    { component: 'decisions', open: false }
  ]
}

const PersonProfile = React.memo(function PersonProfile({ id }: { id: UUID }) {
  const { i18n } = useTranslation()
  const navigate = useNavigate()

  const { roles } = useContext(UserContext)
  const { person, permittedActions } = useContext(PersonContext)

  const layout = useMemo(() => getLayout(layouts, roles), [roles])

  return (
    <Container>
      <div className="person-profile-wrapper" data-person-id={id}>
        <ContentArea opaque>
          <HeaderRow>
            <Title size={1} noMargin>
              {i18n.titles.personProfile}
            </Title>
            <FixedSpaceColumn>
              {person.isSuccess &&
                (person.value.dateOfDeath ||
                  person.value.restrictedDetailsEnabled) && (
                  <InfoLabelContainer>
                    {person.value.dateOfDeath && (
                      <CircularLabel
                        text={`${i18n.common.form.dateOfDeath}: ${
                          person.value.dateOfDeath?.format() ?? ''
                        }`}
                        background="black"
                        color="white"
                        data-qa="deceased-label"
                      />
                    )}
                    {person.value.restrictedDetailsEnabled && (
                      <WarningLabel
                        text={i18n.personProfile.restrictedDetails}
                        data-qa="restriction-details-enabled-label"
                      />
                    )}
                  </InfoLabelContainer>
                )}
              {permittedActions.has('READ_TIMELINE') && (
                <InlineButton
                  text={i18n.personProfile.timeline}
                  onClick={() => navigate(`/profile/${id}/timeline`)}
                  icon={faListTimeline}
                  data-qa="timeline-button"
                />
              )}
            </FixedSpaceColumn>
          </HeaderRow>
        </ContentArea>
        <Gap size="s" />
        <PersonFridgeHead />
        <Gap size="s" />
        <FixedSpaceColumn spacing="s">
          {layout.map(({ component, open }) => {
            const Component = components[component]
            return <Component key={component} id={id} open={open} />
          })}
        </FixedSpaceColumn>
      </div>
    </Container>
  )
})

export default React.memo(function PersonProfileWrapper() {
  const { id } = useNonNullableParams<{ id: UUID }>()
  return (
    <PersonContextProvider id={id}>
      <PersonProfile id={id} />
    </PersonContextProvider>
  )
})
