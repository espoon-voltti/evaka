// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo } from 'react'

import type { Result } from '../api'

type DataStatus = 'failure' | 'loading' | 'success'

const mapToDataStatus = (result: Result<unknown>): DataStatus =>
  result.mapAll({
    failure: () => 'failure',
    loading: () => 'loading',
    success: (_, isReloading) => (isReloading ? 'loading' : 'success')
  })

export function useDataStatus(result: Result<unknown>) {
  return useMemo(() => mapToDataStatus(result), [result])
}
