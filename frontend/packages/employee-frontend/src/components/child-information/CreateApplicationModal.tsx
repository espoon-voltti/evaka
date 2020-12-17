// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import FormModal from 'components/common/FormModal'
import { useTranslation } from 'state/i18n'
import { faFileAlt } from '@evaka/lib-icons'
import { UIContext } from 'state/ui'
import { PersonDetails } from 'types/person'
import { Label } from '@evaka/lib-components/src/typography'
import { formatName } from 'utils'
import { FixedSpaceColumn } from 'components/shared/layout/flex-helpers'
import Radio from '@evaka/lib-components/src/atoms/form/Radio'
import PersonSearch from 'components/common/PersonSearch'
import { UUID } from 'types'
import { DatePicker } from 'components/common/DatePicker'
import LocalDate from '@evaka/lib-common/src/local-date'
import Select from 'components/common/Select'
import { ApplicationType } from 'types/application'
import {
  createPaperApplication,
  PaperApplicationRequest
} from 'api/applications'
import { getEmployeeUrlPrefix } from '~constants'
import Checkbox from '@evaka/lib-components/src/atoms/form/Checkbox'
import VtjPersonSearch from '~components/common/VtjPersonSearch'
import CreatePersonInput from '~components/common/CreatePersonInput'
import { CreatePersonBody } from '~api/person'

interface CreateApplicationModalProps {
  child: PersonDetails
  guardians: PersonDetails[]
}

const applicationTypes: ApplicationType[] = ['DAYCARE', 'PRESCHOOL', 'CLUB']

type PersonType = 'GUARDIAN' | 'DB_SEARCH' | 'VTJ' | 'NEW_NO_SSN'

