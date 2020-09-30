// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios, { AxiosResponse } from 'axios'
import { googleApiKey } from '../../shared/config'
import {
  AUTOCOMPLETE_DISTANCE,
  ESPOO_COORDINATES,
  GOOGLE_MAPS_API_URL
} from '../constants'

interface AutocompletePrediction {
  place_id: string
  description: string
}

interface AutocompleteData {
  status: string
  predictions: AutocompletePrediction[]
  error_message: string
}

interface GeocodeResult {
  place_id: string
  formatted_address: string
  geometry: { location: { lat: string; lng: string } }
}

interface GeocodeData {
  status: string
  results: GeocodeResult[]
  error_message: string
}

export async function autocomplete(address: string) {
  return axios
    .get(`${GOOGLE_MAPS_API_URL}/place/autocomplete/json`, {
      params: {
        input: address,
        location: ESPOO_COORDINATES,
        radius: AUTOCOMPLETE_DISTANCE,
        strictbounds: true,
        key: googleApiKey
      }
    })
    .then((res: AxiosResponse<AutocompleteData>) => {
      const statusCode = res.data.status
      if (statusCode === 'ZERO_RESULTS') {
        return []
      }

      if (statusCode !== 'OK') {
        throw Error(
          `Invalid autocomplete response: ${JSON.stringify(
            res.data
          )}. Status code: ${res.status}`
        )
      }

      return res.data.predictions.map(
        ({ place_id: id, description: name }) => ({ id, name })
      )
    })
}

export async function geocode(id: string) {
  return axios
    .get(`${GOOGLE_MAPS_API_URL}/geocode/json`, {
      params: {
        place_id: id,
        key: googleApiKey
      }
    })
    .then((res: AxiosResponse<GeocodeData>) => {
      if (res.data.status !== 'OK') {
        throw Error(
          `Invalid geocode response: ${JSON.stringify(
            res.data
          )}. Status code: ${res.status}`
        )
      }

      return res.data.results.map(
        ({
          place_id: id,
          formatted_address: name,
          geometry: {
            location: { lat, lng }
          }
        }) => ({
          type: 'Feature',
          geometry: {
            type: 'Point',
            coordinates: [lat, lng],
            properties: { id, name }
          }
        })
      )
    })
}
