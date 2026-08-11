/**
 * Smoke-check that a mock server is answering the v1 client contract on :8081.
 * Usage: start mock:prism or mock:mockoon in another terminal, then npm run mock:verify
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

  // ADR 003: the return-status route echoes bookingStatus/returnNotes from the request body
  // (Mockoon templating), unlike every other static-fixture route — assert that round-trip here.
  const returnNotesSample = "Verified via mock:verify";
  const returnPatch = await check("PATCH", "/api/returns/8/status", {
    bookingStatus: "COMPLETED",
    returnNotes: returnNotesSample,
  });
  if (returnPatch.ok && returnPatch.json?.returnNotes !== returnNotesSample) {
    console.error(
      `FAIL PATCH /api/returns/8/status did not echo returnNotes (got: ${JSON.stringify(returnPatch.json?.returnNotes)})`
    );
    returnPatch.ok = false;
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
  console.error("Start it with: npm run mock:prism   OR   npm run mock:mockoon");
  process.exit(1);
});