function CreateApplicationModal({
  child,
  guardians
}: CreateApplicationModalProps) {
  const { i18n } = useTranslation()
  const i18nView = i18n.childInformation.application.create
  const { clearUiMode } = useContext(UIContext)

  const [personType, setPersonType] = useState<PersonType>(
    guardians.length > 0 ? 'GUARDIAN' : 'DB_SEARCH'
  )
  const [guardianId, setGuardiaId] = useState<UUID | undefined>(
    guardians.length > 0 ? guardians[0].id : undefined
  )
  const [personId, setPersonId] = useState<UUID | undefined>(undefined)
  const [newVtjPersonSsn, setNewVtjPersonSsn] = useState<string | undefined>(
    undefined
  )

  const [type, setType] = useState<ApplicationType>('DAYCARE')
  const [sentDate, setSentDate] = useState<LocalDate>(LocalDate.today())
  const [hideFromGuardian, setHideFromGuardian] = useState(false)
  const [createPersonBody, setCreatePersonInfo] = useState<
    Partial<CreatePersonBody>
  >({})

  const [isSubmitting, setIsSubmitting] = useState(false)

  function createPersonInfoIsValid(): boolean {
    const hasContent = (s: string | undefined): boolean =>
      typeof s === 'string' && s.length > 0
    return (
      createPersonBody.dateOfBirth != null &&
      hasContent(createPersonBody.firstName) &&
      hasContent(createPersonBody.lastName) &&
      hasContent(createPersonBody.phone) &&
      hasContent(createPersonBody.streetAddress) &&
      hasContent(createPersonBody.postalCode) &&
      hasContent(createPersonBody.postOffice)
    )
  }

  function canSubmit() {
    if (isSubmitting) return false
    switch (personType) {
      case 'GUARDIAN':
        return !!guardianId
      case 'DB_SEARCH':
        return !!personId
      case 'VTJ':
        return !!newVtjPersonSsn
      case 'NEW_NO_SSN':
        return createPersonInfoIsValid()
      default:
        return false
    }
  }

  function submit() {
    if (!canSubmit()) return

    const commonBody: PaperApplicationRequest = {
      childId: child.id,
      type,
      sentDate,
      hideFromGuardian
    }

    const apiCall =
      personType === 'GUARDIAN'
        ? () =>
            createPaperApplication({
              ...commonBody,
              guardianId: guardianId ?? ''
            })
        : personType === 'DB_SEARCH'
        ? () =>
            createPaperApplication({
              ...commonBody,
              guardianId: personId ?? ''
            })
        : personType === 'VTJ'
        ? () =>
            createPaperApplication({
              ...commonBody,
              guardianSsn: newVtjPersonSsn
            })
        : personType === 'NEW_NO_SSN'
        ? () =>
            createPaperApplication({
              ...commonBody,
              guardianToBeCreated: createPersonBody as CreatePersonBody
            })
        : null

    if (apiCall) {
      setIsSubmitting(true)

      apiCall()
        .then((id) => {
          window.location.href = `${getEmployeeUrlPrefix()}/employee/applications/${id}?create=true`
        })
        .finally(() => setIsSubmitting(false))
    }
  }

  return (
    <FormModal
      title={i18nView.modalTitle}
      icon={faFileAlt}
      iconColour={'blue'}
      resolveLabel={i18nView.createButton}
      rejectLabel={i18n.common.cancel}
      reject={() => clearUiMode()}
      resolve={() => submit()}
      resolveDisabled={!canSubmit()}
    >
      <FixedSpaceColumn spacing="L">
        <div>
          <Label>
            {formatName(child.firstName, child.lastName, i18n, true)}
          </Label>
          <div>{child.socialSecurityNumber || child.dateOfBirth.format()}</div>
          <div>{`${child.streetAddress ?? ''}, ${child.postalCode ?? ''} ${
            child.postOffice ?? ''
          }`}</div>
        </div>

        <div>
          <FixedSpaceColumn>
            <Label>{i18nView.applier}</Label>

            {guardians.length > 0 && (
              <div>
                <Radio
                  checked={personType === 'GUARDIAN'}
                  label={i18nView.personTypes.GUARDIAN}
                  onChange={() => setPersonType('GUARDIAN')}
                />
                <div>
                  <Select
                    options={guardians.map((guardian) => ({
                      value: guardian.id,
                      label: formatName(
                        guardian.firstName,
                        guardian.lastName,
                        i18n,
                        false
                      )
                    }))}
                    onChange={(value) =>
                      value && 'value' in value
                        ? setGuardiaId(value.value)
                        : undefined
                    }
                    value={guardians
                      .filter((guardian) => guardian.id === guardianId)
                      .map((guardian) => ({
                        value: guardian.id,
                        label: formatName(
                          guardian.firstName,
                          guardian.lastName,
                          i18n,
                          false
                        )
                      }))}
                    onFocus={() => setPersonType('GUARDIAN')}
                    data-qa="select-guardian"
                  />
                </div>
              </div>
            )}

            <div>
              <Radio
                checked={personType === 'DB_SEARCH'}
                label={i18nView.personTypes.DB_SEARCH}
                onChange={() => setPersonType('DB_SEARCH')}
              />
              <PersonSearch
                onResult={(res) => setPersonId(res?.id)}
                onFocus={() => setPersonType('DB_SEARCH')}
              />
            </div>

            <div>
              <Radio
                checked={personType === 'VTJ'}
                label={i18nView.personTypes.VTJ}
                onChange={() => setPersonType('VTJ')}
              />
              <VtjPersonSearch
                onResult={(res) =>
                  setNewVtjPersonSsn(res?.socialSecurityNumber || undefined)
                }
                onFocus={() => setPersonType('VTJ')}
              />
            </div>

            <div>
              <Radio
                dataQa={'radio-new-no-ssn'}
                checked={personType === 'NEW_NO_SSN'}
                label={i18nView.personTypes.NEW_NO_SSN}
                onChange={() => setPersonType('NEW_NO_SSN')}
              />
              <CreatePersonInput
                createPersonInfo={createPersonBody}
                setCreatePersonInfo={setCreatePersonInfo}
                personType={personType}
                onFocus={() => setPersonType('NEW_NO_SSN')}
              />
            </div>
          </FixedSpaceColumn>
        </div>

        <div>
          <Label>{i18nView.applicationType}</Label>
          <div>
            <Select
              options={applicationTypes.map((type) => ({
                value: type,
                label: i18nView.applicationTypes[type]
              }))}
              onChange={(value) =>
                value && 'value' in value
                  ? setType(value.value as ApplicationType)
                  : undefined
              }
              value={{
                value: type,
                label: i18nView.applicationTypes[type]
              }}
            />
          </div>
        </div>

        <div>
          <Label>{i18nView.sentDate}</Label>
          <div>
            <DatePicker
              date={sentDate}
              onChange={setSentDate}
              type="full-width"
            />
          </div>
        </div>

        <FixedSpaceColumn>
          <Checkbox
            checked={hideFromGuardian}
            label={i18nView.hideFromGuardian}
            onChange={(checked) => setHideFromGuardian(checked)}
          />
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </FormModal>
  )
}

export default CreateApplicationModal
