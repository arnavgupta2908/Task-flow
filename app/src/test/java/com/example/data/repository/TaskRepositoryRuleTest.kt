package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TaskRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createTask_validPayload_createsDocumentAndReturnsId() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = TaskRepository(firestore)

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTask(
        title = TASK_TITLE,
        description = "Testing intelligent priority",
        importance = 3,
        estimatedEffortMinutes = 30,
        dueDate = System.currentTimeMillis() + 3600000L
      )
    }
    assertTrue("Create task should succeed", createResult.isSuccess)
    val taskId = createResult.getOrThrow()
    assertTrue("TaskId should not be empty", taskId.isNotEmpty())
  }

  @Test
  fun observeTasks_authenticatedOwner_emitsMatchingTasks() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = TaskRepository(firestore)
    val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTask(TASK_TITLE).getOrThrow()
    }

    val emittedTasks = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeTasks().first { list -> list.any { it.id == taskId } }
    }
    assertTrue("Emitted list should contain created task", emittedTasks.any { it.id == taskId })
  }

  @Test
  fun getTaskById_authenticatedOwner_returnsTask() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = TaskRepository(firestore)
    val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTask(TASK_TITLE).getOrThrow()
    }

    val taskResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getTaskById(taskId)
    }
    assertTrue("Fetching task should succeed", taskResult.isSuccess)
    val task = taskResult.getOrThrow()
    assertEquals(taskId, task.id)
    assertEquals(TASK_TITLE, task.title)
  }

  @Test
  fun getTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = TaskRepository(firestore)
    val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createTask(TASK_TITLE).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = TaskRepository(firestore)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        bobRepo.getTaskById(taskId).getOrThrow()
      }
      fail("Cross-user access should have failed")
    } catch (e: Exception) {
      val firestoreException = e.unwrapFirestoreException()
      val isDenied = firestoreException?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                     e is SecurityException
      assertTrue("Cross-user access must be denied: $e", isDenied)
    }
  }

  @Test
  fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = TaskRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repository.observeTasks().first() }
      fail("Unauthenticated observation should fail")
    } catch (e: Exception) {
      assertNotNull("Expected unauthenticated query failure", e)
    }
  }

  @Test
  fun updateTask_allowedFields_succeeds() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = TaskRepository(firestore)
    val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTask(TASK_TITLE).getOrThrow()
    }

    val task = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getTaskById(taskId).getOrThrow()
    }

    val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.toggleTaskCompleted(task)
    }
    assertTrue("Toggle completed should succeed", updateResult.isSuccess)

    val updatedTask = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.getTaskById(taskId).getOrThrow()
    }
    assertTrue("Task should now be completed", updatedTask.completed)
  }

  @Test
  fun deleteTask_authenticatedOwner_succeeds() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = TaskRepository(firestore)
    val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTask(TASK_TITLE).getOrThrow()
    }

    val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.deleteTask(taskId)
    }
    assertTrue("Delete task should succeed", deleteResult.isSuccess)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@test.com"
    const val BOB_EMAIL = "bob@test.com"
    const val TASK_TITLE = "Organize sprint backlog"
    const val DEFAULT_TIMEOUT_MS = 6000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
