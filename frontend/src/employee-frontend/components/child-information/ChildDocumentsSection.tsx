// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'

import { ChildId } from 'lib-common/generated/api-types/shared'

import { ChildContext } from '../../state'

import ChildDocuments from './ChildDocuments'

interface Props {
  childId: ChildId
}

export default React.memo(function ChildDocumentsSection({ childId }: Props) {
  const { permittedActions } = useContext(ChildContext)

  const hasChildDocumentsCreatePermission = useMemo(
    () => permittedActions.has('CREATE_CHILD_DOCUMENT'),
    [permittedActions]
  )

  return (
    <div>
      <ChildDocuments
        hasCreatePermission={hasChildDocumentsCreatePermission}
        childId={childId}
      />
    </div>
  )
})
