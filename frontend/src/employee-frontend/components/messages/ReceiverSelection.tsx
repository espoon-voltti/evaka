import React, { useEffect, useState } from 'react'
import { UUID } from 'types'
import styled from 'styled-components'
import { Loading, Result } from '@evaka/lib-common/api'
import { useRestApi } from '@evaka/lib-common/utils/useRestApi'
import { H1 } from '@evaka/lib-components/typography'
import {
  Table,
  Tr,
  Th,
  Td,
  Thead,
  Tbody
} from '@evaka/lib-components/layout/Table'
import { defaultMargins } from '@evaka/lib-components/white-space'
import Checkbox from '@evaka/lib-components/atoms/form/Checkbox'
import Button from '@evaka/lib-components/atoms/buttons/Button'
import colors from '@evaka/lib-components/colors'
import { getReceivers } from './api'
import { ReceiverChild, ReceiverGroup } from './types'
import { useTranslation } from '../../state/i18n'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faAngleDown, faAngleUp } from '@evaka/lib-icons'

interface Props {
  unitId: UUID
}
export default React.memo(function ReceiverSelection({ unitId }: Props) {
  const { i18n } = useTranslation()

  const [receiversResult, setReceiversResult] = useState<
    Result<ReceiverGroup[]>
  >(Loading.of())

  const loadReceivers = useRestApi(getReceivers, setReceiversResult)
  useEffect(() => loadReceivers(unitId), [])

  const receivers: ReceiverGroup[] = receiversResult.isSuccess
    ? receiversResult.value
    : []

  const allReceivers: UUID[] = receivers.flatMap((receiverGroup) =>
    receiverGroup.receiverChildren.map(({ childId }: ReceiverChild) => childId)
  )

  const [collapsedGroups, setCollapsedGroups] = useState<UUID[]>([])

  const isCollapsed = (groupId: UUID) => collapsedGroups.indexOf(groupId) > -1

  const setCollapsed = (groupId: UUID) =>
    setCollapsedGroups((old) => [...old, groupId])

  const setNotCollapsed = (groupId: UUID) =>
    setCollapsedGroups((old) => old.filter((id) => id !== groupId))

  const toggleCollapsed = (groupId: UUID) =>
    isCollapsed(groupId) ? setNotCollapsed(groupId) : setCollapsed(groupId)

  const [checkedReceivers, setCheckedReceivers] = useState<UUID[]>([])

  const checkGroup = (checkedGroup: ReceiverGroup) => {
    const checkedChildIds = checkedGroup.receiverChildren.map(
      (child) => child.childId
    )
    setCheckedReceivers((old) => {
      return [...old, ...checkedChildIds]
    })
  }

  const unCheckGroup = (receiverGroup: ReceiverGroup) => {
    const childIds = receiverGroup.receiverChildren.map(
      (child) => child.childId
    )
    setCheckedReceivers((old) =>
      old.filter((earlierId) => childIds.indexOf(earlierId) === -1)
    )
  }

  const isGroupChecked = (receiverGroup: ReceiverGroup) => {
    const receiverIds = receiverGroup.receiverChildren.map(
      ({ childId }: ReceiverChild) => childId
    )
    return receiverIds.every(
      (receiverId) => checkedReceivers.indexOf(receiverId) > -1
    )
  }

  const isAllChecked = () =>
    allReceivers.every(
      (receiverId) => checkedReceivers.indexOf(receiverId) > -1
    )

  const checkAll = () => setCheckedReceivers(allReceivers)

  const isChildChecked = (childId: UUID) =>
    checkedReceivers.indexOf(childId) > -1

  interface ReceiverTriplet {
    unitId: UUID
    groupId?: UUID
    childId?: UUID
  }

  const receiverTriplets: ReceiverTriplet[] = (() => {
    if (isAllChecked()) return [{ unitId }]

    const checkedGroups = receivers.filter((receiverGroup: ReceiverGroup) =>
      isGroupChecked(receiverGroup)
    )
    const unCheckedGroups = receivers.filter(
      (receiverGroup: ReceiverGroup) => !isGroupChecked(receiverGroup)
    )

    const getGroupChildren = (groups: ReceiverGroup[]): UUID[] =>
      groups.flatMap((receiverGroup) =>
        receiverGroup.receiverChildren.map(
          ({ childId }: ReceiverChild) => childId
        )
      )

    const isChildInSomeCheckedGroup = (childId: UUID): boolean =>
      getGroupChildren(checkedGroups).indexOf(childId) > -1

    const checkedChildrenOutsideCheckedGroups = getGroupChildren(
      unCheckedGroups
    ).filter(
      (childId: UUID) =>
        !isChildInSomeCheckedGroup(childId) && isChildChecked(childId)
    )

    const selectedGroupsAsReceiverData = checkedGroups.map(
      ({ groupId }: ReceiverGroup) => ({ unitId, groupId })
    )

    const otherChildrenAsReceiverData = receivers
      .flatMap((receiverGroup) =>
        receiverGroup.receiverChildren.map(({ childId }: ReceiverChild) => ({
          unitId,
          groupId: receiverGroup.groupId,
          childId
        }))
      )
      .filter(
        ({ childId }) =>
          checkedChildrenOutsideCheckedGroups.indexOf(childId) > -1
      )

    return [...selectedGroupsAsReceiverData, ...otherChildrenAsReceiverData]
  })()

  return (
    <Container>
      <H1>{i18n.messages.receiverSelection.title}</H1>
      <Table>
        <Thead>
          <Tr>
            <Th>{i18n.messages.receiverSelection.childName}</Th>
            <Th>{i18n.messages.receiverSelection.childDob}</Th>
            <Th>{i18n.messages.receiverSelection.receivers}</Th>
            <Th>
              <Checkbox
                label={''}
                checked={isAllChecked()}
                onChange={(checked) =>
                  checked ? checkAll() : setCheckedReceivers([])
                }
              />
            </Th>
          </Tr>
        </Thead>
        {receiversResult.isSuccess &&
          receiversResult.value.map((receiverGroup) => (
            <Tbody key={receiverGroup.groupId}>
              <Tr>
                <Td
                  colSpan={3}
                  onClick={() => toggleCollapsed(receiverGroup.groupId)}
                >
                  <IconWrapper>
                    <FontAwesomeIcon
                      icon={
                        isCollapsed(receiverGroup.groupId)
                          ? faAngleUp
                          : faAngleDown
                      }
                    />
                  </IconWrapper>
                  <strong>{receiverGroup.groupName}</strong>
                </Td>
                <Td>
                  <Checkbox
                    label={''}
                    checked={isGroupChecked(receiverGroup)}
                    onChange={(checked) =>
                      checked
                        ? checkGroup(receiverGroup)
                        : unCheckGroup(receiverGroup)
                    }
                  />
                </Td>
              </Tr>
              {!isCollapsed(receiverGroup.groupId) &&
                receiverGroup.receiverChildren.map((receiverChild) => (
                  <Tr key={receiverChild.childId}>
                    <Td>
                      {receiverChild.childFirstName}{' '}
                      {receiverChild.childLastName}
                    </Td>
                    <Td>{receiverChild.childDateOfBirth.format()}</Td>
                    <Td>
                      {receiverChild.receivers.map(
                        ({
                          receiverId,
                          receiverFirstName,
                          receiverLastName
                        }) => (
                          <div key={receiverId}>
                            {receiverFirstName} {receiverLastName}
                          </div>
                        )
                      )}
                    </Td>
                    <Td>
                      <Checkbox
                        label={''}
                        checked={isChildChecked(receiverChild.childId)}
                        onChange={(checked) =>
                          setCheckedReceivers((old) =>
                            checked
                              ? [...old, receiverChild.childId]
                              : old.filter((id) => id !== receiverChild.childId)
                          )
                        }
                        dataQa={`check-receiver-${receiverChild.childId}`}
                      />
                    </Td>
                  </Tr>
                ))}
            </Tbody>
          ))}
      </Table>
      {receiverTriplets.map(({ unitId, groupId, childId }) => (
        <div key={`${unitId}-${groupId}-${childId}`}>
          {unitId} {groupId} {childId}
        </div>
      ))}
      <Button text={i18n.messages.receiverSelection.confirmText} primary />
    </Container>
  )
})

const Container = styled.div`
  flex-grow: 1;
  min-height: 500px;
  overflow-y: auto;
  padding: ${defaultMargins.m};
  background-color: ${colors.greyscale.white};
  display: flex;
  flex-direction: column;
`

const IconWrapper = styled.span`
  color: ${colors.greyscale.medium};
  margin-right: ${defaultMargins.s};
`
