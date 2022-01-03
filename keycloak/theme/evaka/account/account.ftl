<#import "template.ftl" as layout>
<@layout.mainLayout active='account' bodyClass='user' showErrors=true showFeedbackLink=true; section>
<!-- For some reason, in this view, the error a user receives when -->
<!-- trying to change their email to one that's already attached to -->
<!-- an account, is not  a field level error, but a "global error". -->
<!-- This behaviour is different compared to the registration form. -->
<!-- For this reason, we can't omit the messages element from the -->
<!-- account editing page. For now, errors are shown twice in the -->
<!-- view. Once on top and once inline. -->

    <#if section = "header">
        ${msg("editAccountHtmlTitle")}
    <#elseif section = "subtitle">
        <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
    <#elseif section = "content">
        <form action="${url.accountUrl}" method="post">

            <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

            <#if !realm.registrationEmailAsUsername>
                <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('username',properties.kcFormGroupErrorClass!)}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <label for="username" class="${properties.kcLabelClass!}">${msg("username")}<#if properties.hsShowRequired == 'true' && realm.editUsernameAllowed> <span class="required">*</span></#if></label>
                    </div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <div class="${properties.hsInputwrapperClass!}">
                            <input type="text" id="username" class="${properties.kcInputClass!}" name="username" <#if !realm.editUsernameAllowed>disabled="disabled"</#if> value="${(account.username!'')}" />
                        </div>
                        <#if messagesPerField.username != "">
                            <div class="${properties.hsInputHelperText!}">${messagesPerField.username}</div>
                        </#if>
                    </div>
                </div>
            </#if>

            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('email',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="email" class="${properties.kcLabelClass!}">${msg("email")}<#if properties.hsShowRequired == 'true'> <span class="required">*</span></#if></label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="${properties.hsInputwrapperClass!}">
                        <input type="text" id="email" class="${properties.kcInputClass!}" name="email" value="${(account.email!'')}" autofocus />
                    </div>
                    <#if messagesPerField.email != "">
                        <div class="${properties.hsInputHelperText!}">${messagesPerField.email}</div>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('firstName',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="first-name" class="${properties.kcLabelClass!}">${msg("firstName")}<#if properties.hsShowRequired == 'true'> <span class="required">*</span></#if></label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="${properties.hsInputwrapperClass!}">
                        <input type="text" id="first-name" class="${properties.kcInputClass!}" name="firstName" value="${(account.firstName!'')}" />
                    </div>
                    <#if messagesPerField.firstName != "">
                        <div class="${properties.hsInputHelperText!}">${messagesPerField.firstName}</div>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('lastName',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="last-name" class="${properties.kcLabelClass!}">${msg("lastName")}<#if properties.hsShowRequired == 'true'> <span class="required">*</span></#if></label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="${properties.hsInputwrapperClass!}">
                        <input type="text" id="last-name" class="${properties.kcInputClass!}" name="lastName" value="${(account.lastName!'')}" />
                    </div>
                    <#if messagesPerField.lastName != "">
                        <div class="${properties.hsInputHelperText!}">${messagesPerField.lastName}</div>
                    </#if>
                </div>
            </div>

            <div class="hs-form-group">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!} submit">
                    <#if url.referrerURI??><a href="${url.referrerURI}">${kcSanitize(msg("backToApplication")?no_esc)}</a></#if>
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="submitAction" value="Save">${msg("doSave")}</button>
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="submitAction" value="Cancel">${msg("doCancel")}</button>
                </div>
            </div>
        </form>
    </#if>

</@layout.mainLayout>
