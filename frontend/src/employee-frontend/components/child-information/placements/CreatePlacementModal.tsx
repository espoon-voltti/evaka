// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import ReactSelect from 'react-select'
import LocalDate from '@evaka/lib-common/src/local-date'
import { UUID } from '../../../types'
import { useTranslation } from '../../../state/i18n'
import { getApplicationUnits } from '../../../api/daycare'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { PlacementType } from '../../../types/child'
import FormModal from '@evaka/lib-components/src/molecules/modals/FormModal'
import { faMapMarkerAlt } from '@evaka/lib-icons'
import { UIContext } from '../../../state/ui'
import { DatePickerDeprecated } from '@evaka/lib-components/src/molecules/DatePickerDeprecated'
import { createPlacement } from '../../../api/child/placements'
import Select from '../../../components/common/Select'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import { PublicUnit } from '@evaka/lib-common/src/api-types/units/PublicUnit'
import { AlertBox } from '@evaka/lib-components/src/molecules/MessageBoxes'

export interface Props {
  childId: UUID
  reload: () => unknown
}

interface Form {
  type: PlacementType
  unitId: UUID | undefined
  startDate: LocalDate
  endDate: LocalDate
  ghostUnit: boolean
}

const placementTypes: PlacementType[] = [
  'PRESCHOOL',
  'PRESCHOOL_DAYCARE',
  'DAYCARE',
  'DAYCARE_PART_TIME',
  'PREPARATORY',
  'PREPARATORY_DAYCARE',
  'CLUB',
  'TEMPORARY_DAYCARE',
  'TEMPORARY_DAYCARE_PART_DAY'
]

function CreatePlacementModal({ childId, reload }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)
  const [units, setUnits] = useState<Result<PublicUnit[]>>(Loading.of())
  const [form, setForm] = useState<Form>({
    type: 'DAYCARE',
    unitId: undefined,
    startDate: LocalDate.today(),
    endDate: LocalDate.today(),
    ghostUnit: false
  })
  const [submitting, setSubmitting] = useState<boolean>(false)

  // todo: could validate form for inverted date range
  const errors = []

  useEffect(() => {
    setUnits(Loading.of())
    void void getApplicationUnits(form.type, form.startDate).then((units) => {
      setUnits(units)
      if (units.isSuccess && !form.unitId) {
        const defaultUnit = units.value.sort((a, b) =>
          a.name < b.name ? -1 : 1
        )[0]
        setForm({
          ...form,
          unitId: defaultUnit.id,
          ghostUnit: defaultUnit.ghostUnit || false
        })
      }
    })
  }, [setUnits, form.type, form.startDate])

  const submitForm = () => {
    setSubmitting(true)
    createPlacement({
      childId: childId,
      type: form.type,
      unitId: form.unitId || '',
      startDate: form.startDate,
      endDate: form.endDate
    })
      .then((res) => {
        setSubmitting(false)
        if (res.isSuccess) {
          reload()
          clearUiMode()
        }
      })
      .catch(() => setSubmitting(false))
  }

  return (
    <FormModal
      data-qa="create-placement-modal"
      title={i18n.childInformation.placements.createPlacement.title}
      text={i18n.childInformation.placements.createPlacement.text}
      icon={faMapMarkerAlt}
      iconColour={'blue'}
      resolve={{
        action: submitForm,
        label: i18n.common.confirm,
        disabled: errors.length > 0 || submitting || form.ghostUnit,
        info: form.ghostUnit
          ? {
              text: i18n.childInformation.placements.warning.ghostUnit,
              status: 'warning'
            }
          : undefined
      }}
      reject={{
        action: clearUiMode,
        label: i18n.common.cancel
      }}
    >
      <FixedSpaceColumn>
        <section>
          <div className="bold">{i18n.childInformation.placements.type}</div>

          <Select
            options={placementTypes.map((type) => ({
              value: type,
              label: i18n.placement.type[type]
            }))}
            value={{ value: form.type, label: i18n.placement.type[form.type] }}
            onChange={(value) =>
              value && 'value' in value
                ? setForm({
                    ...form,
                    type: value.value as PlacementType
                  })
                : undefined
            }
          />
        </section>

        <section data-qa="unit-select">
          <div className="bold">
            {i18n.childInformation.placements.daycareUnit}
          </div>

          <ReactSelect
            placeholder={i18n.common.select}
            options={units
              .map((us) =>
                us
                  .map(({ id, name, ghostUnit }) => ({
                    label: name,
                    value: id,
                    ghostUnit: ghostUnit
                  }))
                  .sort((a, b) => (a.label < b.label ? -1 : 1))
              )
              .getOrElse([])}
            onChange={(option) =>
              option && 'value' in option
                ? setForm({
                    ...form,
                    unitId: option.value,
                    ghostUnit: option.ghostUnit || false
                  })
                : undefined
            }
            isLoading={units.isLoading}
            loadingMessage={() => i18n.common.loading}
            noOptionsMessage={() => i18n.common.loadingFailed}
          />
        </section>

        <section>
          <div className="bold">{i18n.common.form.startDate}</div>

          <DatePickerDeprecated
            date={form.startDate}
            onChange={(startDate) => setForm({ ...form, startDate })}
            dataQa="create-placement-start-date"
            type="full-width"
          />
        </section>

        <section>
          <div className="bold">{i18n.common.form.endDate}</div>

          <DatePickerDeprecated
            date={form.endDate}
            onChange={(endDate) => setForm({ ...form, endDate })}
            dataQa="create-placement-end-date"
            type="full-width"
          />
        </section>

        {form.type === 'TEMPORARY_DAYCARE' ||
        form.type === 'TEMPORARY_DAYCARE_PART_DAY' ? (
          <AlertBox
            thin
            message={
              i18n.childInformation.placements.createPlacement
                .temporaryDaycareWarning
            }
          />
        ) : null}
      </FixedSpaceColumn>
    </FormModal>
  )
}

export default CreatePlacementModal
