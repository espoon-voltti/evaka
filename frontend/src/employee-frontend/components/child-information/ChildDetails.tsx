// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { useTranslation } from '../../state/i18n'
import { Success } from 'lib-common/api'
import { UUID } from '../../types'
import AdditionalInformation from '../../components/child-information/person-details/AdditionalInformation'
import { ChildContext, ChildState } from '../../state/child'
import PersonDetails from '../../components/person-shared/PersonDetails'
import { CollapsibleContentArea } from '../../../lib-components/layout/Container'
import { H2 } from '../../../lib-components/typography'

interface Props {
  id: UUID
}

const ChildDetails = React.memo(function ChildDetails({ id }: Props) {
  const { i18n } = useTranslation()
  const { person, setPerson } = useContext<ChildState>(ChildContext)

  const [open, setOpen] = useState(true)

  return (
    <div className="person-details-section">
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.personDetails.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
      >
        <PersonDetails
          personResult={person}
          isChild={true}
          onUpdateComplete={(p) => setPerson(Success.of(p))}
        />
        <div className="additional-information">
          <AdditionalInformation id={id} />
        </div>
      </CollapsibleContentArea>
    </div>
  )
})

export default ChildDetails
