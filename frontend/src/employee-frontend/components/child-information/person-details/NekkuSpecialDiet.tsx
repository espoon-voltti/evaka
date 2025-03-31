// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment } from 'react'

import { combine } from 'lib-common/api'
import { AdditionalInformation } from 'lib-common/generated/api-types/daycare'
import { NekkuSpecialDietChoices } from 'lib-common/generated/api-types/nekku'
import { useQueryResult } from 'lib-common/query'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import TextArea from 'lib-components/atoms/form/TextArea'
import { Gap } from 'lib-components/white-space'

import { renderResult } from '../../async-rendering'

import {
  nekkuSpecialDietTypesQuery,
  nekkuSpecialDietFieldsQuery,
  nekkuSpecialDietOptionsQuery
} from './queries'

interface Props {
  dietChoices: NekkuSpecialDietChoices[]
  formData: AdditionalInformation
  editing: boolean
  setField(dietId: string, fieldId: string, value: string): void
  toggleField(dietId: string, fieldId: string, value: string): void
}

export default React.memo(function NekkuSpecialDiet(props: Props) {
  const specialDietTypes = useQueryResult(nekkuSpecialDietTypesQuery())

  const specialDietFields = useQueryResult(nekkuSpecialDietFieldsQuery())

  const specialDietOptions = useQueryResult(nekkuSpecialDietOptionsQuery())

  return renderResult(
    combine(specialDietTypes, specialDietFields, specialDietOptions),
    ([specialDietTypes, specialDietFields, specialDietOptions]) => (
      <div data-qa="nekku-special-diet-editor">
        {specialDietTypes.map((diet) => (
          <Fragment key={diet.id}>
            {specialDietTypes.length > 1 && <p key={diet.name}>{diet.name}</p>}
            <>
              {specialDietFields
                .filter(
                  (value) =>
                    value.diet_id === diet.id && value.type === 'CHECKBOXLIST'
                )
                .map((field) =>
                  props.editing ? (
                    specialDietOptions
                      .filter((value) => value.fieldId === field.id)
                      .sort((a, b) => a.weight - b.weight)
                      .map((option) => (
                        <Checkbox
                          key={field.id + '-' + option.key}
                          label={option.key}
                          checked={
                            props.formData.nekkuSpecialDietChoices.find(
                              (value) =>
                                value.dietId === field.diet_id &&
                                value.fieldId === field.id &&
                                value.value === option.value
                            ) !== undefined
                          }
                          onChange={() =>
                            props.toggleField(
                              field.diet_id,
                              field.id,
                              option.value
                            )
                          }
                        />
                      ))
                  ) : (
                    <p key={field.id}>
                      {props.dietChoices
                        .filter(
                          (value) =>
                            value.dietId === field.diet_id &&
                            value.fieldId === field.id
                        )
                        .map((choice) => choice.value)
                        .join(', ')}
                    </p>
                  )
                )}
              {specialDietFields
                .filter(
                  (value) => value.diet_id === diet.id && value.type === 'TEXT'
                )
                .map((field) =>
                  props.editing ? (
                    <Fragment key={field.id}>
                      <Gap size="s" />
                      {field.name}
                      <TextArea
                        value={
                          props.formData.nekkuSpecialDietChoices.find(
                            (value) =>
                              value.dietId === field.diet_id &&
                              value.fieldId === field.id
                          )?.value ?? ''
                        }
                        onChange={(value) =>
                          props.setField(field.diet_id, field.id, value)
                        }
                      />
                    </Fragment>
                  ) : (
                    <p key={field.name}>
                      {field.name +
                        ': ' +
                        (props.dietChoices.find(
                          (value) =>
                            value.dietId === field.diet_id &&
                            value.fieldId === field.id
                        )?.value ?? '-')}
                    </p>
                  )
                )}
            </>
          </Fragment>
        ))}
      </div>
    )
  )
})
