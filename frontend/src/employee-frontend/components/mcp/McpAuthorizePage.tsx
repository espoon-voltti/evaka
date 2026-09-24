// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'
import styled from 'styled-components'
import { useSearchParams } from 'wouter'

import { combine } from 'lib-common/api'
import type { McpClientId } from 'lib-common/generated/api-types/shared'
import { fromUuid } from 'lib-common/id-type'
import { useQueryResult } from 'lib-common/query'
import { Button } from 'lib-components/atoms/buttons/Button'
import LinkButton from 'lib-components/atoms/buttons/LinkButton'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import Radio from 'lib-components/atoms/form/Radio'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { AlertBox, InfoBox } from 'lib-components/molecules/MessageBoxes'
import { H1, H2, Label, P, Strong } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { getLoginUrl } from '../../api/auth'
import { UserContext } from '../../state/user'
import { hasGlobalAction } from '../../utils/roles'
import { useTitle } from '../../utils/useTitle'
import { renderResult } from '../async-rendering'

import {
  createMcpAuthorizationMutation,
  mcpConfigQuery,
  mcpOAuthClientQuery
} from './queries'
import { useMcpTranslation } from './translations'

const Center = styled.div`
  display: flex;
  justify-content: center;
  margin: 40px 0;
`

const Mono = styled.code`
  word-break: break-all;
`

interface AuthorizationParams {
  clientId: McpClientId
  redirectUri: string
  codeChallenge: string
  codeChallengeMethod: string
  state: string | null
  scope: string | null
  resource: string | null
}

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

function parseParams(search: URLSearchParams): AuthorizationParams | null {
  const clientId = search.get('client_id')
  const redirectUri = search.get('redirect_uri')
  const codeChallenge = search.get('code_challenge')
  const codeChallengeMethod = search.get('code_challenge_method') ?? 'plain'
  const responseType = search.get('response_type') ?? 'code'
  if (
    !clientId ||
    !uuidPattern.test(clientId) ||
    !redirectUri ||
    !codeChallenge ||
    codeChallengeMethod !== 'S256' ||
    responseType !== 'code'
  ) {
    return null
  }
  return {
    clientId: fromUuid<McpClientId>(clientId),
    redirectUri,
    codeChallenge,
    codeChallengeMethod,
    state: search.get('state'),
    scope: search.get('scope'),
    resource: search.get('resource')
  }
}

/**
 * OAuth 2.1 authorization endpoint (consent page) for MCP clients. The AI tool opens this page in
 * the browser; the logged-in admin approves the request and chooses how long the access is valid.
 */
export default React.memo(function McpAuthorizePage() {
  const mcp = useMcpTranslation()
  useTitle(mcp.authorize.title)
  const { loaded, loggedIn, user } = useContext(UserContext)
  const [searchParams] = useSearchParams()
  const params = useMemo(() => parseParams(searchParams), [searchParams])

  if (!loaded) return null

  return (
    <Container>
      <ContentArea $opaque>
        <H1>{mcp.authorize.title}</H1>
        {params === null ? (
          <AlertBox message={mcp.authorize.invalidRequest} wide />
        ) : !loggedIn || !user ? (
          <>
            <P>{mcp.authorize.loginRequired}</P>
            <Center>
              <LinkButton data-qa="mcp-login-btn" href={getLoginUrl('ad')}>
                <span>{mcp.authorize.login}</span>
              </LinkButton>
            </Center>
          </>
        ) : !hasGlobalAction(user, 'MANAGE_MCP_AUTHORIZATIONS') ? (
          <AlertBox message={mcp.authorize.adminRequired} wide />
        ) : (
          <ConsentForm params={params} userName={user.name} />
        )}
      </ContentArea>
    </Container>
  )
})

const ConsentForm = React.memo(function ConsentForm({
  params,
  userName
}: {
  params: AuthorizationParams
  userName: string
}) {
  const mcp = useMcpTranslation()
  const client = useQueryResult(
    mcpOAuthClientQuery({
      clientId: params.clientId,
      redirectUri: params.redirectUri
    })
  )
  const config = useQueryResult(mcpConfigQuery())
  const [validityDays, setValidityDays] = useState<number | null>(null)
  const [approved, setApproved] = useState(false)

  const deny = () => {
    const url = new URL(params.redirectUri)
    url.searchParams.set('error', 'access_denied')
    if (params.state) url.searchParams.set('state', params.state)
    window.location.assign(url.toString())
  }

  if (client.isFailure && client.statusCode === 404) {
    return (
      <AlertBox
        message={mcp.authorize.unknownClient}
        wide
        data-qa="mcp-unknown-client"
      />
    )
  }

  return renderResult(combine(client, config), ([client, config]) => {
    if (!client.redirectUriAccepted) {
      return (
        <AlertBox
          message={mcp.authorize.redirectUriNotRegistered}
          wide
          data-qa="mcp-redirect-uri-rejected"
        />
      )
    }
    const selectedDays = validityDays ?? config.defaultValidityDays
    return (
      <FixedSpaceColumn $spacing="L" data-qa="mcp-consent">
        <P>
          <Strong data-qa="mcp-client-name">{client.clientName}</Strong>{' '}
          {mcp.authorize.clientRequests}
        </P>
        <div>
          <Label>{mcp.authorize.permissions}</Label>
          <ul>
            {mcp.authorize.permissionList.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
        <div>
          <Label>{mcp.authorize.redirectUri}</Label>
          <div>
            <Mono>{params.redirectUri}</Mono>
          </div>
        </div>
        <div>
          <H2 $noMargin>{mcp.authorize.validity}</H2>
          <Gap $size="s" />
          <FixedSpaceColumn $spacing="xs">
            {config.validityOptionsDays.map((days) => (
              <Radio
                key={days}
                label={mcp.authorize.validityDays(days)}
                checked={selectedDays === days}
                onChange={() => setValidityDays(days)}
                data-qa={`mcp-validity-${days}`}
              />
            ))}
          </FixedSpaceColumn>
        </div>
        <InfoBox
          message={`${userName}: ${mcp.authorize.environmentWarning}`}
          wide
          noMargin
        />
        {approved ? (
          <P>{mcp.authorize.approved}</P>
        ) : (
          <FixedSpaceRow $spacing="m">
            <Button
              text={mcp.authorize.deny}
              onClick={deny}
              data-qa="mcp-deny-btn"
            />
            <MutateButton
              primary
              text={mcp.authorize.approve}
              mutation={createMcpAuthorizationMutation}
              onClick={() => ({
                body: {
                  clientId: params.clientId,
                  redirectUri: params.redirectUri,
                  codeChallenge: params.codeChallenge,
                  codeChallengeMethod: params.codeChallengeMethod,
                  state: params.state,
                  scope: params.scope,
                  resource: params.resource,
                  validityDays: selectedDays
                }
              })}
              onSuccess={({ redirectUrl }) => {
                setApproved(true)
                window.location.assign(redirectUrl)
              }}
              data-qa="mcp-approve-btn"
            />
          </FixedSpaceRow>
        )}
      </FixedSpaceColumn>
    )
  })
})
