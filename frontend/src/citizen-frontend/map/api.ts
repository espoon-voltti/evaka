// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'
import sortBy from 'lodash/sortBy'

import { PublicUnit } from 'lib-common/generated/api-types/daycare'
import { Coordinate } from 'lib-common/generated/api-types/shared'
import { JsonOf } from 'lib-common/json'
import { mapConfig } from 'lib-customizations/citizen'

import { MapAddress } from './MapView'
import {
  calcStraightDistance,
  UnitWithDistance,
  UnitWithStraightDistance
} from './distances'

type AutocompleteResponse = {
  features: {
    geometry: {
      coordinates: [number, number]
    }
    properties: {
      name: string
      postalcode?: string
      locality?: string
      localadmin?: string
    }
  }[]
}

export const autocompleteAddress = async (
  text: string
): Promise<MapAddress[]> => {
  const url = '/api/application/map-api/autocomplete'

  return axios
    .get<JsonOf<AutocompleteResponse>>(url, {
      params: {
        text,
        layers: 'address',
        'boundary.rect.min_lon': mapConfig.searchAreaRect.minLongitude,
        'boundary.rect.max_lon': mapConfig.searchAreaRect.maxLongitude,
        'boundary.rect.min_lat': mapConfig.searchAreaRect.minLatitude,
        'boundary.rect.max_lat': mapConfig.searchAreaRect.maxLatitude
      }
    })
    .then((res) =>
      res.data.features.map((feature) => ({
        coordinates: {
          lon: feature.geometry.coordinates[0],
          lat: feature.geometry.coordinates[1]
        },
        streetAddress: feature.properties.name,
        postalCode: feature.properties.postalcode ?? '',
        postOffice:
          feature.properties.locality ?? feature.properties.localadmin ?? ''
      }))
    )
}

type ItineraryResponse = {
  data: Record<
    string,
    {
      itineraries: {
        legs: {
          distance: number
        }[]
      }[]
    }
  >
}

const uuidToKey = (id: string) => `id${id.replace(/-/g, '')}`

const accurateDistancesCount = 15

export async function fetchUnitsWithDistances(
  selectedAddress: MapAddress,
  units: PublicUnit[]
): Promise<UnitWithDistance[]> {
  const startLocation = selectedAddress.coordinates

  const unitsWithStraightDistance = units.map<UnitWithStraightDistance>(
    (unit) => ({
      ...unit,
      straightDistance: unit.location
        ? calcStraightDistance(unit.location, startLocation)
        : null
    })
  )

  if (unitsWithStraightDistance.length === 0) {
    return []
  }
  const unitsToQuery = sortBy(
    unitsWithStraightDistance.filter((u) => u.straightDistance !== null),
    (u) => u.straightDistance
  ).slice(0, accurateDistancesCount)

  const query = `
{
  ${unitsToQuery
    .filter((u) => u.location)
    .map(
      ({ id, location }) => `
    ${uuidToKey(id)}: plan(
      from: {
        lat: ${startLocation.lat},
        lon: ${startLocation.lon}
      },
      to: {
        lat: ${location?.lat ?? 0},
        lon: ${location?.lon ?? 0}
      },
      modes: "WALK"
    ) {
      itineraries{
        legs {
          distance
        }
      }
    }

  `
    )
    .join('')}
}`

  return axios
    .post<JsonOf<ItineraryResponse>>('/api/application/map-api/query', {
      query
    })
    .then((res) =>
      unitsWithStraightDistance.map((unit) => {
        const plan = res.data.data[uuidToKey(unit.id)]
        if (!plan)
          return {
            ...unit,
            drivingDistance: null
          }

        const itineraries = plan.itineraries
        if (itineraries.length === 0)
          return {
            ...unit,
            drivingDistance: null
          }

        const itinerary = itineraries[0]
        const drivingDistance = itinerary.legs.reduce(
          (acc, leg) => acc + leg.distance,
          0
        )
        return {
          ...unit,
          drivingDistance
        }
      })
    )
}

export const fetchDistance = async (
  startLocation: Coordinate,
  endLocation: Coordinate
): Promise<number> => {
  const query = `
{
    plan(
      from: {
        lat: ${startLocation.lat},
        lon: ${startLocation.lon}
      },
      to: {
        lat: ${endLocation.lat},
        lon: ${endLocation.lon}
      },
      modes: "WALK"
    ) {
      itineraries{
        legs {
          distance
        }
      }
    }
}`

  return axios
    .post<JsonOf<ItineraryResponse>>('/api/application/map-api/query', {
      query
    })
    .then((res) => {
      const plan = res.data.data.plan
      if (!plan) throw Error('No plan found')

      const itineraries = plan.itineraries
      if (itineraries.length === 0) throw Error('No itineraries found')

      return itineraries[0].legs.reduce((acc, leg) => acc + leg.distance, 0)
    })
}
