// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext, useEffect, useMemo } from 'react'

import { Loading, Result, wrapResult } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import { UpdateStateFn } from 'lib-common/form-state'
import { Partnership, PersonSummary } from 'lib-common/generated/api-types/pis'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import {
  DatePickerDeprecated,
  DatePickerClearableDeprecated
} from 'lib-components/molecules/DatePickerDeprecated'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Gap } from 'lib-components/white-space'
import { faPen, faUser } from 'lib-icons'

import { DbPersonSearch as PersonSearch } from '../../../components/common/PersonSearch'
import {
  createPartnership,
  getPersonIdentity,
  updatePartnership
} from '../../../generated/api-clients/pis'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { formatName } from '../../../utils'
import RetroactiveConfirmation, {
  isChangeRetroactive
} from '../../common/RetroactiveConfirmation'

const getPersonIdentityResult = wrapResult(getPersonIdentity)
const createPartnershipResult = wrapResult(createPartnership)
const updatePartnershipResult = wrapResult(updatePartnership)

interface Props {
  headPersonId: UUID
  onSuccess: () => void
  partnership?: Partnership
}

export interface FridgePartnerForm {
  partner?: PersonSummary
  startDate: LocalDate
  endDate: LocalDate | null
}

function FridgePartnerModal({ partnership, onSuccess, headPersonId }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)
  const [personData, setPersonData] = useState<Result<PersonSummary>>(
    Loading.of()
  )
  const initialForm: FridgePartnerForm = useMemo(
    () => ({
      partner:
        partnership &&
        partnership.partners.find((partner) => partner.id !== headPersonId),
      startDate: partnership
        ? partnership.startDate
        : LocalDate.todayInSystemTz(),
      endDate: partnership ? partnership.endDate : null
    }),
    [partnership, headPersonId]
  )
  const [form, setForm] = useState(initialForm)
  const retroactive = useMemo(
    () =>
      isChangeRetroactive(
        form.endDate === null || form.endDate.isEqualOrAfter(form.startDate)
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

    if (form.endDate && form.startDate.isAfter(form.endDate)) {
      errors.push(i18n.validationError.invertedDateRange)
    }

    if (
      form.partner?.dateOfDeath &&
      (!form.endDate || form.endDate.isAfter(form.partner.dateOfDeath))
    ) {
      errors.push(
        `${
          i18n.personProfile.fridgePartner.validation.deadPartner
        } (${form.partner.dateOfDeath.format()})`
      )
    }

    const headDateOfDeath = personData
      .map((p) => p.dateOfDeath)
      .getOrElse(undefined)

    if (
      headDateOfDeath &&
      (!form.endDate || form.endDate.isAfter(headDateOfDeath))
    ) {
      errors.push(
        `${
          i18n.personProfile.fridgePartner.validation.deadPerson
        } (${headDateOfDeath.format()})`
      )
    }

    return errors
  }, [i18n, personData, form])

  const [errorStatusCode, setErrorStatusCode] = useState<number>()

  useEffect(() => {
    void getPersonIdentityResult({ personId: headPersonId }).then(setPersonData)
  }, [headPersonId, setPersonData])

  const onSubmit = () => {
    if (!form.partner) return

    const apiCall = partnership
      ? updatePartnershipResult({
          partnershipId: partnership.id,
          body: { startDate: form.startDate, endDate: form.endDate }
        })
      : createPartnershipResult({
          body: {
            person1Id: headPersonId,
            person2Id: form.partner.id,
            startDate: form.startDate,
            endDate: form.endDate
          }
        })

    void apiCall.then((res) => {
      if (res.isFailure) {
        setErrorStatusCode(res.statusCode)

        if (res.statusCode !== 409) {
          clearUiMode()
          setErrorMessage({
            type: 'error',
            title: partnership
              ? i18n.personProfile.fridgePartner.error.edit.title
              : i18n.personProfile.fridgePartner.error.add.title,
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

  const assignFridgePartnerForm: UpdateStateFn<FridgePartnerForm> = (
    values
  ) => {
    const mergedFridgePartner = { ...form, ...values }
    setForm(mergedFridgePartner)
  }

  return (
    <FormModal
      title={
        partnership
          ? i18n.personProfile.fridgePartner.editPartner
          : i18n.personProfile.fridgePartner.newPartner
      }
      icon={partnership ? faPen : faUser}
      type="info"
      resolveAction={onSubmit}
      resolveLabel={i18n.common.confirm}
      resolveDisabled={
        !form.partner ||
        validationErrors.length > 0 ||
        (retroactive && !confirmedRetroactive)
      }
      rejectAction={clearUiMode}
      rejectLabel={i18n.common.cancel}
      data-qa="fridge-partner-modal"
    >
      {errorStatusCode === 409 && (
        <section className="error">
          {i18n.personProfile.fridgePartner.error.conflict}
        </section>
      )}
      <section>
        {partnership ? (
          <div>
            {form.partner
              ? `${formatName(
                  form.partner.firstName,
                  form.partner.lastName,
                  i18n,
                  true
                )} (${form.partner.socialSecurityNumber ?? ''})`
              : ''}
          </div>
        ) : (
          <>
            <div className="bold">
              {i18n.personProfile.fridgePartner.searchTitle}
            </div>
            <PersonSearch
              onResult={(person) =>
                assignFridgePartnerForm({ partner: person })
              }
              onlyAdults
              data-qa="fridge-partner-person-search"
            />
          </>
        )}
      </section>
      <section>
        <div className="bold">{i18n.common.form.startDate}</div>
        <DatePickerDeprecated
          date={form.startDate}
          onChange={(startDate) => assignFridgePartnerForm({ startDate })}
          type="full-width"
          data-qa="fridge-partner-start-date"
        />
      </section>
      <section>
        <div className="bold">{i18n.common.form.endDate}</div>
        <DatePickerClearableDeprecated
          date={form.endDate}
          onChange={(endDate) => assignFridgePartnerForm({ endDate })}
          onCleared={() => assignFridgePartnerForm({ endDate: null })}
          type="full-width"
        />
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
      </section>
    </FormModal>
  )
}

export default FridgePartnerModal
