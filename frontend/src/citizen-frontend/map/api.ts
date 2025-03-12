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

// Digitransit limits the maximum duration of a walk to 1.5 hours.
// To increase the range of the search, we increase the speed of walking.
// We're only interested in the distance, so the speed doesn't matter.
const digitransitMaxSpeed = 300

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
  const url = '/api/citizen/public/map-api/autocomplete'

  return axios
    .get<JsonOf<AutocompleteResponse>>(url, {
      params: {
        text,
        layers: 'address',
        'boundary.rect.min_lon': mapConfig.searchAreaRect.minLongitude,
        'boundary.rect.max_lon': mapConfig.searchAreaRect.maxLongitude,
        'boundary.rect.min_lat': mapConfig.searchAreaRect.minLatitude,
        'boundary.rect.max_lat': mapConfig.searchAreaRect.maxLatitude
      },
      headers: {
        'x-evaka-csrf': '1'
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
      edges: {
        node: {
          walkDistance: number
        }
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

  const unitsToQuery = sortBy(
    unitsWithStraightDistance.filter(
      (u) => u.location !== null && u.straightDistance !== null
    ),
    (u) => u.straightDistance
  ).slice(0, accurateDistancesCount)

  if (unitsToQuery.length === 0) {
    return []
  }

  const query = `
{
  ${unitsToQuery
    .map(
      ({ id, location }) => `
  ${uuidToKey(id)}: planConnection(
    origin: {
      location: {
        coordinate: {
          latitude: ${startLocation.lat}
          longitude: ${startLocation.lon}
        }
      }
    }
    destination: {
      location: {
        coordinate: {
          latitude: ${location?.lat ?? 0}
          longitude: ${location?.lon ?? 0}
        }
      }
    }
    modes: {
      direct: WALK
      directOnly: true
    }
    preferences: {
      street: {
        walk: {
          speed: ${digitransitMaxSpeed}
        }
      }
    }
  ) {
    edges {
      node {
        walkDistance
      }
    }
  }

`
    )
    .join('')}
}`

  return axios
    .post<JsonOf<ItineraryResponse>>(
      '/api/citizen/public/map-api/query',
      {
        query
      },
      {
        headers: {
          'x-evaka-csrf': '1'
        }
      }
    )
    .then((res) =>
      unitsWithStraightDistance.map((unit) => {
        const plan = res.data.data[uuidToKey(unit.id)]
        if (!plan)
          return {
            ...unit,
            drivingDistance: null
          }

        const edges = plan.edges
        if (edges.length === 0)
          return {
            ...unit,
            drivingDistance: null
          }

        const drivingDistance = edges[0].node.walkDistance
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
  planConnection(
    origin: {
      location: {
        coordinate: {
          latitude: ${startLocation.lat}
          longitude: ${startLocation.lon}
        }
      }
    }
    destination: {
      location: {
        coordinate: {
          latitude: ${endLocation.lat}
          longitude: ${endLocation.lon}
        }
      }
    }
    modes: {
      direct: WALK
      directOnly: true
    }
    preferences: {
      street: {
        walk: {
          speed: ${digitransitMaxSpeed}
        }
      }
    }
  ) {
    edges {
      node {
        walkDistance
      }
    }
  }
}`

  return axios
    .post<JsonOf<ItineraryResponse>>(
      '/api/citizen/public/map-api/query',
      {
        query
      },
      {
        headers: {
          'x-evaka-csrf': '1'
        }
      }
    )
    .then((res) => {
      const planConnection = res.data.data.planConnection
      if (!planConnection) throw Error('No planConnection found')

      const edges = planConnection.edges
      if (edges.length === 0) throw Error('No edges found')

      return edges[0].node.walkDistance
    })
}
