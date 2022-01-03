const fetch = require("node-fetch");

async function fetchTranslationDataFromSource(translationSource, debug) {
  const { source, meta } = translationSource;

  if (debug) {
    process.stdout.write("Fetching data for\n");
    process.stdout.write(`${meta.language}: ${source}\n`);
  }

  const res = await fetch(source);

  if (res.status !== 200) {
    throw Error(
      `Could not fetch ${meta.language} translation data: ${res.status} ${res.statusText}`
    );
  }

  if (debug) {
    process.stdout.write("Fetched data for\n");
    process.stdout.write(`${meta.language}: ${source}\n`);
  }

  const body = await res.text();

  return {
    language: meta.language,
    data: body,
  };
}

async function getTranslationDataFromSource(translationSources, debug) {
  const translationData = await Promise.all(
    translationSources.map((translationSource) =>
      fetchTranslationDataFromSource(translationSource, debug)
    )
  );

  const dataAsObject = {};
  translationData.forEach(({ language, data }) => {
    dataAsObject[language] = data;
  });

  return dataAsObject;
}

module.exports = getTranslationDataFromSource;
