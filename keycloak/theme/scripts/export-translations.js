const fs = require("fs");
const properties = require("properties");
const config = require("./config");

// Reads current translations and saves them into a .csv file
function exportMessages(source, out) {
  properties.parse(source, { path: true }, (error, obj) => {
    if (error) {
      return console.error(error.message);
    }

    const csv = Object.entries(obj).reduce((acc, row) => {
      const key = row[0];
      const value = `"${row[1]}"`;

      return acc + [key, value].join(",") + "\n";
    }, "");

    fs.writeFile(out, csv, "utf8", (err) => {
      if (err) {
        console.log(err);
      }
    });
  });
}

function exportTranslations() {
  config.MODULES.forEach((module) => {
    config.LANGUAGES.forEach((language) => {
      const name = "messages_" + language;
      const source = `./evaka/${module}/messages/${name}.properties`;
      const output = `./${module}_${name}.csv`;

      exportMessages(source, output);
    });
  });
}

exportTranslations();
