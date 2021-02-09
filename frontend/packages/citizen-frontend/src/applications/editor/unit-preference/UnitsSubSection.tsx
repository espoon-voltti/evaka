// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { H3, Label, P } from '@evaka/lib-components/src/typography'
import {
  FixedSpaceColumn,
  FixedSpaceRow,
  FixedSpaceFlexWrap
} from '@evaka/lib-components/src/layout/flex-helpers'
import { Gap } from '@evaka/lib-components/src/white-space'
import { Loading, Result, Success } from '@evaka/lib-common/src/api'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { ApplicationUnitType, getApplicationUnits } from '~applications/api'
import { AlertBox } from '@evaka/lib-components/src/molecules/MessageBoxes'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import PreferredUnitBox from '~applications/editor/unit-preference/PreferredUnitBox'
import { SelectionChip } from '@evaka/lib-components/src/atoms/Chip'
import MultiSelect from '@evaka/lib-components/src/atoms/form/MultiSelect'
import colors from '@evaka/lib-components/src/colors'
import ExternalLink from '@evaka/lib-components/src/atoms/ExternalLink'
import { useTranslation } from '~localization'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import { UnitPreferenceSectionProps } from '~applications/editor/unit-preference/UnitPreferenceSection'

const maxUnits = 3

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
  const [units, setUnits] = useState<Result<PublicUnit[]>>(Loading.of())
  const loadUnits = useRestApi(getApplicationUnits, setUnits)
  const [displayFinnish, setDisplayFinnish] = useState(true)
  const [displaySwedish, setDisplaySwedish] = useState(false)

  useEffect(() => {
    if (!preferredStartDate) {
      setUnits(Success.of([]))
    } else {
      const unitType: ApplicationUnitType =
        applicationType === 'CLUB'
          ? 'CLUB'
          : applicationType === 'DAYCARE'
          ? 'DAYCARE'
          : preparatory
          ? 'PREPARATORY'
          : 'PRESCHOOL'

      loadUnits(unitType, preferredStartDate, shiftCare)
    }
  }, [applicationType, preparatory, preferredStartDate?.formatIso(), shiftCare])

  return (
    <>
      <H3>{t.applications.editor.unitPreference.units.title}</H3>

      <P
        dangerouslySetInnerHTML={{
          __html:
            t.applications.editor.unitPreference.units.info[applicationType]
        }}
      />

      <ExternalLink
        href="/"
        text={t.applications.editor.unitPreference.units.mapLink}
        newTab
      />

      <Gap size={'s'} />

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
          <Gap size={'xs'} />
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

          <Gap size={'m'} />

          {units.isLoading && <SpinnerSegment />}
          {units.isFailure && <ErrorSegment />}
          {units.isSuccess && (
            <FixedSpaceFlexWrap horizontalSpacing={'L'} verticalSpacing={'s'}>
              <FixedWidthDiv>
                <Label htmlFor="unit-selector">
                  {t.applications.editor.unitPreference.units.select.label} *
                </Label>
                <Gap size={'xs'} />
                <MultiSelect
                  data-qa={'preferredUnits-input'}
                  inputId="unit-selector"
                  value={units.value.filter(
                    (u) =>
                      !!formData.preferredUnits.find((u2) => u2.id === u.id)
                  )}
                  options={units.value.filter(
                    (u) =>
                      (displayFinnish && u.language === 'fi') ||
                      (displaySwedish && u.language === 'sv')
                  )}
                  getOptionId={(unit) => unit.id}
                  getOptionLabel={(unit) => unit.name}
                  getOptionSecondaryText={(unit) => unit.streetAddress}
                  onChange={(selected) => {
                    if (selected.length <= maxUnits) {
                      updateFormData({
                        preferredUnits: selected
                      })
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
                  {t.applications.editor.unitPreference.units.preferences.info}
                </Info>
                <Gap size="xs" />
              </FixedWidthDiv>
              <FixedWidthDiv>
                <Label>
                  {t.applications.editor.unitPreference.units.preferences.label}
                </Label>
                <Gap size={'xs'} />
                {!verificationRequested &&
                  formData.preferredUnits.length === 0 && (
                    <Info>
                      {
                        t.applications.editor.unitPreference.units.preferences
                          .noSelections
                      }
                    </Info>
                  )}
                {verificationRequested &&
                  errors.preferredUnits?.arrayErrors && (
                    <AlertBox
                      message={
                        t.validationErrors[errors.preferredUnits.arrayErrors]
                      }
                      thin
                    />
                  )}
                <FixedSpaceColumn spacing={'s'}>
                  {formData.preferredUnits
                    .map((u) => units.value.find((u2) => u.id === u2.id))
                    .map((unit, i) =>
                      unit ? (
                        <PreferredUnitBox
                          key={unit.id}
                          unit={unit}
                          n={i + 1}
                          remove={() =>
                            updateFormData({
                              preferredUnits: [
                                ...formData.preferredUnits.slice(0, i),
                                ...formData.preferredUnits.slice(i + 1)
                              ]
                            })
                          }
                          moveUp={
                            i > 0
                              ? () =>
                                  updateFormData({
                                    preferredUnits: [
                                      ...formData.preferredUnits.slice(
                                        0,
                                        i - 1
                                      ),
                                      formData.preferredUnits[i],
                                      formData.preferredUnits[i - 1],
                                      ...formData.preferredUnits.slice(i + 1)
                                    ]
                                  })
                              : null
                          }
                          moveDown={
                            i < formData.preferredUnits.length - 1
                              ? () =>
                                  updateFormData({
                                    preferredUnits: [
                                      ...formData.preferredUnits.slice(0, i),
                                      formData.preferredUnits[i + 1],
                                      formData.preferredUnits[i],
                                      ...formData.preferredUnits.slice(i + 2)
                                    ]
                                  })
                              : null
                          }
                        />
                      ) : null
                    )}
                </FixedSpaceColumn>
              </FixedWidthDiv>
            </FixedSpaceFlexWrap>
          )}
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
  color: ${colors.greyscale.dark};
  margin: 0;
`
