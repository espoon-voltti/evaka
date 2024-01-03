// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'

import { useQuery } from 'lib-common/query'
import { SelectionChip } from 'lib-components/atoms/Chip'
import ExternalLink from 'lib-components/atoms/ExternalLink'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { H3, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { getMaxPreferredUnits } from 'lib-customizations/citizen'
import colors from 'lib-customizations/common'

import PreferredUnitBox from '../../../applications/editor/unit-preference/PreferredUnitBox'
import { useTranslation } from '../../../localization'
import { applicationUnitsQuery } from '../../queries'

import { UnitPreferenceSectionProps } from './UnitPreferenceSection'

export default React.memo(function UnitsSubSection({
  formData,
  updateFormData,
  errors,
  verificationRequested,
  applicationType,
  preparatory,
  preferredStartDate,
  shiftCare
}: UnitPreferenceSectionProps) {
  const t = useTranslation()
  const [displayFinnish, setDisplayFinnish] = useState(true)
  const [displaySwedish, setDisplaySwedish] = useState(false)

  const { data: units = null } = useQuery(
    applicationUnitsQuery(
      applicationType,
      preparatory,
      preferredStartDate,
      shiftCare
    )
  )
  useEffect(() => {
    updateFormData((prev) => ({
      preferredUnits: units
        ? prev.preferredUnits.filter(({ id }) =>
            units.some((unit) => unit.id === id)
          )
        : prev.preferredUnits
    }))
  }, [units, updateFormData])

  const maxUnits = getMaxPreferredUnits(applicationType)

  return (
    <>
      <H3>{t.applications.editor.unitPreference.units.title(maxUnits)}</H3>
      {t.applications.editor.unitPreference.units.info[applicationType]}

      <ExternalLink
        href="/map"
        text={t.applications.editor.unitPreference.units.mapLink}
        newTab
      />

      <Gap size="s" />

      {!preferredStartDate ? (
        <div>
          <AlertBox
            message={
              t.applications.editor.unitPreference.units.startDateMissing
            }
          />
        </div>
      ) : (
        <>
          <Label>
            {t.applications.editor.unitPreference.units.languageFilter.label}
          </Label>
          <Gap size="xs" />
          <FixedSpaceRow>
            <SelectionChip
              text={
                t.applications.editor.unitPreference.units.languageFilter.fi
              }
              selected={displayFinnish}
              onChange={setDisplayFinnish}
            />
            <SelectionChip
              text={
                t.applications.editor.unitPreference.units.languageFilter.sv
              }
              selected={displaySwedish}
              onChange={setDisplaySwedish}
            />
          </FixedSpaceRow>

          <Gap size="m" />

          <FixedSpaceFlexWrap horizontalSpacing="L" verticalSpacing="s">
            <FixedWidthDiv>
              <Label htmlFor="unit-selector">
                {t.applications.editor.unitPreference.units.select.label(
                  maxUnits
                )}{' '}
                *
              </Label>
              <Gap size="xs" />
              <MultiSelect
                data-qa="preferredUnits-input"
                inputId="unit-selector"
                value={
                  units
                    ? units.filter(
                        (u) =>
                          !!formData.preferredUnits.find((u2) => u2.id === u.id)
                      )
                    : []
                }
                options={
                  units
                    ? units.filter(
                        (u) =>
                          (displayFinnish && u.language === 'fi') ||
                          (displaySwedish && u.language === 'sv')
                      )
                    : []
                }
                getOptionId={(unit) => unit.id}
                getOptionLabel={(unit) => unit.name}
                getOptionSecondaryText={(unit) => unit.streetAddress}
                onChange={(selected) => {
                  if (selected.length <= maxUnits) {
                    updateFormData((prev) => ({
                      ...prev,
                      preferredUnits: selected
                    }))
                  }
                }}
                maxSelected={maxUnits}
                isClearable={false}
                placeholder={
                  formData.preferredUnits.length < maxUnits
                    ? t.applications.editor.unitPreference.units.select
                        .placeholder
                    : t.applications.editor.unitPreference.units.select
                        .maxSelected
                }
                noOptionsMessage={
                  t.applications.editor.unitPreference.units.select.noOptions
                }
                showValuesInInput={false}
              />

              <Gap size="s" />
              <Info>
                {t.applications.editor.unitPreference.units.preferences.info(
                  maxUnits
                )}
              </Info>
              <Gap size="xs" />
            </FixedWidthDiv>
            <FixedWidthDiv>
              <Label>
                {t.applications.editor.unitPreference.units.preferences.label(
                  maxUnits
                )}
              </Label>
              <Gap size="xs" />
              {!verificationRequested &&
                formData.preferredUnits.length === 0 && (
                  <Info>
                    {
                      t.applications.editor.unitPreference.units.preferences
                        .noSelections
                    }
                  </Info>
                )}
              {verificationRequested && errors.preferredUnits?.arrayErrors && (
                <AlertBox
                  message={
                    t.validationErrors[errors.preferredUnits.arrayErrors]
                  }
                  thin
                />
              )}
              <FixedSpaceColumn spacing="s">
                {units
                  ? formData.preferredUnits
                      .map((u) => units.find((u2) => u.id === u2.id))
                      .map((unit, i) =>
                        unit ? (
                          <PreferredUnitBox
                            key={unit.id}
                            unit={unit}
                            n={i + 1}
                            remove={() =>
                              updateFormData((prev) => ({
                                preferredUnits: [
                                  ...prev.preferredUnits.slice(0, i),
                                  ...prev.preferredUnits.slice(i + 1)
                                ]
                              }))
                            }
                            moveUp={
                              i > 0
                                ? () =>
                                    updateFormData((prev) => ({
                                      preferredUnits: [
                                        ...prev.preferredUnits.slice(0, i - 1),
                                        prev.preferredUnits[i],
                                        prev.preferredUnits[i - 1],
                                        ...prev.preferredUnits.slice(i + 1)
                                      ]
                                    }))
                                : null
                            }
                            moveDown={
                              i < formData.preferredUnits.length - 1
                                ? () =>
                                    updateFormData((prev) => ({
                                      preferredUnits: [
                                        ...prev.preferredUnits.slice(0, i),
                                        prev.preferredUnits[i + 1],
                                        prev.preferredUnits[i],
                                        ...prev.preferredUnits.slice(i + 2)
                                      ]
                                    }))
                                : null
                            }
                          />
                        ) : null
                      )
                  : null}
              </FixedSpaceColumn>
            </FixedWidthDiv>
          </FixedSpaceFlexWrap>
        </>
      )}
    </>
  )
})

const FixedWidthDiv = styled.div`
  width: 100%;
  max-width: 480px;
`

const Info = styled(P)`
  color: ${colors.grayscale.g70};
  margin: 0;
`
