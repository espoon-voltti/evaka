// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faCircle } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link, useLocation } from 'wouter'

import type { Action } from 'lib-common/generated/action'
import type { ParentshipWithPermittedActions } from 'lib-common/generated/api-types/pis'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
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
import { fontWeights, H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'
import { faUsers } from 'lib-icons'

import type { Translations } from '../../state/i18n'
import { useTranslation } from '../../state/i18n'
import type { TitleState } from '../../state/title'
import { TitleContext } from '../../state/title'
import { UserContext } from '../../state/user'
import CircularLabel from '../common/CircularLabel'
import WarningLabel from '../common/WarningLabel'
import type { Layouts } from '../layouts'
import { getLayout } from '../layouts'
import { parentshipsQuery } from '../person-profile/queries'

import { AbsenceApplicationsSection } from './AbsenceApplicationsSection'
import Assistance from './Assistance'
import BackupCare from './BackupCare'
import ChildApplications from './ChildApplications'
import ChildDetails from './ChildDetails'
import ChildDocumentsSection from './ChildDocumentsSection'
import ChildIncome from './ChildIncome'
import DailyServiceTimesSection from './DailyServiceTimesSection'
import FamilyContacts from './FamilyContacts'
import FeeAlteration from './FeeAlteration'
import GuardiansAndParents from './GuardiansAndParents'
import PedagogicalDocuments from './PedagogicalDocuments'
import Placements from './Placements'
import ServiceApplicationsSection from './ServiceApplicationsSection'
import type { ChildState } from './state'
import { ChildContext, ChildContextProvider } from './state'

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
  childId: ChildId
  startOpen: boolean
}

function section({
  component: Component,
  enabled = true,
  requireOneOfPermittedActions,
  title,
  dataQa
}: {
  component: React.FunctionComponent<{ childId: ChildId }>
  enabled?: boolean
  requireOneOfPermittedActions: (Action.Child | Action.Person)[]
  title: (i18n: Translations) => string
  dataQa?: string
}): React.FunctionComponent<SectionProps> {
  return function Section({ childId, startOpen }: SectionProps) {
    const { permittedActions } = useContext<ChildState>(ChildContext)
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
        <Component childId={childId} />
      </CollapsibleContentArea>
    )
  }
}

const components = {
  income: section({
    component: ChildIncome,
    requireOneOfPermittedActions: ['READ_INCOME'],
    title: (i18n) => i18n.childInformation.income.title,
    dataQa: 'income-collapsible'
  }),
  'fee-alterations': section({
    component: FeeAlteration,
    requireOneOfPermittedActions: ['READ_FEE_ALTERATIONS'],
    title: (i18n) => i18n.childInformation.feeAlteration.title,
    dataQa: 'fee-alteration-collapsible'
  }),
  guardiansAndParents: section({
    component: GuardiansAndParents,
    requireOneOfPermittedActions: ['READ_GUARDIANS'],
    title: (i18n) => i18n.personProfile.guardiansAndParents,
    dataQa: 'person-guardians-collapsible'
  }),
  placements: section({
    component: Placements,
    requireOneOfPermittedActions: ['READ_PLACEMENT'],
    title: (i18n) => i18n.childInformation.placements.title,
    dataQa: 'child-placements-collapsible'
  }),
  absenceApplications: section({
    component: AbsenceApplicationsSection,
    enabled: !!featureFlags.absenceApplications,
    requireOneOfPermittedActions: ['READ_ABSENCE_APPLICATIONS'],
    title: (i18n) => i18n.childInformation.absenceApplications.title,
    dataQa: 'absence-applications-collapsible'
  }),
  serviceApplications: section({
    component: ServiceApplicationsSection,
    enabled: !!featureFlags.serviceApplications,
    requireOneOfPermittedActions: ['READ_SERVICE_APPLICATIONS'],
    title: (i18n) => i18n.childInformation.serviceApplications.title,
    dataQa: 'service-applications-collapsible'
  }),
  'daily-service-times': section({
    component: DailyServiceTimesSection,
    requireOneOfPermittedActions: ['READ_DAILY_SERVICE_TIMES'],
    title: (i18n) => i18n.childInformation.dailyServiceTimes.title,
    dataQa: 'child-daily-service-times-collapsible'
  }),
  childDocuments: section({
    component: ChildDocumentsSection,
    requireOneOfPermittedActions: ['READ_CHILD_DOCUMENT'],
    title: (i18n) => i18n.childInformation.childDocumentsSectionTitle,
    dataQa: 'child-documents-collapsible'
  }),
  pedagogicalDocuments: section({
    component: PedagogicalDocuments,
    requireOneOfPermittedActions: ['READ_PEDAGOGICAL_DOCUMENTS'],
    title: (i18n) => i18n.childInformation.pedagogicalDocument.title,
    dataQa: 'pedagogical-documents-collapsible'
  }),
  assistance: section({
    component: Assistance,
    requireOneOfPermittedActions: [
      'READ_ASSISTANCE',
      'READ_ASSISTANCE_NEED_DECISIONS',
      'READ_ASSISTANCE_NEED_PRESCHOOL_DECISIONS'
    ],
    title: (i18n) => i18n.childInformation.assistance.title,
    dataQa: 'assistance-collapsible'
  }),
  'backup-care': section({
    component: BackupCare,
    requireOneOfPermittedActions: ['READ_BACKUP_CARE'],
    title: (i18n) => i18n.childInformation.backupCares.title,
    dataQa: 'backup-cares-collapsible'
  }),
  'family-contacts': section({
    component: FamilyContacts,
    requireOneOfPermittedActions: ['READ_FAMILY_CONTACTS'],
    title: (i18n) => i18n.childInformation.familyContacts.title,
    dataQa: 'family-contacts-collapsible'
  }),
  applications: section({
    component: ChildApplications,
    requireOneOfPermittedActions: ['READ_APPLICATION'],
    title: (i18n) => i18n.childInformation.application.title,
    dataQa: 'applications-collapsible'
  })
}

