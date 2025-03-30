// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { ApplicationId } from 'lib-common/generated/api-types/shared'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PlacementApplication } from 'lib-common/generated/api-types/placementdesktop'
import { PlacementDaycare } from 'lib-common/generated/api-types/placementdesktop'
import { TrialPlacementUnitRequest } from 'lib-common/generated/api-types/placementdesktop'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonPlacementApplication } from 'lib-common/generated/api-types/placementdesktop'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.placementdesktop.PlacementDesktopController.getPlacementApplications
*/
export async function getPlacementApplications(): Promise<PlacementApplication[]> {
  const { data: json } = await client.request<JsonOf<PlacementApplication[]>>({
    url: uri`/employee/placement-desktop/applications`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPlacementApplication(e))
}


/**
* Generated from fi.espoo.evaka.placementdesktop.PlacementDesktopController.getPlacementDaycares
*/
export async function getPlacementDaycares(
  request: {
    occupancyDate: LocalDate
  }
): Promise<PlacementDaycare[]> {
  const params = createUrlSearchParams(
    ['occupancyDate', request.occupancyDate.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<PlacementDaycare[]>>({
    url: uri`/employee/placement-desktop/daycares`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.placementdesktop.PlacementDesktopController.setTrialPlacementUnit
*/
export async function setTrialPlacementUnit(
  request: {
    applicationId: ApplicationId,
    body: TrialPlacementUnitRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/placement-desktop/applications/${request.applicationId}/trial-placement-unit`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<TrialPlacementUnitRequest>
  })
  return json
}
