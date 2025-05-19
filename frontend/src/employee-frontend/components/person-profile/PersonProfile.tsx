// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import type { Action } from 'lib-common/generated/action'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { useIdRouteParam } from 'lib-common/useRouteParams'
import { getAge } from 'lib-common/utils/local-date'
import Title from 'lib-components/atoms/Title'
import { Button } from 'lib-components/atoms/buttons/Button'
import {
  CollapsibleContentArea,
  Container,
  ContentArea
} from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faListTimeline } from 'lib-icons'

import type { Translations } from '../../state/i18n'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'
import CircularLabel from '../common/CircularLabel'
import WarningLabel from '../common/WarningLabel'
import type { Layouts } from '../layouts'
import { getLayout } from '../layouts'

import FosterChildren from './FosterChildren'
import PersonApplications from './PersonApplications'
import PersonDecisions from './PersonDecisions'
import PersonDependants from './PersonDependants'
import FamilyOverview from './PersonFamilyOverview'
import PersonFeeDecisions from './PersonFeeDecisions'
import PersonFinanceNotesAndMessages from './PersonFinanceNotesAndMessages'
import PersonFridgeChild from './PersonFridgeChild'
import PersonFridgeHead from './PersonFridgeHead'
import PersonFridgePartner from './PersonFridgePartner'
import PersonIncome from './PersonIncome'
import PersonInvoiceCorrections from './PersonInvoiceCorrections'
import PersonInvoices from './PersonInvoices'
import PersonVoucherValueDecisions from './PersonVoucherValueDecisions'
import { HeaderRow, InfoLabelContainer } from './common'
import type { PersonState } from './state'
import { PersonContext, PersonContextProvider } from './state'

interface SectionProps {
  id: PersonId
  open: boolean
}

function section({
  component: Component,
  enabled = true,
  requireOneOfPermittedActions,
  title,
  dataQa
}: {
  component: React.FunctionComponent<{ id: PersonId }>
  enabled?: boolean
  requireOneOfPermittedActions: Action.Person[]
  title: (i18n: Translations) => string
  dataQa?: string
}): React.FunctionComponent<SectionProps> {
  return function Section({ id, open: startOpen }: SectionProps) {
    const { permittedActions } = useContext<PersonState>(PersonContext)
    const { i18n } = useTranslation()
    const [open, setOpen] = useState(startOpen)
    if (
      !enabled ||
      !requireOneOfPermittedActions.some((action) =>
        permittedActions.has(action)
      )
    ) {
      return null
    }
    return (
      <CollapsibleContentArea
        title={<H2 noMargin>{title(i18n)}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa={dataQa}
      >
        <Component id={id} />
      </CollapsibleContentArea>
    )
  }
}

const components = {
  'family-overview': section({
    component: FamilyOverview,
    requireOneOfPermittedActions: ['READ_FAMILY_OVERVIEW'],
    title: (i18n) => i18n.personProfile.familyOverview.title,
    dataQa: 'family-overview-collapsible'
  }),
  income: section({
    component: PersonIncome,
    requireOneOfPermittedActions: ['READ_INCOME_STATEMENTS', 'READ_INCOME'],
    title: (i18n) => i18n.personProfile.income.title,
    dataQa: 'person-income-collapsible'
  }),
  'fee-decisions': section({
    component: PersonFeeDecisions,
    requireOneOfPermittedActions: ['READ_FEE_DECISIONS'],
    title: (i18n) => i18n.personProfile.feeDecisions.title,
    dataQa: 'person-fee-decisions-collapsible'
  }),
  invoices: section({
    component: PersonInvoices,
    requireOneOfPermittedActions: ['READ_INVOICES'],
    title: (i18n) => i18n.personProfile.invoices,
    dataQa: 'person-invoices-collapsible'
  }),
  invoiceCorrections: section({
    component: PersonInvoiceCorrections,
    requireOneOfPermittedActions: ['READ_INVOICE_CORRECTIONS'],
    title: (i18n) => i18n.personProfile.invoiceCorrections.title,
    dataQa: 'person-invoice-corrections-collapsible'
  }),
  voucherValueDecisions: section({
    component: PersonVoucherValueDecisions,
    requireOneOfPermittedActions: ['READ_VOUCHER_VALUE_DECISIONS'],
    title: (i18n) => i18n.personProfile.voucherValueDecisions.title,
    dataQa: 'person-voucher-value-decisions-collapsible'
  }),
  partners: section({
    component: PersonFridgePartner,
    requireOneOfPermittedActions: ['READ_PARTNERSHIPS'],
    title: (i18n) => i18n.personProfile.partner,
    dataQa: 'person-partners-collapsible'
  }),
  'fridge-children': section({
    component: PersonFridgeChild,
    requireOneOfPermittedActions: ['READ_PARENTSHIPS'],
    title: (i18n) => i18n.personProfile.fridgeChildOfHead,
    dataQa: 'person-children-collapsible'
  }),
  dependants: section({
    component: PersonDependants,
    requireOneOfPermittedActions: ['READ_DEPENDANTS'],
    title: (i18n) => i18n.personProfile.dependants,
    dataQa: 'person-dependants-collapsible'
  }),
  fosterChildren: section({
    component: FosterChildren,
    requireOneOfPermittedActions: ['READ_FOSTER_CHILDREN'],
    title: (i18n) => i18n.personProfile.fosterChildren.sectionTitle,
    dataQa: 'person-foster-children-collapsible'
  }),
  applications: section({
    component: PersonApplications,
    requireOneOfPermittedActions: ['READ_APPLICATIONS'],
    title: (i18n) => i18n.personProfile.applications,
    dataQa: 'person-applications-collapsible'
  }),
  decisions: section({
    component: PersonDecisions,
    requireOneOfPermittedActions: ['READ_DECISIONS'],
    title: (i18n) => i18n.personProfile.decision.decisions,
    dataQa: 'person-decisions-collapsible'
  }),
  'notes-and-messages': section({
    component: PersonFinanceNotesAndMessages,
    requireOneOfPermittedActions: ['READ_FINANCE_NOTES'],
    title: (i18n) => i18n.personProfile.financeNotesAndMessages.title,
    dataQa: 'person-finance-notes-and-messages-collapsible'
  })
}

const layouts: Layouts<typeof components> = {
  ['ADMIN']: [
    { component: 'notes-and-messages', open: true },
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
    { component: 'notes-and-messages', open: true },
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
    { component: 'notes-and-messages', open: true },
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

const PersonProfile = React.memo(function PersonProfile({
  id
}: {
  id: PersonId
}) {
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
              <FixedSpaceRow spacing="L">
                {person.isSuccess &&
                  getAge(person.value.dateOfBirth) >= 10 &&
                  getAge(person.value.dateOfBirth) < 18 && (
                    <Button
                      appearance="inline"
                      text={i18n.personProfile.asChild}
                      onClick={() => navigate(`/child-information/${id}`)}
                    />
                  )}
                {permittedActions.has('READ_TIMELINE') && (
                  <a href={`/employee/profile/${id}/timeline`}>
                    <Button
                      appearance="inline"
                      text={i18n.personProfile.timeline}
                      onClick={() => undefined}
                      icon={faListTimeline}
                      data-qa="timeline-button"
                    />
                  </a>
                )}
              </FixedSpaceRow>
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
  const id = useIdRouteParam<PersonId>('id')
  return (
    <PersonContextProvider id={id}>
      <PersonProfile id={id} />
    </PersonContextProvider>
  )
})
