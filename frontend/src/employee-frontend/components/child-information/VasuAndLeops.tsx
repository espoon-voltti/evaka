// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Loader from 'lib-components/atoms/Loader'
import React, { useCallback, useContext, useEffect, useState } from 'react'
import { useHistory } from 'react-router'
import styled from 'styled-components'
import { Result } from 'lib-common/api'
import { formatDate } from 'lib-common/date'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Radio from 'lib-components/atoms/form/Radio'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Tr } from 'lib-components/layout/Table'
import { FullScreenDimmedSpinner } from 'lib-components/molecules/FullScreenDimmedSpinner'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { H2, H3 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { UUID } from '../../types'
import { VasuStateChip } from '../common/VasuStateChip'
import {
  createVasuDocument,
  getVasuDocumentSummaries,
  updateDocumentState,
  VasuDocumentSummary
} from '../vasu/api'
import { getLastPublished } from '../vasu/vasu-events'
import {
  getVasuTemplateSummaries,
  VasuTemplateSummary
} from '../vasu/templates/api'
import { RequireRole } from '../../utils/roles'

const StateCell = styled(Td)`
  display: flex;
  justify-content: flex-end;
`

interface TemplateSelectionModalProps {
  loading: boolean
  onClose: () => void
  onSelect: (id: UUID) => void
  templates: VasuTemplateSummary[]
}

function TemplateSelectionModal({
  loading,
  onClose,
  onSelect,
  templates
}: TemplateSelectionModalProps) {
  const { i18n } = useTranslation()
  const [templateId, setTemplateId] = useState('')
  return (
    <FormModal
      resolve={{
        action: () => onSelect(templateId),
        disabled: !templateId || loading,
        label: i18n.common.select
      }}
      reject={{ action: onClose, label: i18n.common.cancel }}
    >
      <H3>{i18n.childInformation.vasu.init.chooseTemplate}</H3>
      <div>
        {templates.map((t) => (
          <Radio
            key={t.id}
            disabled={loading}
            checked={t.id === templateId}
            label={`${t.name} (${t.valid.format()})`}
            onChange={() => setTemplateId(t.id)}
          />
        ))}
      </div>
    </FormModal>
  )
}

const InitializationContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-bottom: ${defaultMargins.s};
`

const getValidVasuTemplateSummaries = () => getVasuTemplateSummaries(true)

function VasuInitialization({
  childId,
  allowCreation
}: {
  childId: UUID
  allowCreation: boolean
}) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)
  const history = useHistory()

  const [templates, setTemplates] = useState<Result<VasuTemplateSummary[]>>()
  const loadTemplates = useRestApi(getValidVasuTemplateSummaries, setTemplates)

  const [initializing, setInitializing] = useState(false)

  const handleVasuResult = useCallback(
    (res: Result<UUID>) =>
      res.mapAll({
        failure: () => {
          setErrorMessage({
            type: 'error',
            title: i18n.childInformation.vasu.init.error,
            text: i18n.common.tryAgain,
            resolveLabel: i18n.common.ok
          })
          setInitializing(false)
          setTemplates(undefined)
        },
        loading: () => setInitializing(true),
        success: (id) => history.push(`/vasu/${id}`)
      }),
    [history, i18n, setErrorMessage]
  )
  const createVasu = useRestApi(createVasuDocument, handleVasuResult)

  useEffect(
    function autoSelectTemplate() {
      if (templates?.isSuccess && templates.value.length === 1) {
        createVasu(childId, templates.value[0].id)
      }
    },
    [childId, createVasu, templates]
  )

  return (
    <InitializationContainer>
      <AddButtonRow
        onClick={loadTemplates}
        disabled={
          !allowCreation || templates?.isSuccess || templates?.isLoading
        }
        text={i18n.childInformation.vasu.createNew}
      />
      {templates?.mapAll({
        failure() {
          return <ErrorSegment title={i18n.childInformation.vasu.init.error} />
        },
        loading() {
          return <FullScreenDimmedSpinner />
        },
        success(value) {
          if (value.length === 0) {
            return <div>{i18n.childInformation.vasu.init.noTemplates}</div>
          }
          if (value.length === 1) {
            return null // the template is selected automatically
          }
          return (
            <TemplateSelectionModal
              loading={initializing}
              onClose={() => setTemplates(undefined)}
              onSelect={(id) => createVasu(childId, id)}
              templates={value}
            />
          )
        }
      })}
    </InitializationContainer>
  )
}

interface Props {
  id: UUID
  startOpen: boolean
}

const VasuAndLeops = React.memo(function VasuAndLeops({
  id: childId,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { vasus, setVasus } = useContext(ChildContext)
  const history = useHistory()

  const [open, setOpen] = useState(startOpen)

  const loadVasus = useRestApi(getVasuDocumentSummaries, setVasus)
  useEffect(() => loadVasus(childId), [childId, loadVasus])

  const getDates = ({ modifiedAt, events }: VasuDocumentSummary): string => {
    const publishedAt = getLastPublished(events)
    return [
      `${i18n.childInformation.vasu.modified}: ${formatDate(modifiedAt)}`,
      ...(publishedAt
        ? [
            `${i18n.childInformation.vasu.published}: ${formatDate(
              publishedAt
            )}`.toLowerCase()
          ]
        : [])
    ].join(', ')
  }

  const renderSummaries = () =>
    vasus
      .map((value) =>
        value.map((vasu) => (
          <Tr key={vasu.id}>
            <Td>
              <InlineButton
                onClick={() => history.push(`/vasu/${vasu.id}`)}
                text={vasu.name}
              />
            </Td>
            <Td>{getDates(vasu)}</Td>
            <StateCell>
              <VasuStateChip
                state={vasu.documentState}
                labels={i18n.vasu.states}
              />
            </StateCell>
            <Td>
              {vasu.documentState === 'CLOSED' ? (
                <RequireRole oneOf={['ADMIN']}>
                  <InlineButton
                    onClick={() =>
                      void updateDocumentState({
                        documentId: vasu.id,
                        eventType: 'RETURNED_TO_REVIEWED'
                      }).finally(() => loadVasus(childId))
                    }
                    text={i18n.vasu.transitions.RETURNED_TO_REVIEWED.buttonText}
                  />
                </RequireRole>
              ) : (
                <InlineButton
                  onClick={() => history.push(`/vasu/${vasu.id}/edit`)}
                  text={i18n.common.edit}
                />
              )}
            </Td>
          </Tr>
        ))
      )
      .getOrElse(null)

  const allowCreation = vasus
    .map((docs) => docs.every((doc) => doc.documentState === 'CLOSED'))
    .getOrElse(false)

  return (
    <div>
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.vasu.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
      >
        <VasuInitialization childId={childId} allowCreation={allowCreation} />
        <Table>
          <Tbody>{renderSummaries()}</Tbody>
        </Table>
        {vasus.isLoading && <Loader />}
        {vasus.isFailure && <div>{i18n.common.loadingFailed}</div>}
      </CollapsibleContentArea>
    </div>
  )
})

export default VasuAndLeops
