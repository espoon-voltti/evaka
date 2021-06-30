// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useMemo, useState } from 'react'
import styled from 'styled-components'
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
import { placementTypes } from 'lib-customizations/employee'
import colors from 'lib-customizations/common'

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

function CreatePlacementModal({ childId, reload }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)
  const [units, setUnits] = useState<Result<Unit[]>>(Loading.of())
  const [form, setForm] = useState<Form>({
    type: 'DAYCARE',
    unitId: undefined,
    startDate: LocalDate.today(),
    endDate: LocalDate.today(),
    ghostUnit: false
  })
  const [submitting, setSubmitting] = useState<boolean>(false)

  const activeUnits = useMemo(() => {
    if (form.startDate.isAfter(form.endDate)) return units

    return units.map((list) =>
      list.filter((u) =>
        new DateRange(
          u.openingDate ?? LocalDate.of(1900, 1, 1),
          u.closingDate
        ).overlapsWith(new DateRange(form.startDate, form.endDate))
      )
    )
  }, [units, form.startDate, form.endDate])

  const errors = useMemo(() => {
    const errors = []
    if (!form.unitId) {
      errors.push(i18n.childInformation.placements.createPlacement.unitMissing)
    }

    if (form.ghostUnit) {
      errors.push(i18n.childInformation.placements.warning.ghostUnit)
    }

    if (form.startDate.isAfter(form.endDate)) {
      errors.push(i18n.validationError.invertedDateRange)
    }

    return errors
  }, [i18n, form])

  const loadUnits = useRestApi(getDaycares, setUnits)

  useEffect(loadUnits, [loadUnits])

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
        disabled: errors.length > 0 || submitting
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

        {errors.map((error) => (
          <ValidationError key={error}>{error}</ValidationError>
        ))}
      </FixedSpaceColumn>
    </FormModal>
  )
}

const ValidationError = styled.div`
  font-size: 0.9em;
  color: ${colors.accents.red};
`

export default CreatePlacementModal
