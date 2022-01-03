const config = require("../config");
const getTranslationSources = require("./getTranslationSources");
const getTranslationDataFromSources = require("./getTranslationDataFromSources");
const convertTranslationData = require("./convertTranslationData");
const writeTranslations = require("./writeTranslations");

async function fetchTranslations(sheetId, module, languages, output, debug) {
  const translationSources = getTranslationSources(sheetId, module, languages);
  const translationData = await getTranslationDataFromSources(
    translationSources,
    debug
  );
  const convertedTranslationData = await convertTranslationData(
    translationData,
    debug
  );

  return writeTranslations(output, convertedTranslationData, debug);
}

config.MODULES.forEach((module) => {
  fetchTranslations(
    config.TRANSLATION_SHEET_ID,
    module,
    config.LANGUAGES,
    `./evaka/${module}/messages`
  );
});
