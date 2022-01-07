// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { faExclamationTriangle } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useContext } from 'react'
import styled from 'styled-components'
import { AdRole } from 'lib-common/api-types/employee-auth'
import colors from 'lib-customizations/common'
import { faCopy, faPen, faSync, faTrash } from 'lib-icons'
import StatusLabel from '../../components/common/StatusLabel'
import Tooltip from '../../components/common/Tooltip'
import { UserContext } from '../../state/user'
import { DateRangeOpen, getStatusLabelByDateRange } from '../../utils/date'
import { requireRole } from '../../utils/roles'

const ToolbarWrapper = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
`

const ToolbarButton = styled.button`
  cursor: ${(p) => (p.disabled ? 'not-allowed' : 'pointer')};
  margin-left: 10px;
  background: none;
  border: none;
`

const ToolbarStatus = styled.div`
  width: 100px;
  margin-left: 20px;
  display: flex;
  justify-content: flex-end;
`

interface Warning {
  text: string
  tooltipId: string
}

interface ToolbarProps {
  dateRange?: DateRangeOpen
  conflict?: boolean
  warning?: Warning
  disableAll?: boolean
  onEdit?: () => undefined | void
  onDelete?: () => undefined | void
  onRetry?: () => undefined | void
  onCopy?: () => undefined | void
  editableFor?: AdRole[]
  deletableFor?: AdRole[]
  dataQaEdit?: string
  dataQaDelete?: string
  dataQaRetry?: string
  dataQaCopy?: string
  dataQa?: string
}
function Toolbar({
  dateRange,
  onEdit,
  onDelete,
  onRetry,
  onCopy,
  warning,
  conflict,
  disableAll,
  editableFor,
  deletableFor,
  dataQaEdit,
  dataQaDelete,
  dataQaRetry,
  dataQaCopy,
  dataQa
}: ToolbarProps) {
  const { roles } = useContext(UserContext)

  const editAllowed: boolean =
    editableFor === undefined || requireRole(roles, ...editableFor)
  const deleteAllowed: boolean =
    deletableFor === undefined || requireRole(roles, ...deletableFor)

  return (
    <ToolbarWrapper data-qa={dataQa}>
      {warning && (
        <Tooltip
          tooltipId={warning?.tooltipId}
          tooltipText={warning?.text}
          place="right"
        >
          <FontAwesomeIcon
            icon={faExclamationTriangle}
            size="1x"
            color={colors.accents.warningOrange}
          />
        </Tooltip>
      )}

      {onRetry && editAllowed && (
        <ToolbarButton
          onClick={() => onRetry()}
          data-qa={dataQaRetry}
          disabled={disableAll}
        >
          <FontAwesomeIcon
            icon={faSync}
            color={disableAll ? colors.greyscale.medium : colors.main.primary}
            size="lg"
          />
        </ToolbarButton>
      )}

      {onCopy && editAllowed && (
        <ToolbarButton
          onClick={() => onCopy()}
          data-qa={dataQaCopy}
          disabled={disableAll}
        >
          <FontAwesomeIcon
            icon={faCopy}
            color={disableAll ? colors.greyscale.medium : colors.main.primary}
            size="lg"
          />
        </ToolbarButton>
      )}

      {onEdit && editAllowed && (
        <ToolbarButton
          onClick={() => onEdit()}
          data-qa={dataQaEdit}
          disabled={disableAll}
        >
          <FontAwesomeIcon
            icon={faPen}
            color={disableAll ? colors.greyscale.medium : colors.main.primary}
            size="lg"
          />
        </ToolbarButton>
      )}

      {onDelete && deleteAllowed && (
        <ToolbarButton
          onClick={() => onDelete()}
          data-qa={dataQaDelete}
          disabled={disableAll}
        >
          <FontAwesomeIcon
            icon={faTrash}
            color={disableAll ? colors.greyscale.medium : colors.main.primary}
            size="lg"
          />
        </ToolbarButton>
      )}

      {dateRange && (
        <ToolbarStatus>
          <StatusLabel
            status={
              conflict ? 'conflict' : getStatusLabelByDateRange(dateRange)
            }
          />
        </ToolbarStatus>
      )}
    </ToolbarWrapper>
  )
}

export default Toolbar
