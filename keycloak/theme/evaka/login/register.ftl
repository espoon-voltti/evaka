<#import "template.ftl" as layout>
<@layout.registrationLayout showAlerts=false; section>
    <#if section = "header">
        ${msg("registerTitle")}
    <#elseif section = "form">
        <div id="hs-age-check-error" class="${properties.hsAlertClass!} ${properties.hsAlertErrorClass!}" style="display: none;">
            <div class="${properties.hsAlertLabelClass!}">
                <span class="${properties.kcFeedbackErrorIcon!}"></span>
                ${kcSanitize(msg("doAcknowledgeAgeError"))}
            </div>
        </div>

        <form id="kc-register-form" class="${properties.kcFormClass!}" action="${url.registrationAction}" method="post">
            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('firstName',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="firstName" class="${properties.kcLabelClass!}">${msg("firstName")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="${properties.hsInputwrapperClass!}">
                        <input type="text" id="firstName" class="${properties.kcInputClass!}" name="firstName" value="${(register.formData.firstName!'')}" />
                    </div>
                    <#if messagesPerField.firstName != "">
                        <div class="${properties.hsInputHelperText!}">${messagesPerField.firstName}</div>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('lastName',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="lastName" class="${properties.kcLabelClass!}">${msg("lastName")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="${properties.hsInputwrapperClass!}">
                        <input type="text" id="lastName" class="${properties.kcInputClass!}" name="lastName" value="${(register.formData.lastName!'')}" />
                    </div>
                    <#if messagesPerField.lastName != "">
                        <div class="${properties.hsInputHelperText!}">${messagesPerField.lastName}</div>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('email',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="email" class="${properties.kcLabelClass!}">${msg("email")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="${properties.hsInputwrapperClass!}">
                        <input type="text" id="email" class="${properties.kcInputClass!}" name="email" value="${(register.formData.email!'')}" autocomplete="email" />
                    </div>
                    <#if messagesPerField.email != "">
                        <div class="${properties.hsInputHelperText!}">${messagesPerField.email}</div>
                    </#if>
                </div>
            </div>

          <#if !realm.registrationEmailAsUsername>
            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('username',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="username" class="${properties.kcLabelClass!}">${msg("username")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="${properties.hsInputwrapperClass!}">
                        <input type="text" id="username" class="${properties.kcInputClass!}" name="username" value="${(register.formData.username!'')}" autocomplete="username" />
                    </div>
                    <#if messagesPerField.username != "">
                        <div class="${properties.hsInputHelperText!}">${messagesPerField.username}</div>
                    </#if>
                </div>
            </div>
          </#if>

            <#if passwordRequired??>
            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('password',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="${properties.hsInputwrapperClass!}">
                        <input type="password" id="password" class="${properties.kcInputClass!}" name="password" autocomplete="new-password"/>
                    </div>
                    <#if messagesPerField.password != "">
                        <div class="${properties.hsInputHelperText!}">${messagesPerField.password}</div>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('password-confirm',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="password-confirm" class="${properties.kcLabelClass!}">${msg("passwordConfirm")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="${properties.hsInputwrapperClass!}">
                        <input type="password" id="password-confirm" class="${properties.kcInputClass!}" name="password-confirm" autocomplete="new-password" />
                    </div>
                    <#if messagesPerField["password-confirm"] != "">
                        <div class="${properties.hsInputHelperText!}">${messagesPerField["password-confirm"]}</div>
                    </#if>
                </div>
            </div>
            </#if>

            <#if recaptchaRequired??>
            <div class="form-group">
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="g-recaptcha" data-size="compact" data-sitekey="${recaptchaSiteKey}"></div>
                </div>
            </div>
            </#if>

            <!-- These elements are only used with custom validation. Their values are ephemeral. -->
            <!-- 1. They doen't get saved into server -->
            <!-- 2. They doen't persist over page reloads -->
            <div id="hs-acknowledgements-form-group" class="${properties.kcFormGroupClass!}">
                <div class="hds-checkbox">
                    <input class="hds-checkbox__input" type="checkbox" name="acknowledgements" id="hs-acknowledgements" />
                    <label for="hs-acknowledgements" class="hds-checkbox__label">${msg("doAcknowledgeResources", kcSanitize(msg("doAcknowledgeResourcesPrivacyPolicyLink")), kcSanitize(msg("doAcknowledgeResourcesyDataProtectionLink")), 'target="_blank" rel="noopener noreferrer"')?no_esc}</label>
                </div>
            </div>
            <div id="hs-age-check-form-group" class="${properties.kcFormGroupClass!}">
                <div class="hds-checkbox">
                    <input class="hds-checkbox__input" type="checkbox" name="ageCheck" id="hs-age-check" />
                    <label for="hs-age-check" class="hds-checkbox__label">${kcSanitize(msg("doAcknowledgeAge"))}</label>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doRegisterShort")}"/>
                    <a class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
