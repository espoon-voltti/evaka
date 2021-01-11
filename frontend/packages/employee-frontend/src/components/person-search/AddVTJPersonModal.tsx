// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import styled from 'styled-components'
import { faPlus } from '@evaka/lib-icons'
import Spinner from '@evaka/lib-components/src/atoms/state/Spinner'
import { Gap } from '@evaka/lib-components/src/white-space'
import { Label } from '@evaka/lib-components/src/typography'
import InputField from '@evaka/lib-components/src/atoms/form/InputField'
import FormModal from '@evaka/lib-components/src/molecules/modals/FormModal'
import { useTranslation } from '~state/i18n'
import { Loading, Result } from '@evaka/lib-common/src/api'
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
      setPerson(Loading.of())
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
      iconColour={'blue'}
      icon={faPlus}
      title={i18n.personSearch.addPersonFromVTJ.title}
      resolve={{
        action: onConfirm,
        label: i18n.personSearch.addPersonFromVTJ.modalConfirmLabel,
        disabled: !(person && person.isSuccess) || requestInFlight
      }}
      reject={{
        action: closeModal,
        label: i18n.common.cancel
      }}
      size={'md'}
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
            {person?.isLoading ? <InputSpinner /> : null}
          </InputWrapper>
        </Label>
        <Gap size="s" />
        <SsnResultContainer>
          {person?.isSuccess ? (
            <>
              <div>{`${person.value.lastName ?? ''} ${
                person.value.firstName ?? ''
              }`}</div>
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
