const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_uid_123";
const MEMBER_UID = "member_uid_456";
const OUTSIDER_UID = "outsider_uid_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const validHash = "a".repeat(64);
const validSalt = "0123456789abcdef0123456789abcdef";

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

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

async function seedStore(ownerUid = OWNER_UID, memberUids = [OWNER_UID], invitedEmails = []) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const ts = pastDate(20000);
    await db.collection("stores").doc(ownerUid).set({
      ownerUid,
      memberUids,
      invitedEmails,
      storeName: "MBG STOCK Libreville",
      currency: "FCFA",
      defaultUnitsPerCrate: 24,
      defaultMinThreshold: 10,
      dataSaverEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });
}

test("Unauthenticated user cannot read or write any MBG STOCK collection", async () => {
  await seedStore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("stores").doc(OWNER_UID).get());
  await assertFails(unauthDb.collection("stores").where("memberUids", "array-contains", OWNER_UID).get());
  await assertFails(unauthDb.collection("stores").doc(OWNER_UID).collection("products").get());
});

test("Owner can create store, query it, and update settings; ghost fields and future timestamps fail", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: "owner@mbg.ga", email_verified: true }).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    ownerDb.collection("stores").doc(OWNER_UID).set({
      ownerUid: OWNER_UID,
      memberUids: [OWNER_UID],
      invitedEmails: [],
      storeName: "MBG STOCK Depot",
      currency: "FCFA",
      defaultUnitsPerCrate: 24,
      defaultMinThreshold: 10,
      dataSaverEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Filtered query matching Kotlin StoreRepository
  await assertSucceeds(
    ownerDb.collection("stores").where("memberUids", "array-contains", OWNER_UID).get()
  );

  // Unfiltered list fails
  await assertFails(ownerDb.collection("stores").get());

  // Partial update succeeds
  await assertSucceeds(
    ownerDb.collection("stores").doc(OWNER_UID).update({
      storeName: "MBG STOCK Akanda",
      defaultMinThreshold: 12,
      updatedAt: pastDate(1000),
    })
  );

  // Shadow update with ghost field fails
  await assertFails(
    ownerDb.collection("stores").doc(OWNER_UID).update({
      storeName: "MBG STOCK Hack",
      isVerified: true,
      updatedAt: pastDate(1000),
    })
  );

  // Future timestamp fails
  await assertFails(
    ownerDb.collection("stores").doc(OWNER_UID).update({
      storeName: "Future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Staff accounts: Owner manages staff with hashed passwords; outsider denied", async () => {
  await seedStore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const ts = pastDate(8000);

  await assertSucceeds(
    ownerDb.collection("stores").doc(OWNER_UID).collection("staff").doc("staff_1").set({
      storeId: OWNER_UID,
      ownerUid: OWNER_UID,
      memberUids: [OWNER_UID],
      username: "gerant1",
      displayName: "Moussa K.",
      role: "GERANT",
      passwordSalt: validSalt,
      passwordHash: validHash,
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Filtered query succeeds for owner
  await assertSucceeds(
    ownerDb.collection("stores").doc(OWNER_UID).collection("staff")
      .where("memberUids", "array-contains", OWNER_UID).get()
  );

  // Outsider cannot read staff (unfiltered, cross-user filtered, or direct doc get)
  await assertFails(
    outsiderDb.collection("stores").doc(OWNER_UID).collection("staff").get()
  );
  await assertFails(
    outsiderDb.collection("stores").doc(OWNER_UID).collection("staff")
      .where("memberUids", "array-contains", OWNER_UID).get()
  );
  await assertFails(
    outsiderDb.collection("stores").doc(OWNER_UID).collection("staff").doc("staff_1").get()
  );
});

test("Products, Stock Movements & Sales: valid payloads succeed, negative stock & immutable history tampering fail", async () => {
  await seedStore(OWNER_UID, [OWNER_UID, MEMBER_UID], ["staff@mbg.ga"]);
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const memberDb = testEnv.authenticatedContext(MEMBER_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID).firestore();
  const ts = pastDate(8000);

  // Owner creates Regab product (NO id field inside doc body)
  await assertSucceeds(
    ownerDb.collection("stores").doc(OWNER_UID).collection("products").doc("prod_regab").set({
      storeId: OWNER_UID,
      ownerUid: OWNER_UID,
      memberUids: [OWNER_UID, MEMBER_UID],
      name: "Régab 65cl",
      category: "BOISSONS",
      purchasePrice: 550,
      sellingPrice: 700,
      quantity: 30,
      minThreshold: 10,
      unitsPerCrate: 12,
      createdByName: "Patron",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Member can query products and perform stock quantity deduction
  await assertSucceeds(
    memberDb.collection("stores").doc(OWNER_UID).collection("products")
      .where("memberUids", "array-contains", MEMBER_UID).get()
  );
  await assertSucceeds(
    memberDb.collection("stores").doc(OWNER_UID).collection("products").doc("prod_regab").update({
      quantity: 22,
      updatedAt: pastDate(1000),
    })
  );

  // Negative quantity must be rejected by rules
  await assertFails(
    memberDb.collection("stores").doc(OWNER_UID).collection("products").doc("prod_regab").update({
      quantity: -1,
      updatedAt: pastDate(500),
    })
  );

  // Member cannot rename or delete product (only owner can edit metadata/delete)
  await assertFails(
    memberDb.collection("stores").doc(OWNER_UID).collection("products").doc("prod_regab").update({
      name: "Hijacked",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    memberDb.collection("stores").doc(OWNER_UID).collection("products").doc("prod_regab").delete()
  );

  // Create immutable SaleRecord and StockMovement
  await assertSucceeds(
    memberDb.collection("stores").doc(OWNER_UID).collection("sales").doc("sale_uuid_1").set({
      storeId: OWNER_UID,
      ownerUid: OWNER_UID,
      memberUids: [OWNER_UID, MEMBER_UID],
      operatorId: "staff_1",
      operatorName: "Moussa K.",
      operatorRole: "GERANT",
      totalAmount: 5600,
      totalUnits: 8,
      itemCount: 1,
      itemsSummary: "8x Régab 65cl",
      lines: [
        {
          productId: "prod_regab",
          productName: "Régab 65cl",
          quantity: 8,
          unitPrice: 700,
          lineTotal: 5600,
        },
      ],
      saleDateMs: Date.now() - 2000,
      createdAt: pastDate(2000),
    })
  );

  await assertSucceeds(
    memberDb.collection("stores").doc(OWNER_UID).collection("movements").doc("mov_1").set({
      storeId: OWNER_UID,
      ownerUid: OWNER_UID,
      memberUids: [OWNER_UID, MEMBER_UID],
      type: "SORTIE",
      productId: "prod_regab",
      productName: "Régab 65cl",
      cratesCount: 0,
      extraBottles: 8,
      unitsPerCrate: 12,
      quantity: 8,
      stockBefore: 30,
      stockAfter: 22,
      unitPrice: 700,
      totalAmount: 5600,
      operatorId: "staff_1",
      operatorName: "Moussa K.",
      operatorRole: "GERANT",
      referenceId: "sale_uuid_1",
      operationDateMs: Date.now() - 2000,
      createdAt: pastDate(2000),
    })
  );

  // History & Sales are strictly immutable (cannot update or delete, even by owner)
  await assertFails(
    ownerDb.collection("stores").doc(OWNER_UID).collection("sales").doc("sale_uuid_1").update({
      totalAmount: 0,
    })
  );
  await assertFails(
    ownerDb.collection("stores").doc(OWNER_UID).collection("sales").doc("sale_uuid_1").delete()
  );
  await assertFails(
    ownerDb.collection("stores").doc(OWNER_UID).collection("movements").doc("mov_1").delete()
  );

  // Outsider cannot read products or sales (direct get, unfiltered, or cross-user filtered)
  await assertFails(
    outsiderDb.collection("stores").doc(OWNER_UID).collection("products").doc("prod_regab").get()
  );
  await assertFails(
    outsiderDb.collection("stores").doc(OWNER_UID).collection("sales").get()
  );
  await assertFails(
    outsiderDb.collection("stores").doc(OWNER_UID).collection("sales")
      .where("memberUids", "array-contains", OWNER_UID).get()
  );
});

test("Email invite check: unverified email spoof is rejected, verified invited user can join", async () => {
  await seedStore(OWNER_UID, [OWNER_UID], ["invited@mbg.ga"]);

  // Unverified email spoof must fail
  const spoofDb = testEnv.authenticatedContext(MEMBER_UID, { email: "invited@mbg.ga", email_verified: false }).firestore();
  await assertFails(
    spoofDb.collection("stores").where("invitedEmails", "array-contains", "invited@mbg.ga").get()
  );

  // Verified invited user can read invite and add own UID to memberUids
  const verifiedDb = testEnv.authenticatedContext(MEMBER_UID, { email: "invited@mbg.ga", email_verified: true }).firestore();
  await assertSucceeds(
    verifiedDb.collection("stores").where("invitedEmails", "array-contains", "invited@mbg.ga").get()
  );
  await assertSucceeds(
    verifiedDb.collection("stores").doc(OWNER_UID).update({
      memberUids: [OWNER_UID, MEMBER_UID],
      updatedAt: pastDate(500),
    })
  );
});
