// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect } from 'react'
import { UUID } from '../../types'
import { useTranslation } from '../../state/i18n'
import { Loading, Success } from 'lib-common/api'
import { getPersonDetails } from '../../api/person'
import { useContext } from 'react'
import { PersonContext, PersonState } from '../../state/person'
import Loader from 'lib-components/atoms/Loader'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import { PersonDetails as PersonDetailsType } from '../../types/person'
import PersonDetails from '../../components/person-shared/PersonDetails'
import { faUser } from 'lib-icons'
import { TitleContext, TitleState } from '../../state/title'
import { UnwrapResult } from '../async-rendering'

interface Props {
  id: UUID
}

const PersonFridgeHead = React.memo(function PersonFridgeHead({ id }: Props) {
  const { i18n } = useTranslation()
  const { person, setPerson, reloadFamily } =
    useContext<PersonState>(PersonContext)
  const { setTitle, formatTitleName } = useContext<TitleState>(TitleContext)

  const loadData = () => {
    setPerson(Loading.of())
    void getPersonDetails(id).then(setPerson)
  }

  // FIXME: This component shouldn't know about family's dependency on its data
  const updateData = (p: PersonDetailsType) => {
    setPerson(Success.of(p))
    reloadFamily(id)
  }

  useEffect(loadData, [id, setPerson])

  useEffect(() => {
    if (person.isSuccess) {
      const name = formatTitleName(
        person.value.firstName,
        person.value.lastName
      )
      setTitle(`${name} | ${i18n.titles.customers}`)
    }
  }, [formatTitleName, i18n.titles.customers, person, setTitle])

  return (
    <div className="person-fridge-head-wrapper">
      <CollapsibleSection
        icon={faUser}
        title={i18n.personProfile.personDetails}
      >
        <UnwrapResult result={person}>
          {(person) => (
            <PersonDetails
              person={person}
              isChild={false}
              onUpdateComplete={(p) => updateData(p)}
            />
          )}
        </UnwrapResult>
      </CollapsibleSection>
      {person.isLoading && <Loader />}
      {person.isFailure && <div>{i18n.common.loadingFailed}</div>}
    </div>
  )
})

export default PersonFridgeHead