const layouts: Layouts<typeof components> = {
  ['ADMIN']: [
    { component: 'family-contacts', open: false },
    { component: 'guardiansAndParents', open: false },
    { component: 'placements', open: false },
    { component: 'absenceApplications', open: false },
    { component: 'serviceApplications', open: false },
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
    { component: 'income', open: false }
  ],
  ['DIRECTOR']: [
    { component: 'family-contacts', open: false },
    { component: 'guardiansAndParents', open: false },
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
    { component: 'income', open: false }
  ],
  ['SERVICE_WORKER']: [
    { component: 'guardiansAndParents', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'assistance', open: false },
    { component: 'applications', open: false },
    { component: 'family-contacts', open: false },
    { component: 'childDocuments', open: false },
    {
      component: 'pedagogicalDocuments',
      open: false
    },
    { component: 'fee-alterations', open: false }
  ],
  ['FINANCE_ADMIN']: [
    { component: 'income', open: true },
    { component: 'fee-alterations', open: true },
    { component: 'guardiansAndParents', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'family-contacts', open: false },
    { component: 'childDocuments', open: false },
    {
      component: 'pedagogicalDocuments',
      open: false
    },
    { component: 'assistance', open: false },
    { component: 'applications', open: false }
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
    { component: 'absenceApplications', open: false },
    { component: 'serviceApplications', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'assistance', open: false },
    { component: 'family-contacts', open: false },
    { component: 'childDocuments', open: false },
    {
      component: 'pedagogicalDocuments',
      open: false
    },
    { component: 'applications', open: false },
    { component: 'fee-alterations', open: false }
  ],
  ['STAFF']: [
    { component: 'family-contacts', open: true },
    { component: 'placements', open: false },
    { component: 'absenceApplications', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'childDocuments', open: false },
    {
      component: 'pedagogicalDocuments',
      open: false
    },
    { component: 'guardiansAndParents', open: false },
    { component: 'assistance', open: false },
    { component: 'applications', open: false },
    { component: 'fee-alterations', open: false }
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
    { component: 'applications', open: false },
    { component: 'fee-alterations', open: false }
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
    { component: 'applications', open: false },
    { component: 'fee-alterations', open: false }
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
  id: ChildId
}) {
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()
  const { roles } = useContext(UserContext)
  const { person } = useContext<ChildState>(ChildContext)

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

  const parentships = useQueryResult(parentshipsQuery({ childId: id }))
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
              <FixedSpaceRow spacing="L">
                {person.isSuccess &&
                  getAge(person.value.dateOfBirth) >= 10 &&
                  getAge(person.value.dateOfBirth) < 18 && (
                    <Button
                      appearance="inline"
                      text={i18n.childInformation.asAdult}
                      onClick={() => navigate(`/profile/${id}`)}
                    />
                  )}
                {currentHeadOfChildId && (
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
                )}
              </FixedSpaceRow>
            </div>
          </HeaderRow>
        </ContentArea>

        <Gap size="m" />

        <ChildDetails id={id} />

        <Gap size="m" />

        <FixedSpaceColumn spacing="m">
          {layout.map(({ component, open }) => {
            const Component = components[component]
            return <Component childId={id} key={component} startOpen={open} />
          })}
        </FixedSpaceColumn>
      </div>
    </Container>
  )
})

export default React.memo(function ChildInformationWrapper() {
  const id = useIdRouteParam<ChildId>('id')
  return (
    <ChildContextProvider id={id}>
      <ChildInformation id={id} />
    </ChildContextProvider>
  )
})
