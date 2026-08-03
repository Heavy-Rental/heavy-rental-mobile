/**
 * Start Mockoon CLI from the environment generated from OpenAPI + examples.
 * Listens on 0.0.0.0:8081 so the Android emulator can reach it via 10.0.2.2:8081.
 */
import { spawn } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { prepareMocks } from "./lib/run-prepare.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "..");

prepareMocks();

const envPath = path.join(root, "mocks", "mockoon", "heavy-rental.environment.json");
if (!fs.existsSync(envPath)) {
  console.error(`Missing Mockoon environment: ${envPath}`);
  process.exit(1);
}

const port = process.env.MOCK_PORT || "8081";

console.log(`Starting Mockoon on port ${port}`);
console.log(`Environment: ${path.relative(root, envPath)}`);
console.log(`Android emulator BASE_URL: http://10.0.2.2:${port}/`);
console.log("");

const mockoonBin = path.join(root, "node_modules", "@mockoon", "cli", "bin", "run");

const child = spawn(
  process.execPath,
  [
    mockoonBin,
    "start",
    "--data",
    envPath,
    "--port",
    port,
    "--hostname",
    process.env.MOCK_HOST || "0.0.0.0",
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
