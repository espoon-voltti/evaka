<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Async & Scheduled Jobs

## Purpose

eVaka provides a framework for executing background tasks safely with transactional guarantees. The framework handles two types of jobs: one-off asynchronous jobs planned during transaction processing, and scheduled recurring jobs triggered by cron expressions or daily schedules.

**Implementation**: `service/src/main/kotlin/fi/espoo/evaka/shared/async/AsyncJobRunner.kt`, `AsyncJob.kt`, `service/src/main/kotlin/fi/espoo/evaka/shared/job/ScheduledJobRunner.kt`, `ScheduledJobs.kt`

## Architecture & Transactional Safety

### The Problem

Database transactions cannot safely perform side effects like sending emails or calling external APIs. Consider this scenario:

```kotlin
tx.execute { sql("INSERT INTO users ...") }
emailClient.send(welcomeEmail)  // Email sent
// Transaction rolls back due to an error
// Email already sent, but user was never created!
```

Once the email is sent, it cannot be unsent if the transaction fails. This creates data inconsistency.

### The Solution

The async job framework solves this by **planning** jobs within transactions. The job is persisted to the database as part of the transaction commit:

```kotlin
tx.execute { sql("INSERT INTO users ...") }
asyncJobRunner.plan(tx, listOf(AsyncJob.SendWelcomeEmail(...)), runAt = clock.now())
// If transaction commits → job executes
// If transaction rolls back → job never persisted, never executes
```

This provides **exactly-once semantics**: the job executes if and only if the transaction commits successfully.

### Retry Mechanism

Jobs that fail are automatically retried:
- Fixed retry interval between attempts (default: 5 minutes)
- Configurable retry count (default: 288 retries = 24 hours)
- After exhausting retries → developer alert for manual investigation

### Critical: Idempotency Requirement

**Job handlers MUST be idempotent** (safe to execute multiple times with the same effect). Network failures, timeouts, or crashes can result in duplicate executions.

❌ **Bad pattern** - Not idempotent:
```kotlin
fun sendParentEmails(job: AsyncJob.NotifyBothParents) {
    emailClient.send(job.parent1Email, message)
    emailClient.send(job.parent2Email, message)  // parent1 gets duplicate on retry
}
```

✅ **Good pattern** - Idempotent:
```kotlin
// Plan one job per email, each independently retryable
asyncJobRunner.plan(tx, listOf(
    AsyncJob.SendEmail(job.parent1Email, message),
    AsyncJob.SendEmail(job.parent2Email, message)
), runAt = clock.now())
```

If the first email succeeds and the second fails, only the second job retries. Each email is sent exactly once.

## Planning a Job

The most common use case is planning an existing job type within a transaction.

### Basic Example

```kotlin
fun createUser(tx: Database.Transaction, clock: EvakaClock, request: CreateUserRequest) {
    val userId = tx.createQuery {
        sql("""
            INSERT INTO users (name, email)
            VALUES (${bind(request.name)}, ${bind(request.email)})
            RETURNING id
        """)
    }.exactlyOne<UserId>()

    // Plan email job within same transaction
    asyncJobRunner.plan(
        tx,
        payloads = listOf(AsyncJob.SendWelcomeEmail(userId)),
        retryCount = 10,
        runAt = clock.now()
    )
}
```

### Retry Count Guidance

Choose retry count based on likely failure types:

- **High retry count** - Transient failures: network issues, email service downtime, temporary API unavailability
- **Low retry count** - Likely permanent failures where retrying is futile

## Creating a New Job Type

Adding a new job type requires three steps: define the payload, register the handler, and assign to a pool.

### Step 1: Define the Payload

Add a data class to the `AsyncJob` sealed interface in `AsyncJob.kt`:

```kotlin
data class SendWelcomeEmail(
    val userId: PersonId,
    val language: Language
) : AsyncJob {
    override val user: AuthenticatedUser? = null
}
```

### Step 2: Register the Handler

Register in the service class that will process this job type:

```kotlin
@Service
class UserEmailJobs(
    private val emailClient: EmailClient,
    asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    init {
        asyncJobRunner.registerHandler(::sendWelcomeEmail)
    }

    fun sendWelcomeEmail(
        dbc: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendWelcomeEmail
    ) {
        val user = dbc.read { it.getUser(job.userId) }

        emailClient.send(Email.create(
            toAddress = user.email,
            subject = "Welcome!",
            body = "Welcome ${user.name}"
        ))
    }
}
```

**Alternative: Lambda style**
```kotlin
init {
    asyncJobRunner.registerHandler<SendWelcomeEmail> { dbc, clock, job ->
        // implementation
    }
}
```

