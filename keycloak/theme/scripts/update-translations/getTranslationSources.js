function getSheetUrl(sheetId, sheetName) {
  return `https://docs.google.com/spreadsheets/d/${sheetId}/gviz/tq?tqx=out:csv&sheet=${sheetName}`;
}

function getTranslationSources(sheetId, module, languages) {
  var sources = [];

  languages.forEach((language) => {
    var sheetName = `${module}_${language}`;
    var source = getSheetUrl(sheetId, sheetName);

    sources.push({
      source,
      meta: { language: sheetName },
    });
  });

  return sources;
}

module.exports = getTranslationSources;
