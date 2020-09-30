// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { faPlus } from 'icon-set'
import Spinner from '~components/shared/atoms/state/Spinner'
import { Gap } from '~components/shared/layout/white-space'
import { Label } from '~components/shared/Typography'
import InputField from '~components/shared/atoms/form/InputField'
import FormModal from '~components/common/FormModal'
import { useTranslation } from '~state/i18n'
import { isFailure, isLoading, isSuccess, Loading, Result } from '~api'
import { getOrCreatePersonBySsn } from '~api/person'
import { PersonDetails } from '~types/person'
import { isSsnValid } from '~utils/validation/validations'

export default React.memo(function VTJModal({
  closeModal
}: {
  closeModal: () => void
}) {
  const { i18n } = useTranslation()
  const [ssn, setSsn] = useState('')
  const [person, setPerson] = useState<Result<PersonDetails>>()
  const [requestInFlight, setRequestInFlight] = useState(false)
  const [saveError, setSaveError] = useState(false)

  useEffect(() => {
    if (isSsnValid(ssn)) {
      setPerson(Loading())
      void getOrCreatePersonBySsn(ssn, true).then(setPerson)
    }
  }, [ssn])

  const updateSsn = (ssn: string) => {
    setSsn(ssn)
    setPerson(undefined)
  }

  const onConfirm = () => {
    setRequestInFlight(true)
    setSaveError(false)
    getOrCreatePersonBySsn(ssn, false)
      .then((result) => {
        if (isSuccess(result)) {
          closeModal()
        }
        if (isFailure(result)) {
          setSaveError(true)
        }
      })
      .catch(() => {
        setRequestInFlight(false)
      })
  }

  return (
    <FormModal
      iconColour={'blue'}
      icon={faPlus}
      title={i18n.personSearch.addPersonFromVTJ.title}
      resolve={onConfirm}
      reject={closeModal}
      resolveLabel={i18n.personSearch.addPersonFromVTJ.modalConfirmLabel}
      rejectLabel={i18n.common.cancel}
      size={'md'}
      resolveDisabled={!(person && isSuccess(person)) || requestInFlight}
    >
      <ModalContent>
        <Label>
          {i18n.personSearch.addPersonFromVTJ.ssnLabel}
          <InputWrapper>
            <InputField
              value={ssn}
              onChange={updateSsn}
              width="full"
              dataQa="ssn-input"
            />
            {person && isLoading(person) ? <InputSpinner /> : null}
          </InputWrapper>
        </Label>
        <Gap size="s" />
        <SsnResultContainer>
          {person && isSuccess(person) ? (
            <>
              <div>{`${person.data.lastName ?? ''} ${
                person.data.firstName ?? ''
              }`}</div>
              <div>
                {person.data.restrictedDetailsEnabled
                  ? i18n.personSearch.addPersonFromVTJ.restrictedDetails
                  : person.data.streetAddress}
              </div>
            </>
          ) : null}
          {person && isFailure(person) ? (
            <div>
              {person.error.statusCode === 400
                ? i18n.personSearch.addPersonFromVTJ.badRequest
                : person.error.statusCode === 404
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
  width: 80%;
  align-items: center;
  padding: 0em 2em;
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
