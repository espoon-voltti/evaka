// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import useRouteParams from 'lib-common/useRouteParams'

export type SelectedGroupId = { type: 'all' } | { type: 'one'; id: UUID }

interface SelectedGroup {
  selectedGroupId: SelectedGroupId
  groupRoute: string
}

export const useSelectedGroup = (): SelectedGroup => {
  const { unitId, groupId: rawGroupId } = useRouteParams(
    ['unitId'],
    ['groupId']
  )
  const selectedGroupId: SelectedGroupId =
    rawGroupId === undefined || rawGroupId === 'all'
      ? { type: 'all' }
      : { type: 'one', id: rawGroupId }
  const groupRoute = `/units/${unitId}/groups/${rawGroupId}`
  return { selectedGroupId, groupRoute }
}
