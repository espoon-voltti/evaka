// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { concat } from 'lodash'
import {
  CareAreaResponseJSON,
  CareTypes,
  ClubGroupResponseJSON,
  LocationJSON,
  LocationResponseJSON,
  UUID
} from './service-client'

export interface ClubGroupedCareAreaJSON {
  id: UUID
  name: string
  shortName: string
  daycares: Array<LocationResponseJSON | ClubGroupLocationResponseJSON>
}

export interface ClubGroupLocationResponseJSON extends ClubGroupResponseJSON {
  clubName: string
  type: CareTypes[]
  care_area_id: UUID
  location: LocationJSON | null
  address: string
  postalCode: string | null
  pobox: string | null
  phone: string | null
}

export function replaceClubsWithClubGroups(
  areas: CareAreaResponseJSON[],
  groups: ClubGroupResponseJSON[]
): ClubGroupedCareAreaJSON[] {
  return areas.map((area) => {
    const notClubs = area.daycares.filter(
      (daycare) => !daycare.type.includes('CLUB')
    )
    const clubs = area.daycares.filter((daycare) =>
      daycare.type.includes('CLUB')
    )
    const clubIds = clubs.map((club) => club.id)
    const clubGroups = groups
      .filter((group) => clubIds.includes(group.clubId))
      .map<ClubGroupLocationResponseJSON>((group) => {
        const club = clubs.find((club) => club.id === group.clubId)
        if (!club) throw Error('No club found for group')
        return {
          ...group,
          clubName: club.name,
          type: club.type,
          care_area_id: club.care_area_id,
          location: club.location,
          address: club.address,
          pobox: club.pobox,
          postalCode: club.postalCode,
          phone: club.phone
        }
      })

    return {
      ...area,
      daycares: concat<LocationResponseJSON | ClubGroupLocationResponseJSON>(
        notClubs,
        clubGroups
      )
    }
  })
}
