// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext, useEffect, useState } from 'react'
import { faPen } from 'lib-icons'
import { UpdateStateFn } from 'lib-common/form-state'
import { PersonDetails } from '../../types/person'
import { useTranslation } from '../../state/i18n'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import LabelValueList from '../../components/common/LabelValueList'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import { patchPersonDetails, updateSsnAddingDisabled } from '../../api/person'
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
import Checkbox from 'lib-components/atoms/form/Checkbox'
import {
  InfoButton,
  ExpandingInfoBox
} from 'lib-components/molecules/ExpandingInfo'
import { Loading, Result, Success } from 'lib-common/api'
import { Action } from 'lib-common/generated/action'

const PostalCodeAndOffice = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: flex-start;

  #postal-code {
    width: 40%;
  }

  #post-office {
    width: 100%;
  }
`

interface Props {
  person: PersonDetails
  isChild: boolean
  onUpdateComplete: (data: PersonDetails) => void
  permittedActions: Set<Action.Child | Action.Person>
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
  ophPersonOid: string
}

const RightAlignedRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
`

const PersonDetails = React.memo(function PersonDetails({
  person,
  isChild,
  onUpdateComplete,
  permittedActions
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
    forceManualFeeDecisions: false,
    ophPersonOid: ''
  })
  const [ssnDisableRequest, setSsnDisableRequest] = useState<Result<void>>(
    Success.of()
  )
  const disableSsnAdding = useCallback(
    (disabled: boolean) => {
      setSsnDisableRequest(Loading.of())
      void updateSsnAddingDisabled(person.id, disabled).then((result) => {
        setSsnDisableRequest(result)

        if (result.isSuccess) {
          onUpdateComplete({
            ...person,
            ssnAddingDisabled: disabled
          })
        }
      })
    },
    [person, onUpdateComplete, setSsnDisableRequest]
  )
  const [showSsnAddingDisabledInfo, setShowSsnAddingDisabledInfo] =
    useState(false)
  const toggleShowSsnAddingDisabledInfo = useCallback(
    () => setShowSsnAddingDisabledInfo((state) => !state),
    [setShowSsnAddingDisabledInfo]
  )

  useEffect(() => {
    if (editing) {
      setForm({
        firstName: person.firstName || '',
        lastName: person.lastName || '',
        dateOfBirth: person.dateOfBirth,
        email: person.email || '',
        phone: person.phone || '',
        backupPhone: person.backupPhone || '',
        streetAddress: person.streetAddress || '',
        postalCode: person.postalCode || '',
        postOffice: person.postOffice || '',
        invoiceRecipientName: person.invoiceRecipientName ?? '',
        invoicingStreetAddress: person.invoicingStreetAddress ?? '',
        invoicingPostalCode: person.invoicingPostalCode ?? '',
        invoicingPostOffice: person.invoicingPostOffice ?? '',
        forceManualFeeDecisions: person.forceManualFeeDecisions ?? false,
        ophPersonOid: person.ophPersonOid ?? ''
      })
    }
  }, [person, editing])

  // clear ui mode when dismounting component
  useEffect(() => {
    return clearUiMode
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

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
      {(!isChild || person.socialSecurityNumber === null) && (
        <RightAlignedRow>
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
        </RightAlignedRow>
      )}
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.common.form.lastName,
            dataQa: 'person-last-name',
            valueWidth: '100%',
            value: powerEditing ? (
              <InputField
                width={'L'}
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
            valueWidth: '100%',
            value: powerEditing ? (
              <InputField
                width={'L'}
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
            value: person.socialSecurityNumber ?? (
              <FixedSpaceColumn spacing="xs">
                {editing || !permittedActions.has('ADD_SSN') ? (
                  <span data-qa="no-ssn">{i18n.personProfile.noSsn}</span>
                ) : (
                  <InlineButton
                    onClick={() => toggleUiMode('add-ssn-modal')}
                    text={i18n.personProfile.addSsn}
                    disabled={!permittedActions.has('ADD_SSN')}
                    data-qa="add-ssn-button"
                  />
                )}
                {permittedActions.has('DISABLE_SSN') ? (
                  <FixedSpaceRow spacing="s" alignItems="center">
                    <Checkbox
                      checked={person.ssnAddingDisabled}
                      label={i18n.personProfile.ssnAddingDisabledCheckbox}
                      disabled={ssnDisableRequest.isLoading}
                      onChange={disableSsnAdding}
                      data-qa="disable-ssn-adding"
                    />
                    <InfoButton
                      onClick={toggleShowSsnAddingDisabledInfo}
                      aria-label={i18n.common.openExpandingInfo}
                    />
                  </FixedSpaceRow>
                ) : (
                  person.ssnAddingDisabled && (
                    <FixedSpaceRow spacing="s" alignItems="center">
                      <span>
                        {i18n.personProfile.ssnAddingDisabledCheckbox}
                      </span>
                      <InfoButton
                        onClick={toggleShowSsnAddingDisabledInfo}
                        aria-label={i18n.common.openExpandingInfo}
                      />
                    </FixedSpaceRow>
                  )
                )}
              </FixedSpaceColumn>
            )
          },
          ...(showSsnAddingDisabledInfo
            ? [
                {
                  value: (
                    <ExpandingInfoBox
                      info={i18n.personProfile.ssnAddingDisabledInfo}
                      close={toggleShowSsnAddingDisabledInfo}
                    />
                  ),
                  onlyValue: true
                }
              ]
            : []),
          {
            label: i18n.common.form.address,
            dataQa: 'person-address',
            valueWidth: '100%',
            value: powerEditing ? (
              <>
                <InputField
                  width={'L'}
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
          ...(requireRole(roles, 'ADMIN', 'UNIT_SUPERVISOR', 'DIRECTOR')
            ? [
                {
                  label: i18n.common.form.ophPersonOid,
                  dataQa: 'person-oph-person-oid',
                  valueWidth: '100%',
                  value:
                    powerEditing || editing ? (
                      <>
                        <InputField
                          width={'L'}
                          value={form.ophPersonOid}
                          placeholder={i18n.common.form.ophPersonOid}
                          onChange={(value) =>
                            updateForm({
                              ophPersonOid: value
                            })
                          }
                        />
                      </>
                    ) : (
                      person.ophPersonOid ?? ''
                    )
                }
              ]
            : []),
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
