// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { execFileSync, spawn } from "node:child_process";
import {
  existsSync,
  readFileSync,
  statSync,
  writeFileSync,
  unlinkSync,
} from "node:fs";
import { basename, dirname, resolve } from "node:path";
import { homedir, userInfo } from "node:os";
import { parseArgs } from "node:util";

function shellQuote(s) {
  return "'" + s.replace(/'/g, "'\\''") + "'";
}

const projectRoot = resolve(import.meta.dirname, "..");
const projectName = basename(projectRoot);
const profile = `sandbox-${projectName}`;
const hostUser = userInfo();
const hostHome = process.env.HOME ?? hostUser.homedir;

function loadConfig() {
  const configPath = resolve(import.meta.dirname, "sandbox.json");
  if (!existsSync(configPath)) {
    return { mounts: [], copies: [], ports: [] };
  }
  const raw = JSON.parse(readFileSync(configPath, "utf-8"));
  return {
    cpu: raw.cpu ?? 8,
    memory: raw.memory ?? 12,
    disk: raw.disk ?? 160,
    mounts: raw.mounts ?? [],
    copies: raw.copies ?? [],
    ports: raw.ports ?? [],
    notify: raw.notify ?? null,
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

function parseMount(spec) {
  const parts = spec.split(":");
  let src;
  let mode;

  switch (parts.length) {
    case 1:
      src = expandTilde(parts[0], hostHome);
      mode = "ro";
      break;
    case 2:
      if (isMode(parts[1])) {
        src = expandTilde(parts[0], hostHome);
        mode = parts[1];
      } else {
        src = expandTilde(parts[0], hostHome);
        const dst = expandTilde(parts[1], hostHome);
        if (dst !== src) {
          throw new Error(
            `Invalid mount: ${spec}\n` +
              `Colima VM mounts don't support remapping destinations. ` +
              `Source and destination must be the same path.`
          );
        }
        mode = "ro";
      }
      break;
    case 3:
      src = expandTilde(parts[0], hostHome);
      mode = parts[2];
      break;
    default:
      throw new Error(`Invalid mount: ${spec}`);
  }

  return { location: src, writable: mode === "rw" };
}

function parseCopy(spec) {
  const parts = spec.split(":");
  const src = expandTilde(parts[0], hostHome);
  const dst =
    parts.length > 1
      ? expandTilde(parts[1], hostHome)
      : expandTilde(parts[0], hostHome);
  return { src, dst };
}

// --- Colima helpers ---

function colima(args, opts) {
  return execFileSync("colima", args, { stdio: "inherit", ...opts });
}

function ssh(cmd, opts) {
  return colima(["ssh", "--profile", profile, "--", ...cmd], opts);
}

function vmIsRunning() {
  try {
    execFileSync("colima", ["status", "--profile", profile], {
      stdio: "ignore",
    });
    return true;
  } catch {
    return false;
  }
}

function vmExists() {
  try {
    const output = execFileSync("colima", ["list", "--json"], {
      stdio: ["ignore", "pipe", "ignore"],
    });
    return output
      .toString()
      .split("\n")
      .filter(Boolean)
      .some((line) => JSON.parse(line).name === profile);
  } catch {
    return false;
  }
}

// --- VM config ---

function colimaConfigPath() {
  return resolve(homedir(), ".colima", profile, "colima.yaml");
}

function readVmConfig() {
  const configPath = colimaConfigPath();
  if (!existsSync(configPath)) {
    return null;
  }
  const content = readFileSync(configPath, "utf-8");

  const cpu = parseInt(content.match(/^cpu:\s*(\d+)/m)?.[1]);
  const memory = parseInt(content.match(/^memory:\s*(\d+)/m)?.[1]);
  const disk = parseInt(content.match(/^disk:\s*(\d+)/m)?.[1]);

  const mounts = [];
  const mountsMatch = content.match(/^mounts:\n((?:[\s-].*\n)*)/m);
  if (mountsMatch) {
    const mountBlock = mountsMatch[1];
    const entryRegex = /- location:\s*(.+)\n\s+writable:\s*(true|false)/g;
    let m;
    while ((m = entryRegex.exec(mountBlock)) !== null) {
      mounts.push({ location: m[1].trim(), writable: m[2] === "true" });
    }
  }

  return { cpu, memory, disk, mounts };
}

function desiredVmConfig(config) {
  const mounts = [{ location: projectRoot, writable: true }];
  for (const m of config.mounts) {
    const parsed = parseMount(m);
    if (isDirectory(parsed.location)) {
      mounts.push(parsed);
    }
  }
  const seen = new Set();
  const uniqueMounts = [];
  for (const m of mounts) {
    if (!seen.has(m.location)) {
      seen.add(m.location);
      uniqueMounts.push(m);
    }
  }
  return {
    cpu: config.cpu,
    memory: config.memory,
    disk: config.disk,
    mounts: uniqueMounts,
  };
}

function mountsEqual(a, b) {
  if (a.length !== b.length) return false;
  const key = (m) => `${m.location}:${m.writable}`;
  const setA = new Set(a.map(key));
  return b.every((m) => setA.has(key(m)));
}

function diffConfig(desired, actual) {
  const changes = [];
  if (desired.cpu !== actual.cpu) {
    changes.push(`cpu: ${actual.cpu} → ${desired.cpu}`);
  }
  if (desired.memory !== actual.memory) {
    changes.push(`memory: ${actual.memory} → ${desired.memory}`);
  }
  if (desired.disk !== actual.disk) {
    changes.push(`disk: ${actual.disk} → ${desired.disk}`);
  }
  if (!mountsEqual(desired.mounts, actual.mounts)) {
    changes.push("mounts changed");
  }
  return changes;
}

function writeVmConfig(desired) {
  const configPath = colimaConfigPath();
  let content = readFileSync(configPath, "utf-8");

  content = content.replace(/^cpu:\s*\d+/m, `cpu: ${desired.cpu}`);
  content = content.replace(/^memory:\s*\d+/m, `memory: ${desired.memory}`);
  content = content.replace(/^disk:\s*\d+/m, `disk: ${desired.disk}`);

  const mountsYaml = desired.mounts
    .map(
      (m) =>
        `  - location: ${m.location}\n    writable: ${m.writable}`
    )
    .join("\n");
  content = content.replace(
    /^mounts:\n(?:[\s-].*\n)*/m,
    `mounts:\n${mountsYaml}\n`
  );

  writeFileSync(configPath, content);
}

// --- Notify listener ---

const notifyPort = 39813;
const notifyPidFile = resolve(import.meta.dirname, "notify.pid");

function listenerRunning() {
  if (!existsSync(notifyPidFile)) return false;
  try {
    process.kill(parseInt(readFileSync(notifyPidFile, "utf-8")), 0);
    return true;
  } catch {
    return false;
  }
}

function ensureListener(notifyCommand) {
  if (!notifyCommand || listenerRunning()) return;
  const script = `
    const net = require("net");
    const { execFile } = require("child_process");
    net.createServer((conn) => {
      conn.on("data", () => {
        execFile("bash", ["-c", ${JSON.stringify(notifyCommand)}]);
      });
      conn.resume();
    }).listen(${notifyPort}, "127.0.0.1");
  `;
  const child = spawn(process.execPath, ["-e", script], {
    detached: true,
    stdio: "ignore",
  });
  child.unref();
  writeFileSync(notifyPidFile, String(child.pid));
}

function stopListener() {
  if (existsSync(notifyPidFile)) {
    try {
      process.kill(parseInt(readFileSync(notifyPidFile, "utf-8")), "SIGTERM");
    } catch {}
    try {
      unlinkSync(notifyPidFile);
    } catch {}
  }
}

// --- Port forwarding ---

const portFwdPidFile = resolve(import.meta.dirname, "portfwd.pid");

function stopPortForwarding() {
  if (existsSync(portFwdPidFile)) {
    try {
      process.kill(parseInt(readFileSync(portFwdPidFile, "utf-8")), "SIGTERM");
    } catch {}
    try {
      unlinkSync(portFwdPidFile);
    } catch {}
  }
}

function ensurePortForwarding(ports) {
  if (ports.length === 0) return;
  if (existsSync(portFwdPidFile)) {
    try {
      process.kill(parseInt(readFileSync(portFwdPidFile, "utf-8")), 0);
      return; // already running
    } catch {}
  }

  const sshArgs = ["-N"];
  for (const port of ports) {
    const [host, container] = port.includes(":")
      ? port.split(":")
      : [port, port];
    sshArgs.push("-L", `${host}:localhost:${container}`);
  }

  const child = spawn(
    "colima",
    ["ssh", "--profile", profile, "--", ...sshArgs],
    { detached: true, stdio: "ignore" }
  );
  child.unref();
  writeFileSync(portFwdPidFile, String(child.pid));
}

// --- Colima YAML generation ---

function isDirectory(path) {
  try {
    return statSync(path).isDirectory();
  } catch {
    return false;
  }
}

function buildStartArgs(config) {
  const mounts = [{ location: projectRoot, writable: true }];
  for (const m of config.mounts) {
    const parsed = parseMount(m);
    if (!isDirectory(parsed.location)) {
      throw new Error(
        `Mount "${m}" points to a non-directory path: ${parsed.location}\n` +
          `Colima only supports directory mounts. Use "copies" for files.`
      );
    }
    mounts.push(parsed);
  }

  // Deduplicate mounts by location
  const seen = new Set();
  const uniqueMounts = [];
  for (const m of mounts) {
    if (!seen.has(m.location)) {
      seen.add(m.location);
      uniqueMounts.push(m);
    }
  }

  const mountArgs = uniqueMounts.flatMap((m) => [
    "--mount",
    m.writable ? `${m.location}:w` : m.location,
  ]);

  return [
    "--profile", profile,
    "--cpu", String(config.cpu),
    "--memory", String(config.memory),
    "--disk", String(config.disk),
    "--arch", "aarch64",
    "--runtime", "docker",
    "--activate=false",
    "--vm-type", "vz",
    "--mount-type", "virtiofs",
    "--mount-inotify",
    "--vz-rosetta",
    "--ssh-agent",
    ...mountArgs,
  ];
}

function resetSshControlMaster() {
  const sockPath = resolve(
    homedir(),
    ".colima",
    "_lima",
    `colima-${profile}`,
    "ssh.sock"
  );
  try {
    execFileSync("ssh", ["-S", sockPath, "-O", "exit", "dummy"], {
      stdio: "ignore",
    });
  } catch {}
}

function vmUsername() {
  return execFileSync(
    "colima",
    ["ssh", "--profile", profile, "--", "whoami"],
    { encoding: "utf-8" }
  ).trim();
}

function provision() {
  console.log("Provisioning VM...");
  const vmUser = vmUsername();
  ssh([
    "sudo", "bash", "-c",
    [
      "if [ -f /etc/sandbox-provisioned ]; then exit 0; fi",
      `mkdir -p ${shellQuote(hostHome)}`,
      `vmHome=$(ls -d /home/${vmUser}.linux /home/${vmUser}.guest 2>/dev/null | head -1)`,
      `sed -i "s|:$vmHome:|:${hostHome}:|" /etc/passwd`,
      `cp -a "$vmHome/." ${shellQuote(hostHome)}/`,
      "apt-get update",
      "apt-get install -y --no-install-recommends locales netcat-openbsd",
      "sed -i 's/# en_US.UTF-8/en_US.UTF-8/' /etc/locale.gen",
      "locale-gen en_US.UTF-8 fi_FI.UTF-8",
      "touch /etc/sandbox-provisioned",
    ].join(" && "),
  ]);
  resetSshControlMaster();
}

// --- Commands ---

function runCopies(config) {
  for (const copy of config.copies) {
    const { src, dst } = parseCopy(copy);
    console.log(`Copying ${src} → ${dst}`);
    const dstDir = dirname(dst);
    ssh(["sudo", "mkdir", "-p", dstDir]);
    ssh(["sudo", "chown", `${hostUser.uid}:${hostUser.gid}`, dstDir]);
    execFileSync("bash", [
      "-c",
      `cat ${shellQuote(src)} | colima ssh --profile ${profile} -- bash -c ${shellQuote("cat > " + shellQuote(dst))}`,
    ]);
  }
}

function create() {
  if (vmExists()) {
    console.error(
      `VM "${profile}" already exists. Use 'sandbox recreate' or 'sandbox rm' first.`
    );
    process.exit(1);
  }

  const config = loadConfig();
  const startArgs = buildStartArgs(config);

  console.log(`Starting Colima VM (profile: ${profile})...`);
  colima(["start", ...startArgs]);

  provision();

  const postCreatePath = resolve(import.meta.dirname, "post-create.sh");
  if (existsSync(postCreatePath)) {
    console.log("Running post-create.sh...");
    ssh(["bash", "-c", `cd ${shellQuote(projectRoot)} && bash ${shellQuote(postCreatePath)}`]);
  }

  runCopies(config);

  // Run post-create.local.sh
  const postCreateLocalPath = resolve(
    import.meta.dirname,
    "post-create.local.sh"
  );
  if (existsSync(postCreateLocalPath)) {
    console.log("Running post-create.local.sh...");
    ssh(["bash", "-c", `cd ${shellQuote(projectRoot)} && bash ${shellQuote(postCreateLocalPath)}`]);
  }

  console.log("VM ready.");
}

function apply() {
  if (!vmExists()) {
    console.error("VM does not exist. Use 'sandbox create' first.");
    process.exit(1);
  }

  const config = loadConfig();
  const desired = desiredVmConfig(config);
  const actual = readVmConfig();

  if (!actual) {
    console.error("Could not read Colima config. Use 'sandbox recreate' instead.");
    process.exit(1);
  }

  const changes = diffConfig(desired, actual);

  if (changes.length === 0) {
    console.log("VM config is up to date.");
  } else {
    console.log("Applying config changes:");
    for (const change of changes) {
      console.log(`  ${change}`);
    }
    writeVmConfig(desired);
    console.log("Restarting VM...");
    stopListener();
    stopPortForwarding();
    colima(["stop", "--profile", profile]);
    colima(["start", "--profile", profile]);
  }

  runCopies(config);
  console.log("Done.");
}

function ensureVmRunning() {
  if (vmIsRunning()) return;
  if (vmExists()) {
    console.log(`Starting Colima VM (profile: ${profile})...`);
    colima(["start", "--profile", profile]);
  } else {
    create();
  }
}

function exec(cmd) {
  ensureVmRunning();

  const config = loadConfig();

  const actual = readVmConfig();
  if (actual) {
    const desired = desiredVmConfig(config);
    const changes = diffConfig(desired, actual);
    if (changes.length > 0) {
      console.error(
        "Warning: sandbox.json has changed. Run 'sandbox apply' to update."
      );
    }
  }

  ensureListener(config.notify);
  ensurePortForwarding(config.ports);

  const envExports = ["COLORTERM"]
    .filter((name) => process.env[name])
    .map((name) => `export ${name}=${shellQuote(process.env[name])}`)
    .join("; ");
  const prefix = envExports ? `${envExports}; ` : "";
  const command =
    cmd.length > 0
      ? ["bash", "-c", `${prefix}cd ${shellQuote(projectRoot)} && exec ${cmd.map((a) => shellQuote(a)).join(" ")}`]
      : ["bash", "-c", `${prefix}cd ${shellQuote(projectRoot)} && exec bash`];
  try {
    ssh(command);
  } catch (e) {
    process.exit(e.status ?? 1);
  }
}

function stop() {
  stopListener();
  stopPortForwarding();
  colima(["stop", "--profile", profile]);
}

function rm() {
  stopListener();
  stopPortForwarding();
  colima(["delete", "--profile", profile, "--force", "--data"]);
}

function recreate() {
  stopListener();
  stopPortForwarding();
  try {
    colima(["delete", "--profile", profile, "--force", "--data"], { stdio: "ignore" });
  } catch {}
  create();
}

function status() {
  colima(["status", "--profile", profile]);
}

// --- CLI ---

const usage = `Usage: sandbox <command> [options...]

Commands:
  create                 Create and provision the VM
  exec [cmd...]          Run a command in the VM (default: bash)
  apply                  Apply config changes without recreating
  stop                   Stop the VM
  rm                     Remove the VM
  recreate               Remove and recreate the VM
  status                 Show VM status`;

const { values, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    help: { type: "boolean", short: "h", default: false },
  },
});

const [command, ...rest] = positionals;

if (values.help || command === undefined) {
  console.log(usage);
} else if (command === "create") {
  create();
} else if (command === "exec") {
  exec(rest);
} else if (command === "apply") {
  apply();
} else if (command === "stop") {
  stop();
} else if (command === "rm") {
  rm();
} else if (command === "recreate") {
  recreate();
} else if (command === "status") {
  status();
} else {
  console.error(`Unknown command: ${command}\nRun 'sandbox --help' for usage.`);
  process.exit(1);
}
