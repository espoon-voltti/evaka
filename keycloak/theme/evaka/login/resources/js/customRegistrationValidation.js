(function () {
  // <-- Constants
  var HS_HAS_ERROR_CLASS = "hs-has-error";
  var KC_REGISTER_FORM_ID = "kc-register-form";
  var HS_ACKNOWLEDGEMENTS_FORM_GROUP_ID = "hs-acknowledgements-form-group";
  var HS_ACKNOWLEDGEMENTS_INPUT_ID = "hs-acknowledgements";
  var HS_AGE_CHECK_FORM_GROUP_ID = "hs-age-check-form-group";
  var HS_AGE_CHECK_INPUT_ID = "hs-age-check";
  var HS_FORM_GROUP = "hs-form-group";
  var HS_AGE_CHECK_ERROR_ID = "hs-age-check-error";
  // --> Constants

  // <-- Selectors
  function getRegistrationForm() {
    return document.getElementById(KC_REGISTER_FORM_ID);
  }
  function getHsAcknowledgementsFormGroup() {
    return document.getElementById(HS_ACKNOWLEDGEMENTS_FORM_GROUP_ID);
  }
  function getHsAcknowledgementsInput() {
    return document.getElementById(HS_ACKNOWLEDGEMENTS_INPUT_ID);
  }
  function getHsAgeCheckFormGroup() {
    return document.getElementById(HS_AGE_CHECK_FORM_GROUP_ID);
  }
  function getHsAgeCheckInput() {
    return document.getElementById(HS_AGE_CHECK_INPUT_ID);
  }
  function getClosesFormGroup(element) {
    return element.closest("." + HS_FORM_GROUP);
  }
  function getAgeCheckError() {
    return document.getElementById(HS_AGE_CHECK_ERROR_ID);
  }
  // --> Selectors

  // <-- Utils
  function getIsCheckboxChecked(checkboxElement) {
    return checkboxElement.checked === true;
  }
  // --> Utils

  // <-- Handlers
  var handleRegistrationFormSubmit = function (event) {
    var isAcknowledgedValid = getIsCheckboxChecked(
      event.target.acknowledgements
    );
    var isRequiredAge = getIsCheckboxChecked(event.target.ageCheck);

    // Toggle the error class in the form groups.
    getHsAcknowledgementsFormGroup().classList.toggle(
      HS_HAS_ERROR_CLASS,
      !isAcknowledgedValid
    );
    getHsAgeCheckFormGroup().classList.toggle(
      HS_HAS_ERROR_CLASS,
      !isRequiredAge
    );

    if (!isRequiredAge) {
      // Toggle error messages
      getAgeCheckError().style.display = "block";
    }

    // If our custom conditions do not pass, we prevent the form from
    // submitting. Note that this behaviour will in essence create two
    // validation steps if the user has multiple errors on the form:
    // 1) Custom validation is ran in the browser
    // 2) Default validation is ran on the server
    //
    // This means that the user may have to fix errors twice. Not ideal
    // but this seemed like the most pragmatic approach.
    if (!isAcknowledgedValid || !isRequiredAge) {
      event.preventDefault();
    }
  };
  var handleCheckboxChange = function (event) {
    var hsAcknowledgementsFormGroup = getClosesFormGroup(event.target);
    var isError = hsAcknowledgementsFormGroup.classList.contains(
      HS_HAS_ERROR_CLASS
    );
    var isNowValid = getIsCheckboxChecked(event.target);

    // When the form group still has an error label, but actually has a
    // valid value, remove the error value.
    if (isError && isNowValid) {
      hsAcknowledgementsFormGroup.classList.remove(HS_HAS_ERROR_CLASS);

      if (event.target.id === HS_AGE_CHECK_INPUT_ID) {
        // Toggle error messages
        getAgeCheckError().style.display = "none";
      }
    }
  };
  // --> Handlers

  document.addEventListener("DOMContentLoaded", function () {
    var registrationForm = getRegistrationForm();
    var hsAcknowledgementsInput = getHsAcknowledgementsInput();
    var hsAgeCheckInput = getHsAgeCheckInput();

    if (registrationForm) {
      registrationForm.addEventListener("submit", handleRegistrationFormSubmit);
    }

    if (hsAcknowledgementsInput) {
      hsAcknowledgementsInput.addEventListener("change", handleCheckboxChange);
    }

    if (hsAgeCheckInput) {
      hsAgeCheckInput.addEventListener("change", handleCheckboxChange);
    }
  });

  window.onunload = function () {
    var registrationForm = getRegistrationForm();
    var hsAcknowledgementsInput = getHsAcknowledgementsInput();
    var hsAgeCheckInput = getHsAgeCheckInput();

    if (registrationForm) {
      registrationForm.removeEventListener(
        "submit",
        handleRegistrationFormSubmit
      );
    }

    if (hsAcknowledgementsInput) {
      hsAcknowledgementsInput.removeEventListener(
        "change",
        handleCheckboxChange
      );
    }

    if (hsAgeCheckInput) {
      hsAgeCheckInput.removeEventListener("change", handleCheckboxChange);
    }
  };
})();
