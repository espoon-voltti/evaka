// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'

import { UnitPreferenceFormData } from 'lib-common/api-types/application/ApplicationFormData'
import { getErrorCount } from 'lib-common/form-validation'
import { ApplicationType } from 'lib-common/generated/api-types/application'
import LocalDate from 'lib-common/local-date'
import { constantQuery, useQuery } from 'lib-common/query'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { faExclamation } from 'lib-icons'

import EditorSection from '../../../applications/editor/EditorSection'
import SiblingBasisSubSection from '../../../applications/editor/unit-preference/SiblingBasisSubSection'
import UnitsSubSection from '../../../applications/editor/unit-preference/UnitsSubSection'
import { useTranslation } from '../../../localization'
import { OverlayContext } from '../../../overlay/state'
import { applicationUnitsQuery } from '../../queries'
import { ApplicationFormDataErrors } from '../validations'

export type UnitPreferenceSectionCommonProps = {
  formData: UnitPreferenceFormData
  updateFormData: (
    updater: (prev: UnitPreferenceFormData) => Partial<UnitPreferenceFormData>
  ) => void
  errors: ApplicationFormDataErrors['unitPreference']
  verificationRequested: boolean
  applicationType: ApplicationType
  preparatory: boolean
  preferredStartDate: LocalDate | null
  shiftCare: boolean
}

export type UnitPreferenceSectionProps = UnitPreferenceSectionCommonProps & {
  applicationType: ApplicationType
  preparatory: boolean
  preferredStartDate: LocalDate | null
  shiftCare: boolean
}

export default React.memo(function UnitPreferenceSection(
  props: UnitPreferenceSectionProps
) {
  const t = useTranslation()

  const {
    updateFormData,
    applicationType,
    preparatory,
    preferredStartDate,
    shiftCare
  } = props

  const { setInfoMessage, clearInfoMessage } = useContext(OverlayContext)

  const { data: units = null } = useQuery(
    preferredStartDate
      ? applicationUnitsQuery({
          type:
            applicationType === 'CLUB'
              ? 'CLUB'
              : applicationType === 'DAYCARE'
                ? 'DAYCARE'
                : preparatory
                  ? 'PREPARATORY'
                  : 'PRESCHOOL',
          date: preferredStartDate,
          shiftCare
        })
      : constantQuery([])
  )

  useEffect(() => {
    updateFormData((prev) => {
      if (
        units &&
        prev.preferredUnits.some((u1) => !units.some((u2) => u1.id === u2.id))
      ) {
        setInfoMessage({
          title: t.applications.editor.unitChangeWarning.title,
          text: t.applications.editor.unitChangeWarning.text,
          type: 'warning',
          icon: faExclamation,
          resolve: {
            action: clearInfoMessage,
            label: t.applications.editor.unitChangeWarning.ok
          }
        })
      }
      return {
        preferredUnits: units
          ? prev.preferredUnits.filter(({ id }) =>
              units.some((unit) => unit.id === id)
            )
          : prev.preferredUnits
      }
    })
  }, [
    units,
    updateFormData,
    setInfoMessage,
    clearInfoMessage,
    t.applications.editor.unitChangeWarning
  ])

  return (
    <EditorSection
      title={t.applications.editor.unitPreference.title}
      validationErrors={
        props.verificationRequested ? getErrorCount(props.errors) : 0
      }
      data-qa="unitPreference-section"
    >
      <SiblingBasisSubSection {...props} />
      <HorizontalLine />
      <UnitsSubSection {...props} units={units} />
    </EditorSection>
  )
})
