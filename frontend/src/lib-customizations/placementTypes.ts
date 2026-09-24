// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// oxlint-disable-next-line typescript/ban-ts-comment
// @ts-ignore
import defaultsUntyped from '@evaka/customizations/employee'

import type { PlacementType } from 'lib-common/generated/api-types/placement'

import type { EmployeeCustomizations } from './types'

// The citizen application editor needs the municipality's placement types,
// which are an employee customization. This module exposes only that value so
// that citizen-frontend does not pull in the whole employee module (and with
// it the employee translations) into its bundle.

// oxlint-disable-next-line typescript/no-unsafe-assignment
const defaults: EmployeeCustomizations = defaultsUntyped

const overrides =
  typeof window !== 'undefined'
    ? window.evaka?.employeeCustomizations
    : undefined

// Arrays are replaced as a whole by the customization merge logic, so an
// override wins over the default without merging.
export const placementTypes: PlacementType[] =
  overrides?.placementTypes ?? defaults.placementTypes
