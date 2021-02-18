import axios from 'axios'
import _ from 'lodash'
import { JsonOf } from '@evaka/lib-common/src/json'
import { Failure, Result, Success } from '@evaka/lib-common/src/api'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import { Coordinate } from '@evaka/lib-common/src/api-types/units/Coordinate'
import { client } from '~api-client'
import { MapAddress } from '~map/MapView'
import { UnitWithDistance, UnitWithStraightDistance } from '~map/distances'
import { isAutomatedTest } from '@evaka/lib-common/src/utils/helpers'

export async function fetchUnits(): Promise<Result<PublicUnit[]>> {
  return client
    .get<JsonOf<PublicUnit[]>>('/public/units/all')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

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

export const queryAutocomplete = async (
  text: string
): Promise<Result<MapAddress[]>> => {
  const url = isAutomatedTest
    ? '/api/internal/dev-api/digitransit/autocomplete'
    : 'https://api.digitransit.fi/geocoding/v1/autocomplete'

  return axios
    .get<JsonOf<AutocompleteResponse>>(url, {
      params: {
        text,
        layers: 'address',
        'boundary.rect.min_lon': 24.271362626190594,
        'boundary.rect.max_lon': 25.32055693401933,
        'boundary.rect.min_lat': 59.9451623086072,
        'boundary.rect.max_lat': 60.35391259995084
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
    .then((options) => Success.of(options))
    .catch((e) => Failure.fromError(e))
}

type ItineraryResponse = {
  data: {
    [key: string]: {
      itineraries: {
        legs: {
          distance: number
        }[]
      }[]
    }
  }
}

const uuidToKey = (id: string) => `id${id.replace(/-/g, '')}`

const accurateDistancesCount = 15

export const queryDistances = async (
  startLocation: Coordinate,
  endLocations: UnitWithStraightDistance[]
): Promise<Result<UnitWithDistance[]>> => {
  if (endLocations.length === 0) {
    return Success.of([])
  } else if (isAutomatedTest) {
    return Success.of(
      endLocations.map((unit, index) => ({
        ...unit,
        drivingDistance: index + 1
      }))
    )
  }
  const unitsToQuery = _.sortBy(
    endLocations.filter((u) => u.straightDistance !== null),
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
    .post<JsonOf<ItineraryResponse>>(
      'https://api.digitransit.fi/routing/v1/routers/finland/index/graphql',
      {
        query
      }
    )
    .then((res) => {
      return endLocations.map((unit) => {
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
    })
    .then((res) => Success.of(res))
    .catch((e) => Failure.fromError(e))
}

export const queryDistance = async (
  startLocation: Coordinate,
  endLocations: Coordinate
): Promise<Result<number>> => {
  if (isAutomatedTest) {
    return Success.of(7)
  }
  const query = `
{
    plan(
      from: {
        lat: ${startLocation.lat},
        lon: ${startLocation.lon}
      },
      to: {
        lat: ${endLocations.lat},
        lon: ${endLocations.lon}
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
    .post<JsonOf<ItineraryResponse>>(
      'https://api.digitransit.fi/routing/v1/routers/finland/index/graphql',
      {
        query
      }
    )
    .then((res) => {
      const plan = res.data.data['plan']
      if (!plan) throw Error('No plan found')

      const itineraries = plan.itineraries
      if (itineraries.length === 0) throw Error('No itineraries found')

      return itineraries[0].legs.reduce((acc, leg) => acc + leg.distance, 0)
    })
    .then((res) => Success.of(res))
    .catch((e) => Failure.fromError(e))
}
