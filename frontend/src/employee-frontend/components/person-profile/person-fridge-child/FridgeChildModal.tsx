// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext, useEffect, useMemo } from 'react'

import { Loading, Result, wrapResult } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { UpdateStateFn } from 'lib-common/form-state'
import { Parentship, PersonSummary } from 'lib-common/generated/api-types/pis'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'
import { faChild } from 'lib-icons'

import { getPerson } from '../../../api/person'
import { DbPersonSearch as PersonSearch } from '../../../components/common/PersonSearch'
import {
  createParentship,
  updateParentship
} from '../../../generated/api-clients/pis'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { formatName } from '../../../utils'
import RetroactiveConfirmation, {
  isChangeRetroactive
} from '../../common/RetroactiveConfirmation'

const createParentshipResult = wrapResult(createParentship)
const updateParentshipResult = wrapResult(updateParentship)

interface Props {
  headPersonId: UUID
  onSuccess: () => void
  parentship?: Parentship
}

export interface FridgeChildForm {
  child?: PersonSummary
  startDate: LocalDate
  endDate: LocalDate
}

function FridgeChildModal({ headPersonId, onSuccess, parentship }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)
  const [personData, setPersonData] = useState<Result<PersonSummary>>(
    Loading.of()
  )
  const initialForm: FridgeChildForm = useMemo(
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

    const headDateOfDeath = personData
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
  }, [i18n, personData, form])

  const [errorStatusCode, setErrorStatusCode] = useState<number>()

  useEffect(() => {
    void getPerson(headPersonId).then(setPersonData)
  }, [headPersonId, setPersonData])

  const childFormActions = () => {
    if (!form.child) return

    const apiCall = parentship
      ? updateParentshipResult({
          id: parentship.id,
          body: { startDate: form.startDate, endDate: form.endDate }
        })
      : createParentshipResult({
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
        onSuccess()
      }
    })
  }

  const assignFridgeChildForm: UpdateStateFn<FridgeChildForm> = (value) => {
    const mergedFridgeChild = { ...form, ...value }
    setForm(mergedFridgeChild)
  }

  return (
    <>
      {validationErrors && (
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
                {formatName(
                  parentship.child.firstName,
                  parentship.child.lastName,
                  i18n,
                  true
                )}
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
                  onlyChildren
                  data-qa="fridge-child-person-search"
                />
              </>
            )}
          </section>
          <section>
            <div className="bold">{i18n.common.form.startDate}</div>
            <DatePickerDeprecated
              date={form.startDate}
              onChange={(startDate) => assignFridgeChildForm({ startDate })}
              minDate={form.child?.dateOfBirth}
              maxDate={form.child?.dateOfBirth.addYears(18).subDays(1)}
              type="full-width"
              data-qa="fridge-child-start-date"
            />
          </section>
          <section>
            <div className="bold">{i18n.common.form.endDate}</div>
            <DatePickerDeprecated
              date={form.endDate}
              onChange={(endDate) => assignFridgeChildForm({ endDate })}
              minDate={form.child?.dateOfBirth}
              maxDate={form.child?.dateOfBirth.addYears(18).subDays(1)}
              type="full-width"
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
      )}{' '}
    </>
  )
}

export default FridgeChildModal
