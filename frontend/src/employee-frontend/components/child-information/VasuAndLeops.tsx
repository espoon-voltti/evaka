// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'
import { useHistory } from 'react-router'
import styled from 'styled-components'
import { combine, Result } from 'lib-common/api'
import { formatDate } from 'lib-common/date'
import LocalDate from 'lib-common/local-date'
import { useApiState, useRestApi } from 'lib-common/utils/useRestApi'
import { AddButtonRow } from 'lib-components/atoms/buttons/AddButton'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import Radio from 'lib-components/atoms/form/Radio'
import ErrorSegment from 'lib-components/atoms/state/ErrorSegment'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Tr } from 'lib-components/layout/Table'
import FullScreenDimmedSpinner from 'lib-components/molecules/FullScreenDimmedSpinner'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { UIContext } from '../../state/ui'
import { VasuStateChip } from '../common/VasuStateChip'
import {
  VasuDocumentSummary,
  VasuTemplateSummary
} from 'lib-common/generated/api-types/vasu'
import {
  createVasuDocument,
  getVasuDocumentSummaries,
  updateDocumentState
} from '../vasu/api'
import { getLastPublished } from '../vasu/vasu-events'
import { getVasuTemplateSummaries } from '../vasu/templates/api'
import { RequireRole } from '../../utils/roles'
import { renderResult, UnwrapResult } from '../async-rendering'
import { UUID } from 'lib-common/types'
import {
  DaycarePlacementWithDetails,
  PlacementType
} from 'lib-common/generated/api-types/placement'

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
      title={i18n.childInformation.vasu.init.chooseTemplate}
      resolveAction={() => onSelect(templateId)}
      resolveDisabled={!templateId || loading}
      resolveLabel={i18n.common.select}
      rejectAction={onClose}
      rejectLabel={i18n.common.cancel}
    >
      {templates.map((t) => (
        <Radio
          key={t.id}
          disabled={loading}
          checked={t.id === templateId}
          label={`${t.name} (${t.valid.format()})`}
          onChange={() => setTemplateId(t.id)}
        />
      ))}
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

const preschoolPlacementTypes: readonly PlacementType[] = [
  'PRESCHOOL',
  'PRESCHOOL_DAYCARE',
  'PREPARATORY',
  'PREPARATORY_DAYCARE'
]

function VasuInitialization({
  childId,
  allowCreation,
  placements
}: {
  childId: UUID
  allowCreation: boolean
  placements: Result<DaycarePlacementWithDetails[]>
}) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)
  const history = useHistory()

  const [templates, setTemplates] = useState<Result<VasuTemplateSummary[]>>()
  const loadTemplates = useRestApi(getValidVasuTemplateSummaries, setTemplates)

  const filteredTemplates = useMemo(
    () =>
      templates
        ? combine(placements, templates).map(([placements, templates]) => {
            const placementsNotInFuture = placements.filter(
              ({ startDate }) => !startDate.isAfter(LocalDate.today())
            )

            const useableType = placementsNotInFuture.some(({ type }) =>
              preschoolPlacementTypes.includes(type)
            )
              ? 'PRESCHOOL'
              : 'DAYCARE'

            return templates.filter(({ type }) => type === useableType)
          })
        : undefined,
    [placements, templates]
  )

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
      if (
        filteredTemplates?.isSuccess &&
        filteredTemplates.value.length === 1
      ) {
        createVasu(childId, filteredTemplates.value[0].id)
      }
    },
    [childId, createVasu, filteredTemplates]
  )

  return (
    <InitializationContainer>
      <AddButtonRow
        onClick={loadTemplates}
        disabled={
          !allowCreation ||
          filteredTemplates?.isSuccess ||
          filteredTemplates?.isLoading
        }
        text={i18n.childInformation.vasu.createNew}
        data-qa="add-new-vasu-button"
      />
      {filteredTemplates && (
        <UnwrapResult
          result={filteredTemplates}
          loading={() => <FullScreenDimmedSpinner />}
          failure={() => (
            <ErrorSegment title={i18n.childInformation.vasu.init.error} />
          )}
        >
          {(value) => {
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
          }}
        </UnwrapResult>
      )}
    </InitializationContainer>
  )
}

interface Props {
  id: UUID
  startOpen: boolean
}

export default React.memo(function VasuAndLeops({
  id: childId,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions, placements } = useContext(ChildContext)
  const [vasus, loadVasus] = useApiState(
    () => getVasuDocumentSummaries(childId),
    [childId]
  )
  const history = useHistory()

  const [open, setOpen] = useState(startOpen)

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

  const allowCreation = useMemo(
    () =>
      vasus
        .map((docs) => docs.every((doc) => doc.documentState === 'CLOSED'))
        .getOrElse(false),
    [vasus]
  )

  const noPermission = useMemo(
    () =>
      !permittedActions.has('READ_VASU_DOCUMENT') ||
      placements
        .map(
          (ps) =>
            !ps.some((placement) =>
              placement.daycare.enabledPilotFeatures.includes(
                'VASU_AND_PEDADOC'
              )
            )
        )
        .getOrElse(true),
    [permittedActions, placements]
  )
  if (noPermission) {
    return null
  }

  return (
    <div>
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.vasu.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="vasu-and-leops-collapsible"
      >
        <VasuInitialization
          childId={childId}
          allowCreation={allowCreation}
          placements={placements}
        />
        {renderResult(vasus, (vasus) => (
          <Table>
            <Tbody>
              {vasus.map((vasu) => (
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
                            }).finally(() => loadVasus())
                          }
                          text={
                            i18n.vasu.transitions.RETURNED_TO_REVIEWED
                              .buttonText
                          }
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
              ))}
            </Tbody>
          </Table>
        ))}
      </CollapsibleContentArea>
    </div>
  )
})