### Step 3: Assign to Pool

Add the job class to the appropriate pool in `AsyncJob.kt` companion object:

```kotlin
companion object {
    val email = AsyncJobRunner.Pool(
        AsyncJobPool.Id(AsyncJob::class, "email"),
        AsyncJobPool.Config(concurrency = 1),
        setOf(
            SendWelcomeEmail::class,  // Add your job here
            SendPasswordChangedEmail::class,
            // ... other email jobs
        )
    )
}
```

**Available pools:**
- `main` - General background work
- `email` - Email sending
- `urgent` - Time-sensitive operations
- `nightly` - Scheduled nightly batch jobs
- `suomiFi` - Suomi.fi message sending
- `varda` - Varda integration
- `archival` - Document archival jobs

## Scheduled Jobs

Scheduled jobs are recurring jobs defined in `ScheduledJobs.kt`. The scheduler plans async jobs at specified times, which then execute the job logic.

### Scheduling Mechanisms

There are two ways to schedule jobs, depending on whether precise timing matters:

**1. Nightly Pool (Recommended for most batch jobs)**

Use `JobSchedule.nightly()` when you don't need precise timing control. All nightly jobs:
- Share a single execution time (default: 00:10, configurable via `Nightly.configureNightlyTime()`)
- Run serially through the `nightly` pool (concurrency=1)
- Don't require picking arbitrary times like 2:00, 2:05, 2:10, etc.

```kotlin
enum class ScheduledJob(...) {
    CleanupExpiredData(
        ScheduledJobs::cleanupExpiredData,
        ScheduledJobSettings(
            enabled = true,
            schedule = JobSchedule.nightly()  // Runs at 00:10, in series with other nightly jobs
        )
    ),
    GenerateFinanceDecisions(
        ScheduledJobs::generateFinanceDecisions,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly())
    ),
}
```

Behind the scenes: Creates `AsyncJob.RunNightlyJob` → assigned to `nightly` pool → runs serially.

**2. Specific Times (For timing-sensitive operations)**

Use `JobSchedule.daily(LocalTime)` or `JobSchedule.cron(String)` when precise timing matters:
- External system integration requires specific time
- Job must run multiple times per day
- Job needs to run at a time different from other batch jobs

```kotlin
EndOfDayAttendanceUpkeep(
    ScheduledJobs::endOfDayAttendanceUpkeep,
    ScheduledJobSettings(
        enabled = true,
        schedule = JobSchedule.daily(LocalTime.of(0, 0))  // Must run exactly at midnight
    )
),
EndOfDayStaffAttendanceUpkeep(
    ScheduledJobs::endOfDayStaffAttendanceUpkeep,
    ScheduledJobSettings(
        enabled = true,
        schedule = JobSchedule.cron("0 55 * * * *")  // Every hour at minute 55
    )
),
FreezeVoucherValueReports(
    ScheduledJobs::freezeVoucherValueReports,
    ScheduledJobSettings(
        enabled = true,
        schedule = JobSchedule.cron("0 0 0 25 * ?")  // Monthly on 25th
    )
),
```

**Cron format:** `second minute hour day month weekday`

Behind the scenes: Creates `AsyncJob.RunScheduledJob` → assigned to `main` pool → can run in parallel with other jobs.

### Implementation Pattern

```kotlin
@Component
class ScheduledJobs(...) {
    fun cleanupExpiredData(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            tx.execute { sql("DELETE FROM temp_data WHERE expires_at < ${bind(clock.now())}") }
        }
    }
}
```

The scheduled job function can also plan additional async jobs if needed (e.g., sending notification emails).

### Configuration

Jobs can be enabled/disabled via `ScheduledJobsEnv` configuration without code changes.

## Testing

Use `runPendingJobsSync()` to execute pending jobs synchronously in tests.

```kotlin
@Test
fun `sends email when user created`() {
    val clock = MockEvakaClock(HelsinkiDateTime.now())

    db.transaction { tx ->
        createUser(tx, clock, CreateUserRequest("Alice", "alice@example.com"))
    }

    // Execute planned jobs synchronously
    asyncJobRunner.runPendingJobsSync(clock)

    // Assert email was sent
    assertEquals(1, emailClient.sentEmails.size)
    assertEquals("alice@example.com", emailClient.sentEmails[0].toAddress)
}
```

Jobs can plan other jobs during execution. In those cases call `runPendingJobsSync()` multiple times until all chained jobs complete.

End-to-end tests can also call `runPendingJobsSync()` through a dev API request.

## See Also

- [Database API](database.md) - Transaction management and queries
- [Testing Conventions](testing.md) - Testing patterns and fixtures
