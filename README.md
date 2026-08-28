# Consolidated shipping with Temporal Java

This is a runnable demo of orders that fan out into independent item Workflows and converge on one shipment Workflow per patient and ship-to address.

Everything runs in containers: Temporal Server, PostgreSQL, the Temporal UI, the Java backend/Worker, and a separate static web UI. No local Java installation is required.

## Run it

```bash
docker compose up --build
```

Open:

- Demo UI: http://localhost:3000
- Temporal UI: http://localhost:8080
- Backend API: http://localhost:8081/api/demo

The production-like default consolidation window is eight hours. Override any timing with an ISO-8601 duration:

```bash
CONSOLIDATION_WINDOW=PT5M \
SHIPMENT_ACTIVITY_DURATION=PT1M \
ITEM_STEP_DURATION=PT1S \
docker compose up --build
```

You can also copy `.env.example` to `.env` and edit it.

Stop the stack without deleting Temporal history:

```bash
docker compose down
```

For a completely clean demo, including deletion of the local PostgreSQL/Temporal volume:

```bash
docker compose down -v
```

## Try the Continue-As-New path

For a quick manual run, use a shorter batch window such as `PT20S` while retaining the one-minute shipment activity.

1. Fire a three-item showcase order and approve its human task.
2. Wait until the consolidation card says `SHIPPING`.
3. Fire another order for the same patient and address during that one-minute activity.
4. The new items appear in **Arrived during shipment**.
5. Once the first shipment finishes, the consolidation Workflow continues as new. The same Workflow ID now shows batch 2, whose deadline is still based on the first queued item's arrival time.

Use a different patient or address to see an independent consolidation Workflow ID.

## Design

```text
Order Workflow
  ├─ item Workflow: no final activities
  ├─ item Workflow: approval, wait for shipment, then final activities
  └─ item Workflow: submit, run activities concurrently, then wait for shipment
          │
          └─ short gateway Activity ── Signal-with-Start(addItem)
                                      │
patient + address consolidation Workflow
  collect from first arrival → durable timer → shipment Activity
                                      │
                                      └─ shipmentCompleted signal to every item Workflow
```

The deterministic ID is `consolidation-{patientId}-{addressId}`. Signal-with-Start atomically starts that Workflow if it is absent or signals the current run if it exists. A stable item Workflow ID is also used as the submission ID, so Activity retries and delivery duplicates are ignored.

Signal-with-Start is intentionally used instead of Update-with-Start. The enqueue operation does not need a business response: the actual response is the later `shipmentCompleted` signal. An Update would add a request/response lifecycle without improving this flow.

The consolidation window and synthetic activity durations are read from environment variables in one backend configuration class. They are placed in the first consolidation run's input and explicitly carried into Continue-As-New, which keeps replay deterministic even if a container is later restarted with different environment values.

### Arrivals during shipment

When the timer fires, the Workflow snapshots the current batch and changes phase to `SHIPPING`. Signal handlers remain available while the shipment Activity is running. New signals go into a separate next-batch map and record the first next-batch arrival time.

After shipping:

1. The Workflow signals all item Workflows in the shipped snapshot.
2. It waits for message handlers to finish.
3. If the next-batch map is non-empty, it passes that map, its original first-arrival timestamp, timing configuration, and deduplication IDs to Continue-As-New.

Signal-with-Start is safe at the Continue-As-New boundary: Temporal ensures a signal racing with completion is processed rather than silently lost. The explicit state transfer handles signals already processed by the old run.

## Source layout

- `src/main/java/.../workflow`: stable Workflow interfaces
- `src/main/java/.../workflow/impl`: deterministic Workflow implementations
- `src/main/java/.../activity`: side effects and the client-side Signal-with-Start gateway
- `src/main/java/.../api`: Spring REST API and demo-only in-memory UI registry
- `src/main/java/.../domain`: serialized contracts shared by Workflows, Activities, and API
- `ui`: dependency-free HTML/CSS/JavaScript frontend, served by its own Nginx container

The in-memory API registry is only a convenience for this demo dashboard; Temporal remains the source of truth for execution state. Restarting the backend clears the dashboard's order list but does not delete Temporal histories.

## Tests

The main workflow test deliberately blocks the first shipment Activity, sends another item during that Activity, and verifies that batch 2 runs after Continue-As-New:

```bash
docker build --target build -t consolidated-shipping-build .
```

Or, with Maven installed:

```bash
mvn test
```
