// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext, useEffect, useMemo } from 'react'
import LocalDate from 'lib-common/local-date'
import { UpdateStateFn } from '../../../../lib-common/form-state'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Partnership } from '../../../types/fridge'
import { UUID } from '../../../types'
import { Loading, Result } from 'lib-common/api'
import { faPen, faUser } from 'lib-icons'
import { formatName } from '../../../utils'
import { DbPersonSearch as PersonSearch } from '../../../components/common/PersonSearch'
import {
  DatePickerDeprecated,
  DatePickerClearableDeprecated
} from 'lib-components/molecules/DatePickerDeprecated'
import { addPartnership, updatePartnership } from '../../../api/partnerships'
import { getPersonDetails } from '../../../api/person'
import { PersonDetails } from '../../../types/person'

interface Props {
  headPersonId: UUID
  onSuccess: () => void
  partnership?: Partnership
}

export interface FridgePartnerForm {
  partner?: PersonDetails
  startDate: LocalDate
  endDate: LocalDate | null
}

function FridgePartnerModal({ partnership, onSuccess, headPersonId }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)
  const [personData, setPersonData] = useState<Result<PersonDetails>>(
    Loading.of()
  )
  const initialForm: FridgePartnerForm = {
    partner:
      partnership &&
      partnership.partners.find((partner) => partner.id !== headPersonId),
    startDate: partnership ? partnership.startDate : LocalDate.today(),
    endDate: partnership ? partnership.endDate : null
  }
  const [form, setForm] = useState(initialForm)

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
    void getPersonDetails(headPersonId).then(setPersonData)
  }, [headPersonId, setPersonData])

  const onSubmit = () => {
    if (!form.partner) return

    const apiCall = partnership
      ? updatePartnership(partnership.id, form.startDate, form.endDate)
      : addPartnership(
          headPersonId,
          form.partner.id,
          form.startDate,
          form.endDate
        )

    void apiCall.then((res: Result<Partnership>) => {
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
      iconColour={'blue'}
      resolve={{
        action: onSubmit,
        label: i18n.common.confirm,
        disabled: !form.partner || validationErrors.length > 0
      }}
      reject={{
        action: clearUiMode,
        label: i18n.common.cancel
      }}
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
                  i18n
                )} (${form.partner.socialSecurityNumber ?? ''})`
              : ''}
          </div>
        ) : (
          <>
            <div className="bold">
              {i18n.personProfile.fridgePartner.searchTitle}
            </div>
            <PersonSearch
              onResult={(person: PersonDetails | undefined) =>
                assignFridgePartnerForm({ partner: person })
              }
              onlyAdults
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
