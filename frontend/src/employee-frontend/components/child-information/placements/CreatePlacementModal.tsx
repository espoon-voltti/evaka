// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import { wrapResult } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Select from 'lib-components/atoms/dropdowns/Select'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags, placementTypes } from 'lib-customizations/employee'
import { faMapMarkerAlt } from 'lib-icons'

import { createPlacement } from '../../../generated/api-clients/placement'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import RetroactiveConfirmation, {
  isChangeRetroactive
} from '../../common/RetroactiveConfirmation'
import { unitsQuery } from '../../unit/queries'

const createPlacementResult = wrapResult(createPlacement)

export interface Props {
  childId: UUID
  reload: () => unknown
}

interface Form {
  type: PlacementType
  startDate: LocalDate
  endDate: LocalDate | null
  unit: { id: string; name: string; ghostUnit: boolean } | null
  placeGuarantee: boolean
}

function CreatePlacementModal({ childId, reload }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)
  const units = useQueryResult(unitsQuery({ includeClosed: true }))
  const [form, setForm] = useState<Form>({
    type: 'DAYCARE',
    unit: null,
    startDate: LocalDate.todayInSystemTz(),
    endDate: null,
    placeGuarantee: false
  })
  const [submitting, setSubmitting] = useState<boolean>(false)
  const retroactive = useMemo(
    () =>
      isChangeRetroactive(
        form.endDate && form.endDate.isEqualOrAfter(form.startDate)
          ? new FiniteDateRange(form.startDate, form.endDate)
          : null,
        null,
        false,
        LocalDate.todayInHelsinkiTz()
      ),
    [form]
  )
  const [confirmedRetroactive, setConfirmedRetroactive] = useState(false)

  const unitOptions = useMemo(() => {
    const activeUnits = form.startDate.isAfter(
      form.endDate ? form.endDate : LocalDate.todayInSystemTz()
    )
      ? units
      : units.map((list) =>
          list.filter((u) =>
            new DateRange(
              u.openingDate ?? LocalDate.of(1900, 1, 1),
              u.closingDate
            ).overlapsWith(new DateRange(form.startDate, form.endDate))
          )
        )
    return activeUnits
      .map((us) =>
        us
          .map(({ id, name, ghostUnit }) => ({ id, name, ghostUnit }))
          .sort((a, b) => (a.name < b.name ? -1 : 1))
      )
      .getOrElse([])
  }, [units, form.startDate, form.endDate])

  const errors = useMemo(() => {
    const errors = []
    if (!form.unit?.id) {
      errors.push(i18n.childInformation.placements.createPlacement.unitMissing)
    }

    if (form.unit?.ghostUnit) {
      errors.push(i18n.childInformation.placements.warning.ghostUnit)
    }

    if (form.endDate === null) {
      errors.push(i18n.validationError.endDateIsMandatoryField)
    } else {
      if (form.startDate.isAfter(form.endDate)) {
        errors.push(i18n.validationError.invertedDateRange)
      }
    }

    return errors
  }, [i18n, form])

  const submitForm = () => {
    if (!form.unit?.id) return

    setSubmitting(true)
    createPlacementResult({
      body: {
        childId,
        type: form.type,
        unitId: form.unit.id,
        startDate: form.startDate,
        endDate: form.endDate!,
        placeGuarantee: form.placeGuarantee
      }
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
      type="info"
      resolveAction={submitForm}
      resolveLabel={i18n.common.confirm}
      resolveDisabled={
        errors.length > 0 ||
        (retroactive && !confirmedRetroactive) ||
        submitting
      }
      rejectAction={clearUiMode}
      rejectLabel={i18n.common.cancel}
    >
      <FixedSpaceColumn>
        <section>
          <div className="bold">{i18n.childInformation.placements.type}</div>

          <Select
            items={placementTypes}
            selectedItem={form.type}
            onChange={(type) =>
              type
                ? setForm({
                    ...form,
                    type
                  })
                : undefined
            }
            getItemLabel={(type) => i18n.placement.type[type]}
          />
        </section>

        {form.type === 'TEMPORARY_DAYCARE' ||
        form.type === 'TEMPORARY_DAYCARE_PART_DAY' ? (
          <>
            <AlertBox
              thin
              message={
                i18n.childInformation.placements.createPlacement
                  .temporaryDaycareWarning
              }
            />
            <Gap size="xs" />
          </>
        ) : null}

        <section data-qa="unit-select">
          <div className="bold">
            {i18n.childInformation.placements.daycareUnit}
          </div>

          <Combobox
            items={unitOptions}
            selectedItem={form.unit}
            onChange={(unit) => setForm((prev) => ({ ...prev, unit }))}
            placeholder={i18n.common.select}
            getItemLabel={({ name }) => name}
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
            date={form.endDate || undefined}
            onChange={(endDate) => setForm({ ...form, endDate })}
            data-qa="create-placement-end-date"
            type="full-width"
          />
        </section>

        {featureFlags.placementGuarantee && (
          <section>
            <ExpandingInfo
              info={
                i18n.childInformation.placements.createPlacement.placeGuarantee
                  .info
              }
              width="auto"
            >
              <Checkbox
                label={
                  i18n.childInformation.placements.createPlacement
                    .placeGuarantee.title
                }
                checked={form.placeGuarantee}
                onChange={(checked) =>
                  setForm({ ...form, placeGuarantee: checked })
                }
                data-qa="create-placement-place-guarantee"
              />
            </ExpandingInfo>
          </section>
        )}

        {retroactive && (
          <RetroactiveConfirmation
            confirmed={confirmedRetroactive}
            setConfirmed={setConfirmedRetroactive}
          />
        )}

        {errors.map((error) => (
          <ValidationError key={error}>{error}</ValidationError>
        ))}
      </FixedSpaceColumn>
    </FormModal>
  )
}

const ValidationError = styled.div`
  font-size: 0.9em;
  color: ${colors.status.danger};
`

export default CreatePlacementModal
