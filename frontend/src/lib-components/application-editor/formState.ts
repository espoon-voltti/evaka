// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo } from 'react'

import type { Result } from 'lib-common/api'
import type { ApplicationFormData } from 'lib-common/application/ApplicationFormData'
import type { ApplicationDetails } from 'lib-common/generated/api-types/application'
import type { ServiceNeedOptionPublicInfo } from 'lib-common/generated/api-types/serviceneed'
import { constantQuery, useQueryResult } from 'lib-common/query'

import type { ApplicationEditorDeps } from './types'

type SectionUpdaters = {
  serviceNeed: (patch: Partial<ApplicationFormData['serviceNeed']>) => void
  contactInfo: (patch: Partial<ApplicationFormData['contactInfo']>) => void
  additionalDetails: (
    patch: Partial<ApplicationFormData['additionalDetails']>
  ) => void
  // Unit preference needs the previous value: reordering and removing preferred
  // units are derived from the current list, not from a fixed patch.
  unitPreference: (
    update: (
      prev: ApplicationFormData['unitPreference']
    ) => Partial<ApplicationFormData['unitPreference']>
  ) => void
}

/**
 * Referentially stable per-section updaters.
 *
 * Stability matters: the sections are memoized, and several of them key effects
 * on their `updateFormData` prop (e.g. the stale-preferred-unit cleanup), so a
 * fresh closure per render re-runs those effects on every render.
 */
export function useSectionUpdaters(
  setFormData: (
    update: (old: ApplicationFormData) => ApplicationFormData
  ) => void
): SectionUpdaters {
  return useMemo(() => {
    const patchUpdater =
      <K extends 'serviceNeed' | 'contactInfo' | 'additionalDetails'>(
        section: K
      ) =>
      (patch: Partial<ApplicationFormData[K]>) =>
        setFormData((old) => ({
          ...old,
          [section]: { ...old[section], ...patch }
        }))

    return {
      serviceNeed: patchUpdater('serviceNeed'),
      contactInfo: patchUpdater('contactInfo'),
      additionalDetails: patchUpdater('additionalDetails'),
      unitPreference: (update) =>
        setFormData((old) => ({
          ...old,
          unitPreference: {
            ...old.unitPreference,
            ...update(old.unitPreference)
          }
        }))
    }
  }, [setFormData])
}

/**
 * Which placement types' service need options are offered depends on the
 * application type and the municipality's feature flags. Kept in one place so
 * the citizen form compositions and the employee edit view cannot disagree.
 */
export function useApplicationServiceNeedOptions(
  deps: ApplicationEditorDeps,
  application: ApplicationDetails
): Result<ServiceNeedOptionPublicInfo[]> {
  const { featureFlags, serviceNeedOptionPublicInfosQuery } = deps

  return useQueryResult(
    application.type === 'DAYCARE' &&
      featureFlags.daycareApplication.serviceNeedOption
      ? serviceNeedOptionPublicInfosQuery({
          placementTypes: ['DAYCARE', 'DAYCARE_PART_TIME']
        })
      : application.type === 'PRESCHOOL' &&
          featureFlags.preschoolApplication.serviceNeedOption
        ? serviceNeedOptionPublicInfosQuery({
            placementTypes: [
              'PRESCHOOL_DAYCARE',
              ...(application.form.preferences.serviceNeed?.serviceNeedOption
                ?.validPlacementType === 'PRESCHOOL_CLUB'
                ? (['PRESCHOOL_CLUB'] as const)
                : [])
            ]
          })
        : constantQuery([])
  )
}
