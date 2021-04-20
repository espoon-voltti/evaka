// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { LatLngExpression } from 'leaflet'

export interface CitizenCustomizations {
  fiCustomizations: CitizenLocalizations
  enCustomizations: CitizenLocalizations
  svCustomizations: CitizenLocalizations
  cityLogo: {
    src: string
    alt: string
  }
  mapConfig: MapConfig
  mapSearchAreaRect: {
    minLongitude: number
    maxLongitude: number
    minLatitude: number
    maxLatitude: number
  }
  featureFlags: FeatureFlags
}

interface CitizenLocalizations {
  footer: {
    cityLabel: string
    privacyPolicyLink: string
    sendFeedbackLink: string
  }
}

interface MapConfig {
  center: LatLngExpression
  initialZoom: number
  addressZoom: number
  searchAreaRect: {
    minLonyAttachmentsEnabled: boolean
}

export interface EmployeeCustomizations {
  cityLogo: {
    src: string
    alt: string
  }
  featureFlags: FeatureFlags
}
