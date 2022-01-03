var tar = require("tar");

var archiveName = "evaka.tar.gz";

tar
  .create(
    {
      gzip: true,
      file: archiveName,
    },
    ["./evaka"]
  )
  .then(() => {
    process.stdout.write(`"evaka" theme packed successfully: ${archiveName}`);
  });
