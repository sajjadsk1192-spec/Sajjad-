const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";
const ADMIN_EMAIL = "sajjad.khansk1192@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- 1. Unauthenticated Denials ---
test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      email: "alice@example.com",
      phone: "",
      coinBalance: 100,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    unauthDb.collection("draws").where("visibility", "==", "PUBLIC").get()
  );
  await assertFails(unauthDb.collection("app_state").doc("config").get());
  await assertFails(unauthDb.collection("payment_orders").get());
});

// --- 2. User Profile Lifecycle, Batch Welcome Bonus, Draw Entry & Shadow Updates ---
test("Owner: batch create profile + welcome tx, enter draw, and reject ghost/tampered writes", async () => {
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();
  const created = new Date(Date.now() - 10000);

  // Mirror LuckyDrawRepository.ensureUserProfile() batch write (uses existsAfter in rules)
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("users").doc(ALICE_UID), {
    userId: ALICE_UID,
    name: "Alice Morgan",
    email: "alice@example.com",
    phone: "",
    coinBalance: 100,
    createdAt: created,
    updatedAt: created,
  });
  batch.set(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_welcome"),
    {
      userId: ALICE_UID,
      title: "Welcome Bonus",
      coinDelta: 100,
      type: "WELCOME",
      createdAt: created,
    }
  );
  await assertSucceeds(batch.commit());

  // Read own profile & subcollection transactions
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").get()
  );

  // Mirror enterDailyDraw (-50 coins + ticket fields)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coinBalance: 50,
      todayTicketNumber: "DLD-4242",
      lastTicketDate: "2026-11-24",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow Update Test: ghost field `isAdmin` or `role` must be rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      name: "Alice Admin",
      role: "ADMIN",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Tampered @DocumentId `id` key on create must be rejected
  await assertFails(
    aliceDb.collection("users").doc("alice_other").set({
      id: "alice_other",
      userId: "alice_other",
      name: "Alice",
      email: "alice@example.com",
      coinBalance: 100,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Arbitrary coin inflation on create (must be 100) must be rejected
  await assertFails(
    testEnv
      .authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true })
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .set({
        userId: BOB_UID,
        name: "Bob",
        email: "bob@example.com",
        phone: "",
        coinBalance: 999999,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Future timestamp tampering must be rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      name: "Future Alice",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

// --- 3. PII Isolation & Cross-User Access Denial ---
test("Cross-user isolation: Alice cannot read or modify Bob's profile, transactions, or orders", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      name: "Bob",
      email: "bob@example.com",
      phone: "+15550009999",
      coinBalance: 500,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("users").doc(BOB_UID).collection("transactions").doc("bob_tx").set({
      userId: BOB_UID,
      title: "Welcome Bonus",
      coinDelta: 100,
      type: "WELCOME",
      createdAt: created,
    });
    await db.collection("payment_orders").doc("ORD-BOB1").set({
      userId: BOB_UID,
      packageName: "Lucky Pack",
      coinsPurchased: 550,
      amountCents: 500,
      paymentMethod: "GOOGLE_PAY",
      maskedAccount: "Google Pay Wallet",
      status: "PAID",
      receiptReference: "TXN-123456",
      paidAtMillis: Date.now() - 5000,
      createdAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();

  // PII Blanket Test: non-owner cannot get or list other users
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({ coinBalance: 0, updatedAt: new Date() })
  );
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("transactions").get());

  // Query Trust Test on payment_orders: unfiltered or cross-user query fails; owner-filtered succeeds
  await assertFails(aliceDb.collection("payment_orders").get());
  await assertFails(aliceDb.collection("payment_orders").where("userId", "==", BOB_UID).get());
  await assertSucceeds(
    aliceDb.collection("payment_orders").where("userId", "==", ALICE_UID).get()
  );
});

// --- 4. Payment Order Terminal State Locking & Coin Purchase ---
test("Owner: payment order PROCESSING -> PAID succeeds once, then terminal-locked", async () => {
  const created = new Date(Date.now() - 10000);
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      email: "alice@example.com",
      phone: "",
      coinBalance: 100,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create PROCESSING order
  await assertSucceeds(
    aliceDb.collection("payment_orders").doc("ORD-ALICE1").set({
      userId: ALICE_UID,
      packageName: "Lucky Pack",
      coinsPurchased: 550,
      amountCents: 500,
      paymentMethod: "EASYPAISA",
      maskedAccount: "Easypaisa •••• 4567",
      status: "PROCESSING",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Cannot create already-PAID order directly
  await assertFails(
    aliceDb.collection("payment_orders").doc("ORD-ALICE-CHEAT").set({
      userId: ALICE_UID,
      packageName: "Lucky Pack",
      coinsPurchased: 550,
      amountCents: 500,
      paymentMethod: "GOOGLE_PAY",
      maskedAccount: "Google Pay Wallet",
      status: "PAID",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Settle PROCESSING -> PAID + credit balance
  await assertSucceeds(
    aliceDb.collection("payment_orders").doc("ORD-ALICE1").update({
      status: "PAID",
      receiptReference: "TXN-654321",
      paidAtMillis: Date.now() - 1000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coinBalance: 650,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal State Lock: cannot mutate PAID order again
  await assertFails(
    aliceDb.collection("payment_orders").doc("ORD-ALICE1").update({
      status: "FAILED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

// --- 5. Admin RBAC, Email Spoofing Test & Draw Settlement ---
test("Admin-only config/draws enforce email_verified, block spoofing, and mirror Kotlin queries", async () => {
  const created = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      name: "Alice",
      email: "alice@example.com",
      phone: "+15552345678",
      coinBalance: 100,
      todayTicketNumber: "DLD-1111",
      lastTicketDate: "2026-11-24",
      createdAt: created,
      updatedAt: created,
    });
  });

  // Email Spoofing Test: matching admin email with email_verified=false MUST FAIL
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertFails(
    spoofAdminDb.collection("app_state").doc("config").set({
      configuredDrawPrizeCoins: 20000,
      houseBalanceCoins: 0,
      houseDrawsCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(spoofAdminDb.collection("users").get());

  // Regular player cannot write config, draws, or PRIZE_WIN tx
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  await assertFails(
    aliceDb.collection("draws").doc("draw_fake").set({
      winnerUserId: ALICE_UID,
      winnerName: "Alice",
      winnerPhoneMasked: "••• ••5678",
      ticketCode: "DLD-1111",
      grossPrizeCoins: 10000,
      prizeCoins: 7500,
      houseCutCoins: 2500,
      prizeUsdFormatted: "$75.00",
      visibility: "PUBLIC",
      drawnAtMillis: Date.now() - 1000,
      createdAt: created,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("fake_win").set({
      userId: ALICE_UID,
      title: "Fake Win",
      coinDelta: 7500,
      type: "PRIZE_WIN",
      createdAt: created,
    })
  );

  // Verified Owner Admin succeeds on config, player list, draw settlement, and payout
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(adminDb.collection("payment_orders").get());

  await assertSucceeds(
    adminDb.collection("app_state").doc("config").set({
      configuredDrawPrizeCoins: 10000,
      houseBalanceCoins: 2500,
      houseDrawsCount: 1,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("draws").doc("draw_1").set({
      winnerUserId: ALICE_UID,
      winnerName: "Alice",
      winnerPhoneMasked: "••• ••5678",
      ticketCode: "DLD-1111",
      grossPrizeCoins: 10000,
      prizeCoins: 7500,
      houseCutCoins: 2500,
      prizeUsdFormatted: "$75.00",
      visibility: "PUBLIC",
      drawnAtMillis: Date.now() - 1000,
      createdAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({
      coinBalance: 7600,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_win_1").set({
      userId: ALICE_UID,
      title: "Lucky Draw Win (75%) #DLD-1111",
      coinDelta: 7500,
      type: "PRIZE_WIN",
      createdAt: created,
    })
  );

  // Authenticated player: filtered draws query succeeds; unfiltered query fails
  await assertSucceeds(aliceDb.collection("draws").where("visibility", "==", "PUBLIC").get());
  await assertFails(aliceDb.collection("draws").get());
  await assertSucceeds(aliceDb.collection("app_state").doc("config").get());
  await assertSucceeds(aliceDb.collection("admins").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("admins").doc(ADMIN_UID).get());
});
