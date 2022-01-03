(function () {
  // <-- Constants
  var HS_INPUT_CONTAINER_CLASS = "hds-text-input--invalid";
  var HS_INPUT_CONTAINER_ERROR_CLASS = "hds-text-input--invalid";
  var HS_INPUT_CLASS = "hds-text-input__input";
  var HS_INPUT_TEXT_CLASS = "hds-text-input__helper-text";
  // --> Constants

  // <-- Selectors
  function getErrorInputs() {
    return document.querySelectorAll(
      "." + HS_INPUT_CONTAINER_ERROR_CLASS + " ." + HS_INPUT_CLASS
    );
  }
  function getInputContainerByInput(input) {
    return input.closest("." + HS_INPUT_CONTAINER_CLASS);
  }
  function getErrorTextByInput(input) {
    return getInputContainerByInput(input).querySelector(
      "." + HS_INPUT_TEXT_CLASS
    );
  }
  // --> Selectors

  // <-- Handlers
  var handleErrorInputFocus = function (event) {
    var errorInput = event.target;
    var errorInputContainer = getInputContainerByInput(errorInput);
    var errorText = getErrorTextByInput(errorInput);

    if (errorInputContainer) {
      errorInputContainer.classList.remove(HS_INPUT_CONTAINER_ERROR_CLASS);
    }

    if (errorText) {
      errorText.remove();
    }
  };
  // --> Handlers

  document.addEventListener("DOMContentLoaded", function () {
    var errorInputs = getErrorInputs();

    errorInputs.forEach((errorInput) => {
      errorInput.addEventListener("focusin", handleErrorInputFocus, {
        once: true,
      });
    });
  });

  window.onunload = function () {
    // If there are remaining errors, clean up their event listeners.
    // Not sure if it's possible to end up a situation where this can
    // happen.
    var errorInputs = getErrorInputs();

    if (errorInputs) {
      errorInputs.forEach((errorInput) => {
        errorInput.removeEventListener("focusin", handleErrorInputFocus);
      });
    }
  };
})();
