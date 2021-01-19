// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { H2, H3, Label } from '@evaka/lib-components/src/typography'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import { UnitPreferenceFormData } from '~applications/editor/ApplicationFormData'
import {
  FixedSpaceColumn,
  FixedSpaceRow,
  FixedSpaceFlexWrap
} from '@evaka/lib-components/src/layout/flex-helpers'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import { Gap } from '@evaka/lib-components/src/white-space'
import HorizontalLine from '@evaka/lib-components/src/atoms/HorizontalLine'
import { Loading, Result, Success } from '@evaka/lib-common/src/api'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { ApplicationUnitType, getApplicationUnits } from '~applications/api'
import { ApplicationType } from '~applications/types'
import LocalDate from '@evaka/lib-common/src/local-date'
import { AlertBox } from '@evaka/lib-components/src/molecules/MessageBoxes'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import PreferredUnitBox from '~applications/editor/PreferredUnitBox'
import { SelectionChip } from '@evaka/lib-components/src/atoms/Chip'
import MultiSelect from '@evaka/lib-components/src/atoms/form/MultiSelect'
import colors from '@evaka/lib-components/src/colors'
import ExternalLink from '@evaka/lib-components/src/atoms/ExternalLink'
import { useTranslation } from '~localization'

const maxUnits = 3

export type UnitPreferenceSectionProps = {
  formData: UnitPreferenceFormData
  updateFormData: (update: Partial<UnitPreferenceFormData>) => void
  applicationType: ApplicationType
  preparatory: boolean
  preferredStartDate: LocalDate | null
}

export default React.memo(function UnitPreferenceSection({
  formData,
  updateFormData,
  applicationType,
  preparatory,
  preferredStartDate
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
        applicationType === 'club'
          ? 'CLUB'
          : applicationType === 'daycare'
          ? 'DAYCARE'
          : preparatory
          ? 'PREPARATORY'
          : 'PRESCHOOL'

      loadUnits(unitType, preferredStartDate)
    }
  }, [applicationType, preparatory, preferredStartDate])

  return (
    <ContentArea opaque paddingVertical="L">
      <H2>{t.applications.editor.unitPreference.title}</H2>

      <H3>{t.applications.editor.unitPreference.siblingBasis.title}</H3>
      <p>{t.applications.editor.unitPreference.siblingBasis.p1}</p>
      <p>{t.applications.editor.unitPreference.siblingBasis.p2}</p>

      <Checkbox
        checked={formData.siblingBasis}
        label={t.applications.editor.unitPreference.siblingBasis.checkbox}
        onChange={(checked) => updateFormData({ siblingBasis: checked })}
      />
      {formData.siblingBasis && (
        <>
          <Gap size={'s'} />
          <FixedSpaceRow spacing={'m'}>
            <FixedSpaceColumn spacing={'xs'}>
              <Label>
                {t.applications.editor.unitPreference.siblingBasis.names}
              </Label>
              <InputField
                value={formData.siblingName}
                onChange={(value) => updateFormData({ siblingName: value })}
                width={'L'}
              />
            </FixedSpaceColumn>
            <FixedSpaceColumn spacing={'xs'}>
              <Label>
                {t.applications.editor.unitPreference.siblingBasis.ssn}
              </Label>
              <InputField
                value={formData.siblingSsn}
                onChange={(value) => updateFormData({ siblingSsn: value })}
              />
            </FixedSpaceColumn>
          </FixedSpaceRow>
        </>
      )}

      <HorizontalLine />

      <H3>{t.applications.editor.unitPreference.units.title}</H3>

      {!preferredStartDate ? (
        <div>
          <AlertBox
            thin
            message={
              t.applications.editor.unitPreference.units.startDateMissing
            }
          />
        </div>
      ) : (
        <>
          <p>{t.applications.editor.unitPreference.units.p1}</p>
          <p>{t.applications.editor.unitPreference.units.p2}</p>

          <ExternalLink
            href="/"
            text={t.applications.editor.unitPreference.units.mapLink}
            newTab
          />

          <Gap size={'s'} />

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
              onClick={setDisplayFinnish}
            />
            <SelectionChip
              text={
                t.applications.editor.unitPreference.units.languageFilter.sv
              }
              selected={displaySwedish}
              onClick={setDisplaySwedish}
            />
          </FixedSpaceRow>

          <Gap size={'s'} />

          {units.isLoading && <SpinnerSegment />}
          {units.isFailure && <ErrorSegment />}
          {units.isSuccess && (
            <FixedSpaceFlexWrap horizontalSpacing={'L'} verticalSpacing={'s'}>
              <FixedWidthDiv>
                <Label>
                  {t.applications.editor.unitPreference.units.select.label}
                </Label>
                <Gap size={'xs'} />
                <MultiSelect
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
                  isClearable={false}
                  placeholder={
                    t.applications.editor.unitPreference.units.select
                      .placeholder
                  }
                  noOptionsMessage={
                    t.applications.editor.unitPreference.units.select.noOptions
                  }
                />
              </FixedWidthDiv>
              <FixedWidthDiv>
                <Label>
                  {t.applications.editor.unitPreference.units.preferences.label}
                </Label>
                <Gap size={'xs'} />
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
                  <Info>
                    {
                      t.applications.editor.unitPreference.units.preferences
                        .info
                    }
                  </Info>
                </FixedSpaceColumn>
              </FixedWidthDiv>
            </FixedSpaceFlexWrap>
          )}
        </>
      )}
    </ContentArea>
  )
})

const FixedWidthDiv = styled.div`
  width: 100%;
  max-width: 480px;
`

const Info = styled.p`
  color: ${colors.greyscale.dark};
  margin: 0;
`
