// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export interface CitizenCustomizations {
  fiCustomizations: CitizenLocalizations
  enCustomizations: CitizenLocalizations
  svCustomizations: CitizenLocalizations
  cityLogo: {
    src: string
    alt: string
  }
  featureFlags: FeatureFlags
}

interface CitizenLocalizations {
  footer: {
    cityLabel: string
  }
}

interface FeatureFlags {
  urgencyAttachmentsEnabled: boolean
}

export interface EmployeeCustomizations {
  cityLogo: {
    src: string
    alt: string
  }
  featureFlags: FeatureFlags
}
