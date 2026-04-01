// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import styled from 'styled-components'

import type { UpdateStateFn } from 'lib-common/form-state'
import type { Action } from 'lib-common/generated/action'
import type {
  PersonBasicInfo,
  PersonSensitiveDetails
} from 'lib-common/generated/api-types/pis'
import { isoLanguages } from 'lib-common/generated/language'
import LocalDate from 'lib-common/local-date'
import { useMutation, useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import {
  InlineExternalLinkButton,
  InlineInternalLinkButton
} from 'lib-components/atoms/buttons/InlineLinkButton'
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
import { Gap } from 'lib-components/white-space'
import {
  faCalendar,
  faChevronDown,
  faChevronUp,
  faFileAlt,
  faPen,
  faSync
} from 'lib-icons'

import { getAddressPagePdf } from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
import type { UiState } from '../../state/ui'
import { UIContext } from '../../state/ui'
import { isEmailValid } from '../../utils/validation/validations'
import {
  disableSsnMutation,
  sensitiveDetailsQuery,
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
  person: PersonBasicInfo
  isChild: boolean
  permittedActions: Set<Action.Child | Action.Person>
  sensitiveDetailsOpen: boolean
  onToggleSensitiveDetails: () => void
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

const CenteredRow = styled.div`
  display: flex;
  justify-content: center;
`

const RightAlignedRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
`

const ButtonSpacer = styled.div`
  margin-right: 25px;
`

interface SensitiveDetailsProps {
  person: PersonBasicInfo
  sensitiveDetails: PersonSensitiveDetails
  isChild: boolean
  permittedActions: Set<Action.Child | Action.Person>
  editing: boolean
  powerEditing: boolean
  form: Form
  updateForm: UpdateStateFn<Form>
  emailIsValid: boolean
}

const SensitivePersonDetails = React.memo(function SensitivePersonDetails({
  person,
  sensitiveDetails,
  isChild,
  permittedActions,
  editing,
  powerEditing,
  form,
  updateForm,
  emailIsValid
}: SensitiveDetailsProps) {
  const { i18n } = useTranslation()
  const { toggleUiMode } = useContext<UiState>(UIContext)
  const { mutate: disableSsnAdding, isPending: disablingSsn } =
    useMutation(disableSsnMutation)

  const [showSsnAddingDisabledInfo, setShowSsnAddingDisabledInfo] =
    useState(false)
  const toggleShowSsnAddingDisabledInfo = useCallback(
    () => setShowSsnAddingDisabledInfo((state) => !state),
    [setShowSsnAddingDisabledInfo]
  )

  const canEditPersonalDetails = permittedActions.has('UPDATE_PERSONAL_DETAILS')

  const language = useMemo(
    () =>
      sensitiveDetails.language
        ? (Object.values(isoLanguages).find(
            ({ alpha2 }) => alpha2 === sensitiveDetails.language
          )?.nameFi ?? sensitiveDetails.language)
        : null,
    [sensitiveDetails.language]
  )

  return (
    <LabelValueList
      spacing="small"
      contents={[
        {
          label: i18n.childInformation.personDetails.language,
          dataQa: 'person-language',
          value: language
        },
        {
          label: i18n.common.form.socialSecurityNumber,
          dataQa: 'person-ssn',
          value: sensitiveDetails.socialSecurityNumber ?? (
            <FixedSpaceColumn $spacing="xs">
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
                <FixedSpaceRow $spacing="s" $alignItems="center">
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
                  <FixedSpaceRow $spacing="s" $alignItems="center">
                    <span>{i18n.personProfile.ssnAddingDisabledCheckbox}</span>
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
                  onChange={(value) => updateForm({ streetAddress: value })}
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
                  : `${sensitiveDetails.streetAddress ?? ''}, ${
                      sensitiveDetails.postalCode ?? ''
                    } ${sensitiveDetails.postOffice ?? ''}`}
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
                  updateForm({ municipalityOfResidence: value })
                }
              />
            ) : (
              <span data-qa="person-details-municipality-of-residence">
                {person.restrictedDetailsEnabled
                  ? i18n.common.form.addressRestricted
                  : sensitiveDetails.municipalityOfResidence}
              </span>
            )
        },
        {
          label: i18n.common.form.updatedFromVtj,
          value: sensitiveDetails.updatedFromVtj
            ? sensitiveDetails.updatedFromVtj.format()
            : ''
        },
        ...(permittedActions.has('READ_OPH_OID')
          ? [
              {
                label: i18n.common.form.ophPersonOid,
                dataQa: 'person-oph-person-oid',
                valueWidth: '100%',
                value:
                  editing && permittedActions.has('UPDATE_OPH_OID') ? (
                    <InputField
                      width="L"
                      value={form.ophPersonOid}
                      placeholder={i18n.common.form.ophPersonOid}
                      onChange={(value) =>
                        updateForm({ ophPersonOid: value.trim() })
                      }
                    />
                  ) : (
                    (sensitiveDetails.ophPersonOid ?? '')
                  )
              }
            ]
          : []),
        ...(!isChild && permittedActions.has('READ_INVOICE_ADDRESS')
          ? [
              {
                label: i18n.common.form.invoicingAddress,
                value:
                  editing && permittedActions.has('UPDATE_INVOICE_ADDRESS') ? (
                    <>
                      <InputField
                        value={form.invoiceRecipientName}
                        placeholder={i18n.common.form.invoiceRecipient}
                        onChange={(value) =>
                          updateForm({ invoiceRecipientName: value })
                        }
                      />
                      <InputField
                        value={form.invoicingStreetAddress}
                        placeholder={i18n.common.form.streetAddress}
                        onChange={(value) =>
                          updateForm({ invoicingStreetAddress: value })
                        }
                      />
                      <PostalCodeAndOffice>
                        <InputField
                          id="postal-code"
                          value={form.invoicingPostalCode}
                          placeholder={i18n.common.form.postalCode}
                          onChange={(value) =>
                            updateForm({ invoicingPostalCode: value })
                          }
                        />
                        <InputField
                          id="post-office"
                          value={form.invoicingPostOffice}
                          placeholder={i18n.common.form.postOffice}
                          onChange={(value) =>
                            updateForm({ invoicingPostOffice: value })
                          }
                        />
                      </PostalCodeAndOffice>
                    </>
                  ) : sensitiveDetails.invoicingStreetAddress &&
                    sensitiveDetails.invoicingPostalCode &&
                    sensitiveDetails.invoicingPostOffice ? (
                    <>
                      {sensitiveDetails.invoiceRecipientName ? (
                        <div>{sensitiveDetails.invoiceRecipientName}</div>
                      ) : null}
                      <div>
                        {`${sensitiveDetails.invoicingStreetAddress}, ${sensitiveDetails.invoicingPostalCode} ${sensitiveDetails.invoicingPostOffice}`}
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
                      label={i18n.personProfile.forceManualFeeDecisionsChecked}
                      checked={form.forceManualFeeDecisions === true}
                      onChange={() =>
                        updateForm({ forceManualFeeDecisions: true })
                      }
                      data-qa="force-manual-fee-decisions-true"
                    />
                    <Radio
                      label={
                        i18n.personProfile.forceManualFeeDecisionsUnchecked
                      }
                      checked={form.forceManualFeeDecisions === false}
                      onChange={() =>
                        updateForm({ forceManualFeeDecisions: false })
                      }
                      data-qa="force-manual-fee-decisions-false"
                    />
                  </FixedSpaceColumn>
                ) : sensitiveDetails.forceManualFeeDecisions ? (
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
                            status: 'warning' as const
                          }
                    }
                    data-qa="person-email-input"
                  />
                ) : (
                  sensitiveDetails.email
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
                  sensitiveDetails.phone
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
                  sensitiveDetails.backupPhone
                )
              }
            ]
          : [])
      ]}
    />
  )
})

export default React.memo(function PersonDetails({
  person,
  isChild,
  permittedActions,
  sensitiveDetailsOpen,
  onToggleSensitiveDetails
}: Props) {
  const { i18n } = useTranslation()
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

  const sensitiveDetails: PersonSensitiveDetails | undefined = useQueryResult(
    sensitiveDetailsQuery({ personId: person.id }),
    { enabled: sensitiveDetailsOpen }
  ).getOrElse(undefined)

  useEffect(() => {
    if (editing && sensitiveDetails) {
      setForm({
        firstName: person.firstName || '',
        lastName: person.lastName || '',
        dateOfBirth: person.dateOfBirth,
        email: sensitiveDetails.email || '',
        phone: sensitiveDetails.phone || '',
        backupPhone: sensitiveDetails.backupPhone || '',
        streetAddress: sensitiveDetails.streetAddress || '',
        postalCode: sensitiveDetails.postalCode || '',
        postOffice: sensitiveDetails.postOffice || '',
        municipalityOfResidence: sensitiveDetails.municipalityOfResidence || '',
        invoiceRecipientName: sensitiveDetails.invoiceRecipientName ?? '',
        invoicingStreetAddress: sensitiveDetails.invoicingStreetAddress ?? '',
        invoicingPostalCode: sensitiveDetails.invoicingPostalCode ?? '',
        invoicingPostOffice: sensitiveDetails.invoicingPostOffice ?? '',
        forceManualFeeDecisions:
          sensitiveDetails.forceManualFeeDecisions ?? false,
        ophPersonOid: sensitiveDetails.ophPersonOid ?? ''
      })
    }
  }, [person, sensitiveDetails, editing])

  useEffect(() => clearUiMode, []) // eslint-disable-line react-hooks/exhaustive-deps

  const powerEditing =
    editing && sensitiveDetails?.socialSecurityNumber === null

  const updateForm: UpdateStateFn<Form> = (values) => {
    setForm({
      ...form,
      ...values
    })
  }

  const canEditPersonalDetails = permittedActions.has('UPDATE_PERSONAL_DETAILS')

  return (
    <>
      {uiMode === 'add-ssn-modal' && <AddSsnModal personId={person.id} />}
      <RightAlignedRow>
        {isChild &&
        permittedActions.has('READ_ATTENDANCE_REPORT') &&
        uiMode !== 'person-details-editing' ? (
          <ButtonSpacer>
            <InlineInternalLinkButton
              to={`/reports/child-attendance/${person.id}`}
              icon={faCalendar}
              text={i18n.childInformation.personDetails.attendanceReport}
            />
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
            <InlineExternalLinkButton
              href={getAddressPagePdf({
                guardianId: person.id
              }).url.toString()}
              icon={faFileAlt}
              text={i18n.personProfile.downloadAddressPage}
              newTab={true}
              data-qa="button-open-address-page"
            />
          </ButtonSpacer>
        ) : null}
        {sensitiveDetailsOpen &&
        ((!isChild && permittedActions.has('UPDATE')) ||
          (sensitiveDetails?.socialSecurityNumber === null &&
            canEditPersonalDetails) ||
          permittedActions.has('UPDATE_INVOICE_ADDRESS') ||
          permittedActions.has('UPDATE_OPH_OID')) ? (
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
          }
        ]}
      />
      <Gap $size="L" />
      <CenteredRow>
        <Button
          appearance="inline"
          onClick={onToggleSensitiveDetails}
          disabled={editing}
          data-qa="toggle-person-details-button"
          text={
            sensitiveDetailsOpen
              ? i18n.personProfile.hideDetails
              : i18n.personProfile.showDetails
          }
          icon={sensitiveDetailsOpen ? faChevronUp : faChevronDown}
          order="text-icon"
        />
      </CenteredRow>
      <Gap $size="L" />
      {sensitiveDetailsOpen && sensitiveDetails && (
        <SensitivePersonDetails
          person={person}
          sensitiveDetails={sensitiveDetails}
          isChild={isChild}
          permittedActions={permittedActions}
          editing={editing}
          powerEditing={powerEditing}
          form={form}
          updateForm={updateForm}
          emailIsValid={emailIsValid}
        />
      )}
      {editing && (
        <RightAlignedRow>
          <FixedSpaceRow>
            <LegacyButton
              onClick={() => clearUiMode()}
              text={i18n.common.cancel}
            />
            <MutateButton
              primary
              disabled={!emailIsValid || !sensitiveDetails}
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
