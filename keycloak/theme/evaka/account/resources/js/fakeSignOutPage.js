// For signing out we want to show a separate page which contains a
// text description explaining the effects of signing out. Without the
// explanation, some users may no understand how the sign out affects
// sessions in other services such as Tunnistamo.
(function () {
  // <-- Store
  var cachedContent = null;
  var cachedActiveTab = null;
  // --> Store

  // <-- Constants
  var HS_SIGN_OUT_TAB_CONTROL_ID = "hs-sign-out-tab-control";
  var HS_ACCOUNT_PAGE_CONTENT_CLASS = "content";
  var HS_DESCRIPTION_STRING_CONTAINER_ID = "hs-sign-out-page-description";
  var HS_BUTTON_ROW_CLASS = "hs-button-row";
  var HS_SIGN_OUT_HREF_CONTAINER_ID = "hs-sign-out-href";
  var HS_SIGN_OUT_LABEL_CONTAINER_ID = "hs-sign-out-page-sign-out-button-label";
  var HS_SIGN_OUT_CANCEL_LABEL_CONTAINER_ID =
    "hs-sign-out-page-sign-out-cancel-button-label";
  var buttonClasses = [
    "hds-button",
    "hds-button__label",
    "hds-button--fullwidth",
  ];
  // --> Constants

  // <-- Selectors
  function getTabButton() {
    return document.getElementById(HS_SIGN_OUT_TAB_CONTROL_ID);
  }
  function getContent() {
    return document.querySelector("." + HS_ACCOUNT_PAGE_CONTENT_CLASS);
  }
  function getDescription() {
    return getTextBasedOnId(HS_DESCRIPTION_STRING_CONTAINER_ID);
  }
  function getSignOutHref() {
    return getTextBasedOnId(HS_SIGN_OUT_HREF_CONTAINER_ID);
  }
  function getSignOutButtonLabel() {
    return getTextBasedOnId(HS_SIGN_OUT_LABEL_CONTAINER_ID);
  }
  function getSignOutCancelButtonLabel() {
    return getTextBasedOnId(HS_SIGN_OUT_CANCEL_LABEL_CONTAINER_ID);
  }
  // --> Selectors

  // <-- Utils
  function getTextBasedOnId(elementId) {
    var element = document.getElementById(elementId);

    if (!element) {
      return "";
    }

    return element.innerText;
  }

  function buildSignOutPageContent() {
    var container = document.createElement("div");
    container.classList.add("content");

    var description = document.createElement("p");
    description.classList.add("hs-p");
    description.innerText = getDescription();

    var signOutButton = document.createElement("a");
    signOutButton.href = getSignOutHref();
    signOutButton.innerText = getSignOutButtonLabel();
    signOutButton.classList.add(...buttonClasses, "hds-button--primary");

    var cancelButton = document.createElement("button");
    cancelButton.innerText = getSignOutCancelButtonLabel();
    cancelButton.addEventListener("click", handleCancelButtonClick);
    cancelButton.classList.add(...buttonClasses, "hds-button--secondary");

    var buttonRow = document.createElement("div");
    buttonRow.classList.add(HS_BUTTON_ROW_CLASS);
    buttonRow.append(signOutButton, cancelButton);
    buttonRow.id = "kc-form-buttons";

    container.append(description, buttonRow);

    return container;
  }
  function findActiveTab() {
    var tabs = document.querySelectorAll(".tabs > ul > li");

    return Array.from(tabs.values()).findIndex((tab) =>
      tab.classList.contains("active")
    );
  }
  function setActiveTab(index) {
    var tabs = document.querySelectorAll(".tabs > ul > li");

    tabs.forEach((tab) => tab.classList.remove("active"));
    tabs[index].classList.add("active");
  }
  // --> Utils

  // <-- Handlers
  var handleTabButtonClick = function () {
    var content = getContent();
    var isSigOutViewVisible = cachedContent !== null;

    if (!isSigOutViewVisible) {
      var nextContent = buildSignOutPageContent();

      cachedContent = content;
      cachedActiveTab = findActiveTab();
      setActiveTab(2);
      content.parentElement.replaceChild(nextContent, content);
    }
  };

  var handleCancelButtonClick = function () {
    var content = getContent();
    var nextContent = cachedContent;
    var nextActiveTab = cachedActiveTab;

    cachedContent = null;
    cachedActiveTab = null;
    setActiveTab(nextActiveTab);
    content.parentElement.replaceChild(nextContent, content);
  };
  // --> Handlers

  document.addEventListener("DOMContentLoaded", function () {
    var signOutPageControlButton = getTabButton();

    signOutPageControlButton.addEventListener("click", handleTabButtonClick);
  });

  window.onunload = function () {
    var signOutPageControlButton = getTabButton();

    signOutPageControlButton.removeEventListener("click", handleTabButtonClick);
  };
})();
