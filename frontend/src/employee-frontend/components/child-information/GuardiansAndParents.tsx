// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import { ChildId } from 'lib-common/generated/api-types/shared'
import { Gap } from 'lib-components/white-space'

import FosterParents from './FosterParents'
import FridgeParents from './FridgeParents'
import Guardians from './Guardians'

interface Props {
  childId: ChildId
}

export default React.memo(function GuardiansAndParents({ childId }: Props) {
  return (
    <div>
      <Gap size="m" />
      <Guardians />
      <Gap size="XL" />
      <FridgeParents />
      <Gap size="XL" />
      <FosterParents childId={childId} />
    </div>
  )
})
