// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect, useState } from 'react'

import type { IncomeTypeOptions } from 'employee-frontend/api/income'
import { getIncomeOptions } from 'employee-frontend/api/income'
import type { Result } from 'lib-common/api'
import { Loading } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'

export function useIncomeTypeOptions() {
  const [incomeTypeOptions, setIncomeTypeOptions] = useState<
    Result<IncomeTypeOptions>
  >(Loading.of())

  const loadIncomeTypeOptions = useRestApi(
    getIncomeOptions,
    setIncomeTypeOptions
  )
  useEffect(() => {
    void loadIncomeTypeOptions()
  }, [loadIncomeTypeOptions])

  return incomeTypeOptions
}
