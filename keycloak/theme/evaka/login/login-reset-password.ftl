<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false; section>
    <#if section = "header">
        <h2 class="${properties.customH2Class}">${msg("emailForgotTitle")}</h2>
    <#elseif section = "form">
        <p class="${properties.hsPClass!}">${msg("emailInstruction1")}</p>
        <div id="kc-form-wrapper">
            <form id="kc-reset-password-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="username" class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <#if auth?has_content && auth.showUsername()>
                            <input type="text" id="username" name="username" class="${properties.kcInputClass!}" autofocus value="${auth.attemptedUsername}"/>
                        <#else>
                            <input type="text" id="username" name="username" class="${properties.kcInputClass!}" autofocus/>
                        </#if>
                    </div>
                </div>
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("changePassword")}"/>
                    <a class="${properties.hsLinkClass!} ${properties.hsBackToLoginLinkClass}" href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a>
                </div>
            </form>
        </div>
    </#if>
</@layout.registrationLayout>
