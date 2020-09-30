// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as types from '@/store/mutation-types'
import daycareAPI from '@/api/daycare'
import geoJsonUtils from 'geojson-utils'
import {
  APPLICATION_TYPE,
  SERVICE_CLASSES,
  applicationTypeToDaycareTypes,
  UNIT_TYPE
} from '@/constants'
import intersection from 'lodash/intersection'
import {
  distanceMatches,
  languageMatches
} from '@/utils/helpers'
import { Module } from 'vuex'
import { RootState } from '@/store'

const roundTheClockMatches = (filters) => (unit) => {
  let roundTheClock = false
  if (typeof unit.roundTheClock !== 'undefined') {
    roundTheClock = unit.roundTheClock
  }
  return filters.roundTheClock === roundTheClock
}

const districtMatches = (filters) => (unit) => {
  let districtFilterResult = true
  if (
    unit.location != null &&
    typeof filters.district !== 'undefined' &&
    filters.district !== null
  ) {
    districtFilterResult = geoJsonUtils.pointInPolygon(
      {
        type: 'point',
        coordinates: [unit.location.lon, unit.location.lat]
      },
      filters.district.geometry
    )
  }
  return districtFilterResult
}

const daycareTypeMatches = (filters) => (unit) => {
  const unitTypes = unit.type.map((type) => SERVICE_CLASSES[type])
  return (
    filters.daycareType.length === 0 ||
    intersection(unitTypes, filters.daycareType).length !== 0
  )
}

export const filterByType = (type) => (units) =>
  units.filter((i) =>
    i.type.some(
      (t) => applicationTypeToDaycareTypes(type).indexOf(t.toUpperCase()) > -1
    )
  )

export const toMarker = (unit) => ({
  ...unit,
  position: {
    lat: unit.location.lat,
    lng: unit.location.lon
  },
  services: unit.type
})

export interface UnitsState {
  units: any[]
  clubTerms: any[]
  activeTerm: any | null
  unitsLoaded: boolean
  applicationUnits: { loading: boolean; data: any[] }
}

const initialState: UnitsState = {
  units: [],
  clubTerms: [
    {
      id: 'id',
      start: '2020-08-13',
      end: '2021-06-04',
      applicationPeriodStart: '2020-03-02',
      applicationPeriodEnd: '2020-03-31'
    }
  ],
  activeTerm: {
    id: 'id',
    start: '2020-08-13',
    end: '2021-06-04',
    applicationPeriodStart: '2020-03-02',
    applicationPeriodEnd: '2020-03-31'
  },
  unitsLoaded: false,
  applicationUnits: { loading: true, data: [] }
}

const module: Module<UnitsState, RootState> = {
  state: initialState,
  getters: {
    filteredUnits: (_, { units, filters }) => {
      if (filters.daycareType === 'centre') {
        return filterByType(APPLICATION_TYPE.DAYCARE)(units)
          .filter((unit) => unit.location !== null)
          .filter((unit) => distanceMatches(unit, filters))
          .filter((unit) => languageMatches(unit, filters))
      } else if (filters.daycareType === 'preschool') {
        return units
          .filter((unit) => unit.type.includes(UNIT_TYPE.PRESCHOOL))
          .filter((unit) => unit.location !== null)
          .filter((unit) => distanceMatches(unit, filters))
          .filter((unit) => languageMatches(unit, filters))
      } else {
        return units
          .filter((unit) => unit.type.includes(UNIT_TYPE.CLUB))
          .filter((unit) => unit.location !== null)
          .filter((unit) => distanceMatches(unit, filters))
          .filter((unit) => languageMatches(unit, filters))
      }
    },
    units: ({ units }) =>
      units.map((unit) => ({
        ...unit,
        // FIXME: Set default language to FI before language data is in db
        language: unit.language ? unit.language : 'FI'
      })),
    markers: (_, { filteredUnits }) => filteredUnits.map(toMarker),
    clubTerms: (state) => state.clubTerms,
    activeTerm: (state) => state.activeTerm,
    unitsLoaded: (state) => state.unitsLoaded,
    applicationUnits: (state) => state.applicationUnits
  },
  actions: {
    updateUnits({ commit, getters }) {
      const filters = getters.filters
      const units = initialState.units
        .filter(roundTheClockMatches(filters))
        .filter((unit) => distanceMatches(unit, filters))
        .filter(districtMatches(filters))
        .filter(daycareTypeMatches(filters))

      commit(types.UPDATE_UNITS, units)
    },
    loadUnits({ commit }) {
      return daycareAPI.getAreas().then((areas) => {
        const units = areas.flatMap((area) => area.daycares)
        commit(types.UPDATE_UNITS, units)
      })
    },
    updateTerm({ commit }, id) {
      commit(types.UPDATE_ACTIVE_TERM, id)
    },
    async loadApplicationUnits({ commit }, { date, type }) {
      commit(types.LOAD_APPLICATION_UNITS, { loading: true, data: [] })
      const units = await daycareAPI.getApplicationUnits(date, type)
      commit(types.LOAD_APPLICATION_UNITS, { loading: false, data: units })
    }
  },
  mutations: {
    [types.UPDATE_UNITS](state, units) {
      state.units = units
      state.unitsLoaded = true
    },
    [types.UPDATE_ACTIVE_TERM](state, term) {
      state.activeTerm = term
    },
    [types.LOAD_APPLICATION_UNITS](state, { loading, data }) {
      state.applicationUnits = { loading, data }
    }
  }
}

export default module
