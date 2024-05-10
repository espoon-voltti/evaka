// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'

import { isLoading } from 'lib-common/api'
import { UUID } from 'lib-common/types'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'

import FosterParents from './FosterParents'
import FridgeParents from './FridgeParents'
import Guardians from './Guardians'

interface Props {
  childId: UUID
  startOpen: boolean
}

export default React.memo(function GuardiansAndParents({
  childId,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { guardians, permittedActions } = useContext(ChildContext)

  const [open, setOpen] = useState(startOpen)

  return (
    <div>
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.personProfile.guardiansAndParents}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="person-guardians-collapsible"
        data-isloading={isLoading(guardians)}
      >
        <Gap size="m" />
        <Guardians />
        <Gap size="XL" />
        <FridgeParents />
        {permittedActions.has('READ_FOSTER_PARENTS') && (
          <>
            <Gap size="XL" />
            <FosterParents childId={childId} />
          </>
        )}
      </CollapsibleContentArea>
    </div>
  )
})
