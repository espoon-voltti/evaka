<#import "template.ftl" as layout>
<@layout.mainLayout active='password' bodyClass='password' showErrors=true showFeedbackLink=true; section>
    <#if section = "header">
        ${msg("changePasswordHtmlTitle")}
    <#elseif section = "subtitle">
        <span class="subtitle">${msg("allFieldsRequired")}</span>
    <#elseif section = "content">
        <form action="${url.passwordUrl}" method="post">
            <input type="text" id="username" name="username" value="${(account.username!'')}" autocomplete="username" readonly="readonly" style="display:none;">

            <#if password.passwordSet>
              <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('password',properties.kcFormGroupErrorClass!)}">
                  <div class="${properties.kcLabelWrapperClass!}">
                      <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>
                  </div>
                  <div class="${properties.kcInputWrapperClass!}">
                      <div class="${properties.hsInputwrapperClass!}">
                          <input type="password" id="password" class="${properties.kcInputClass!}" name="password" autofocus autocomplete="new-password" />
                      </div>
                      <#if messagesPerField.password != "">
                          <div class="${properties.hsInputHelperText!}">${messagesPerField.password}</div>
                      </#if>
                  </div>
              </div>
            </#if>

            <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('passwordNew',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="password-new" class="${properties.kcLabelClass!}">${msg("passwordNew")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="${properties.hsInputwrapperClass!}">
                        <input type="password" id="password-new" class="${properties.kcInputClass!}" name="password-new" autocomplete="new-password" />
                    </div>
                    <#if messagesPerField.passwordNew != "">
                        <div class="${properties.hsInputHelperText!}">${messagesPerField.passwordNew}</div>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!} ${messagesPerField.printIfExists('passwordConfirm',properties.kcFormGroupErrorClass!)}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="password-confirm" class="${properties.kcLabelClass!}">${msg("passwordConfirm")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <div class="${properties.hsInputwrapperClass!}">
                        <input type="password" id="password-confirm" class="${properties.kcInputClass!}" name="password-confirm" autocomplete="new-password" />
                    </div>
                    <#if messagesPerField.passwordNew != "">
                        <div class="${properties.hsInputHelperText!}">${messagesPerField.passwordNew}</div>
                    </#if>
                </div>
            </div>

            <div class="hs-form-group">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!} submit">
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="submitAction" value="Save">${msg("doSave")}</button>
                </div>
            </div>
        </form>
    </#if>

</@layout.mainLayout>
