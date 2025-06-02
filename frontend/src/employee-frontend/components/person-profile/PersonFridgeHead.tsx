// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import { isLoading } from 'lib-common/api'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { usePersonName } from 'lib-components/molecules/PersonNames'
import { H2 } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { useTitle } from '../../utils/useTitle'
import { renderResult } from '../async-rendering'
import PersonDetails from '../person-shared/PersonDetails'

import type { PersonState } from './state'
import { PersonContext } from './state'

export default React.memo(function PersonFridgeHead() {
  const { i18n } = useTranslation()
  const { person, permittedActions } = useContext<PersonState>(PersonContext)
  const [open, setOpen] = useState(true)

  useTitle(
    `${usePersonName(
      person.isSuccess ? person.value : undefined,
      'Last First'
    )} | ${i18n.titles.customers}`,
    { preventUpdate: !person.isSuccess }
  )

  return (
    <div data-qa="person-info-section" data-isloading={isLoading(person)}>
      <CollapsibleContentArea
        title={<H2>{i18n.personProfile.personDetails}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="person-info-collapsible"
      >
        {renderResult(person, (person) => (
          <PersonDetails
            person={person}
            isChild={false}
            permittedActions={permittedActions}
          />
        ))}
      </CollapsibleContentArea>
    </div>
  )
})
