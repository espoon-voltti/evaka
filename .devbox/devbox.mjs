// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { execFileSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import { basename, dirname, resolve } from "node:path";
import { userInfo } from "node:os";
import { parseArgs } from "node:util";

const projectRoot = resolve(import.meta.dirname, "..");
const projectName = basename(projectRoot);
const containerName = `devbox-${projectName}`;
const imageName = `devbox-${projectName}`;
const hostUser = userInfo();
const containerHome = hostUser.homedir;

function loadConfig() {
  const configPath = resolve(import.meta.dirname, "devbox.json");
  if (!existsSync(configPath)) {
    return { mounts: [], copies: [], ports: [] };
  }
  const raw = JSON.parse(readFileSync(configPath, "utf-8"));
  return {
    mounts: raw.mounts ?? [],
    copies: raw.copies ?? [],
    ports: raw.ports ?? [],
  };
}

function expandTilde(path, home) {
  if (path.startsWith("~/")) {
    return home + path.slice(1);
  }
  return path;
}

function isMode(s) {
  return s === "ro" || s === "rw";
}

function parseMount(parts, hostHome) {
  const home = containerHome;
  let src;
  let dst;
  let mode;

  switch (parts.length) {
    case 1:
      src = expandTilde(parts[0], hostHome);
      dst = expandTilde(parts[0], home);
      mode = "ro";
      break;
    case 2:
      if (isMode(parts[1])) {
        src = expandTilde(parts[0], hostHome);
        dst = expandTilde(parts[0], home);
        mode = parts[1];
      } else {
        src = expandTilde(parts[0], hostHome);
        dst = expandTilde(parts[1], home);
        mode = "ro";
      }
      break;
    case 3:
      src = expandTilde(parts[0], hostHome);
      dst = expandTilde(parts[1], home);
      mode = parts[2];
      break;
    default:
      throw new Error(`Invalid mount: ${parts.join(":")}`);
  }

  return { src, dst, mode };
}

function parseCopy(spec, hostHome) {
  const home = containerHome;
  const parts = spec.split(":");
  const src = expandTilde(parts[0], hostHome);
  const dst =
    parts.length > 1
      ? expandTilde(parts[1], home)
      : expandTilde(parts[0], home);
  return { src, dst };
}

function ancestorsUnderHome(path) {
  const dirs = [];
  let dir = dirname(path);
  while (dir.startsWith(containerHome + "/") && dir !== containerHome) {
    dirs.push(dir);
    dir = dirname(dir);
  }
  return dirs;
}

function docker(args, stdio = "inherit") {
  execFileSync("docker", args, { stdio });
}

function containerIsRunning() {
  try {
    const output = execFileSync(
      "docker",
      ["inspect", "-f", "{{.State.Running}}", containerName],
      { stdio: ["ignore", "pipe", "ignore"] }
    );
    return output.toString().trim() === "true";
  } catch {
    return false;
  }
}

function containerExists() {
  try {
    execFileSync("docker", ["container", "inspect", containerName], {
      stdio: "ignore",
    });
    return true;
  } catch {
    return false;
  }
}

function imageExists() {
  try {
    execFileSync("docker", ["image", "inspect", imageName], {
      stdio: "ignore",
    });
    return true;
  } catch {
    return false;
  }
}

function build(options) {
  docker([
    "build",
    ...(options?.noCache ? ["--no-cache"] : []),
    "-t",
    imageName,
    "--build-arg",
    `USER_NAME=${hostUser.username}`,
    "--build-arg",
    `USER_UID=${hostUser.uid}`,
    "--build-arg",
    `USER_GID=${hostUser.gid}`,
    "--build-arg",
    `USER_HOME=${hostUser.homedir}`,
    import.meta.dirname,
  ]);
}

function create() {
  if (!imageExists()) {
    build();
  }

  const config = loadConfig();
  const hostHome = process.env.HOME ?? "/root";

  const mounts = config.mounts.map((m) => {
    const parts = m.split(":");
    return parseMount(parts, hostHome);
  });

  const args = [
    "run",
    "-d",
    "--name",
    containerName,
    "--privileged",
    "-v",
    `${projectRoot}:${projectRoot}`,
    "-w",
    projectRoot,
  ];

  for (const { src, dst, mode } of mounts) {
    args.push("-v", `${src}:${dst}:${mode}`);
  }

  for (const port of config.ports) {
    args.push("-p", port.includes(":") ? port : `${port}:${port}`);
  }

  args.push(imageName, "bash", "-c", "sudo bash -c 'dockerd &>/var/log/dockerd.log' & sleep infinity");
  docker(args);

  const dirsToChown = new Set();
  for (const dst of [projectRoot, ...mounts.map((m) => m.dst)]) {
    for (const dir of ancestorsUnderHome(dst)) {
      dirsToChown.add(dir);
    }
  }
  if (dirsToChown.size > 0) {
    docker([
      "exec",
      containerName,
      "sudo",
      "chown",
      `${hostUser.uid}:${hostUser.gid}`,
      ...dirsToChown,
    ]);
  }

  const postCreatePath = resolve(import.meta.dirname, "post-create.sh");
  if (existsSync(postCreatePath)) {
    docker(["exec", containerName, "bash", postCreatePath]);
  }

  const copyDsts = [];
  for (const copy of config.copies) {
    const { src, dst } = parseCopy(copy, hostHome);
    docker(["cp", src, `${containerName}:${dst}`]);
    copyDsts.push(dst);
  }
  if (copyDsts.length > 0) {
    docker([
      "exec",
      containerName,
      "sudo",
      "chown",
      `${hostUser.uid}:${hostUser.gid}`,
      ...copyDsts,
    ]);
  }

  const postCreateLocalPath = resolve(
    import.meta.dirname,
    "post-create.local.sh"
  );
  if (existsSync(postCreateLocalPath)) {
    docker(["exec", containerName, "bash", postCreateLocalPath]);
  }
}

function ensureRunning() {
  if (containerIsRunning()) {
    return;
  }
  if (containerExists()) {
    docker(["start", containerName]);
  } else {
    create();
  }
}

function exec(cmd) {
  ensureRunning();
  const command = cmd.length > 0 ? cmd : ["bash"];
  const ttyFlag = process.stdin.isTTY ? "-it" : "-i";
  const envArgs = [];
  for (const name of ["TERM", "COLORTERM"]) {
    if (process.env[name]) {
      envArgs.push("-e", `${name}=${process.env[name]}`);
    }
  }
  docker(["exec", ttyFlag, ...envArgs, containerName, ...command]);
}

function recreate(options) {
  try {
    docker(["rm", "-f", containerName], "ignore");
  } catch {}
  build(options);
  create();
}

const usage = `Usage: devbox <command> [options...]

Commands:
  build [--no-cache]     Build the container image
  exec [cmd...]          Run a command in the container (default: bash)
  recreate [--no-cache]  Remove, rebuild and recreate the container`;

const { values, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    "no-cache": { type: "boolean", default: false },
    help: { type: "boolean", short: "h", default: false },
  },
});

const [command, ...rest] = positionals;

if (values.help || command === undefined) {
  console.log(usage);
} else if (command === "build") {
  build({ noCache: values["no-cache"] });
} else if (command === "exec") {
  exec(rest);
} else if (command === "recreate") {
  recreate({ noCache: values["no-cache"] });
} else {
  console.error(`Unknown command: ${command}\nRun 'devbox --help' for usage.`);
  process.exit(1);
}
