// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import {
  BrowserRouter as Router,
  Route,
  Switch,
  Redirect
} from 'react-router-dom'
import ChildInformation from '~/components/ChildInformation'
import StateProvider from '~/state/StateProvider'
import PersonProfile from '~components/PersonProfile'
import ErrorMessage from '~components/common/ErrorMessage'
import Unit from '~components/Unit'
import Header from '~components/Header'
import Search from '~components/Search'
import ensureAuthenticated from './components/ensureAuthenticated'
import LoginPage from '~components/LoginPage'
import Units from '~components/Units'
import ApplicationsPage from 'components/applications/ApplicationsPage'
import InvoicesPage from '~components/invoices/InvoicesPage'
import InvoicePage from '~components/invoice/InvoicePage'
import FeeDecisionsPage from '~components/fee-decisions/FeeDecisionsPage'
import FeeDecisionDetailsPage from '~components/fee-decision-details/FeeDecisionDetailsPage'
import Absences from '~components/absences/Absences'
import GroupCaretakers from '~components/GroupCaretakers'
import PlacementDraftPage from '~components/placement-draft/PlacementDraft'
import DecisionPage from '~components/decision-draft/DecisionDraft'
import Reports from '~components/Reports'
import ReportDuplicatePeople from '~components/reports/DuplicatePeople'
import ReportFamilyConflicts from '~components/reports/FamilyConflicts'
import ReportFamilyContacts from '~components/reports/FamilyContacts'
import ReportMissingHeadOfFamily from '~components/reports/MissingHeadOfFamily'
import ReportMissingServiceNeed from '~components/reports/MissingServiceNeed'
import ReportPartnersInDifferentAddress from '~components/reports/PartnersInDifferentAddress'
import ReportChildrenInDifferentAddress from '~components/reports/ChildrenInDifferentAddress'
import ReportChildAgeLanguage from '~components/reports/ChildAgeLanguage'
import ReportApplications from '~components/reports/Applications'
import ReportAssistanceNeeds from '~components/reports/AssistanceNeeds'
import ReportAssistanceActions from '~components/reports/AssistanceActions'
import ReportOccupancies from '~components/reports/Occupancies'
import ReportInvoices from '~components/reports/Invoices'
import ReportEndedPlacements from '~components/reports/EndedPlacements'
import ReportStartingPlacements from '~components/reports/StartingPlacements'
import ReportPresences from '~components/reports/PresenceReport'
import ReportServiceNeeds from '~components/reports/ServiceNeeds'
import ReportRaw from '~components/reports/Raw'
import { RouteWithTitle } from '~components/RouteWithTitle'
import { useTranslation } from '~state/i18n'
import { UserContext, UserContextProvider } from '~state/user'
import CreateUnitPage from '~components/unit/CreateUnitPage'
import UnitDetailsPage from '~components/unit/UnitDetailsPage'
import ApplicationPage from 'components/ApplicationPage'
import { hasRole } from '~utils/roles'
import { getAuthStatus, AuthStatus } from '~api/auth'

function RedirectToMainPage() {
  const { loggedIn, roles } = useContext(UserContext)

  if (!loggedIn) {
    return <Redirect to={'/login'} />
  }

  if (hasRole(roles, 'SERVICE_WORKER')) {
    return <Redirect to={'/applications'} />
  } else if (hasRole(roles, 'UNIT_SUPERVISOR') || hasRole(roles, 'STAFF')) {
    return <Redirect to={'/units'} />
  } else {
    return <Redirect to={'/search'} />
  }
}

