// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import { faPen } from 'lib-icons'
import { Result } from 'lib-common/api'
import { UpdateStateFn } from '../../../lib-common/form-state'
import { PersonDetails } from '../../types/person'
import { useTranslation } from '../../state/i18n'
import { useContext, useEffect, useState } from 'react'
import Loader from 'lib-components/atoms/Loader'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import LabelValueList from '../../components/common/LabelValueList'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { patchPersonDetails } from '../../api/person'
import { UIContext, UiState } from '../../state/ui'
import AddSsnModal from '../../components/person-shared/person-details/AddSsnModal'
import { UserContext } from '../../state/user'
import styled from 'styled-components'
import { RequireRole, requireRole } from '../../utils/roles'
import LocalDate from 'lib-common/local-date'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'

const FlexContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  flex-direction: row-reverse;
  justify-content: space-between;
  align-items: baseline;
`

const PostalCodeAndOffice = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;

  #postal-code {
    width: 30%;
  }

  #post-office {
    width: 65%;
  }
`

interface Props {
  personResult: Result<PersonDetails>
  isChild: boolean
  onUpdateComplete?: (data: PersonDetails) => void
}

interface Form {
  firstName: string
  lastName: string
  dateOfBirth: LocalDate
  email: string
  phone: string
  backupPhone: string
  streetAddress: string
  postalCode: string
  postOffice: string
  invoiceRecipientName: string
  invoicingStreetAddress: string
  invoicingPostalCode: string
  invoicingPostOffice: string
  forceManualFeeDecisions: boolean
}

const RightAlignedRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
`

const PersonDetails = React.memo(function PersonDetails({
  personResult,
  isChild,
  onUpdateComplete
}: Props) {
  const { i18n } = useTranslation()
  const { roles } = useContext(UserContext)
  const { uiMode, toggleUiMode, clearUiMode } = useContext<UiState>(UIContext)
  const editing = uiMode === 'person-details-editing'
  const [form, setForm] = useState<Form>({
    firstName: '',
    lastName: '',
    dateOfBirth: LocalDate.today(),
    email: '',
    phone: '',
    backupPhone: '',
    streetAddress: '',
    postalCode: '',
    postOffice: '',
    invoiceRecipientName: '',
    invoicingStreetAddress: '',
    invoicingPostalCode: '',
    invoicingPostOffice: '',
    forceManualFeeDecisions: false
  })

  useEffect(() => {
    if (editing && personResult.isSuccess) {
      setForm({
        firstName: personResult.value.firstName || '',
        lastName: personResult.value.lastName || '',
        dateOfBirth: personResult.value.dateOfBirth,
        email: personResult.value.email || '',
        phone: personResult.value.phone || '',
        backupPhone: personResult.value.backupPhone || '',
        streetAddress: personResult.value.streetAddress || '',
        postalCode: personResult.value.postalCode || '',
        postOffice: personResult.value.postOffice || '',
        invoiceRecipientName: personResult.value.invoiceRecipientName ?? '',
        invoicingStreetAddress: personResult.value.invoicingStreetAddress ?? '',
        invoicingPostalCode: personResult.value.invoicingPostalCode ?? '',
        invoicingPostOffice: personResult.value.invoicingPostOffice ?? '',
        forceManualFeeDecisions:
          personResult.value.forceManualFeeDecisions ?? false
      })
    }
  }, [personResult, editing])

  // clear ui mode when dismounting component
  useEffect(() => {
    return clearUiMode
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  if (personResult.isLoading) {
    return <Loader />
  }
  if (personResult.isFailure) {
    return <div>{i18n.common.loadingFailed}</div>
  }

  const person = personResult.value
  const powerEditing = editing && person.socialSecurityNumber == null

  const updateForm: UpdateStateFn<Form> = (values) => {
    setForm({
      ...form,
      ...values
    })
  }

  const onSubmit = () => {
    void patchPersonDetails(person.id, form).then((res) => {
      if (res.isSuccess) {
        if (onUpdateComplete) onUpdateComplete(res.value)
        clearUiMode()
      }
    })
  }

  return (
    <>
      {uiMode === 'add-ssn-modal' && (
        <AddSsnModal personId={person.id} onUpdateComplete={onUpdateComplete} />
      )}
      <FlexContainer>
        {(!isChild || person.socialSecurityNumber === null) && (
          <RequireRole
            oneOf={[
              'ADMIN',
              'SERVICE_WORKER',
              'FINANCE_ADMIN',
              'UNIT_SUPERVISOR'
            ]}
          >
            <InlineButton
              icon={faPen}
              onClick={() => toggleUiMode('person-details-editing')}
              data-qa="edit-person-settings-button"
              text={i18n.common.edit}
            />
          </RequireRole>
        )}
        <div />
        <LabelValueList
          spacing="small"
          contents={[
            {
              label: i18n.common.form.lastName,
              dataQa: 'person-last-name',
              value: powerEditing ? (
                <InputField
                  value={form.lastName}
                  onChange={(value) => updateForm({ lastName: value })}
                  data-qa="input-last-name"
                />
              ) : (
                person.lastName
              )
            },
            {
              label: i18n.common.form.firstNames,
              dataQa: 'person-first-names',
              value: powerEditing ? (
                <InputField
                  value={form.firstName}
                  onChange={(value) => updateForm({ firstName: value })}
                  data-qa="input-first-name"
                />
              ) : (
                person.firstName
              )
            },
            {
              label: i18n.common.form.birthday,
              dataQa: 'person-birthday',
              value: powerEditing ? (
                <DatePickerDeprecated
                  type="full-width"
                  date={form.dateOfBirth}
                  onChange={(dateOfBirth) => updateForm({ dateOfBirth })}
                  maxDate={LocalDate.today()}
                  data-qa="input-birthday"
                />
              ) : (
                person.dateOfBirth.format()
              )
            },
            {
              label: i18n.childInformation.personDetails.language,
              dataQa: 'person-language',
              value:
                person.language === 'fi'
                  ? i18n.language.fi
                  : person.language === 'sv'
                  ? i18n.language.sv
                  : person.language
            },
            {
              label: i18n.common.form.socialSecurityNumber,
              dataQa: 'person-ssn',
              value:
                person.socialSecurityNumber ||
                (editing || !requireRole(roles, 'SERVICE_WORKER') ? (
                  i18n.personProfile.noSsn
                ) : (
                  <InlineButton
                    onClick={() => toggleUiMode('add-ssn-modal')}
                    text={i18n.personProfile.addSsn}
                  />
                ))
            },
            {
              label: i18n.common.form.address,
              dataQa: 'person-address',
              value: powerEditing ? (
                <>
                  <InputField
                    value={form.streetAddress}
                    placeholder={i18n.common.form.streetAddress}
                    onChange={(value) =>
                      updateForm({
                        streetAddress: value
                      })
                    }
                  />
                  <PostalCodeAndOffice>
                    <InputField
                      id="postal-code"
                      value={form.postalCode}
                      placeholder={i18n.common.form.postalCode}
                      onChange={(value) => updateForm({ postalCode: value })}
                    />
                    <InputField
                      id="post-office"
                      value={form.postOffice}
                      placeholder={i18n.common.form.postOffice}
                      onChange={(value) => updateForm({ postOffice: value })}
                    />
                  </PostalCodeAndOffice>
                </>
              ) : (
                <span data-qa="person-details-street-address">
                  {person.restrictedDetailsEnabled
                    ? i18n.common.form.addressRestricted
                    : `${person.streetAddress ?? ''}, ${
                        person.postalCode ?? ''
                      } ${person.postOffice ?? ''}`}
                </span>
              )
            },
            ...(!isChild && requireRole(roles, 'FINANCE_ADMIN')
              ? [
                  {
                    label: i18n.common.form.invoicingAddress,
                    value:
                      powerEditing || editing ? (
                        <>
                          <InputField
                            value={form.invoiceRecipientName}
                            placeholder={i18n.common.form.invoiceRecipient}
                            onChange={(value) =>
                              updateForm({
                                invoiceRecipientName: value
                              })
                            }
                          />

                          <InputField
                            value={form.invoicingStreetAddress}
                            placeholder={i18n.common.form.streetAddress}
                            onChange={(value) =>
                              updateForm({
                                invoicingStreetAddress: value
                              })
                            }
                          />
                          <PostalCodeAndOffice>
                            <InputField
                              id="postal-code"
                              value={form.invoicingPostalCode}
                              placeholder={i18n.common.form.postalCode}
                              onChange={(value) =>
                                updateForm({
                                  invoicingPostalCode: value
                                })
                              }
                            />
                            <InputField
                              id="post-office"
                              value={form.invoicingPostOffice}
                              placeholder={i18n.common.form.postOffice}
                              onChange={(value) =>
                                updateForm({
                                  invoicingPostOffice: value
                                })
                              }
                            />
                          </PostalCodeAndOffice>
                        </>
                      ) : person.invoicingStreetAddress &&
                        person.invoicingPostalCode &&
                        person.invoicingPostOffice ? (
                        <>
                          {person.invoiceRecipientName ? (
                            <div>{person.invoiceRecipientName}</div>
                          ) : null}
                          <div>
                            {`${person.invoicingStreetAddress}, ${person.invoicingPostalCode} ${person.invoicingPostOffice}`}
                          </div>
                        </>
                      ) : (
                        ''
                      )
                  },
                  {
                    label: i18n.personProfile.forceManualFeeDecisionsLabel,
                    value: editing ? (
                      <FixedSpaceColumn data-qa="force-manual-fee-decisions">
                        <Radio
                          label={
                            i18n.personProfile.forceManualFeeDecisionsChecked
                          }
                          checked={form.forceManualFeeDecisions === true}
                          onChange={() =>
                            updateForm({
                              forceManualFeeDecisions: true
                            })
                          }
                          data-qa={`force-manual-fee-decisions-true`}
                        />
                        <Radio
                          label={
                            i18n.personProfile.forceManualFeeDecisionsUnchecked
                          }
                          checked={form.forceManualFeeDecisions === false}
                          onChange={() =>
                            updateForm({
                              forceManualFeeDecisions: false
                            })
                          }
                          data-qa={`force-manual-fee-decisions-false`}
                        />
                      </FixedSpaceColumn>
                    ) : person.forceManualFeeDecisions ? (
                      i18n.personProfile.forceManualFeeDecisionsChecked
                    ) : (
                      i18n.personProfile.forceManualFeeDecisionsUnchecked
                    )
                  }
                ]
              : []),
            ...(!isChild
              ? [
                  {
                    label: i18n.common.form.email,
                    value: editing ? (
                      <InputField
                        value={form.email}
                        onChange={(value) => updateForm({ email: value })}
                      />
                    ) : (
                      person.email
                    )
                  },
                  {
                    label: i18n.common.form.phone,
                    value: editing ? (
                      <InputField
                        value={form.phone}
                        onChange={(value) => updateForm({ phone: value })}
                      />
                    ) : (
                      person.phone
                    )
                  },
                  {
                    label: i18n.common.form.backupPhone,
                    value: editing ? (
                      <InputField
                        value={form.backupPhone}
                        onChange={(value) => updateForm({ backupPhone: value })}
                      />
                    ) : (
                      person.backupPhone
                    )
                  }
                ]
              : [])
          ]}
        />
      </FlexContainer>
      {editing && (
        <RightAlignedRow>
          <FixedSpaceRow>
            <Button onClick={() => clearUiMode()} text={i18n.common.cancel} />
            <Button
              primary
              disabled={false}
              onClick={() => onSubmit()}
              data-qa="confirm-edited-person-button"
              text={i18n.common.confirm}
            />
          </FixedSpaceRow>
        </RightAlignedRow>
      )}
    </>
  )
})

export default PersonDetails
