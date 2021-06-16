// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import ReactSelect from 'react-select'
import LocalDate from 'lib-common/local-date'
import { UUID } from '../../../types'
import { useTranslation } from '../../../state/i18n'
import { Loading, Result } from 'lib-common/api'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { faMapMarkerAlt } from 'lib-icons'
import { UIContext } from '../../../state/ui'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { createPlacement } from '../../../api/child/placements'
import Select from '../../../components/common/Select'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import { PlacementType } from 'lib-common/api-types/serviceNeed/common'
import { Unit } from '../../../types/unit'
import { getDaycares } from '../../../api/unit'
import { useRestApi } from '../../../../lib-common/utils/useRestApi'
import DateRange from 'lib-common/date-range'

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
  const [units, setUnits] = useState<Result<Unit[]>>(Loading.of())
  const [activeUnits, setActiveUnits] = useState<Result<Unit[]>>(Loading.of())
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

  const loadUnits = useRestApi(getDaycares, setUnits)

  useEffect(loadUnits, [loadUnits])

  useEffect(() => {
    if (form.startDate.isAfter(form.endDate)) return

    setActiveUnits(
      units.map((list) =>
        list.filter((u) =>
          new DateRange(
            u.openingDate ?? LocalDate.of(1900, 1, 1),
            u.closingDate
          ).overlapsWith(new DateRange(form.startDate, form.endDate))
        )
      )
    )
  }, [units, setActiveUnits, form.startDate, form.endDate])

  const submitForm = () => {
    if (!form.unitId) return

    setSubmitting(true)
    createPlacement({
      childId: childId,
      type: form.type,
      unitId: form.unitId,
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
            options={activeUnits
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
            isLoading={activeUnits.isLoading}
            loadingMessage={() => i18n.common.loading}
            noOptionsMessage={() => i18n.common.loadingFailed}
          />
        </section>

        <section>
          <div className="bold">{i18n.common.form.startDate}</div>

          <DatePickerDeprecated
            date={form.startDate}
            onChange={(startDate) => setForm({ ...form, startDate })}
            data-qa="create-placement-start-date"
            type="full-width"
          />
        </section>

        <section>
          <div className="bold">{i18n.common.form.endDate}</div>

          <DatePickerDeprecated
            date={form.endDate}
            onChange={(endDate) => setForm({ ...form, endDate })}
            data-qa="create-placement-end-date"
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
