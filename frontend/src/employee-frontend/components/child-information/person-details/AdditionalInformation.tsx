// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import { AdditionalInformation } from 'lib-common/generated/api-types/daycare'
import { ChildId } from 'lib-common/generated/api-types/shared'
import {
  MealTexture,
  SpecialDiet
} from 'lib-common/generated/api-types/specialdiet'
import { IsoLanguage, isoLanguages } from 'lib-common/generated/language'
import { useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import TextArea from 'lib-components/atoms/form/TextArea'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H4 } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/employee'
import { faPen } from 'lib-icons'

import LabelValueList from '../../../components/common/LabelValueList'
import { useTranslation } from '../../../state/i18n'
import { UIContext, UiState } from '../../../state/ui'
import { formatParagraphs } from '../../../utils/html-utils'
import { RequireRole } from '../../../utils/roles'
import { renderResult } from '../../async-rendering'
import { FlexRow } from '../../common/styled/containers'
import { textAreaRows } from '../../utils'
import {
  getAdditionalInfoQuery,
  updateAdditionalInfoMutation
} from '../queries'

import {
  mealTexturesQuery,
  nekkuDietTypesquery,
  specialDietsQuery
} from './queries'

const TextAreaInput = styled(TextArea)`
  width: 100%;
`

const RightAlignedRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  padding-top: 20px;
`

const filterLanguages = (
  input: string,
  items: readonly IsoLanguage[]
): IsoLanguage[] => {
  const filter = input.toLowerCase()
  return items.filter((item) => item.nameFi.includes(filter))
}

const filterDiets = (
  input: string,
  diets: readonly SpecialDiet[]
): SpecialDiet[] => {
  const filter = input.toLowerCase()
  return diets.filter((diet) =>
    getDietCaption(diet).toLowerCase().startsWith(filter)
  )
}

const filterMealTextures = (
  input: string,
  mealTextures: readonly MealTexture[]
): MealTexture[] => {
  const filter = input.toLowerCase()
  return mealTextures.filter((mealTexture) =>
    getMealTextureCaption(mealTexture).toLowerCase().startsWith(filter)
  )
}

interface Props {
  childId: ChildId
}

function getDietCaption(diet: SpecialDiet) {
  return diet.abbreviation
}

function getMealTextureCaption(mealTexture: MealTexture) {
  return mealTexture.name
}

export default React.memo(function AdditionalInformation({ childId }: Props) {
  const { i18n } = useTranslation()
  const additionalInformation = useQueryResult(
    getAdditionalInfoQuery({ childId })
  )
  const { uiMode, toggleUiMode, clearUiMode } = useContext<UiState>(UIContext)
  const [form, setForm] = useState<AdditionalInformation>({
    additionalInfo: '',
    allergies: '',
    diet: '',
    preferredName: '',
    medication: '',
    languageAtHome: '',
    languageAtHomeDetails: '',
    specialDiet: null,
    mealTexture: null,
    nekkuDiet: null
  })

  const editing = uiMode === 'child-additional-details-editing'

  const startEdit = useCallback(() => {
    if (additionalInformation.isSuccess) {
      setForm({
        additionalInfo: additionalInformation.value.additionalInfo,
        allergies: additionalInformation.value.allergies,
        diet: additionalInformation.value.diet,
        preferredName: additionalInformation.value.preferredName,
        medication: additionalInformation.value.medication,
        languageAtHome: additionalInformation.value.languageAtHome,
        languageAtHomeDetails:
          additionalInformation.value.languageAtHomeDetails,
        specialDiet: additionalInformation.value.specialDiet,
        mealTexture: additionalInformation.value.mealTexture,
        nekkuDiet: additionalInformation.value.nekkuDiet
      })
      toggleUiMode('child-additional-details-editing')
    }
  }, [additionalInformation, toggleUiMode])

  const valueWidth = '600px'

  const languages = useMemo(
    () => sortBy(Object.values(isoLanguages), ({ nameFi }) => nameFi),
    []
  )
  const specialDiets = useQueryResult(specialDietsQuery(), {
    enabled: editing
  }).getOrElse([])
  const specialDietsOrdered = useMemo(
    () => sortBy(specialDiets, getDietCaption),
    [specialDiets]
  )
  const mealTextures = useQueryResult(mealTexturesQuery(), {
    enabled: editing
  }).getOrElse([])
  const mealTexturesOrdered = useMemo(
    () => sortBy(mealTextures, getMealTextureCaption),
    [mealTextures]
  )

  const nekkuDiets = useQueryResult(nekkuDietTypesquery()).getOrElse([])

  return (
    <div data-qa="additional-information-section">
      <FlexRow justifyContent="space-between">
        <H4>{i18n.childInformation.additionalInformation.title}</H4>
        {!editing && (
          <RequireRole
            oneOf={[
              'SERVICE_WORKER',
              'FINANCE_ADMIN',
              'UNIT_SUPERVISOR',
              'ADMIN',
              'STAFF',
              'SPECIAL_EDUCATION_TEACHER'
            ]}
          >
            <Button
              appearance="inline"
              icon={faPen}
              onClick={startEdit}
              data-qa="edit-child-settings-button"
              text={i18n.common.edit}
            />
          </RequireRole>
        )}
      </FlexRow>
      {renderResult(additionalInformation, (data) => (
        <>
          <LabelValueList
            spacing="small"
            contents={[
              {
                label:
                  i18n.childInformation.additionalInformation.preferredName,
                value: editing ? (
                  <TextAreaInput
                    value={form.preferredName}
                    onChange={(value) =>
                      setForm({ ...form, preferredName: value })
                    }
                    rows={textAreaRows(form.preferredName)}
                  />
                ) : (
                  formatParagraphs(data.preferredName)
                ),
                valueWidth
              },
              {
                label:
                  i18n.childInformation.additionalInformation.additionalInfo,
                value: editing ? (
                  <TextAreaInput
                    value={form.additionalInfo}
                    onChange={(value) =>
                      setForm({ ...form, additionalInfo: value })
                    }
                    rows={textAreaRows(form.additionalInfo)}
                  />
                ) : (
                  formatParagraphs(data.additionalInfo)
                ),
                valueWidth
              },
              {
                label: i18n.childInformation.additionalInformation.allergies,
                value: editing ? (
                  <TextAreaInput
                    value={form.allergies}
                    onChange={(value) => setForm({ ...form, allergies: value })}
                    rows={textAreaRows(form.allergies)}
                  />
                ) : (
                  formatParagraphs(data.allergies)
                ),
                valueWidth
              },
              {
                label: i18n.childInformation.additionalInformation.diet,
                value: editing ? (
                  <TextAreaInput
                    value={form.diet}
                    onChange={(value) => setForm({ ...form, diet: value })}
                    rows={textAreaRows(form.diet)}
                  />
                ) : (
                  formatParagraphs(data.diet)
                ),
                valueWidth
              },
              {
                label: i18n.childInformation.additionalInformation.medication,
                value: editing ? (
                  <TextAreaInput
                    value={form.medication}
                    onChange={(value) =>
                      setForm({ ...form, medication: value })
                    }
                    rows={textAreaRows(form.medication)}
                    data-qa="medication-input"
                  />
                ) : (
                  <span data-qa="medication">
                    {formatParagraphs(data.medication)}
                  </span>
                ),
                valueWidth
              },
              {
                label: i18n.childInformation.personDetails.languageAtHome,
                value: editing ? (
                  <>
                    <Combobox
                      data-qa="input-language-at-home"
                      items={languages}
                      getItemDataQa={(item) => `language-${item.id}`}
                      selectedItem={
                        (form.languageAtHome
                          ? isoLanguages[form.languageAtHome]
                          : null) ?? null
                      }
                      onChange={(item) =>
                        setForm({ ...form, languageAtHome: item?.id ?? '' })
                      }
                      filterItems={filterLanguages}
                      getItemLabel={(item) => item?.nameFi}
                      placeholder={
                        i18n.childInformation.personDetails.placeholder
                          .languageAtHome
                      }
                      clearable={true}
                    />
                    <TextArea
                      data-qa="input-language-at-home-details"
                      value={form.languageAtHomeDetails}
                      placeholder={
                        i18n.childInformation.personDetails.placeholder
                          .languageAtHomeDetails
                      }
                      onChange={(languageAtHomeDetails) =>
                        setForm({ ...form, languageAtHomeDetails })
                      }
                    />
                  </>
                ) : (
                  <>
                    <div data-qa="person-language-at-home">
                      {isoLanguages[data.languageAtHome]?.nameFi ?? ''}
                    </div>
                    <div data-qa="person-language-at-home-details">
                      {data.languageAtHomeDetails}
                    </div>
                  </>
                )
              },
              ...(featureFlags.jamixIntegration
                ? [
                    {
                      label: i18n.childInformation.personDetails.specialDiet,
                      value: editing ? (
                        <>
                          <Combobox
                            data-qa="diet-input"
                            items={specialDietsOrdered}
                            getItemDataQa={(item) => `diet-${item.id}`}
                            selectedItem={form.specialDiet}
                            onChange={(diet) =>
                              setForm({ ...form, specialDiet: diet ?? null })
                            }
                            filterItems={filterDiets}
                            getItemLabel={getDietCaption}
                            placeholder={
                              i18n.childInformation.personDetails.placeholder
                                .specialDiet
                            }
                            clearable={true}
                          />
                        </>
                      ) : (
                        <>
                          <div data-qa="diet-value-display">
                            {data.specialDiet
                              ? getDietCaption(data.specialDiet)
                              : '-'}
                          </div>
                        </>
                      )
                    },
                    {
                      label: i18n.childInformation.personDetails.mealTexture,
                      value: editing ? (
                        <>
                          <Combobox
                            data-qa="meal-texture-input"
                            items={mealTexturesOrdered}
                            getItemDataQa={(item) => `meal-texture-${item.id}`}
                            selectedItem={form.mealTexture}
                            onChange={(mealTexture) =>
                              setForm({
                                ...form,
                                mealTexture: mealTexture ?? null
                              })
                            }
                            filterItems={filterMealTextures}
                            getItemLabel={getMealTextureCaption}
                            placeholder={
                              i18n.childInformation.personDetails.placeholder
                                .mealTexture
                            }
                            clearable={true}
                          />
                        </>
                      ) : (
                        <>
                          <div data-qa="meal-texture-value-display">
                            {data.mealTexture
                              ? getMealTextureCaption(data.mealTexture)
                              : '-'}
                          </div>
                        </>
                      )
                    }
                  ]
                : []),
              ...(featureFlags.nekkuIntegration
                ? [
                    {
                      label: i18n.childInformation.personDetails.nekkuDiet,
                      value: editing ? (
                        <>
                          <Combobox
                            data-qa="nekku-diet-input"
                            items={nekkuDiets}
                            selectedItem={nekkuDiets.find(
                              (value) => value.type === form.nekkuDiet
                            )}
                            onChange={(diet) =>
                              setForm({
                                ...form,
                                nekkuDiet: diet?.type ?? null
                              })
                            }
                            getItemLabel={(diet) => diet?.name ?? '-'}
                          />
                        </>
                      ) : (
                        <>
                          <div data-qa="nekku-diet-display">
                            {nekkuDiets.find(
                              (value) => value.type === data.nekkuDiet
                            )?.name ?? '-'}
                          </div>
                        </>
                      )
                    }
                  ]
                : [])
            ]}
          />
          {editing && (
            <RightAlignedRow>
              <FixedSpaceRow>
                <LegacyButton onClick={clearUiMode} text={i18n.common.cancel} />
                <MutateButton
                  primary
                  disabled={false}
                  mutation={updateAdditionalInfoMutation}
                  onClick={() => ({ childId, body: form })}
                  onSuccess={clearUiMode}
                  data-qa="confirm-edited-child-button"
                  text={i18n.common.confirm}
                />
              </FixedSpaceRow>
            </RightAlignedRow>
          )}
        </>
      ))}
    </div>
  )
})
