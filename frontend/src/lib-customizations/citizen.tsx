// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-ignore
import customizations from '@evaka/customizations/citizen'
import type { CitizenCustomizations } from './types'

// eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
const {
  fiCustomizations,
  enCustomizations,
  svCustomizations,
  cityLogo,
  mapConfig,
  featureFlags
}: CitizenCustomizations = customizations
export {
  fiCustomizations,
  enCustomizations,
  svCustomizations,
  cityLogo,
  mapConfig,
  featureFlags
}
