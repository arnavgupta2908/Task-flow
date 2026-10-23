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

function sampleTask(userId, overrides = {}) {
  const now = new Date();
  return {
    userId,
    title: "Complete quarterly report",
    description: "Detailed analysis and summaries",
    importance: 3,
    estimatedEffortMinutes: 60,
    dueDate: Date.now() + 86400000,
    completed: false,
    category: "Work",
    priorityScore: 78.5,
    notificationEnabled: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Unauthenticated user: cannot read tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tasks").get());
});

test("Unauthenticated user: cannot create tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tasks").doc("task_1").set(sampleTask("anon")));
});

test("Authenticated user: cannot read another user's task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("bob_task").set(sampleTask(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tasks").doc("bob_task").get());
});

test("Authenticated user: can read their own task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("alice_task").set(sampleTask(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tasks").doc("alice_task").get());
});

test("Authenticated user: can query their own tasks with matching userId filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("alice_task").set(sampleTask(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tasks").where("userId", "==", ALICE_UID).get());
});

test("Authenticated user: fails query without userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tasks").get());
});

test("Authenticated user: can create valid task for themselves", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tasks").doc("task_alice").set(sampleTask(ALICE_UID)));
});

test("Authenticated user: cannot create task with spoofed userId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tasks").doc("spoofed_task").set(sampleTask(BOB_UID)));
});

test("Authenticated user: cannot update another user's task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("bob_task").set(sampleTask(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tasks").doc("bob_task").update({ title: "Hacked!" }));
});

test("Authenticated user: can update allowed fields of their own task", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("alice_task").set(sampleTask(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tasks").doc("alice_task").update({
      completed: true,
      updatedAt: new Date(),
    })
  );
});

test("Authenticated user: cannot modify immutable createdAt field", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("alice_task").set(sampleTask(ALICE_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("tasks").doc("alice_task").update({
      createdAt: new Date(Date.now() - 100000),
    })
  );
});

test("Authenticated user: can delete their own task but not others", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tasks").doc("alice_task").set(sampleTask(ALICE_UID));
    await context.firestore().collection("tasks").doc("bob_task").set(sampleTask(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tasks").doc("bob_task").delete());
  await assertSucceeds(aliceDb.collection("tasks").doc("alice_task").delete());
});
