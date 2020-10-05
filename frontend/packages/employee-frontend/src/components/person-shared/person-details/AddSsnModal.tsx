// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import styled from 'styled-components'
import InputField from '~components/shared/atoms/form/InputField'
import FormModal from '~components/common/FormModal'
import { useTranslation } from '~state/i18n'
import { UIContext } from '~state/ui'
import { UUID } from '~types'
import { addSsn } from '~api/person'
import { PersonDetails } from '~types/person'
import { isFailure, isSuccess } from '~api'
import { isSsnValid } from '~utils/validation/validations'
import { EspooColours } from '~utils/colours'

const Error = styled.div`
  display: flex;
  justify-content: center;
  color: ${EspooColours.red};
  margin: 20px;
`

interface Props {
  personId: UUID
  onUpdateComplete?: (data: PersonDetails) => void
}

function AddSsnModal({ personId, onUpdateComplete }: Props) {
  const { i18n } = useTranslation()
  const { clearUiMode } = useContext(UIContext)
  const [ssn, setSsn] = useState<string>('')
  const [submitting, setSubmitting] = useState<boolean>(false)
  const [error, setError] = useState<'invalid' | 'conflict' | 'unknown'>()

  useEffect(() => {
    setError(undefined)
  }, [ssn])

  const submit = () => {
    let isMounted = true
    setSubmitting(true)
    addSsn(personId, ssn)
      .then((result) => {
        if (isSuccess(result)) {
          if (onUpdateComplete) onUpdateComplete(result.data)
          clearUiMode()
          isMounted = false
        } else if (isFailure(result)) {
          if (result.error.statusCode === 400) {
            setError('invalid')
          } else if (result.error.statusCode === 409) {
            setError('conflict')
          } else {
            setError('unknown')
          }
        }
      })
      .catch(() => void setError('unknown'))
      .finally(() => isMounted && setSubmitting(false))
  }

  return (
    <FormModal
      title={i18n.personProfile.addSsn}
      resolve={() => submit()}
      resolveLabel={i18n.common.confirm}
      reject={() => clearUiMode()}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={submitting || !isSsnValid(ssn.toUpperCase())}
    >
      <InputField value={ssn} onChange={(value) => setSsn(value)} />
      <Error>
        {error === 'invalid'
          ? i18n.personProfile.ssnInvalid
          : error === 'conflict'
          ? i18n.personProfile.ssnConflict
          : error === 'unknown'
          ? i18n.common.error.unknown
          : ''}
      </Error>
    </FormModal>
  )
}

export default AddSsnModal
