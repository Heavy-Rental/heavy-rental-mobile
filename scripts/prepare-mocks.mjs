/**
 * Builds mock assets from specification/api (OpenAPI + examples):
 *  - mocks/.generated/openapi.bundled.yaml  (inline examples for Prism)
 *  - mocks/mockoon/heavy-rental.environment.json
 *  - mocks/.generated/*-item.json          (single-resource fixtures)
 *
 * Source of truth: specification/api/heavyrental-openapi.yaml + specification/api/examples/
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { parse as parseYaml, stringify as stringifyYaml } from "yaml";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "..");
const specsApi = path.join(root, "specification", "api");
const examplesDir = path.join(specsApi, "examples");
const generatedDir = path.join(root, "mocks", ".generated");
const mockoonDir = path.join(root, "mocks", "mockoon");

const UUID = {
  env: "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  getBookings: "11111111-1111-4111-8111-111111111101",
  getBooking: "11111111-1111-4111-8111-111111111102",
  putBooking: "11111111-1111-4111-8111-111111111103",
  getDeliveries: "11111111-1111-4111-8111-111111111104",
  patchDelivery: "11111111-1111-4111-8111-111111111105",
  getReturns: "11111111-1111-4111-8111-111111111106",
  patchReturn: "11111111-1111-4111-8111-111111111107",
  resp: (n) => `22222222-2222-4222-8222-2222222222${String(n).padStart(2, "0")}`,
};

function readJson(name) {
  return JSON.parse(fs.readFileSync(path.join(examplesDir, name), "utf8"));
}

function writeJson(filePath, data) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, JSON.stringify(data, null, 2) + "\n", "utf8");
}

function writeText(filePath, text) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, text, "utf8");
}

function relFromMockoon(absPath) {
  return path.relative(mockoonDir, absPath).split(path.sep).join("/");
}

function jsonResponse({ uuid, label, statusCode = 200, body, filePath, isDefault = true }) {
  const useFile = Boolean(filePath);
  return {
    uuid,
    body: useFile ? "" : typeof body === "string" ? body : JSON.stringify(body ?? {}, null, 2),
    latency: 0,
    statusCode,
    label: label ?? "",
    headers: [{ key: "Content-Type", value: "application/json" }],
    bodyType: useFile ? "FILE" : "INLINE",
    filePath: useFile ? filePath : "",
    databucketID: "",
    sendFileAsBody: false,
    rules: [],
    rulesOperator: "OR",
    disableTemplating: false,
    fallbackTo404: false,
    default: isDefault,
    crudKey: "id",
    callbacks: [],
  };
}

function httpRoute({ uuid, method, endpoint, documentation, responses }) {
  return {
    uuid,
    type: "http",
    documentation: documentation ?? "",
    method,
    endpoint,
    responses,
    responseMode: null,
    streamingMode: null,
    streamingInterval: 0,
  };
}

function buildMockoonEnvironment(paths) {
  const routes = [
    httpRoute({
      uuid: UUID.getBookings,
      method: "get",
      endpoint: "api/bookings",
      documentation: "List all bookings (primary v1 client load)",
      responses: [
        jsonResponse({
          uuid: UUID.resp(1),
          label: "200 — bookings from specification/api/examples",
          filePath: paths.bookings,
        }),
      ],
    }),
    httpRoute({
      uuid: UUID.getBooking,
      method: "get",
      endpoint: "api/bookings/:bookingId",
      documentation: "Get a single booking",
      responses: [
        jsonResponse({
          uuid: UUID.resp(2),
          label: "200 — sample booking item",
          filePath: paths.bookingItem,
        }),
      ],
    }),
    httpRoute({
      uuid: UUID.putBooking,
      method: "put",
      endpoint: "api/bookings/:bookingId",
      documentation: "Replace a booking (echo-style mock)",
      responses: [
        jsonResponse({
          uuid: UUID.resp(3),
          label: "200 — sample booking item",
          filePath: paths.bookingItem,
        }),
      ],
    }),
    httpRoute({
      uuid: UUID.getDeliveries,
      method: "get",
      endpoint: "api/deliveries",
      documentation: "Today's deliveries",
      responses: [
        jsonResponse({
          uuid: UUID.resp(4),
          label: "200 — deliveries from specification/api/examples",
          filePath: paths.deliveries,
        }),
      ],
    }),
    httpRoute({
      uuid: UUID.patchDelivery,
      method: "patch",
      endpoint: "api/deliveries/:bookingId/status",
      documentation: "Update delivery status (CONFIRMED → MOBILISED)",
      responses: [
        jsonResponse({
          uuid: UUID.resp(5),
          label: "200 — sample delivery item",
          filePath: paths.deliveryItem,
        }),
      ],
    }),
    httpRoute({
      uuid: UUID.getReturns,
      method: "get",
      endpoint: "api/returns",
      documentation: "Today's returns",
      responses: [
        jsonResponse({
          uuid: UUID.resp(6),
          label: "200 — returns from specification/api/examples",
          filePath: paths.returns,
        }),
      ],
    }),
    httpRoute({
      uuid: UUID.patchReturn,
      method: "patch",
      endpoint: "api/returns/:bookingId/status",
      documentation: "Update return status (MOBILISED → COMPLETED)",
      responses: [
        jsonResponse({
          uuid: UUID.resp(7),
          label: "200 — sample return item",
          filePath: paths.returnItem,
        }),
      ],
    }),
  ];

  return {
    uuid: UUID.env,
    lastMigration: 33,
    name: "Heavy Rental API",
    endpointPrefix: "",
    latency: 0,
    port: 8081,
    hostname: "0.0.0.0",
    folders: [],
    routes,
    rootChildren: routes.map((r) => ({ type: "route", uuid: r.uuid })),
    proxyMode: false,
    proxyHost: "",
    proxyRemovePrefix: false,
    tlsOptions: {
      enabled: false,
      type: "CERT",
      pfxPath: "",
      certPath: "",
      keyPath: "",
      caPath: "",
      passphrase: "",
    },
    cors: true,
    headers: [
      { key: "Content-Type", value: "application/json" },
      { key: "Access-Control-Allow-Origin", value: "*" },
      { key: "Access-Control-Allow-Methods", value: "GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD" },
      { key: "Access-Control-Allow-Headers", value: "Content-Type, Authorization" },
    ],
    proxyReqHeaders: [{ key: "", value: "" }],
    proxyResHeaders: [{ key: "", value: "" }],
    data: [],
    callbacks: [],
  };
}

function injectExamplesIntoOpenApi(doc, fixtures) {
  const ensureExample = (operation, status, example) => {
    const response = operation?.responses?.[status];
    const content = response?.content?.["application/json"];
    if (!content) return;
    // Prefer OpenAPI "example" (single) for Prism dynamic mode
    content.example = example;
    // Keep named examples with inline value (not externalValue)
    content.examples = {
      sample: {
        summary: "From specification/api/examples",
        value: example,
      },
    };
  };

  ensureExample(doc.paths?.["/api/bookings"]?.get, "200", fixtures.bookings);
  ensureExample(doc.paths?.["/api/bookings/{bookingId}"]?.get, "200", fixtures.bookingItem);
  ensureExample(doc.paths?.["/api/bookings/{bookingId}"]?.put, "200", fixtures.bookingItem);
  ensureExample(doc.paths?.["/api/deliveries"]?.get, "200", fixtures.deliveries);
  ensureExample(doc.paths?.["/api/deliveries/{bookingId}/status"]?.patch, "200", fixtures.deliveryItem);
  ensureExample(doc.paths?.["/api/returns"]?.get, "200", fixtures.returns);
  ensureExample(doc.paths?.["/api/returns/{bookingId}/status"]?.patch, "200", fixtures.returnItem);

  // Drop fragile component externalValue examples (replaced by inline above)
  if (doc.components?.examples) {
    delete doc.components.examples;
  }

  return doc;
}

function main() {
  const bookings = readJson("bookings.json");
  const deliveries = readJson("deliveries.json");
  const returns = readJson("returns.json");

  const bookingItem = bookings[0];
  const deliveryItem = {
    ...deliveries.find((d) => d.bookingStatus === "CONFIRMED") ?? deliveries[0],
    bookingStatus: "MOBILISED",
  };
  const returnItem = {
    ...returns.find((r) => r.bookingStatus === "MOBILISED") ?? returns[0],
    bookingStatus: "COMPLETED",
  };

  const bookingItemPath = path.join(generatedDir, "booking-item.json");
  const deliveryItemPath = path.join(generatedDir, "delivery-item.json");
  const returnItemPath = path.join(generatedDir, "return-item.json");

  writeJson(bookingItemPath, bookingItem);
  writeJson(deliveryItemPath, deliveryItem);
  writeJson(returnItemPath, returnItem);

  // Mockoon FILE paths are relative to the environment file directory
  const mockoonPaths = {
    bookings: relFromMockoon(path.join(examplesDir, "bookings.json")),
    deliveries: relFromMockoon(path.join(examplesDir, "deliveries.json")),
    returns: relFromMockoon(path.join(examplesDir, "returns.json")),
    bookingItem: relFromMockoon(bookingItemPath),
    deliveryItem: relFromMockoon(deliveryItemPath),
    returnItem: relFromMockoon(returnItemPath),
  };

  const env = buildMockoonEnvironment(mockoonPaths);
  const envPath = path.join(mockoonDir, "heavy-rental.environment.json");
  writeJson(envPath, env);

  const openapiSrc = path.join(specsApi, "heavyrental-openapi.yaml");
  const doc = parseYaml(fs.readFileSync(openapiSrc, "utf8"));
  injectExamplesIntoOpenApi(doc, {
    bookings,
    deliveries,
    returns,
    bookingItem,
    deliveryItem,
    returnItem,
  });

  const bundledPath = path.join(generatedDir, "openapi.bundled.yaml");
  writeText(
    bundledPath,
    [
      "# GENERATED by scripts/prepare-mocks.mjs — do not edit by hand",
      "# Source: specification/api/heavyrental-openapi.yaml + specification/api/examples/",
      stringifyYaml(doc),
    ].join("\n")
  );

  console.log("Mock assets prepared:");
  console.log(`  ${path.relative(root, bundledPath)}`);
  console.log(`  ${path.relative(root, envPath)}`);
  console.log(`  examples → ${path.relative(root, examplesDir)}`);
}

main();
