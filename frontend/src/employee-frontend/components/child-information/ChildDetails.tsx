// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { useTranslation } from '../../state/i18n'
import AdditionalInformation from '../../components/child-information/person-details/AdditionalInformation'
import { ChildContext, ChildState } from '../../state/child'
import PersonDetails from '../../components/person-shared/PersonDetails'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { renderResult } from '../async-rendering'
import { UUID } from 'lib-common/types'

interface Props {
  id: UUID
}

export default React.memo(function ChildDetails({ id }: Props) {
  const { i18n } = useTranslation()
  const { person, setPerson, permittedActions } =
    useContext<ChildState>(ChildContext)

  const [open, setOpen] = useState(true)

  return (
    <div data-qa="person-details-section" data-isloading={person.isLoading}>
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.personDetails.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
      >
        {renderResult(person, (person) => (
          <PersonDetails
            person={person}
            isChild={true}
            onUpdateComplete={setPerson}
            permittedActions={permittedActions}
          />
        ))}
        {permittedActions.has('READ_ADDITIONAL_INFO') && (
          <div className="additional-information">
            <AdditionalInformation id={id} />
          </div>
        )}
      </CollapsibleContentArea>
    </div>
  )
})
