// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faCircle } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import MessageBlocklist from 'employee-frontend/components/child-information/MessageBlocklist'
import { Loading } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import Title from 'lib-components/atoms/Title'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'
import { faUsers } from 'lib-icons'
import React, { useContext, useEffect, useMemo } from 'react'
import { Link, RouteComponentProps } from 'react-router-dom'
import styled from 'styled-components'
import { getParentshipsByChild } from '../api/parentships'
import { getPersonDetails } from '../api/person'
import Assistance from '../components/child-information/Assistance'
import BackupCare from '../components/child-information/BackupCare'
import ChildApplications from '../components/child-information/ChildApplications'
import ChildDetails from '../components/child-information/ChildDetails'
import FamilyContacts from '../components/child-information/FamilyContacts'
import FeeAlteration from '../components/child-information/FeeAlteration'
import FridgeParents from '../components/child-information/FridgeParents'
import Guardians from '../components/child-information/Guardians'
import Placements from '../components/child-information/Placements'
import WarningLabel from '../components/common/WarningLabel'
import { ChildContext, ChildState } from '../state/child'
import { useTranslation } from '../state/i18n'
import { TitleContext, TitleState } from '../state/title'
import { UserContext } from '../state/user'
import { UUID } from '../types'
import { Parentship } from '../types/fridge'
import { requireRole } from '../utils/roles'
import DailyServiceTimesSection from './child-information/DailyServiceTimesSection'
import VasuAndLeops from './child-information/VasuAndLeops'
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
  'daily-service-times': DailyServiceTimesSection,
  vasuAndLeops: VasuAndLeops,
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
    { component: 'daily-service-times', open: false },
    ...(featureFlags.vasu
      ? [{ component: 'vasuAndLeops' as keyof typeof components, open: false }]
      : []),
    { component: 'assistance', open: false },
    { component: 'applications', open: false },
    { component: 'fee-alterations', open: false }
  ],
  ['SERVICE_WORKER']: [
    { component: 'guardians', open: false },
    { component: 'parents', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'assistance', open: false },
    { component: 'applications', open: false },
    { component: 'family-contacts', open: false }
  ],
  ['FINANCE_ADMIN']: [
    { component: 'fee-alterations', open: true },
    { component: 'guardians', open: false },
    { component: 'parents', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false }
  ],
  ['UNIT_SUPERVISOR']: [
    { component: 'guardians', open: false },
    { component: 'parents', open: false },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'assistance', open: false },
    { component: 'applications', open: false },
    { component: 'family-contacts', open: false },
    ...(featureFlags.vasu
      ? [{ component: 'vasuAndLeops' as keyof typeof components, open: false }]
      : [])
  ],
  ['STAFF']: [
    { component: 'family-contacts', open: true },
    { component: 'placements', open: false },
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    ...(featureFlags.vasu
      ? [{ component: 'vasuAndLeops' as keyof typeof components, open: false }]
      : [])
  ],
  ['SPECIAL_EDUCATION_TEACHER']: [
    { component: 'family-contacts', open: true },
    { component: 'placements', open: false },
    ...(featureFlags.vasu
      ? [{ component: 'vasuAndLeops' as keyof typeof components, open: false }]
      : []),
    { component: 'backup-care', open: false },
    { component: 'daily-service-times', open: false },
    { component: 'assistance', open: false }
  ]
}

const ChildInformation = React.memo(function ChildInformation({
  match
}: RouteComponentProps<{ id: UUID }>) {
  const { i18n } = useTranslation()
  const { id } = match.params

  const { roles } = useContext(UserContext)
  const { person, setPerson, parentships, setParentships } =
    useContext<ChildState>(ChildContext)
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
  }, [id]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (person.isSuccess) {
      const name = formatTitleName(
        person.value.firstName,
        person.value.lastName
      )
      setTitle(`${name} | ${i18n.titles.customers}`)
    }
  }, [person]) // eslint-disable-line react-hooks/exhaustive-deps

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
      <div className="child-information-wrapper" data-person-id={id}>
        <ContentArea opaque>
          <HeaderRow>
            <Title size={1} noMargin>
              {i18n.childInformation.title}
            </Title>
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
        </ContentArea>

        <Gap size={'m'} />

        <ChildDetails id={id} />

        <Gap size={'m'} />

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

export default ChildInformation
