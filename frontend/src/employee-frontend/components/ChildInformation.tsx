// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useMemo } from 'react'
import styled from 'styled-components'

import { useTranslation } from '../state/i18n'
import { Link, RouteComponentProps } from 'react-router-dom'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCircle } from '@fortawesome/free-solid-svg-icons'
import { faUsers } from 'lib-icons'
import LocalDate from 'lib-common/local-date'
import { Container, ContentArea } from 'lib-components/layout/Container'
import Title from 'lib-components/atoms/Title'
import { Gap } from 'lib-components/white-space'
import ChildDetails from '../components/child-information/ChildDetails'
import ServiceNeed from '../components/child-information/ServiceNeed'
import Assistance from '../components/child-information/Assistance'
import FeeAlteration from '../components/child-information/FeeAlteration'
import { UUID } from '../types'
import { Parentship } from '../types/fridge'
import Placements from '../components/child-information/Placements'
import { getPersonDetails } from '../api/person'
import { Loading } from 'lib-common/api'
import WarningLabel from '../components/common/WarningLabel'
import { ChildContext, ChildState } from '../state/child'
import FridgeParents from '../components/child-information/FridgeParents'
import { getParentshipsByChild } from '../api/parentships'
import { UserContext } from '../state/user'
import { TitleContext, TitleState } from '../state/title'
import { getLayout, Layouts } from './layouts'
import BackupCare from '../components/child-information/BackupCare'
import Guardians from '../components/child-information/Guardians'
import FamilyContacts from '../components/child-information/FamilyContacts'
import { requireRole } from '../utils/roles'
import ChildApplications from '../components/child-information/ChildApplications'
import MessageBlocklist from '@evaka/employee-frontend/components/child-information/MessageBlocklist'

const HeaderRow = styled.div`
  display: flex;
  justify-content: space-between;
  width: 70%;
`

const Section = styled.div`
  margin-bottom: 70px;
  width: 70%;
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
    font-weight: 600;
    line-height: 26px;
    text-transform: uppercase;
  }
`

const components = {
  'fee-alterations': FeeAlteration,
  guardians: Guardians,
  parents: FridgeParents,
  placements: Placements,
  'service-need': ServiceNeed,
  assistance: Assistance,
  'backup-care': BackupCare,
  'family-contacts': FamilyContacts,
  applications: ChildApplications,
  'message-blocklist': MessageBlocklist
}

const layouts: Layouts<typeof components> = {
  ['ADMIN']: [
    { component: 'family-contacts', open: false },
    { component: 'guardians', open: false },
    { component: 'parents', open: false },
    { component: 'message-blocklist', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'service-need', open: false },
    { component: 'assistance', open: false },
    { component: 'applications', open: false },
    { component: 'fee-alterations', open: false }
  ],
  ['SERVICE_WORKER']: [
    { component: 'guardians', open: false },
    { component: 'parents', open: false },
    { component: 'message-blocklist', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'service-need', open: false },
    { component: 'assistance', open: false },
    { component: 'applications', open: false }
  ],
  ['FINANCE_ADMIN']: [
    { component: 'fee-alterations', open: true },
    { component: 'guardians', open: false },
    { component: 'parents', open: false },
    { component: 'message-blocklist', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'service-need', open: false }
  ],
  ['UNIT_SUPERVISOR']: [
    { component: 'guardians', open: false },
    { component: 'parents', open: false },
    { component: 'message-blocklist', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'service-need', open: false },
    { component: 'assistance', open: false },
    { component: 'applications', open: false }
  ],
  ['STAFF']: [
    { component: 'family-contacts', open: true },
    { component: 'message-blocklist', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'service-need', open: false }
  ],
  ['SPECIAL_EDUCATION_TEACHER']: [
    { component: 'family-contacts', open: true },
    { component: 'message-blocklist', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'service-need', open: false },
    { component: 'assistance', open: false }
  ]
}

const ChildInformation = React.memo(function ChildInformation({
  match
}: RouteComponentProps<{ id: UUID }>) {
  const { i18n } = useTranslation()
  const { id } = match.params

  const { roles } = useContext(UserContext)
  const {
    person,
    setPerson,
    parentships,
    setParentships
  } = useContext<ChildState>(ChildContext)
  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)

  useEffect(() => {
    setPerson(Loading.of())
    setParentships(Loading.of())
    void getPersonDetails(id).then(setPerson)

    if (
      requireRole(roles, 'SERVICE_WORKER', 'UNIT_SUPERVISOR', 'FINANCE_ADMIN')
    ) {
      void getParentshipsByChild(id).then(setParentships)
    }
  }, [id])

  useEffect(() => {
    if (person.isSuccess) {
      const name = formatTitleName(
        person.value.firstName,
        person.value.lastName
      )
      setTitle(`${name} | ${i18n.titles.customers}`)
    }
  }, [person])

  const layout = useMemo(() => getLayout(layouts, roles), [roles])

  function getCurrentHeadOfChildId(fridgeRelations: Parentship[]) {
    const currentDate = LocalDate.today()
    const currentRelation = fridgeRelations.find(
      (r) =>
        !r.startDate.isAfter(currentDate) &&
        (r.endDate ? !r.endDate.isBefore(currentDate) : true)
    )
    return currentRelation?.headOfChildId
  }

  const currentHeadOfChildId = parentships
    .map(getCurrentHeadOfChildId)
    .getOrElse(undefined)

  return (
    <Container>
      <Gap size={'L'} />
      <ContentArea opaque>
        <div className="child-information-wrapper" data-person-id={id}>
          <HeaderRow>
            <Title size={1}>{i18n.childInformation.title}</Title>
            <div>
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
          <Section>
            <ChildDetails id={id} />
          </Section>
          {layout.map(({ component, open }) => {
            const Component = components[component]
            return (
              <Section key={component}>
                <Component id={id} open={open} />
              </Section>
            )
          })}
        </div>
      </ContentArea>
    </Container>
  )
})

export default ChildInformation
