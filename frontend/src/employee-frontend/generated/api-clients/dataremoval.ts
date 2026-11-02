// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { DataRetentionSchemaGraph } from 'lib-common/generated/api-types/dataremoval'
import type { JsonOf } from 'lib-common/json'
import { client } from '../../api/client'
import { uri } from 'lib-common/uri'


/**
* Generated from evaka.core.dataremoval.DataRetentionSchemaController.getDataRetentionSchema
*/
export async function getDataRetentionSchema(): Promise<DataRetentionSchemaGraph> {
  const { data: json } = await client.request<JsonOf<DataRetentionSchemaGraph>>({
    url: uri`/employee/data-retention/schema`.toString(),
    method: 'GET'
  })
  return json
}
