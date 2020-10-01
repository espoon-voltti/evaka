// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState, useContext, useEffect } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import {
  allPropertiesTrue,
  isDateRangeValid
} from '~utils/validation/validations'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faQuestionCircle } from '@evaka/icons'
import FormModal from '~components/common/FormModal'
import { Section } from '~components/shared/alpha'
import { Partnership } from '~types/fridge'
import { UUID } from '~types'
import { isFailure, Result } from '~api'
import { faPen, faUser } from '@evaka/icons'
import { formatName } from '~utils'
import PersonSearch from '~components/common/PersonSearch'
import { DatePicker, DatePickerClearable } from '~components/common/DatePicker'
import { addPartnership, updatePartnership } from '~api/partnerships'
import { PersonDetails } from '~types/person'

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

interface FormValidationResult {
  valid: boolean
  fields: {
    dateRange: boolean
    partner: boolean
  }
}

function FridgePartnerModal({ partnership, onSuccess, headPersonId }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode, setErrorMessage } = useContext(UIContext)

  const initialForm: FridgePartnerForm = {
    partner:
      partnership &&
      partnership.partners.find((partner) => partner.id !== headPersonId),
    startDate: partnership ? partnership.startDate : LocalDate.today(),
    endDate: partnership ? partnership.endDate : null
  }
  const [form, setForm] = useState(initialForm)

  const [validationResult, setValidationResult] = useState<
    FormValidationResult
  >()
  const [errorStatusCode, setErrorStatusCode] = useState<number>()

  useEffect(() => {
    const fieldsValid = {
      dateRange: isDateRangeValid(form.startDate, form.endDate),
      partner: !!form.partner
    }
    setValidationResult({
      valid: allPropertiesTrue(fieldsValid),
      fields: fieldsValid
    })
  }, [form])

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
      if (isFailure(res)) {
        setErrorStatusCode(res.error.statusCode)

        if (res.error.statusCode !== 409) {
          clearUiMode()
          setErrorMessage({
            type: 'error',
            title: partnership
              ? i18n.personProfile.fridgePartner.error.edit.title
              : i18n.personProfile.fridgePartner.error.add.title,
            text: i18n.common.tryAgain
          })
        }
      } else {
        clearUiMode()
        onSuccess()
      }
    })
  }

  const assignFridgePartnerForm = (values: Partial<FridgePartnerForm>) => {
    const mergedFridgePartner = { ...form, ...values }
    setForm(mergedFridgePartner)
  }

  return (
    <>
      {validationResult && (
        <FormModal
          title={
            partnership
              ? i18n.personProfile.fridgePartner.editPartner
              : i18n.personProfile.fridgePartner.newPartner
          }
          icon={partnership ? faPen : faUser}
          iconColour={'blue'}
          resolveLabel={i18n.common.confirm}
          rejectLabel={i18n.common.cancel}
          reject={() => clearUiMode()}
          resolveDisabled={!validationResult.valid}
          resolve={() => onSubmit()}
        >
          {errorStatusCode === 409 && (
            <Section className="error">
              {i18n.personProfile.fridgePartner.error.conflict}
            </Section>
          )}
          <Section>
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
          </Section>
          <Section>
            <div className="bold">{i18n.common.form.startDate}</div>
            <DatePicker
              date={form.startDate}
              onChange={(startDate) => assignFridgePartnerForm({ startDate })}
              type="full-width"
            />
          </Section>
          <Section>
            <div className="bold">{i18n.common.form.endDate}</div>
            <DatePickerClearable
              date={form.endDate}
              onChange={(endDate) => assignFridgePartnerForm({ endDate })}
              onCleared={() => assignFridgePartnerForm({ endDate: null })}
              type="full-width"
            />
            {!validationResult.fields.dateRange && (
              <div className="error">
                {i18n.validationError.dateRange}{' '}
                <FontAwesomeIcon
                  size="lg"
                  icon={faQuestionCircle}
                  title={i18n.validationError.invertedDateRange}
                />
              </div>
            )}
          </Section>
        </FormModal>
      )}
    </>
  )
}

export default FridgePartnerModal
