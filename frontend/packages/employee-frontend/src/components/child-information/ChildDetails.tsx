// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { useTranslation } from '~/state/i18n'
import { Success } from '~api'
import { faChild } from '@evaka/icons'
import { UUID } from '~/types'
import AdditionalInformation from '~components/child-information/person-details/AdditionalInformation'
import { ChildContext, ChildState } from '~state/child'
import PersonDetails from '~components/person-shared/PersonDetails'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'

interface Props {
  id: UUID
}

const ChildDetails = React.memo(function ChildDetails({ id }: Props) {
  const { i18n } = useTranslation()
  const { person, setPerson } = useContext<ChildState>(ChildContext)

  return (
    <div className="person-details-section">
      <CollapsibleSection
        icon={faChild}
        title={i18n.childInformation.personDetails.title}
      >
        <PersonDetails
          personResult={person}
          isChild={true}
          onUpdateComplete={(p) => setPerson(Success(p))}
        />
        <div className="additional-information">
          <AdditionalInformation id={id} />
        </div>
      </CollapsibleSection>
    </div>
  )
})

export default ChildDetails
