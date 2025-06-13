// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import noop from 'lodash/noop'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'
import { Link, useLocation } from 'wouter'

import type { UpdateStateFn } from 'lib-common/form-state'
import type { Action } from 'lib-common/generated/action'
import type { PersonJSON } from 'lib-common/generated/api-types/pis'
import { isoLanguages } from 'lib-common/generated/language'
import LocalDate from 'lib-common/local-date'
import { useMutation } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  ExpandingInfoBox,
  InfoButton
} from 'lib-components/molecules/ExpandingInfo'
import LabelValueList from 'lib-components/molecules/LabelValueList'
import DatePicker from 'lib-components/molecules/date-picker/DatePicker'
import { featureFlags } from 'lib-customizations/employee'
import { faCalendar, faCopy, faFileAlt, faPen, faSync } from 'lib-icons'

import { getAddressPagePdf } from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
import type { UiState } from '../../state/ui'
import { UIContext } from '../../state/ui'
import { isEmailValid } from '../../utils/validation/validations'
import {
  disableSsnMutation,
  duplicatePersonMutation,
  updatePersonAndFamilyFromVtjMutation,
  updatePersonDetailsMutation
} from '../person-profile/queries'

import AddSsnModal from './person-details/AddSsnModal'

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
  person: PersonJSON
  isChild: boolean
  permittedActions: Set<Action.Child | Action.Person>
}

