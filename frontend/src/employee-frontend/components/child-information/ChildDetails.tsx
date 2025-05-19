// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import { isLoading } from 'lib-common/api'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import AdditionalInformation from '../../components/child-information/person-details/AdditionalInformation'
import PersonDetails from '../../components/person-shared/PersonDetails'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import type { ChildState } from './state'
import { ChildContext } from './state'

interface Props {
  id: ChildId
}

export default React.memo(function ChildDetails({ id }: Props) {
  const { i18n } = useTranslation()
  const { person, permittedActions } = useContext<ChildState>(ChildContext)

  const [open, setOpen] = useState(true)

  return (
    <div data-qa="person-details-section" data-isloading={isLoading(person)}>
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.personDetails.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
      >
        <Gap size="s" />
        {renderResult(person, (person) => (
          <PersonDetails
            person={person}
            isChild={true}
            permittedActions={permittedActions}
          />
        ))}
        {permittedActions.has('READ_ADDITIONAL_INFO') && (
          <div className="additional-information">
            <AdditionalInformation childId={id} />
          </div>
        )}
      </CollapsibleContentArea>
    </div>
  )
})
