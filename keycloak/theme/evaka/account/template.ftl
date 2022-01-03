<#macro mainLayout active bodyClass showErrors=true showHeader=false showFeedbackLink=false>
<!doctype html>
<html lang="${locale.current}">
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width,initial-scale=1">

    <title>${msg("accountManagementTitle")}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico">
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script type="text/javascript" src="${url.resourcesPath}/${script}"></script>
        </#list>
    </#if>
</head>
<body class="admin-console user ${bodyClass}">

    <#if showHeader>
        <header class="navbar navbar-default navbar-pf navbar-main header">
            <nav class="navbar" role="navigation">
                <div class="navbar-header">
                    <div class="container">
                        <h1 class="navbar-title">Keycloak</h1>
                    </div>
                </div>
                <div class="navbar-collapse navbar-collapse-1">
                    <div class="container">
                        <ul class="nav navbar-nav navbar-utility">
                            <#if realm.internationalizationEnabled>
                                <li>
                                    <div class="kc-dropdown" id="kc-locale-dropdown">
                                        <a href="#" id="kc-current-locale-link">${locale.current}</a>
                                        <ul>
                                            <#list locale.supported as l>
                                                <li class="kc-dropdown-item"><a href="${l.url}">${l.label}</a></li>
                                            </#list>
                                        </ul>
                                    </div>
                                <li>
                            </#if>
                            <#if referrer?has_content && referrer.url?has_content><li><a href="${referrer.url}" id="referrer">${msg("backTo",referrer.name)}</a></li></#if>
                            <li><a href="${url.logoutUrl}">${msg("doSignOut")}</a></li>
                        </ul>
                    </div>
                </div>
            </nav>
        </header>
    </#if>

    <div class="container hs-card">

        <div class="title">
            <img class="${properties.hsLogo!}" src="${url.resourcesPath}/img/${msg("evakaLogo")}.svg" alt="${msg("evakaLogoAlt")}">
            <h2 class="${properties.customH2Class}"><#nested "header"></h2>
        </div>

        <div class="tabs">
            <ul>
                <li class="<#if active=='account'>active</#if>"><a href="${url.accountUrl}">${msg("account")}</a></li>
                <#if features.passwordUpdateSupported><li class="<#if active=='password'>active</#if>"><a href="${url.passwordUrl}">${msg("password")}</a></li></#if>
                <li><button type="button" id="hs-sign-out-tab-control">${msg("doSignOut")}</button></li>
                <#-- The following menu items are all disabled for users when this theme is selected -->
                <#--  <li class="<#if active=='totp'>active</#if>"><a href="${url.totpUrl}">${msg("authenticator")}</a></li>
                <#if features.identityFederation><li class="<#if active=='social'>active</#if>"><a href="${url.socialUrl}">${msg("federatedIdentity")}</a></li></#if>
                <li class="<#if active=='sessions'>active</#if>"><a href="${url.sessionsUrl}">${msg("sessions")}</a></li>
                <li class="<#if active=='applications'>active</#if>"><a href="${url.applicationsUrl}">${msg("applications")}</a></li>
                <#if features.log><li class="<#if active=='log'>active</#if>"><a href="${url.logUrl}">${msg("log")}</a></li></#if>
                <#if realm.userManagedAccessAllowed && features.authorization><li class="<#if active=='authorization'>active</#if>"><a href="${url.resourceUrl}">${msg("myResources")}</a></li></#if>  -->
            </ul>
        </div>

        <div class="content">
            <#if message?has_content>
                <#if !showErrors && message.type == 'error'>
                <#else>
                    <div class="${properties.hsAlertClass!}<#if message.type = 'success'> ${properties.hsAlertSuccessClass!}</#if><#if message.type = 'warning'> ${properties.hsAlertWarningClass!}</#if><#if message.type = 'error'> ${properties.hsAlertErrorClass!}</#if><#if message.type = 'info'> ${properties.hsAlertInfoClass!}</#if>">
                        <div class="${properties.hsAlertLabelClass!}">
                            <#if message.type = 'success'><span class="${properties.kcFeedbackSuccessIcon!}"></span></#if>
                            <#if message.type = 'warning'><span class="${properties.kcFeedbackWarningIcon!}"></span></#if>
                            <#if message.type = 'error'><span class="${properties.kcFeedbackErrorIcon!}"></span></#if>
                            <#if message.type = 'info'><span class="${properties.kcFeedbackInfoIcon!}"></span></#if>
                            ${kcSanitize(message.summary)?no_esc}
                        </div>
                    </div>
                </#if>
            </#if>

            <#if properties.hsShowRequired == 'true'>
                <div class="subtitle">
                    <#nested "subtitle">
                </div>
            </#if>

            <#nested "content">
        </div>

        <#if showFeedbackLink>
            <div class="hs-footer">
                <a href="${msg('doGiveFeedbackLink')}" target="_blank" rel="noopener noreferrer">${msg("doGiveFeedback")}</a>
            </div>
        </#if>
    </div>

    <!-- Used for fake signout page -->
    <div style="visibility: hidden; display: none;">
        <span id="hs-sign-out-page-description">${msg("signOutDescription")}</span>
        <span id="hs-sign-out-href">${url.logoutUrl}</span>
        <span id="hs-sign-out-page-sign-out-button-label">${msg("doSignOut")}</span>
        <span id="hs-sign-out-page-sign-out-cancel-button-label">${msg("doCancel")}</span>
    </div>

</body>
</html>
</#macro>
