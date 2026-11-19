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
const CHARLIE_UID = "charlie_789";
const WORKSPACE_ID = "EQP_815A";

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

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("patios").get());
  await assertFails(unauthDb.collection("slots").get());
  await assertFails(unauthDb.collection("history").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("patios").doc("patio_1").set({
      userId: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      memberUids: [ALICE_UID],
      name: "Pátio Sul",
      kind: "patio",
      last: "M",
      ts: new Date(Date.now() - 1000),
    })
  );
});

test("Users and Workspace: profile creation without PII, team join, and stranger isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("workspaces").doc(WORKSPACE_ID).set({
      ownerId: ALICE_UID,
      name: "Fazenda Esperança",
      memberUids: [ALICE_UID],
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "João",
      workspaceId: WORKSPACE_ID,
      updatedAt: now,
    })
  );

  // Reject PII field like email inside public-within-workspace users/{uid}
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      name: "Maria",
      email: "maria@example.com",
      workspaceId: WORKSPACE_ID,
      updatedAt: now,
    })
  );

  // Bob joins workspace by code and creates his profile
  await assertSucceeds(
    bobDb.collection("workspaces").doc(WORKSPACE_ID).update({
      memberUids: [ALICE_UID, BOB_UID],
    })
  );
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      uid: BOB_UID,
      name: "Maria",
      workspaceId: WORKSPACE_ID,
      updatedAt: now,
    })
  );

  // Alice and Bob (same workspaceId) can read each other's display name
  await assertSucceeds(aliceDb.collection("users").doc(BOB_UID).get());
  await assertSucceeds(bobDb.collection("users").doc(ALICE_UID).get());

  // Charlie (different workspace) cannot read Alice's user profile
  await assertSucceeds(
    charlieDb.collection("users").doc(CHARLIE_UID).set({
      uid: CHARLIE_UID,
      name: "Carlos",
      workspaceId: "OUTRA_EQUIPE",
      updatedAt: now,
    })
  );
  await assertFails(charlieDb.collection("users").doc(ALICE_UID).get());
});

test("Multi-phone sharing: patios and slots shared across members, isolated from non-members", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("patios").doc("patio_1").set({
      userId: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      memberUids: [ALICE_UID, BOB_UID],
      name: "Pátio 1",
      kind: "patio",
      last: "D",
      ts: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("slots").doc("patio_1_B_006").set({
      userId: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      memberUids: [ALICE_UID, BOB_UID],
      pid: "patio_1",
      s: "B",
      i: 6,
      t: "B",
      n: "815",
      f: 140,
      c: 1,
      updatedByUid: ALICE_UID,
      updatedByName: "João",
      updatedAt: now,
    })
  );

  // Bob can query with array-contains memberUids and edit the slot with attribution
  await assertSucceeds(
    bobDb.collection("patios").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("slots").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("slots").doc("patio_1_B_006").update({
      f: 90,
      updatedByUid: BOB_UID,
      updatedByName: "Maria",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Unfiltered list queries still fail
  await assertFails(aliceDb.collection("patios").get());
  await assertFails(bobDb.collection("slots").get());

  // Stranger Charlie cannot read, modify, or delete Alice & Bob's patio or slot
  await assertFails(charlieDb.collection("patios").doc("patio_1").get());
  await assertFails(charlieDb.collection("slots").doc("patio_1_B_006").get());
  await assertFails(
    charlieDb.collection("slots").doc("patio_1_B_006").update({
      f: 10,
      updatedByUid: CHARLIE_UID,
      updatedByName: "Carlos",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(charlieDb.collection("patios").doc("patio_1").delete());
});

test("History collection: append-only (create & list allowed, update & delete strictly forbidden)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("history").doc("h_001").set({
      ts: now,
      u: ALICE_UID,
      t: "João adicionou o bloco 815 (140 fardos) no Pátio 1, rua B, posição 7",
      p: "Pátio 1",
      workspaceId: WORKSPACE_ID,
      memberUids: [ALICE_UID, BOB_UID],
    })
  );

  // Author and team member can read/list
  await assertSucceeds(
    bobDb.collection("history").where("memberUids", "array-contains", BOB_UID).get()
  );

  // Non-member cannot read
  await assertFails(charlieDb.collection("history").doc("h_001").get());

  // Spoofing another author UID fails
  await assertFails(
    bobDb.collection("history").doc("h_002").set({
      ts: now,
      u: ALICE_UID,
      t: "Texto falsificado",
      p: "Pátio 1",
      workspaceId: WORKSPACE_ID,
      memberUids: [ALICE_UID, BOB_UID],
    })
  );

  // Update and delete on history MUST ALWAYS fail even for the author
  await assertFails(
    aliceDb.collection("history").doc("h_001").update({ t: "Editado" })
  );
  await assertFails(aliceDb.collection("history").doc("h_001").delete());
});

test("Conferência: member toggles slot c=1/c=0 and records immutable history", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("patios").doc("patio_1").set({
      userId: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      memberUids: [ALICE_UID, BOB_UID],
      name: "Pátio 1",
      kind: "patio",
      last: "D",
      ts: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("slots").doc("patio_1_A_000").set({
      userId: ALICE_UID,
      workspaceId: WORKSPACE_ID,
      memberUids: [ALICE_UID, BOB_UID],
      pid: "patio_1",
      s: "A",
      i: 0,
      t: "B",
      n: "815",
      f: 140,
      c: 0,
      updatedByUid: ALICE_UID,
      updatedByName: "João",
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("slots").doc("patio_1_A_000").update({
      c: 1,
      memberUids: [ALICE_UID, BOB_UID],
      updatedByUid: BOB_UID,
      updatedByName: "Maria",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    bobDb.collection("history").doc("h_conf_1").set({
      ts: new Date(Date.now() - 1000),
      u: BOB_UID,
      t: "Maria conferiu o bloco 815",
      p: "Pátio 1",
      workspaceId: WORKSPACE_ID,
      memberUids: [ALICE_UID, BOB_UID],
    })
  );
});

