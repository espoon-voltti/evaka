// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { ChildId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { useIdRouteParam } from 'lib-common/useRouteParams'

import { renderResult } from '../../../async-rendering'
import { useTranslation } from '../../../localization'
import useTitle from '../../../useTitle'

import { NewAbsenceApplicationForm } from './NewAbsenceApplicationForm'
import { getAbsenceApplicationPossibleDateRangesQuery } from './queries'

export const NewAbsenceApplicationPage = () => {
  const childId = useIdRouteParam<ChildId>('childId')
  const i18n = useTranslation()
  useTitle(i18n, i18n.children.absenceApplication.title)
  const absenceApplicationDateRanges = useQueryResult(
    getAbsenceApplicationPossibleDateRangesQuery({ childId: childId })
  )
  return (
    <>
      {renderResult(
        absenceApplicationDateRanges,
        (absenceApplicationDateRanges) => (
          <NewAbsenceApplicationForm
            childId={childId}
            absenceApplicationDateRanges={absenceApplicationDateRanges}
          />
        )
      )}
    </>
  )
}
