// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext, useMemo } from 'react'

import type { Result } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import type { UpdateStateFn } from 'lib-common/form-state'
import type {
  ParentshipDetailed,
  PersonSummary
} from 'lib-common/generated/api-types/pis'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useMutationResult } from 'lib-common/query'
import { PersonName } from 'lib-components/molecules/PersonNames'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'
import { faChild } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { DbPersonSearch as PersonSearch } from '../../common/PersonSearch'
import RetroactiveConfirmation, {
  isChangeRetroactive
} from '../../common/RetroactiveConfirmation'
import { createParentshipMutation, updateParentshipMutation } from '../queries'
import { PersonContext } from '../state'

interface Props {
  headPersonId: PersonId
  parentship?: ParentshipDetailed
}

export interface FridgeChildForm {
  child?: PersonSummary
  startDate: LocalDate | null
  endDate: LocalDate | null
}

function FridgeChildModal({ headPersonId, parentship }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)
  const { person } = useContext(PersonContext)
  const initialForm = useMemo(
    () => ({
      child: parentship && parentship.child,
      startDate: parentship
        ? parentship.startDate
        : LocalDate.todayInSystemTz(),
      endDate: parentship ? parentship.endDate : LocalDate.todayInSystemTz()
    }),
    [parentship]
  )
  const [form, setForm] = useState<FridgeChildForm>(initialForm)
  const retroactive = useMemo(
    () =>
      isChangeRetroactive(
        form.startDate &&
          form.endDate &&
          form.endDate.isEqualOrAfter(form.startDate)
          ? new DateRange(form.startDate, form.endDate)
          : null,
        new DateRange(initialForm.startDate, initialForm.endDate),
        false, // only range can be edited
        LocalDate.todayInHelsinkiTz()
      ),
    [form, initialForm]
  )
  const [confirmedRetroactive, setConfirmedRetroactive] = useState(false)

  const validationErrors = useMemo(() => {
    const errors = []

    if (!form.startDate || !form.endDate) {
      return [i18n.validationError.mandatoryField]
    }

    if (form.startDate.isAfter(form.endDate)) {
      errors.push(i18n.validationError.invertedDateRange)
    }

    if (
      form.child?.dateOfDeath &&
      form.endDate.isAfter(form.child.dateOfDeath)
    ) {
      errors.push(
        `${
          i18n.personProfile.fridgeChild.validation.deadChild
        } (${form.child.dateOfDeath.format()})`
      )
    }

    const headDateOfDeath = person
      .map((p) => p.dateOfDeath)
      .getOrElse(undefined)

    if (headDateOfDeath && form.endDate.isAfter(headDateOfDeath)) {
      errors.push(
        `${
          i18n.personProfile.fridgeChild.validation.deadAdult
        } (${headDateOfDeath.format()})`
      )
    }

    return errors
  }, [i18n, person, form])

  const [errorStatusCode, setErrorStatusCode] = useState<number>()

  const { mutateAsync: updateParentship } = useMutationResult(
    updateParentshipMutation
  )
  const { mutateAsync: createParentship } = useMutationResult(
    createParentshipMutation
  )

  const childFormActions = () => {
    if (!form.child || !form.startDate || !form.endDate) return

    const apiCall = parentship
      ? updateParentship({
          headOfChildId: headPersonId,
          id: parentship.id,
          body: { startDate: form.startDate, endDate: form.endDate }
        })
      : createParentship({
          body: {
            headOfChildId: headPersonId,
            childId: form.child.id,
            startDate: form.startDate,
            endDate: form.endDate
          }
        })

    void apiCall.then((res: Result<void>) => {
      if (res.isFailure) {
        if (res.statusCode === 409) {
          setErrorStatusCode(res.statusCode)
        } else {
          clearUiMode()
          setErrorMessage({
            type: 'error',
            title: parentship
              ? i18n.personProfile.fridgeChild.error.edit.title
              : i18n.personProfile.fridgeChild.error.add.title,
            text: i18n.common.tryAgain,
            resolveLabel: i18n.common.ok
          })
        }
      } else {
        clearUiMode()
      }
    })
  }

  const assignFridgeChildForm: UpdateStateFn<FridgeChildForm> = (value) => {
    const mergedFridgeChild = { ...form, ...value }
    setForm(mergedFridgeChild)
  }

  return (
    <FormModal
      title={
        parentship
          ? i18n.personProfile.fridgeChild.editChild
          : i18n.personProfile.fridgeChild.newChild
      }
      icon={faChild}
      type="info"
      resolveAction={childFormActions}
      resolveLabel={i18n.common.confirm}
      resolveDisabled={
        !form.child ||
        validationErrors.length > 0 ||
        (retroactive && !confirmedRetroactive)
      }
      rejectAction={clearUiMode}
      rejectLabel={i18n.common.cancel}
      data-qa="fridge-child-modal"
    >
      {errorStatusCode === 409 && (
        <section className="error">
          {i18n.personProfile.fridgeChild.error.conflict}
        </section>
      )}
      <section>
        {parentship ? (
          <div>
            <PersonName person={parentship.child} format="Last First" />
          </div>
        ) : (
          <>
            <div className="bold">
              {i18n.personProfile.fridgeChild.searchTitle}
            </div>
            <PersonSearch
              onResult={(person) => {
                let endDate = form.endDate
                if (person) {
                  endDate = person.dateOfBirth.addYears(18).subDays(1)
                }
                assignFridgeChildForm({ child: person, endDate })
              }}
              ageLessThan={18}
              excludePeople={[headPersonId]}
              data-qa="fridge-child-person-search"
            />
          </>
        )}
      </section>
      <section>
        <div className="bold">{i18n.common.form.startDate}</div>
        <DatePicker
          date={form.startDate}
          onChange={(startDate) => assignFridgeChildForm({ startDate })}
          minDate={form.child?.dateOfBirth}
          maxDate={form.child?.dateOfBirth.addYears(18).subDays(1)}
          locale="fi"
          data-qa="fridge-child-start-date"
        />
      </section>
      <section>
        <div className="bold">{i18n.common.form.endDate}</div>
        <DatePicker
          date={form.endDate}
          onChange={(endDate) => assignFridgeChildForm({ endDate })}
          minDate={form.child?.dateOfBirth}
          maxDate={form.child?.dateOfBirth.addYears(18).subDays(1)}
          locale="fi"
          data-qa="fridge-child-end-date"
        />
      </section>
      {retroactive && (
        <>
          <Gap size="xs" />
          <RetroactiveConfirmation
            confirmed={confirmedRetroactive}
            setConfirmed={setConfirmedRetroactive}
          />
        </>
      )}
      {validationErrors.map((error) => (
        <div className="error" key={error}>
          {error}
        </div>
      ))}
    </FormModal>
  )
}

export default FridgeChildModal
