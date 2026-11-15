const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const ts = () => new Date(Date.now() - 2000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const ADMIN_UID = "admin_uid_999";
const ADMIN_EMAIL = "ai.master.bot.official@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated caller cannot read users, memories, or app_config", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("memories").get());
  await assertFails(unauthDb.collection("app_config").doc("global").get());
});

test("Owner can create own user profile as free/active and read it", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_ai",
      fullName: "Alice",
      email: "alice@example.com",
      phone: "",
      authProvider: "google",
      emailVerified: true,
      tier: "free",
      status: "active",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Privilege escalation blocked: user cannot create profile with tier=ultra or ghost field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_ai",
      fullName: "Alice",
      email: "alice@example.com",
      tier: "ultra",
      status: "active",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_ai",
      fullName: "Alice",
      email: "alice@example.com",
      tier: "free",
      status: "active",
      isAdmin: true,
      createdAt: ts(),
      updatedAt: ts(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice conversations or memories", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").set({
      userId: ALICE_UID,
      category: "preference",
      title: "Language",
      content: "Prefers concise Hinglish replies",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("memories").doc("mem_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("memories").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Email spoofing test: unverified admin email cannot write app_config or read audit_logs", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofDb.collection("app_config").doc("global").set({
      maintenanceMode: true,
      latestVersionName: "1.0",
      latestVersionCode: 1,
      defaultModel: "gemini-2.5-flash",
      updatedAt: ts(),
    })
  );
  await assertFails(spoofDb.collection("audit_logs").get());
});

test("Verified admin can manage app_config, announcements, licenses, and audit_logs", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("app_config").doc("global").set({
      maintenanceMode: false,
      latestVersionName: "1.0.0",
      latestVersionCode: 1,
      defaultModel: "gemini-2.5-flash",
      announcementBanner: "Welcome to NISHA AI",
      updatedBy: ADMIN_UID,
      updatedAt: ts(),
    })
  );

  await assertSucceeds(
    adminDb.collection("announcements").doc("ann_1").set({
      title: "Release 1.0",
      body: "NISHA AI is live.",
      targetMode: "all",
      createdBy: ADMIN_UID,
      createdAt: ts(),
    })
  );

  await assertSucceeds(
    adminDb.collection("audit_logs").doc("log_1").set({
      actorUid: ADMIN_UID,
      actorEmail: ADMIN_EMAIL,
      action: "UPDATE_CONFIG",
      target: "app_config/global",
      details: "Initialized global configuration",
      createdAt: ts(),
    })
  );

  // Audit logs are immutable: update/delete must fail even for admin
  await assertFails(
    adminDb.collection("audit_logs").doc("log_1").update({ details: "tampered" })
  );
  await assertFails(
    adminDb.collection("audit_logs").doc("log_1").delete()
  );
});

test("Regular user can read broadcast announcements and own licenses, not others", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("announcements").doc("ann_all").set({
      title: "System Update",
      body: "All systems green.",
      targetMode: "all",
      createdBy: ADMIN_UID,
      createdAt: ts(),
    })
  );

  await assertSucceeds(
    adminDb.collection("licenses").doc("lic_alice").set({
      userId: ALICE_UID,
      plan: "pro",
      status: "active",
      source: "admin_grant",
      createdAt: ts(),
      updatedAt: ts(),
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("announcements").where("targetMode", "==", "all").get()
  );
  await assertSucceeds(
    aliceDb.collection("licenses").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("licenses").doc("lic_alice").get()
  );
});
