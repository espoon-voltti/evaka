// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChildAndPermittedActions } from 'lib-common/generated/api-types/children'
import { JsonOf } from 'lib-common/json'

import { client } from '../api-client'

export function getChildren(): Promise<ChildAndPermittedActions[]> {
  return client
    .get<JsonOf<ChildAndPermittedActions[]>>('/citizen/children')
    .then((res) => res.data)
}
