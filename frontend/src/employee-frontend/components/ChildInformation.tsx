// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faCircle } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import { Action } from 'lib-common/generated/action'
import { ParentshipWithPermittedActions } from 'lib-common/generated/api-types/pis'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import Title from 'lib-components/atoms/Title'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { fontWeights } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faUsers } from 'lib-icons'

import { ChildContext, ChildContextProvider, ChildState } from '../state/child'
import { useTranslation } from '../state/i18n'
import { TitleContext, TitleState } from '../state/title'
import { UserContext } from '../state/user'

import Assistance from './child-information/Assistance'
import BackupCare from './child-information/BackupCare'
import ChildApplications from './child-information/ChildApplications'
import ChildConsentsSection from './child-information/ChildConsentsSection'
import ChildDetails from './child-information/ChildDetails'
import ChildDocumentsSection from './child-information/ChildDocumentsSection'
import ChildIncome from './child-information/ChildIncome'
import DailyServiceTimesSection from './child-information/DailyServiceTimesSection'
import FamilyContacts from './child-information/FamilyContacts'
import FeeAlteration from './child-information/FeeAlteration'
import GuardiansAndParents from './child-information/GuardiansAndParents'
import MessageBlocklist from './child-information/MessageBlocklist'
import PedagogicalDocuments from './child-information/PedagogicalDocuments'
import Placements from './child-information/Placements'
import CircularLabel from './common/CircularLabel'
import WarningLabel from './common/WarningLabel'
import { getLayout, Layouts } from './layouts'

const HeaderRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`

const HeadOfFamilyLink = styled(Link)`
  display: flex;
  padding-bottom: calc(0.375em - 1px);
  padding-top: calc(0.375em - 1px);

  .link-icon {
    font-size: 26px;
    margin-right: 5px;
  }

  .dark-blue {
    color: $blue-darker;
  }

  .link-text {
    color: $blue-darker;
    font-size: 14px;
    font-weight: ${fontWeights.semibold};
    line-height: 26px;
    text-transform: uppercase;
  }
