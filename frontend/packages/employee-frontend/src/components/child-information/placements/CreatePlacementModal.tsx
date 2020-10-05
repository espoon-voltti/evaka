// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import ReactSelect from 'react-select'
import LocalDate from '@evaka/lib-common/src/local-date'
import { UUID } from '~types'
import { useTranslation } from '~state/i18n'
import { getApplicationUnits } from '~api/daycare'
import { isSuccess, Loading, isLoading, Result } from '~api'
import { PlacementType } from '~types/placementdraft'
import FormModal from '~components/common/FormModal'
import { faMapMarkerAlt } from 'icon-set'
import { UIContext } from '~state/ui'
import { DatePicker } from '~components/common/DatePicker'
import Section from '~components/shared/layout/Section'
import { createPlacement } from 'api/child/placements'
import Select from '~components/common/Select'
import { PreferredUnit } from '~types/application'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'

export interface Props {
  childId: UUID
  reload: () => unknown
}

interface Form {
  type: PlacementType
  unitId: UUID | undefined
  startDate: LocalDate
  endDate: LocalDate
}

const placementTypes: PlacementType[] = [
  'PRESCHOOL',
  'PRESCHOOL_DAYCARE',
  'DAYCARE',
  'DAYCARE_PART_TIME',
  'PREPARATORY',
  'PREPARATORY_DAYCARE',
  'CLUB'
]

function CreatePlacementModal({ childId, reload }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)
  const [units, setUnits] = useState<Result<PreferredUnit[]>>(Loading())
  const [form, setForm] = useState<Form>({
    type: 'DAYCARE',
    unitId: undefined,
    startDate: LocalDate.today(),
    endDate: LocalDate.today()
  })
  const [submitting, setSubmitting] = useState<boolean>(false)

  // todo: could validate form for inverted date range
  const errors = []

  useEffect(() => {
    setUnits(Loading())
    void void getApplicationUnits(form.type, form.startDate).then((units) => {
      setUnits(units)
      if (isSuccess(units) && !form.unitId)
        setForm({
          ...form,
          unitId: units.data.sort((a, b) => (a.name < b.name ? -1 : 1))[0].id
        })
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
        if (isSuccess(res)) {
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
      resolveLabel={i18n.common.confirm}
      rejectLabel={i18n.common.cancel}
      reject={() => clearUiMode()}
      resolveDisabled={errors.length > 0 || submitting}
      resolve={() => submitForm()}
    >
      <FixedSpaceColumn>
        <Section>
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
        </Section>

        <Section>
          <div className="bold">
            {i18n.childInformation.placements.daycareUnit}
          </div>

          <ReactSelect
            placeholder={i18n.common.select}
            options={
              isSuccess(units)
                ? units.data
                    .map(({ id, name }) => ({ label: name, value: id }))
                    .sort((a, b) => (a.label < b.label ? -1 : 1))
                : []
            }
            onChange={(option) =>
              option && 'value' in option
                ? setForm({
                    ...form,
                    unitId: option.value
                  })
                : undefined
            }
            isLoading={isLoading(units)}
            loadingMessage={() => i18n.common.loading}
            noOptionsMessage={() => i18n.common.loadingFailed}
          />
        </Section>

        <Section>
          <div className="bold">{i18n.common.form.startDate}</div>

          <DatePicker
            date={form.startDate}
            onChange={(startDate) => setForm({ ...form, startDate })}
            dataQa="create-placement-start-date"
            type="full-width"
          />
        </Section>

        <Section>
          <div className="bold">{i18n.common.form.endDate}</div>

          <DatePicker
            date={form.endDate}
            onChange={(endDate) => setForm({ ...form, endDate })}
            dataQa="create-placement-end-date"
            type="full-width"
          />
        </Section>
      </FixedSpaceColumn>
    </FormModal>
  )
}

export default CreatePlacementModal
