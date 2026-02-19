// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'

import type { Result } from 'lib-common/api'
import { Loading } from 'lib-common/api'
import type { PersonJSON } from 'lib-common/generated/api-types/pis'
import { useMutationResult } from 'lib-common/query'
import InputField from 'lib-components/atoms/form/InputField'
import Spinner from 'lib-components/atoms/state/Spinner'
import { PersonName } from 'lib-components/molecules/PersonNames'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { faPlus } from 'lib-icons'

import { useTranslation } from '../../state/i18n'
import { isSsnValid } from '../../utils/validation/validations'

import { getOrCreatePersonBySsnMutation } from './queries'

export default React.memo(function VTJModal({
  closeModal
}: {
  closeModal: () => void
}) {
  const { i18n } = useTranslation()
  const { mutateAsync: doGetOrCreatePersonBySsn } = useMutationResult(
    getOrCreatePersonBySsnMutation
  )
  const [ssn, setSsn] = useState('')
  const [person, setPerson] = useState<Result<PersonJSON>>()
  const [requestInFlight, setRequestInFlight] = useState(false)
  const [saveError, setSaveError] = useState(false)

  useEffect(() => {
    if (isSsnValid(ssn)) {
      setPerson(Loading.of())
      void doGetOrCreatePersonBySsn({ body: { ssn, readonly: true } }).then(
        setPerson
      )
    }
  }, [ssn, doGetOrCreatePersonBySsn])

  const updateSsn = (ssn: string) => {
    setSsn(ssn)
    setPerson(undefined)
  }

  const onConfirm = () => {
    setRequestInFlight(true)
    setSaveError(false)
    doGetOrCreatePersonBySsn({ body: { ssn, readonly: false } })
      .then((result) => {
        if (result.isSuccess) {
          closeModal()
        }
        if (result.isFailure) {
          setSaveError(true)
        }
      })
      .catch(() => {
        setRequestInFlight(false)
      })
  }

  return (
    <FormModal
      type="info"
      icon={faPlus}
      title={i18n.personSearch.addPersonFromVTJ.title}
      resolveAction={onConfirm}
      resolveLabel={i18n.personSearch.addPersonFromVTJ.modalConfirmLabel}
      resolveDisabled={!(person && person.isSuccess) || requestInFlight}
      rejectAction={closeModal}
      rejectLabel={i18n.common.cancel}
    >
      <ModalContent>
        <Label>
          {i18n.personSearch.addPersonFromVTJ.ssnLabel}
          <InputWrapper>
            <InputField
              value={ssn}
              onChange={updateSsn}
              width="full"
              data-qa="ssn-input"
            />
            {person?.isLoading ? <InputSpinner /> : null}
          </InputWrapper>
        </Label>
        <Gap size="s" />
        <SsnResultContainer>
          {person?.isSuccess ? (
            <>
              <div>
                <PersonName person={person.value} format="Last First" />
              </div>
              <div>
                {person.value.restrictedDetailsEnabled
                  ? i18n.personSearch.addPersonFromVTJ.restrictedDetails
                  : person.value.streetAddress}
              </div>
            </>
          ) : null}
          {person?.isFailure ? (
            <div>
              {person.statusCode === 400
                ? i18n.personSearch.addPersonFromVTJ.badRequest
                : person.statusCode === 404
                  ? i18n.personSearch.addPersonFromVTJ.notFound
                  : i18n.personSearch.addPersonFromVTJ.unexpectedError}
            </div>
          ) : null}
          {saveError ? (
            <>
              <Gap size="s" />
              <div>{i18n.common.error.unknown}</div>
            </>
          ) : null}
        </SsnResultContainer>
      </ModalContent>
    </FormModal>
  )
})

const ModalContent = styled.div`
  align-items: center;
  padding: 0 2em;
  width: auto;
`

const InputWrapper = styled.div`
  position: relative;
`

const InputSpinner = styled(Spinner)`
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
`

const SsnResultContainer = styled.div`
  min-height: 3rem;
`
