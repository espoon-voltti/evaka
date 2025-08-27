// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { ChildId, DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import Select from 'lib-components/atoms/dropdowns/Select'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { featureFlags, placementTypes } from 'lib-customizations/employee'
import { faMapMarkerAlt } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import RetroactiveConfirmation, {
  isChangeRetroactive
} from '../../common/RetroactiveConfirmation'
import {daycaresQuery, getPreschoolTermsQuery} from '../../unit/queries'
import { createPlacementMutation } from '../queries'
import {getPreschoolTerms} from "../../../generated/api-clients/daycare";

export interface Props {
  childId: ChildId
}

interface Form {
  type: PlacementType
  startDate: LocalDate | null
  endDate: LocalDate | null
  unit: { id: DaycareId; name: string; ghostUnit: boolean } | null
  placeGuarantee: boolean
}

function CreatePlacementModal({ childId }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)
  const units = useQueryResult(daycaresQuery({ includeClosed: true }))
  const prechoolTerms = useQueryResult(getPreschoolTermsQuery())

  const [form, setForm] = useState<Form>({
    type: 'DAYCARE',
    unit: null,
    startDate: LocalDate.todayInSystemTz(),
    endDate: null,
    placeGuarantee: false
  })

  const retroactive = useMemo(
    () =>
      isChangeRetroactive(
        form.startDate &&
          form.endDate &&
          form.endDate.isEqualOrAfter(form.startDate)
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
    const { startDate, endDate } = form
    const activeUnits =
      !startDate || !endDate || startDate.isAfter(endDate)
        ? units
        : units.map((list) =>
            list.filter((u) =>
              new DateRange(
                u.openingDate ?? LocalDate.of(1900, 1, 1),
                u.closingDate
              ).overlapsWith(new DateRange(startDate, endDate))
            )
          )
    return activeUnits
      .map((us) =>
        us
          .map(({ id, name, ghostUnit }) => ({ id, name, ghostUnit }))
          .sort((a, b) => (a.name < b.name ? -1 : 1))
      )
      .getOrElse([])
  }, [units, form])

  const errors = useMemo(() => {
    const errors: string[] = []
    if (!form.unit?.id) {
      errors.push(i18n.childInformation.placements.createPlacement.unitMissing)
    }

    if (form.unit?.ghostUnit) {
      errors.push(i18n.childInformation.placements.warning.ghostUnit)
    }

    if (form.startDate === null) {
      errors.push(
        i18n.childInformation.placements.createPlacement.startDateMissing
      )
    }
    if (form.endDate === null) {
      errors.push(i18n.validationError.endDateIsMandatoryField)
    }
    if (
      form.startDate &&
      form.endDate &&
      form.startDate.isAfter(form.endDate)
    ) {
      errors.push(i18n.validationError.invertedDateRange)
    }

    return errors
  }, [i18n, form])

  const { mutateAsync: createPlacement, isPending: submitting } =
    useMutationResult(createPlacementMutation)

  const submitForm = () => {
    if (!form.unit?.id || !form.startDate || !form.endDate) return

    void createPlacement({
      body: {
        childId,
        type: form.type,
        unitId: form.unit.id,
        startDate: form.startDate,
        endDate: form.endDate,
        placeGuarantee: form.placeGuarantee
      }
    }).then((res) => {
      if (res.isSuccess) {
        clearUiMode()
      }
    })
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

          <DatePicker
            date={form.startDate}
            onChange={(startDate) => setForm({ ...form, startDate })}
            data-qa="create-placement-start-date"
            locale="fi"
          />
        </section>

        <section>
          <div className="bold">{i18n.common.form.endDate}</div>

          <DatePicker
            date={form.endDate}
            onChange={(endDate) => setForm({ ...form, endDate })}
            data-qa="create-placement-end-date"
            locale="fi"
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
