/**
 * Start Stoplight Prism mock server from the OpenAPI contract.
 * Listens on 0.0.0.0:8081 so the Android emulator can reach it via 10.0.2.2:8081.
 */
import { spawn } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { prepareAndResolveOpenApi } from "./lib/run-prepare.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "..");

const openapiPath = prepareAndResolveOpenApi();
const port = process.env.MOCK_PORT || "8081";
const host = process.env.MOCK_HOST || "0.0.0.0";

console.log(`Starting Prism mock on http://${host}:${port}`);
console.log(`OpenAPI: ${path.relative(root, openapiPath)}`);
console.log(`Android emulator BASE_URL: http://10.0.2.2:${port}/`);
console.log("");

const prismBin = path.join(
  root,
  "node_modules",
  "@stoplight",
  "prism-cli",
  "dist",
  "index.js"
);

const child = spawn(
  process.execPath,
  [
    prismBin,
    "mock",
    openapiPath,
    "-p",
    port,
    "-h",
    host,
    // Prefer examples from the bundled OpenAPI over random schema data
    "--dynamic",
    "false",
  ],
  {
    cwd: root,
    stdio: "inherit",
    env: process.env,
  }
);

child.on("exit", (code, signal) => {
  if (signal) process.kill(process.pid, signal);
  process.exit(code ?? 1);
});
