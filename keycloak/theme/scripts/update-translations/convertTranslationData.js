const csv = require("csvtojson/v2");
const properties = require("properties");

async function convertTranslationData(translationData, debug) {
  const transforms = await Promise.all(
    Object.keys(translationData).map(async (key) => {
      if (debug) {
        process.stdout.write(`Converting ${key}\n`);
      }

      const data = translationData[key];
      const dataAsJSON = await csv({ noheader: true }).fromString(data);

      const dataAsPropertiesJSON = {};
      dataAsJSON.forEach((row) => {
        const translationKey = row.field1;
        const translationValue = row.field2;

        dataAsPropertiesJSON[translationKey] = translationValue;
      });

      const convertedData = properties.stringify(dataAsPropertiesJSON);

      if (debug) {
        process.stdout.write(`Converted ${key}\n`);
      }

      return { key, data: convertedData };
    })
  );

  return transforms.reduce(
    (acc, { key, data }) => ({
      ...acc,
      [key]: data,
    }),
    {}
  );
}

module.exports = convertTranslationData;
