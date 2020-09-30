// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import { useTranslation } from '~/state/i18n'
import { isFailure, isLoading, isSuccess, Loading } from '~api'
import {
  getAdditionalInformation,
  updateAdditionalInformation
} from '~api/child/additional-information'
import { ChildContext } from '~/state/child'
import { Button, Buttons, Loader, TextArea } from '~components/shared/alpha'
import { UUID } from '~/types'
import LabelValueList from '~components/common/LabelValueList'
import ToolbarAccordion from '~components/common/ToolbarAccordion'
import styled from 'styled-components'
import { faPen } from 'icon-set'
import { UIContext, UiState } from '~state/ui'
import { AdditionalInformation } from '~types/child'
import { formatParagraphs } from '~utils/html-utils'
import { textAreaRows } from '~components/utils'
import { RequireRole } from 'utils/roles'

const FlexContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  flex-direction: row-reverse;
  justify-content: space-between;
  align-items: baseline;
`

const TextAreaInput = styled(TextArea)`
  width: 100%;
`

const RightAlignedRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  padding-top: 20px;
`

const ButtonsContainer = styled(Buttons)`
  margin: 20px 0;
`

interface Props {
  id: UUID
}

const AdditionalInformation = React.memo(function AdditionalInformation({
  id
}: Props) {
  const { i18n } = useTranslation()
  const { additionalInformation, setAdditionalInformation } = useContext(
    ChildContext
  )
  const { uiMode, toggleUiMode, clearUiMode } = useContext<UiState>(UIContext)
  const [toggled, setToggled] = useState(true)
  const [form, setForm] = useState<AdditionalInformation>({
    additionalInfo: '',
    allergies: '',
    diet: ''
  })

  const editing = uiMode == 'child-additional-details-editing'

  const loadData = () => {
    setAdditionalInformation(Loading())
    void getAdditionalInformation(id).then((additionalInformation) => {
      setAdditionalInformation(additionalInformation)
    })
  }

  useEffect(() => {
    loadData()
  }, [id])

  const startEdit = () => {
    if (isSuccess(additionalInformation)) {
      setForm({
        additionalInfo: additionalInformation.data.additionalInfo,
        allergies: additionalInformation.data.allergies,
        diet: additionalInformation.data.diet
      })
      toggleUiMode('child-additional-details-editing')
    }
  }

  const onSubmit = () => {
    void updateAdditionalInformation(id, form).then((res) => {
      if (isSuccess(res)) {
        clearUiMode()
        loadData()
      }
    })
  }

  function renderListItems() {
    if (isLoading(additionalInformation)) {
      return <Loader />
    } else if (isFailure(additionalInformation)) {
      return <div>{i18n.common.loadingFailed}</div>
    } else {
      return (
        <>
          <FlexContainer>
            <RequireRole
              oneOf={[
                'SERVICE_WORKER',
                'FINANCE_ADMIN',
                'UNIT_SUPERVISOR',
                'ADMIN'
              ]}
            >
              <Button
                plain
                type="button"
                width="narrow"
                icon={faPen}
                iconSize="lg"
                onClick={() => startEdit()}
                dataQa="edit-child-settings-button"
              >
                {i18n.common.edit}
              </Button>
            </RequireRole>
            <div />
            <LabelValueList
              spacing="small"
              contents={[
                {
                  label:
                    i18n.childInformation.additionalInformation.additionalInfo,
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
                    formatParagraphs(additionalInformation.data.additionalInfo)
                  ),
                  valueWidth: '400px'
                },
                {
                  label: i18n.childInformation.additionalInformation.allergies,
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
                    formatParagraphs(additionalInformation.data.allergies)
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
                    formatParagraphs(additionalInformation.data.diet)
                  ),
                  valueWidth: '400px'
                }
              ]}
            />
          </FlexContainer>
          {editing && (
            <RightAlignedRow>
              <ButtonsContainer>
                <Button onClick={() => clearUiMode()}>
                  {i18n.common.cancel}
                </Button>
                <Button
                  primary
                  disabled={false}
                  onClick={() => onSubmit()}
                  dataQa="confirm-edited-child-button"
                >
                  {i18n.common.confirm}
                </Button>
              </ButtonsContainer>
            </RightAlignedRow>
          )}
        </>
      )
    }
  }

  return (
    <div className="additional-information-section">
      <ToolbarAccordion
        title={i18n.childInformation.additionalInformation.title}
        onToggle={() => setToggled((prev) => !prev)}
        open={toggled}
        showBorder
      >
        {renderListItems()}
      </ToolbarAccordion>
    </div>
  )
})

export default AdditionalInformation