function App() {
  const { i18n } = useTranslation()
  const [authStatus, setAuthStatus] = useState<AuthStatus>()

  useEffect(() => {
    void getAuthStatus().then(setAuthStatus)
  }, [])

  if (authStatus === undefined) {
    return null
  }

  return (
    <UserContextProvider user={authStatus.user} roles={authStatus.roles}>
      <StateProvider>
        <Router basename="/employee">
          <Header />
          <Switch>
            <RouteWithTitle
              exact
              path="/login"
              component={LoginPage}
              title={i18n.titles.login}
            />
            <RouteWithTitle
              exact
              path="/units"
              component={ensureAuthenticated(Units)}
              title={i18n.titles.units}
            />
            <RouteWithTitle
              exact
              path="/units/new"
              component={ensureAuthenticated(CreateUnitPage)}
              title={i18n.titles.createUnit}
            />
            <RouteWithTitle
              exact
              path="/units/:id"
              component={ensureAuthenticated(Unit)}
            />
            <Route
              exact
              path="/units/:id/details"
              component={ensureAuthenticated(UnitDetailsPage)}
            />
            <Route
              exact
              path="/units/:unitId/family-contacts"
              component={ensureAuthenticated(ReportFamilyContacts)}
            />
            <RouteWithTitle
              exact
              path="/search"
              component={ensureAuthenticated(Search)}
              title={i18n.titles.customers}
            />
            <RouteWithTitle
              exact
              path="/profile/:id"
              component={ensureAuthenticated(PersonProfile)}
            />
            <RouteWithTitle
              exact
              path="/child-information/:id"
              component={ensureAuthenticated(ChildInformation)}
            />
            <RouteWithTitle
              exact
              path="/applications"
              component={ensureAuthenticated(ApplicationsPage)}
              title={i18n.titles.applications}
            />
            <RouteWithTitle
              exact
              path="/applications/:id"
              component={ensureAuthenticated(ApplicationPage)}
              title={i18n.titles.applications}
            />
            <RouteWithTitle
              exact
              path="/applications/:id/placement"
              component={ensureAuthenticated(PlacementDraftPage)}
              title={i18n.titles.placementDraft}
            />
            <RouteWithTitle
              exact
              path="/applications/:id/decisions"
              component={ensureAuthenticated(DecisionPage)}
              title={i18n.titles.decision}
            />
            <RouteWithTitle
              exact
              path="/fee-decisions"
              component={ensureAuthenticated(FeeDecisionsPage)}
              title={i18n.titles.feeDecisions}
            />
            <RouteWithTitle
              exact
              path="/fee-decisions/:id"
              component={ensureAuthenticated(FeeDecisionDetailsPage)}
            />
            <RouteWithTitle
              exact
              path="/invoices"
              component={ensureAuthenticated(InvoicesPage)}
              title={i18n.titles.invoices}
            />
            {/*TODO test this*/}
            <RouteWithTitle
              exact
              path="/invoices/:id"
              component={ensureAuthenticated(InvoicePage)}
            />
            <RouteWithTitle
              exact
              path="/absences/:groupId"
              component={ensureAuthenticated(Absences)}
            />
            <RouteWithTitle
              exact
              path="/units/:unitId/groups/:groupId/caretakers"
              component={ensureAuthenticated(GroupCaretakers)}
            />
            <RouteWithTitle
              exact
              path="/reports"
              component={ensureAuthenticated(Reports)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/family-conflicts"
              component={ensureAuthenticated(ReportFamilyConflicts)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/missing-head-of-family"
              component={ensureAuthenticated(ReportMissingHeadOfFamily)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/missing-service-need"
              component={ensureAuthenticated(ReportMissingServiceNeed)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/applications"
              component={ensureAuthenticated(ReportApplications)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/partners-in-different-address"
              component={ensureAuthenticated(ReportPartnersInDifferentAddress)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/children-in-different-address"
              component={ensureAuthenticated(ReportChildrenInDifferentAddress)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/child-age-language"
              component={ensureAuthenticated(ReportChildAgeLanguage)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/assistance-needs"
              component={ensureAuthenticated(ReportAssistanceNeeds)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/assistance-actions"
              component={ensureAuthenticated(ReportAssistanceActions)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/occupancies"
              component={ensureAuthenticated(ReportOccupancies)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/invoices"
              component={ensureAuthenticated(ReportInvoices)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/starting-placements"
              component={ensureAuthenticated(ReportStartingPlacements)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/ended-placements"
              component={ensureAuthenticated(ReportEndedPlacements)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/duplicate-people"
              component={ensureAuthenticated(ReportDuplicatePeople)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/presences"
              component={ensureAuthenticated(ReportPresences)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/service-needs"
              component={ensureAuthenticated(ReportServiceNeeds)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle
              exact
              path="/reports/raw"
              component={ensureAuthenticated(ReportRaw)}
              title={i18n.titles.reports}
            />
            <RouteWithTitle path="*" component={RedirectToMainPage} />
          </Switch>
          <ErrorMessage />
        </Router>
      </StateProvider>
    </UserContextProvider>
  )
}

export default App