`

interface SectionProps {
  id: UUID
  startOpen: boolean
}

function requireOneOfPermittedActions(
  Component: React.FunctionComponent<SectionProps>,
  ...actions: (Action.Child | Action.Person)[]
): React.FunctionComponent<SectionProps> {
  return function Section({ id, startOpen }: SectionProps) {
    const { permittedActions } = useContext<ChildState>(ChildContext)
    if (actions.some((action) => permittedActions.has(action))) {
      return <Component id={id} startOpen={startOpen} />
    } else {
      return null
    }
  }
}

const components = {
  income: requireOneOfPermittedActions(ChildIncome, 'READ_INCOME'),
  'fee-alterations': requireOneOfPermittedActions(
    FeeAlteration,
    'READ_FEE_ALTERATIONS'
  ),
  guardiansAndParents: requireOneOfPermittedActions(
    GuardiansAndParents,
    'READ_GUARDIANS'
  ),
  placements: requireOneOfPermittedActions(Placements, 'READ_PLACEMENT'),
  'daily-service-times': requireOneOfPermittedActions(
    DailyServiceTimesSection,
    'READ_DAILY_SERVICE_TIMES'
  ),
  childDocuments: requireOneOfPermittedActions(
    ChildDocumentsSection,
    'READ_VASU_DOCUMENT',
    'READ_CHILD_DOCUMENT'
  ),
  pedagogicalDocuments: requireOneOfPermittedActions(
    PedagogicalDocuments,
    'READ_PEDAGOGICAL_DOCUMENTS'
  ),
  assistance: requireOneOfPermittedActions(
    Assistance,
    'READ_ASSISTANCE',
    'READ_ASSISTANCE_NEED_DECISIONS',
    'READ_ASSISTANCE_NEED_PRESCHOOL_DECISIONS'
  ),
  'backup-care': requireOneOfPermittedActions(BackupCare, 'READ_BACKUP_CARE'),
  'family-contacts': requireOneOfPermittedActions(
    FamilyContacts,
    'READ_FAMILY_CONTACTS'
  ),
  applications: requireOneOfPermittedActions(
    ChildApplications,
    'READ_APPLICATION'
  ),
  'message-blocklist': requireOneOfPermittedActions(
    MessageBlocklist,
    'READ_CHILD_RECIPIENTS'
  ),
  'child-consents': requireOneOfPermittedActions(
    ChildConsentsSection,
    'READ_CHILD_CONSENTS'
  )
}

const layouts: Layouts<typeof components> = {
  ['ADMIN']: [
    { component: 'family-contacts', open: false },
    { component: 'guardiansAndParents', open: false },
    { component: 'message-blocklist', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'childDocuments', open: false },
    {
      component: 'pedagogicalDocuments',
      open: false
    },
    { component: 'assistance', open: false },
    { component: 'applications', open: false },
    { component: 'fee-alterations', open: false },
    { component: 'income', open: false },
    { component: 'child-consents', open: false }
  ],
  ['DIRECTOR']: [
    { component: 'family-contacts', open: false },
    { component: 'guardiansAndParents', open: false },
    { component: 'message-blocklist', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'childDocuments', open: false },
    {
      component: 'pedagogicalDocuments',
      open: false
    },
    { component: 'assistance', open: false },
    { component: 'applications', open: false },
    { component: 'fee-alterations', open: false },
    { component: 'income', open: false },
    { component: 'child-consents', open: false }
  ],
  ['SERVICE_WORKER']: [
    { component: 'guardiansAndParents', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'assistance', open: false },
    { component: 'applications', open: false },
    { component: 'family-contacts', open: false },

    { component: 'message-blocklist', open: false },
    { component: 'childDocuments', open: false },
    {
      component: 'pedagogicalDocuments',
      open: false
    },
    { component: 'fee-alterations', open: false },
    { component: 'child-consents', open: false }
  ],
  ['FINANCE_ADMIN']: [
    { component: 'income', open: true },
    { component: 'fee-alterations', open: true },
    { component: 'guardiansAndParents', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },

    { component: 'family-contacts', open: false },
    { component: 'message-blocklist', open: false },
    { component: 'childDocuments', open: false },
    {
      component: 'pedagogicalDocuments',
      open: false
    },
    { component: 'assistance', open: false },
    { component: 'applications', open: false },
    { component: 'child-consents', open: false }
  ],
  ['FINANCE_STAFF']: [
    { component: 'guardiansAndParents', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false }
  ],
  ['UNIT_SUPERVISOR']: [
    { component: 'guardiansAndParents', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'assistance', open: false },
    { component: 'family-contacts', open: false },
    { component: 'childDocuments', open: false },
    {
      component: 'pedagogicalDocuments',
      open: false
    },

    { component: 'message-blocklist', open: false },
    { component: 'applications', open: false },
    { component: 'fee-alterations', open: false },
    { component: 'child-consents', open: false }
  ],
  ['STAFF']: [
    { component: 'family-contacts', open: true },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'childDocuments', open: false },
    {
      component: 'pedagogicalDocuments',
      open: false
    },

    { component: 'guardiansAndParents', open: false },
    { component: 'message-blocklist', open: false },
    { component: 'assistance', open: false },
    { component: 'applications', open: false },
    { component: 'fee-alterations', open: false },
    { component: 'child-consents', open: false }
  ],
  ['SPECIAL_EDUCATION_TEACHER']: [
    { component: 'family-contacts', open: true },
    { component: 'placements', open: false },
    { component: 'childDocuments', open: false },
    {
      component: 'pedagogicalDocuments',
      open: false
    },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'assistance', open: false },

    { component: 'guardiansAndParents', open: false },
    { component: 'message-blocklist', open: false },
    { component: 'applications', open: false },
    { component: 'fee-alterations', open: false },
    { component: 'child-consents', open: false }
  ],
  ['EARLY_CHILDHOOD_EDUCATION_SECRETARY']: [
    { component: 'guardiansAndParents', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'assistance', open: false },
    { component: 'family-contacts', open: false },
    { component: 'childDocuments', open: false },
    {
      component: 'pedagogicalDocuments',
      open: false
    },

    { component: 'message-blocklist', open: false },
    { component: 'applications', open: false },
    { component: 'fee-alterations', open: false },
    { component: 'child-consents', open: false }
  ]
}

function getCurrentHeadOfChildId(
  fridgeRelations: ParentshipWithPermittedActions[]
) {
  const currentDate = LocalDate.todayInSystemTz()
  const currentRelation = fridgeRelations.find(
    ({ data: r }) =>
      !r.startDate.isAfter(currentDate) &&
      (r.endDate ? !r.endDate.isBefore(currentDate) : true)
  )?.data
  return currentRelation?.headOfChildId
}

const ChildInformation = React.memo(function ChildInformation({
  id
}: {
  id: UUID
}) {
  const { i18n } = useTranslation()
  const { roles } = useContext(UserContext)
  const { person, parentships } = useContext<ChildState>(ChildContext)

  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)

  useEffect(() => {
    if (person.isSuccess) {
      const name = formatTitleName(
        person.value.firstName,
        person.value.lastName
      )
      setTitle(`${name} | ${i18n.titles.customers}`)
    }
  }, [formatTitleName, i18n.titles.customers, person, setTitle])

  const layout = useMemo(() => getLayout(layouts, roles), [roles])

  const currentHeadOfChildId = useMemo(
    () => parentships.map(getCurrentHeadOfChildId).getOrElse(undefined),
    [parentships]
  )

  return (
    <Container>
      <div className="child-information-wrapper" data-person-id={id}>
        <ContentArea opaque>
          <HeaderRow>
            <Title size={1} noMargin>
              {i18n.titles.childInformation}
            </Title>
            <div>
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
                <WarningLabel text={i18n.childInformation.restrictedDetails} />
              )}
              {currentHeadOfChildId ? (
                <HeadOfFamilyLink
                  to={`/profile/${currentHeadOfChildId}`}
                  className="person-details-family-link"
                >
                  <span className="fa-layers fa-fw link-icon">
                    <FontAwesomeIcon className="dark-blue" icon={faCircle} />
                    <FontAwesomeIcon
                      icon={faUsers}
                      inverse
                      transform="shrink-6"
                    />
                  </span>
                  <div className="link-text">
                    {i18n.childInformation.personDetails.familyLink}
                  </div>
                </HeadOfFamilyLink>
              ) : null}
            </div>
          </HeaderRow>
        </ContentArea>

        <Gap size="m" />

        <ChildDetails id={id} />

        <Gap size="m" />

        <FixedSpaceColumn spacing="m">
          {layout.map(({ component, open }) => {
            const Component = components[component]
            return <Component id={id} key={component} startOpen={open} />
          })}
        </FixedSpaceColumn>
      </div>
    </Container>
  )
})

export default React.memo(function ChildInformationWrapper() {
  const { id } = useNonNullableParams<{ id: UUID }>()
  return (
    <ChildContextProvider id={id}>
      <ChildInformation id={id} />
    </ChildContextProvider>
  )
})
