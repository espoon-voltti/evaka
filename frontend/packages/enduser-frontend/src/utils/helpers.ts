// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import _ from 'lodash'
import geoJsonUtils from 'geojson-utils'
import { Unit, Filters } from '@/types'
import { PROVIDER_TYPE } from '@/constants'

export const distanceMatches = (unit: Unit, filters: Filters) => {
  let distanceFilterResult = 0
  let distance = 0
  if (unit.location !== null && filters.address !== null) {
    distanceFilterResult = geoJsonUtils.pointDistance(
      { coordinates: [filters.address.lng, filters.address.lat] },
      { coordinates: [unit.location!.lon, unit.location!.lat] }
    )
    distance = filters.address.distance
  }
  return distanceFilterResult - distance <= 0
}

export const languageMatches = (unit: Unit, filters: Filters) => {
  if (unit.language && filters.language && filters.language.length > 0) {
    return filters.language.includes(unit.language.toUpperCase())
  }
  return true
}

export const uuid = () =>
  `${Math.random()
    .toString(32)
    .slice(2)}-${Math.random()
    .toString(32)
    .slice(2)}`

export const currentLanguage = () => {
  let language = localStorage.getItem('enduser_language')
  const params = new URLSearchParams(window.location.search)
  const lang = params.get('lang')
  if (lang && ['fi', 'sv', 'en'].includes(lang)) {
    language = lang
  }
  if (language === null) {
    // IE is the only browser which uses "window.navigator['userLanguage']", and it's the language set in Windows Control Panel - Regional Options
    // and NOT browser language. We assume that a user that uses a machine with "Window Regional" settings set
    // to e.g. Swedish is probably a Swedish speaking user. FireFox and all other browsers use "navigator.language".
    // Chrome sends actual browser language in HTTP 'Accept-Language' header, however a best effor is to use "window.navigator.language".
    language = window.navigator.userLanguage || window.navigator.language
    if (language !== 'fi' && language !== 'sv') {
      language = 'fi'
    }
  }
  return language
}

export const providerIsMunicipal = (provider: string) =>
  provider &&
  provider.length > 0 &&
  (provider.toUpperCase() === PROVIDER_TYPE.MUNICIPAL || provider.toUpperCase() === PROVIDER_TYPE.MUNICIPAL_SCHOOL)

export const providerIsPrivate = (provider: string) =>
  provider &&
  provider.length > 0 &&
  provider.toUpperCase() === PROVIDER_TYPE.PRIVATE

export const providerIsVoucher = (provider: string) =>
  provider &&
  provider.length > 0 &&
  provider.toUpperCase() === PROVIDER_TYPE.PRIVATE_SERVICE_VOUCHER

export const providerIsPurchased = (provider: string) =>
  provider &&
  provider.length > 0 &&
  provider.toUpperCase() === PROVIDER_TYPE.PURCHASED

export const withAsterisk = (str: string): string => (str ? `${str} *` : str)