interface Form {
  firstName: string
  lastName: string
  dateOfBirth: LocalDate | null
  email: string
  phone: string
  backupPhone: string
  streetAddress: string
  postalCode: string
  postOffice: string
  municipalityOfResidence: string
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

const ButtonSpacer = styled.div`
  margin-right: 25px;
`

export default React.memo(function PersonDetails({
  person,
  isChild,
  permittedActions
}: Props) {
  const { i18n } = useTranslation()
  const [, navigate] = useLocation()
  const { uiMode, toggleUiMode, clearUiMode } = useContext<UiState>(UIContext)
  const editing = uiMode === 'person-details-editing'
  const [form, setForm] = useState<Form>({
    firstName: '',
    lastName: '',
    dateOfBirth: LocalDate.todayInSystemTz(),
    email: '',
    phone: '',
    backupPhone: '',
    streetAddress: '',
    postalCode: '',
    postOffice: '',
    municipalityOfResidence: '',
    invoiceRecipientName: '',
    invoicingStreetAddress: '',
    invoicingPostalCode: '',
    invoicingPostOffice: '',
    forceManualFeeDecisions: false,
    ophPersonOid: ''
  })
  const emailIsValid = useMemo<boolean>(
    () => form.email === '' || isEmailValid(form.email),
    [form.email]
  )
  const { mutate: disableSsnAdding, isPending: disablingSsn } =
    useMutation(disableSsnMutation)

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
        municipalityOfResidence: person.municipalityOfResidence || '',
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
  useEffect(() => clearUiMode, []) // eslint-disable-line react-hooks/exhaustive-deps

  const powerEditing = editing && person.socialSecurityNumber == null

  const updateForm: UpdateStateFn<Form> = (values) => {
    setForm({
      ...form,
      ...values
    })
  }

  const canEditPersonalDetails = permittedActions.has('UPDATE_PERSONAL_DETAILS')

  const language = useMemo(
    () =>
      (person.language
        ? Object.values(isoLanguages).find(
            ({ alpha2 }) => alpha2 === person.language
          )
        : null
      )?.nameFi ?? person.language,
    [person.language]
  )

  return (
    <>
      {uiMode === 'add-ssn-modal' && <AddSsnModal personId={person.id} />}
      <RightAlignedRow>
        {featureFlags.personDuplicate &&
        permittedActions.has('DUPLICATE') &&
        person.duplicateOf === null &&
        isChild &&
        uiMode !== 'person-details-editing' ? (
          <ButtonSpacer>
            <MutateButton
              appearance="inline"
              icon={faCopy}
              mutation={duplicatePersonMutation}
              onClick={() => ({ personId: person.id })}
              onSuccess={(personId) => {
                navigate(`/child-information/${personId}`)
              }}
              text={i18n.personProfile.duplicate}
            />
          </ButtonSpacer>
        ) : null}
        {isChild &&
        permittedActions.has('READ_ATTENDANCE_REPORT') &&
        uiMode !== 'person-details-editing' ? (
          <ButtonSpacer>
            <Link to={`/reports/child-attendance/${person.id}`}>
              <Button
                appearance="inline"
                icon={faCalendar}
                onClick={() => undefined}
                text={i18n.childInformation.personDetails.attendanceReport}
              />
            </Link>
          </ButtonSpacer>
        ) : null}
        {permittedActions.has('UPDATE_FROM_VTJ') &&
        uiMode !== 'person-details-editing' ? (
          <ButtonSpacer>
            <MutateButton
              appearance="inline"
              icon={faSync}
              mutation={updatePersonAndFamilyFromVtjMutation}
              onClick={() => ({ personId: person.id })}
              onSuccess={clearUiMode}
              data-qa="update-from-vtj-button"
              text={i18n.personProfile.updateFromVtj}
            />
          </ButtonSpacer>
        ) : null}
        {permittedActions.has('DOWNLOAD_ADDRESS_PAGE') &&
        !isChild &&
        uiMode !== 'person-details-editing' ? (
          <ButtonSpacer>
            <a
              href={getAddressPagePdf({ guardianId: person.id }).url.toString()}
              target="_blank"
              rel="noreferrer"
            >
              <Button
                appearance="inline"
                icon={faFileAlt}
                text={i18n.personProfile.downloadAddressPage}
                onClick={noop}
                data-qa="button-open-address-page"
              />
            </a>
          </ButtonSpacer>
        ) : null}
        {(!isChild && permittedActions.has('UPDATE')) ||
        (person.socialSecurityNumber === null && canEditPersonalDetails) ||
        permittedActions.has('UPDATE_INVOICE_ADDRESS') ||
        permittedActions.has('UPDATE_OPH_OID') ? (
          <Button
            appearance="inline"
            icon={faPen}
            onClick={() => toggleUiMode('person-details-editing')}
            disabled={uiMode === 'person-details-editing'}
            data-qa="edit-person-settings-button"
            text={i18n.common.edit}
          />
        ) : null}
      </RightAlignedRow>
      <LabelValueList
        spacing="small"
        contents={[
          {
            label: i18n.common.form.lastName,
            dataQa: 'person-last-name',
            valueWidth: '100%',
            value:
              powerEditing && canEditPersonalDetails ? (
                <InputField
                  width="L"
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
            value:
              powerEditing && canEditPersonalDetails ? (
                <InputField
                  width="L"
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
            value:
              powerEditing && canEditPersonalDetails ? (
                <DatePicker
                  date={form.dateOfBirth}
                  onChange={(dateOfBirth) => updateForm({ dateOfBirth })}
                  maxDate={LocalDate.todayInSystemTz()}
                  locale="fi"
                  data-qa="input-birthday"
                />
              ) : (
                person.dateOfBirth.format()
              )
          },
          {
            label: i18n.childInformation.personDetails.language,
            dataQa: 'person-language',
            value: language
          },
          {
            label: i18n.common.form.socialSecurityNumber,
            dataQa: 'person-ssn',
            value: person.socialSecurityNumber ?? (
              <FixedSpaceColumn spacing="xs">
                {editing ||
                !permittedActions.has('ADD_SSN') ||
                (person.ssnAddingDisabled &&
                  !permittedActions.has('ENABLE_SSN_ADDING')) ? (
                  <span data-qa="no-ssn">{i18n.personProfile.noSsn}</span>
                ) : (
                  <Button
                    appearance="inline"
                    onClick={() => toggleUiMode('add-ssn-modal')}
                    text={i18n.personProfile.addSsn}
                    disabled={!permittedActions.has('ADD_SSN')}
                    data-qa="add-ssn-button"
                  />
                )}
                {(person.ssnAddingDisabled &&
                  permittedActions.has('ENABLE_SSN_ADDING')) ||
                (!person.ssnAddingDisabled &&
                  permittedActions.has('DISABLE_SSN_ADDING')) ? (
                  <FixedSpaceRow spacing="s" alignItems="center">
                    <Checkbox
                      checked={person.ssnAddingDisabled}
                      label={i18n.personProfile.ssnAddingDisabledCheckbox}
                      disabled={disablingSsn}
                      onChange={(checked) =>
                        disableSsnAdding({
                          personId: person.id,
                          body: { disabled: checked }
                        })
                      }
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
            value:
              powerEditing && canEditPersonalDetails ? (
                <>
                  <InputField
                    width="L"
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
          {
            label: i18n.common.form.municipalityOfResidence,
            dataQa: 'municipality-of-residence',
            valueWidth: '100%',
            value:
              powerEditing && canEditPersonalDetails ? (
                <InputField
                  width="L"
                  value={form.municipalityOfResidence}
                  placeholder={i18n.common.form.municipalityOfResidence}
                  onChange={(value) =>
                    updateForm({
                      municipalityOfResidence: value
                    })
                  }
                />
              ) : (
                <span data-qa="person-details-municipality-of-residence">
                  {person.restrictedDetailsEnabled
                    ? i18n.common.form.addressRestricted
                    : person.municipalityOfResidence}
                </span>
              )
          },
          {
            label: i18n.common.form.updatedFromVtj,
            value: person.updatedFromVtj ? person.updatedFromVtj.format() : ''
          },
          ...(permittedActions.has('READ_OPH_OID')
            ? [
                {
                  label: i18n.common.form.ophPersonOid,
                  dataQa: 'person-oph-person-oid',
                  valueWidth: '100%',
                  value:
                    editing && permittedActions.has('UPDATE_OPH_OID') ? (
                      <>
                        <InputField
                          width="L"
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
                      (person.ophPersonOid ?? '')
                    )
                }
              ]
            : []),
          ...(!isChild && permittedActions.has('READ_INVOICE_ADDRESS')
            ? [
                {
                  label: i18n.common.form.invoicingAddress,
                  value:
                    editing &&
                    permittedActions.has('UPDATE_INVOICE_ADDRESS') ? (
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
                        data-qa="force-manual-fee-decisions-true"
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
                        data-qa="force-manual-fee-decisions-false"
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
                      onChange={(value) => {
                        updateForm({ email: value.trim() })
                      }}
                      info={
                        emailIsValid
                          ? undefined
                          : {
                              text: i18n.validationErrors.email,
                              status: 'warning'
                            }
                      }
                      data-qa="person-email-input"
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
            <LegacyButton
              onClick={() => clearUiMode()}
              text={i18n.common.cancel}
            />
            <MutateButton
              primary
              disabled={!emailIsValid}
              mutation={updatePersonDetailsMutation}
              onClick={() => ({ personId: person.id, body: form })}
              onSuccess={clearUiMode}
              data-qa="confirm-edited-person-button"
              text={i18n.common.confirm}
            />
          </FixedSpaceRow>
        </RightAlignedRow>
      )}
    </>
  )
})
