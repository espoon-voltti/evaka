// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, {
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState
} from 'react'

import { Result } from 'lib-common/api'
import { OtherAssistanceMeasureResponse } from 'lib-common/generated/api-types/assistance'
import { useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { scrollToRef } from 'lib-common/utils/scrolling'
import Title from 'lib-components/atoms/Title'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'

import { ChildContext, ChildState } from '../../../state/child'
import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import {
  createOtherAssistanceMeasureMutation,
  updateOtherAssistanceMeasureMutation
} from '../queries'

import { OtherAssistanceMeasureForm } from './OtherAssistanceMeasureForm'
import { OtherAssistanceMeasureRow } from './OtherAssistanceMeasureRow'
import { TitleRow } from './TitleRow'

export interface Props {
  childId: UUID
  rows: Result<OtherAssistanceMeasureResponse[]>
}

type Mode = { type: 'new' } | { type: 'edit'; id: UUID }

export const OtherAssistanceMeasureSection = React.memo(
  function OtherAssistanceMeasureSection(props: Props) {
    const { i18n } = useTranslation()
    const t = i18n.childInformation.assistance
    const { permittedActions } = useContext<ChildState>(ChildContext)
    const refSectionTop = useRef(null)

    const { mutateAsync: createOtherAssistanceMeasure } = useMutationResult(
      createOtherAssistanceMeasureMutation
    )
    const { mutateAsync: updateOtherAssistanceMeasure } = useMutationResult(
      updateOtherAssistanceMeasureMutation
    )
    const [mode, setMode] = useState<Mode | undefined>(undefined)
    const clearMode = useCallback(() => setMode(undefined), [setMode])

    const childId = props.childId
    const rowsResult = useMemo(
      () =>
        props.rows.map((rows) =>
          orderBy(
            rows,
            [
              (row) => row.data.type,
              (row) => row.data.validDuring.start.formatIso()
            ],
            ['asc', 'desc']
          )
        ),
      [props.rows]
    )

    return renderResult(rowsResult, (rows) => (
      <div ref={refSectionTop}>
        <TitleRow>
          <Title size={4}>{t.otherAssistanceMeasure.title}</Title>
          {permittedActions.has('CREATE_OTHER_ASSISTANCE_MEASURE') && (
            <AddButton
              flipped
              text={t.otherAssistanceMeasure.create}
              onClick={() => {
                setMode({ type: 'new' })
                scrollToRef(refSectionTop)
              }}
              disabled={mode?.type !== undefined}
              data-qa="other-assistance-measure-create-btn"
            />
          )}
        </TitleRow>
        {(mode?.type || rows.length > 0) && (
          <Table>
            <Thead>
              <Tr>
                <Th style={{ width: '30%' }}>
                  {t.fields.otherAssistanceMeasureType}
                </Th>
                <Th style={{ width: '30%' }}>{t.fields.validDuring}</Th>
                <Th style={{ width: '20%' }}>{t.fields.status}</Th>
                <Th />
              </Tr>
            </Thead>
            <Tbody>
              {mode?.type === 'new' ? (
                <OtherAssistanceMeasureForm
                  onSubmit={(data) =>
                    createOtherAssistanceMeasure({ childId, data })
                  }
                  onClose={clearMode}
                />
              ) : undefined}
              {rows.map((row) =>
                mode?.type === 'edit' && mode.id === row.data.id ? (
                  <OtherAssistanceMeasureForm
                    key={row.data.id}
                    otherAssistanceMeasure={row.data}
                    onSubmit={(data) =>
                      updateOtherAssistanceMeasure({
                        childId,
                        id: row.data.id,
                        data
                      })
                    }
                    onClose={clearMode}
                  />
                ) : (
                  <OtherAssistanceMeasureRow
                    key={row.data.id}
                    otherAssistanceMeasure={row}
                    onEdit={() => {
                      setMode({ type: 'edit', id: row.data.id })
                      scrollToRef(refSectionTop)
                    }}
                  />
                )
              )}
            </Tbody>
          </Table>
        )}
      </div>
    ))
  }
)
