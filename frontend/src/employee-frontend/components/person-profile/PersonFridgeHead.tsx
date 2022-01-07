// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faUser } from 'lib-icons'
import React, { useContext, useEffect } from 'react'
import { isLoading } from 'lib-common/api'
import CollapsibleSection from 'lib-components/molecules/CollapsibleSection'
import PersonDetails from '../../components/person-shared/PersonDetails'
import { useTranslation } from '../../state/i18n'
import { PersonContext, PersonState } from '../../state/person'
import { TitleContext, TitleState } from '../../state/title'
import { renderResult } from '../async-rendering'

export default React.memo(function PersonFridgeHead() {
  const { i18n } = useTranslation()
  const { person, setPerson, permittedActions } =
    useContext<PersonState>(PersonContext)
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

  return (
    <div data-qa="person-info-section" data-isloading={isLoading(person)}>
      <CollapsibleSection
        icon={faUser}
        title={i18n.personProfile.personDetails}
        data-qa="person-info-collapsible"
      >
        {renderResult(person, (person) => (
          <PersonDetails
            person={person}
            isChild={false}
            onUpdateComplete={setPerson}
            permittedActions={permittedActions}
          />
        ))}
      </CollapsibleSection>
    </div>
  )
})
