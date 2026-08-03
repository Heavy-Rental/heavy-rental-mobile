import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "../..");
const prepareScript = path.join(root, "scripts", "prepare-mocks.mjs");

export function prepareMocks() {
  const result = spawnSync(process.execPath, [prepareScript], {
    cwd: root,
    stdio: "inherit",
  });
  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

export function prepareAndResolveOpenApi() {
  prepareMocks();
  return path.join(root, "mocks", ".generated", "openapi.bundled.yaml");
}
