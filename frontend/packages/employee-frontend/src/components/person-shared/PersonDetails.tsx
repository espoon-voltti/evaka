// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as React from 'react'
import { faPen } from '@evaka/icons'
import { isFailure, isLoading, isSuccess, Result } from '~api'
import { PersonDetails } from '~/types/person'
import { useTranslation } from '~state/i18n'
import { useContext, useEffect, useState } from 'react'
import {
  Button,
  Buttons,
  Input,
  Loader,
  Radio,
  RadioGroup
} from '~components/shared/alpha'
import LabelValueList from '~components/common/LabelValueList'
import { DatePicker } from '~components/common/DatePicker'
import { patchPersonDetails } from '~api/person'
import { UIContext, UiState } from '~state/ui'
import AddSsnModal from '~components/person-shared/person-details/AddSsnModal'
import { UserContext } from '~state/user'
import styled from 'styled-components'
import { RequireRole, requireRole } from '~utils/roles'
import LocalDate from '@evaka/lib-common/src/local-date'

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
    if (editing && isSuccess(personResult)) {
      setForm({
        firstName: personResult.data.firstName || '',
        lastName: personResult.data.lastName || '',
        dateOfBirth: personResult.data.dateOfBirth,
        email: personResult.data.email || '',
        phone: personResult.data.phone || '',
        streetAddress: personResult.data.streetAddress || '',
        postalCode: personResult.data.postalCode || '',
        postOffice: personResult.data.postOffice || '',
        invoiceRecipientName: personResult.data.invoiceRecipientName ?? '',
        invoicingStreetAddress: personResult.data.invoicingStreetAddress ?? '',
        invoicingPostalCode: personResult.data.invoicingPostalCode ?? '',
        invoicingPostOffice: personResult.data.invoicingPostOffice ?? '',
        forceManualFeeDecisions:
          personResult.data.forceManualFeeDecisions ?? false
      })
    }
  }, [personResult, editing])

  // clear ui mode when dismounting component
  useEffect(() => {
    return clearUiMode
  }, [])

  if (isLoading(personResult)) return <Loader />
  if (isFailure(personResult)) return <div>{i18n.common.loadingFailed}</div>

  const person = personResult.data
  const powerEditing = editing && person.socialSecurityNumber == null

  const updateForm = (values: Partial<Form>) => {
    setForm({
      ...form,
      ...values
    })
  }

  const onSubmit = () => {
    void patchPersonDetails(person.id, form).then((res) => {
      if (isSuccess(res)) {
        if (onUpdateComplete) onUpdateComplete(res.data)
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
            <Button
              plain
              icon={faPen}
              iconSize="lg"
              onClick={() => toggleUiMode('person-details-editing')}
              dataQa="edit-person-settings-button"
            >
              {i18n.common.edit}
            </Button>
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
                <Input
                  value={form.lastName}
                  onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                    updateForm({ lastName: event.target.value })
                  }
                  dataQa="input-last-name"
                />
              ) : (
                person.lastName
              )
            },
            {
              label: i18n.common.form.firstNames,
              dataQa: 'person-first-names',
              value: powerEditing ? (
                <Input
                  value={form.firstName}
                  onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                    updateForm({ firstName: event.target.value })
                  }
                  dataQa="input-first-name"
                />
              ) : (
                person.firstName
              )
            },
            {
              label: i18n.common.form.birthday,
              dataQa: 'person-birthday',
              value: powerEditing ? (
                <DatePicker
                  type="full-width"
                  date={form.dateOfBirth}
                  onChange={(dateOfBirth) => updateForm({ dateOfBirth })}
                  maxDate={LocalDate.today()}
                  dataQa="input-birthday"
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
                  <Button
                    plain
                    className="inline"
                    onClick={() => toggleUiMode('add-ssn-modal')}
                  >
                    {i18n.personProfile.addSsn}
                  </Button>
                ))
            },
            {
              label: i18n.common.form.address,
              dataQa: 'person-address',
              value: powerEditing ? (
                <>
                  <Input
                    value={form.streetAddress}
                    placeholder={i18n.common.form.streetAddress}
                    onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                      updateForm({
                        streetAddress: event.target.value
                      })
                    }
                  />
                  <PostalCodeAndOffice>
                    <Input
                      id="postal-code"
                      value={form.postalCode}
                      placeholder={i18n.common.form.postalCode}
                      onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                        updateForm({ postalCode: event.target.value })
                      }
                    />
                    <Input
                      id="post-office"
                      value={form.postOffice}
                      placeholder={i18n.common.form.postOffice}
                      onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                        updateForm({ postOffice: event.target.value })
                      }
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
                          <Input
                            value={form.invoiceRecipientName}
                            placeholder={i18n.common.form.invoiceRecipient}
                            onChange={(
                              event: React.ChangeEvent<HTMLInputElement>
                            ) =>
                              updateForm({
                                invoiceRecipientName: event.target.value
                              })
                            }
                          />

                          <Input
                            value={form.invoicingStreetAddress}
                            placeholder={i18n.common.form.streetAddress}
                            onChange={(
                              event: React.ChangeEvent<HTMLInputElement>
                            ) =>
                              updateForm({
                                invoicingStreetAddress: event.target.value
                              })
                            }
                          />
                          <PostalCodeAndOffice>
                            <Input
                              id="postal-code"
                              value={form.invoicingPostalCode}
                              placeholder={i18n.common.form.postalCode}
                              onChange={(
                                event: React.ChangeEvent<HTMLInputElement>
                              ) =>
                                updateForm({
                                  invoicingPostalCode: event.target.value
                                })
                              }
                            />
                            <Input
                              id="post-office"
                              value={form.invoicingPostOffice}
                              placeholder={i18n.common.form.postOffice}
                              onChange={(
                                event: React.ChangeEvent<HTMLInputElement>
                              ) =>
                                updateForm({
                                  invoicingPostOffice: event.target.value
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
                      <RadioGroup label="" dataQa="force-manual-fee-decisions">
                        <Radio
                          label={
                            i18n.personProfile.forceManualFeeDecisionsChecked
                          }
                          id="force-manual-fee-decisions-true"
                          value={true}
                          model={form.forceManualFeeDecisions}
                          onChange={(v) =>
                            updateForm({
                              forceManualFeeDecisions: v
                            })
                          }
                          dataQa={`force-manual-fee-decisions-true`}
                        />
                        <Radio
                          label={
                            i18n.personProfile.forceManualFeeDecisionsUnchecked
                          }
                          id="force-manual-fee-decisions-false"
                          value={false}
                          model={form.forceManualFeeDecisions}
                          onChange={(v) =>
                            updateForm({
                              forceManualFeeDecisions: v
                            })
                          }
                          dataQa={`force-manual-fee-decisions-false`}
                        />
                      </RadioGroup>
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
                      <Input
                        value={form.email}
                        onChange={(
                          event: React.ChangeEvent<HTMLInputElement>
                        ) => updateForm({ email: event.target.value })}
                        size={40}
                      />
                    ) : (
                      person.email
                    )
                  },
                  {
                    label: i18n.common.form.phone,
                    value: editing ? (
                      <Input
                        value={form.phone}
                        onChange={(
                          event: React.ChangeEvent<HTMLInputElement>
                        ) => updateForm({ phone: event.target.value })}
                      />
                    ) : (
                      person.phone
                    )
                  }
                ]
              : [])
          ]}
        />
      </FlexContainer>
      {editing && (
        <RightAlignedRow>
          <Buttons>
            <Button onClick={() => clearUiMode()}>{i18n.common.cancel}</Button>
            <Button
              primary
              disabled={false}
              onClick={() => onSubmit()}
              dataQa="confirm-edited-person-button"
            >
              {i18n.common.confirm}
            </Button>
          </Buttons>
        </RightAlignedRow>
      )}
    </>
  )
})

export default PersonDetails
