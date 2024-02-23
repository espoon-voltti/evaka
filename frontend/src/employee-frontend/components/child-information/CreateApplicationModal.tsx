// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useContext, useMemo, useState } from 'react'

import { wrapResult } from 'lib-common/api'
import {
  ApplicationType,
  PaperApplicationCreateRequest
} from 'lib-common/generated/api-types/application'
import { CreatePersonBody } from 'lib-common/generated/api-types/pis'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Select from 'lib-components/atoms/dropdowns/Select'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import Radio from 'lib-components/atoms/form/Radio'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { DatePickerDeprecated } from 'lib-components/molecules/DatePickerDeprecated'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { applicationTypes } from 'lib-customizations/employee'
import { faFileAlt } from 'lib-icons'

import CreatePersonInput from '../../components/common/CreatePersonInput'
import {
  DbPersonSearch as PersonSearch,
  VtjPersonSearch
} from '../../components/common/PersonSearch'
import { getEmployeeUrlPrefix } from '../../constants'
import { createPaperApplication } from '../../generated/api-clients/application'
import { Translations, useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { formatName } from '../../utils'

const createPaperApplicationResult = wrapResult(createPaperApplication)

type PersonType = 'GUARDIAN' | 'DB_SEARCH' | 'VTJ' | 'NEW_NO_SSN'

const personToSelectOption = (
  { firstName, id, lastName }: PersonJSON,
  i18n: Translations
) => ({
  name: formatName(firstName, lastName, i18n),
  id: id
})

const hasContent = (s: string | undefined): s is string =>
  typeof s === 'string' && s.length > 0

interface CreateApplicationModalProps {
  child: PersonJSON
  guardians: PersonJSON[]
}

function CreateApplicationModal({
  child,
  guardians
}: CreateApplicationModalProps) {
  const { i18n } = useTranslation()
  const i18nView = i18n.childInformation.application.create
  const { clearUiMode } = useContext(UIContext)

  const sortedGuardians = useMemo(
    () => sortBy(guardians, ['lastName', 'firstName']),
    [guardians]
  )

  const [personType, setPersonType] = useState<PersonType>(
    sortedGuardians.length > 0 ? 'GUARDIAN' : 'DB_SEARCH'
  )
  const [guardian, setGuardian] = useState(
    sortedGuardians.length > 0
      ? personToSelectOption(sortedGuardians[0], i18n)
      : null
  )
  const [personId, setPersonId] = useState<UUID | undefined>(undefined)
  const [newVtjPersonSsn, setNewVtjPersonSsn] = useState<string | undefined>(
    undefined
  )

  const [type, setType] = useState<ApplicationType>('DAYCARE')
  const [sentDate, setSentDate] = useState<LocalDate>(
    LocalDate.todayInSystemTz()
  )
  const [hideFromGuardian, setHideFromGuardian] = useState(false)
  const [createPersonBody, setCreatePersonInfo] = useState<
    Partial<CreatePersonBody>
  >({})

  const [isSubmitting, setIsSubmitting] = useState(false)

  const validCreatePersonInfo: CreatePersonBody | null = useMemo(() => {
    const {
      dateOfBirth,
      firstName,
      lastName,
      phone,
      streetAddress,
      postalCode,
      postOffice
    } = createPersonBody
    if (
      !!dateOfBirth &&
      hasContent(firstName) &&
      hasContent(lastName) &&
      hasContent(phone) &&
      hasContent(streetAddress) &&
      hasContent(postalCode) &&
      hasContent(postOffice)
    ) {
      return {
        dateOfBirth,
        firstName,
        lastName,
        phone,
        streetAddress,
        postalCode,
        postOffice,
        email: createPersonBody.email ?? null
      }
    } else {
      return null
    }
  }, [createPersonBody])

  function canSubmit() {
    if (isSubmitting) return false
    switch (personType) {
      case 'GUARDIAN':
        return !!guardian
      case 'DB_SEARCH':
        return !!personId
      case 'VTJ':
        return !!newVtjPersonSsn
      case 'NEW_NO_SSN':
        return validCreatePersonInfo !== null
      default:
        return false
    }
  }

  function submit() {
    if (!canSubmit()) return

    const commonBody: PaperApplicationCreateRequest = {
      childId: child.id,
      type,
      sentDate,
      hideFromGuardian,
      guardianId: null,
      guardianSsn: null,
      guardianToBeCreated: null,
      transferApplication: false
    }

    const apiCall =
      personType === 'GUARDIAN'
        ? () =>
            createPaperApplicationResult({
              body: {
                ...commonBody,
                guardianId: guardian?.id ?? ''
              }
            })
        : personType === 'DB_SEARCH'
          ? () =>
              createPaperApplicationResult({
                body: {
                  ...commonBody,
                  guardianId: personId ?? ''
                }
              })
          : personType === 'VTJ'
            ? () =>
                createPaperApplicationResult({
                  body: {
                    ...commonBody,
                    guardianSsn: newVtjPersonSsn ?? null
                  }
                })
            : personType === 'NEW_NO_SSN' && !!validCreatePersonInfo
              ? () =>
                  createPaperApplicationResult({
                    body: {
                      ...commonBody,
                      guardianToBeCreated: validCreatePersonInfo
                    }
                  })
              : null

    if (apiCall) {
      setIsSubmitting(true)

      void apiCall()
        .then((id) => {
          id.isSuccess
            ? (window.location.href = `${getEmployeeUrlPrefix()}/employee/applications/${
                id.value
              }?create=true`)
            : null
        })
        .finally(() => setIsSubmitting(false))
    }
  }

  return (
    <FormModal
      title={i18nView.modalTitle}
      icon={faFileAlt}
      type="info"
      resolveAction={submit}
      resolveLabel={i18nView.createButton}
      resolveDisabled={!canSubmit()}
      rejectAction={clearUiMode}
      rejectLabel={i18n.common.cancel}
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

            {sortedGuardians.length > 0 && (
              <div>
                <Radio
                  checked={personType === 'GUARDIAN'}
                  label={i18nView.personTypes.GUARDIAN}
                  onChange={() => setPersonType('GUARDIAN')}
                  data-qa="select-guardian-radio"
                />
                <div>
                  <Select
                    items={sortedGuardians.map((g) =>
                      personToSelectOption(g, i18n)
                    )}
                    onChange={setGuardian}
                    selectedItem={guardian}
                    onFocus={() => setPersonType('GUARDIAN')}
                    data-qa="select-guardian"
                    getItemValue={({ id }) => id}
                    getItemLabel={({ name }) => name}
                    getItemDataQa={({ name }) => `guardian-${name}`}
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
                data-qa="vtj-person-radio"
              />
              <VtjPersonSearch
                data-qa="select-search-from-vtj-guardian"
                onResult={(res) =>
                  setNewVtjPersonSsn(res?.socialSecurityNumber || undefined)
                }
                onFocus={() => setPersonType('VTJ')}
              />
            </div>

            <div>
              <Radio
                data-qa="radio-new-no-ssn"
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
              data-qa="select-application-type"
              items={applicationTypes}
              onChange={(value) => (value ? setType(value) : undefined)}
              selectedItem={type}
              getItemLabel={(type) => i18nView.applicationTypes[type]}
            />
          </div>
        </div>

        <div>
          <Label>{i18nView.sentDate}</Label>
          <div>
            <DatePickerDeprecated
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
