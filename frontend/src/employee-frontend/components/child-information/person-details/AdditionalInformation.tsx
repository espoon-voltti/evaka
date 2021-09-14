// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useTranslation } from '../../../state/i18n'
import { Loading } from 'lib-common/api'
import {
  getAdditionalInformation,
  updateAdditionalInformation
} from '../../../api/child/additional-information'
import { ChildContext } from '../../../state'
import TextArea from 'lib-components/atoms/form/TextArea'
import Button from 'lib-components/atoms/buttons/Button'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Loader from 'lib-components/atoms/Loader'
import { UUID } from '../../../types'
import LabelValueList from '../../../components/common/LabelValueList'
import styled from 'styled-components'
import { faPen } from 'lib-icons'
import { UIContext, UiState } from '../../../state/ui'
import { AdditionalInformation } from '../../../types/child'
import { formatParagraphs } from '../../../utils/html-utils'
import { textAreaRows } from '../../utils'
import { RequireRole } from '../../../utils/roles'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H4 } from 'lib-components/typography'
import { FlexRow } from '../../common/styled/containers'

const TextAreaInput = styled(TextArea)`
  width: 100%;
`

const RightAlignedRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  padding-top: 20px;
`

interface Props {
  id: UUID
}

const AdditionalInformation = React.memo(function AdditionalInformation({
  id
}: Props) {
  const { i18n } = useTranslation()
  const { additionalInformation, setAdditionalInformation } =
    useContext(ChildContext)
  const { uiMode, toggleUiMode, clearUiMode } = useContext<UiState>(UIContext)
  const [form, setForm] = useState<AdditionalInformation>({
    additionalInfo: '',
    allergies: '',
    diet: '',
    preferredName: '',
    medication: ''
  })

  const editing = uiMode == 'child-additional-details-editing'

  const loadData = () => {
    setAdditionalInformation(Loading.of())
    void getAdditionalInformation(id).then((additionalInformation) => {
      setAdditionalInformation(additionalInformation)
    })
  }

  useEffect(() => {
    loadData()
  }, [id]) // eslint-disable-line react-hooks/exhaustive-deps

  const startEdit = () => {
    if (additionalInformation.isSuccess) {
      setForm({
        additionalInfo: additionalInformation.value.additionalInfo,
        allergies: additionalInformation.value.allergies,
        diet: additionalInformation.value.diet,
        preferredName: additionalInformation.value.preferredName,
        medication: additionalInformation.value.medication
      })
      toggleUiMode('child-additional-details-editing')
    }
  }

  const onSubmit = () => {
    void updateAdditionalInformation(id, form).then((res) => {
      if (res.isSuccess) {
        clearUiMode()
        loadData()
      }
    })
  }

  return (
    <div className="additional-information-section">
      <FlexRow justifyContent="space-between">
        <H4>{i18n.childInformation.additionalInformation.title}</H4>
        <RequireRole
          oneOf={[
            'SERVICE_WORKER',
            'FINANCE_ADMIN',
            'UNIT_SUPERVISOR',
            'ADMIN',
            'STAFF',
            'SPECIAL_EDUCATION_TEACHER'
          ]}
        >
          <InlineButton
            icon={faPen}
            onClick={() => startEdit()}
            data-qa="edit-child-settings-button"
            text={i18n.common.edit}
          />
        </RequireRole>
      </FlexRow>
      {additionalInformation.mapAll({
        loading() {
          return <Loader />
        },
        failure() {
          return <div>{i18n.common.loadingFailed}</div>
        },
        success(data) {
          return (
            <>
              <LabelValueList
                spacing="small"
                contents={[
                  {
                    label:
                      i18n.childInformation.additionalInformation.preferredName,
                    value: editing ? (
                      <TextAreaInput
                        value={form.preferredName || ''}
                        onChange={(
                          event: React.ChangeEvent<HTMLTextAreaElement>
                        ) =>
                          setForm({
                            ...form,
                            preferredName: event.target.value
                          })
                        }
                        rows={textAreaRows(form.preferredName || '')}
                      />
                    ) : (
                      formatParagraphs(data.preferredName || '')
                    ),
                    valueWidth: '400px'
                  },
                  {
                    label:
                      i18n.childInformation.additionalInformation
                        .additionalInfo,
                    value: editing ? (
                      <TextAreaInput
                        value={form.additionalInfo}
                        onChange={(
                          event: React.ChangeEvent<HTMLTextAreaElement>
                        ) =>
                          setForm({
                            ...form,
                            additionalInfo: event.target.value
                          })
                        }
                        rows={textAreaRows(form.additionalInfo)}
                      />
                    ) : (
                      formatParagraphs(data.additionalInfo)
                    ),
                    valueWidth: '400px'
                  },
                  {
                    label:
                      i18n.childInformation.additionalInformation.allergies,
                    value: editing ? (
                      <TextAreaInput
                        value={form.allergies}
                        onChange={(
                          event: React.ChangeEvent<HTMLTextAreaElement>
                        ) =>
                          setForm({
                            ...form,
                            allergies: event.target.value
                          })
                        }
                        rows={textAreaRows(form.allergies)}
                        maxLength={40}
                      />
                    ) : (
                      formatParagraphs(data.allergies)
                    ),
                    valueWidth: '400px'
                  },
                  {
                    label: i18n.childInformation.additionalInformation.diet,
                    value: editing ? (
                      <TextAreaInput
                        value={form.diet}
                        onChange={(
                          event: React.ChangeEvent<HTMLTextAreaElement>
                        ) =>
                          setForm({
                            ...form,
                            diet: event.target.value
                          })
                        }
                        rows={textAreaRows(form.diet)}
                      />
                    ) : (
                      formatParagraphs(data.diet)
                    ),
                    valueWidth: '400px'
                  },
                  {
                    label:
                      i18n.childInformation.additionalInformation.medication,
                    value: editing ? (
                      <TextAreaInput
                        value={form.medication}
                        onChange={(
                          event: React.ChangeEvent<HTMLTextAreaElement>
                        ) =>
                          setForm({
                            ...form,
                            medication: event.target.value
                          })
                        }
                        rows={textAreaRows(form.medication)}
                        data-qa="medication-input"
                      />
                    ) : (
                      <span data-qa="medication">
                        {formatParagraphs(data.medication)}
                      </span>
                    ),
                    valueWidth: '400px'
                  }
                ]}
              />
              {editing && (
                <RightAlignedRow>
                  <FixedSpaceRow>
                    <Button
                      onClick={() => clearUiMode()}
                      text={i18n.common.cancel}
                    />
                    <Button
                      primary
                      disabled={false}
                      onClick={() => onSubmit()}
                      data-qa="confirm-edited-child-button"
                      text={i18n.common.confirm}
                    />
                  </FixedSpaceRow>
                </RightAlignedRow>
              )}
            </>
          )
        }
      })}
    </div>
  )
})

export default AdditionalInformation
