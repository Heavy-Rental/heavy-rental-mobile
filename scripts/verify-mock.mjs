/**
 * Smoke-check that a mock server is answering the v1 client contract on :8081.
 * Usage: start mock:mockoon or mock:prism in another terminal, then npm run mock:verify
 *
 * ADR 003 echo (returnNotes / bookingStatus) is Mockoon-only. Against Prism the
 * echo assertion is skipped when MOCK_EXPECT_ECHO=0:
 *   MOCK_EXPECT_ECHO=0 npm run mock:verify
 */
const port = process.env.MOCK_PORT || "8081";
const base = `http://127.0.0.1:${port}`;

async function check(method, urlPath, body) {
  const res = await fetch(`${base}${urlPath}`, {
    method,
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json;
  try {
    json = JSON.parse(text);
  } catch {
    json = text;
  }
  const ok = res.ok;
  console.log(`${ok ? "OK " : "FAIL"} ${method} ${urlPath} → ${res.status}`);
  if (!ok) {
    console.log(text.slice(0, 400));
  }
  return { ok, status: res.status, json };
}

async function main() {
  console.log(`Verifying mock at ${base}\n`);

  const results = [];
  results.push(await check("GET", "/api/bookings"));
  results.push(await check("GET", "/api/deliveries"));
  results.push(await check("GET", "/api/returns"));
  results.push(
    // Numeric bookingId since HR-78 — string ids like "DLV-003" predate that change.
    await check("PATCH", "/api/deliveries/3/status", {
      bookingStatus: "MOBILISED",
    })
  );

  // ADR 003: Mockoon echoes bookingStatus/returnNotes from the request body.
  // Prism serves the static OpenAPI example (returnNotes: "") — skip the echo
  // assertion when MOCK_EXPECT_ECHO=0. Default remains required (CI / Mockoon).
  const returnNotesSample = "Verified via mock:verify";
  const expectEcho = process.env.MOCK_EXPECT_ECHO !== "0";
  const returnPatch = await check("PATCH", "/api/returns/8/status", {
    bookingStatus: "COMPLETED",
    returnNotes: returnNotesSample,
  });
  if (returnPatch.ok) {
    const echoed = returnPatch.json?.returnNotes;
    if (typeof echoed !== "string") {
      console.error(
        `FAIL PATCH /api/returns/8/status response missing returnNotes (got: ${JSON.stringify(returnPatch.json)})`
      );
      returnPatch.ok = false;
    } else if (expectEcho && echoed !== returnNotesSample) {
      console.error(
        `FAIL PATCH /api/returns/8/status did not echo returnNotes (got: ${JSON.stringify(echoed)})`
      );
      console.error(
        `Full PATCH response: ${JSON.stringify(returnPatch.json)}`
      );
      returnPatch.ok = false;
    } else if (!expectEcho && echoed !== returnNotesSample) {
      console.log(
        `SKIP ADR 003 echo (MOCK_EXPECT_ECHO=0); returnNotes=${JSON.stringify(echoed)}`
      );
    }
  }
  results.push(returnPatch);

  const bookings = results[0].json;
  if (Array.isArray(bookings) && bookings.length > 0) {
    console.log(`\nBookings returned: ${bookings.length}`);
    console.log(`First id: ${bookings[0].bookingId}`);
  }

  const failed = results.filter((r) => !r.ok);
  if (failed.length) {
    console.error(`\n${failed.length} check(s) failed`);
    process.exit(1);
  }
  console.log("\nAll checks passed.");
}

main().catch((err) => {
  console.error(`Could not reach mock server at ${base}`);
  console.error(err.message);
  console.error("Start it with: npm run mock:mockoon   OR   npm run mock:prism");
  process.exit(1);
});
