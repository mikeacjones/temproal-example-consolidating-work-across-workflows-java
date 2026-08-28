const state = { snapshot: null, loading: false };

const $ = (selector) => document.querySelector(selector);
const patientSelect = $("#patient");
const addressSelect = $("#address");
const form = $("#order-form");

const escapeHtml = (value) => String(value ?? "")
  .replaceAll("&", "&amp;")
  .replaceAll("<", "&lt;")
  .replaceAll(">", "&gt;")
  .replaceAll('"', "&quot;")
  .replaceAll("'", "&#039;");

const formatDuration = (value) => value?.replace("PT", "").toLowerCase() || "—";
const formatTime = (epoch) => epoch ? new Date(epoch).toLocaleTimeString() : "—";

function statusClass(status = "") {
  if (status.includes("COMPLETED") || status.includes("SUCCESS")) return "success";
  if (status.includes("FAIL") || status.includes("UNAVAILABLE")) return "error";
  if (status.includes("WAIT") || status.includes("COLLECT") || status.includes("APPROVAL")) return "wait";
  return "";
}

function statusPill(status) {
  return `<span class="pill ${statusClass(status)}">${escapeHtml(status?.replaceAll("_", " ") || "unknown")}</span>`;
}

function populatePatients() {
  const patients = state.snapshot?.patients || [];
  const selected = patientSelect.value;
  patientSelect.innerHTML = patients.map(patient =>
    `<option value="${escapeHtml(patient.id)}">${escapeHtml(patient.name)} · ${escapeHtml(patient.id)}</option>`
  ).join("");
  if (patients.some(patient => patient.id === selected)) patientSelect.value = selected;
  populateAddresses();
}

function populateAddresses() {
  const patient = state.snapshot?.patients?.find(candidate => candidate.id === patientSelect.value);
  const selected = addressSelect.value;
  addressSelect.innerHTML = (patient?.addresses || []).map(address =>
    `<option value="${escapeHtml(address.id)}">${escapeHtml(address.label)} · ${escapeHtml(address.street)}, ${escapeHtml(address.city)}</option>`
  ).join("");
  if (patient?.addresses.some(address => address.id === selected)) addressSelect.value = selected;
}

function renderTiming() {
  const timing = state.snapshot?.timing;
  if (!timing) return;
  $("#timing").innerHTML = `
    <span>Batch window <strong>${escapeHtml(formatDuration(timing.consolidationWindow))}</strong></span>
    <span>Shipment activity <strong>${escapeHtml(formatDuration(timing.shipmentActivityDuration))}</strong></span>
    <span>Item activity <strong>${escapeHtml(formatDuration(timing.itemStepDuration))}</strong></span>
  `;
}

function renderConsolidations() {
  const target = $("#consolidations");
  const workflows = state.snapshot?.consolidations || [];
  if (!workflows.length) {
    target.className = "grid empty-state";
    target.textContent = "No consolidation workflows yet.";
    return;
  }
  target.className = "grid";
  target.innerHTML = workflows.map(entry => {
    const workflow = entry.workflow;
    const current = workflow?.currentBatchItems || [];
    const next = workflow?.nextBatchItems || [];
    return `
      <article class="consolidation-card">
        <div class="card-top">
          <div>
            <h3>${escapeHtml(entry.patientId)} → ${escapeHtml(entry.addressId)}</h3>
            <p class="workflow-id">${escapeHtml(entry.workflowId)}</p>
          </div>
          ${statusPill(entry.temporalStatus)}
        </div>
        <div class="meta-row">
          <span>Run / batch <strong>${workflow?.batchNumber ?? "—"}</strong></span>
          <span>First arrival <strong>${formatTime(workflow?.firstArrivalEpochMillis)}</strong></span>
          <span>Window closes <strong>${formatTime(workflow?.windowClosesEpochMillis)}</strong></span>
        </div>
        <div class="batch-lanes">
          <div class="batch-lane">
            <strong>Current batch · ${current.length}</strong>
            ${current.map(item => `<span>${escapeHtml(item.orderId)} / ${escapeHtml(item.itemId)}</span>`).join("") || "<span>Empty</span>"}
          </div>
          <div class="batch-lane">
            <strong>Arrived during shipment · ${next.length}</strong>
            ${next.map(item => `<span>${escapeHtml(item.orderId)} / ${escapeHtml(item.itemId)}</span>`).join("") || "<span>Empty</span>"}
          </div>
        </div>
      </article>`;
  }).join("");
}

function renderOrders() {
  const target = $("#orders");
  const orders = state.snapshot?.orders || [];
  if (!orders.length) {
    target.className = "stack empty-state";
    target.textContent = "Fire an order to begin.";
    return;
  }
  target.className = "stack";
  target.innerHTML = orders.map(entry => `
    <article class="order-card">
      <div class="card-top">
        <div>
          <h3>Order ${escapeHtml(entry.order.orderId)}</h3>
          <p class="workflow-id">${escapeHtml(entry.order.orderWorkflowId)}</p>
        </div>
        ${statusPill(entry.temporalStatus)}
      </div>
      <div class="meta-row">
        <span>${escapeHtml(entry.order.patientId)}</span>
        <span>→ ${escapeHtml(entry.order.addressId)}</span>
        <span>${escapeHtml(entry.order.preset || "showcase")}</span>
      </div>
      <div class="items">
        ${entry.items.map(item => `
          <div class="item">
            <div class="item-top">
              <h3>${escapeHtml(item.itemId)}</h3>
              ${statusPill(item.state)}
            </div>
            <p>${escapeHtml(item.description)}</p>
            <p class="workflow-id">${escapeHtml(item.itemWorkflowId)}</p>
            <div class="steps">
              ${(item.completedSteps || []).map(step => `<span class="step">${escapeHtml(step)}</span>`).join("")}
            </div>
            ${item.awaitingApproval && !item.approved
              ? `<button data-approve="${escapeHtml(item.itemWorkflowId)}">Approve human task</button>`
              : ""}
          </div>`).join("")}
      </div>
    </article>`).join("");

  target.querySelectorAll("[data-approve]").forEach(button => {
    button.addEventListener("click", () => approveItem(button.dataset.approve, button));
  });
}

async function refresh() {
  if (state.loading) return;
  state.loading = true;
  try {
    const response = await fetch("/api/demo");
    if (!response.ok) throw new Error(`API returned ${response.status}`);
    const firstLoad = !state.snapshot;
    state.snapshot = await response.json();
    $("#connection-status").className = "pill success";
    $("#connection-status").textContent = "Connected";
    if (firstLoad) populatePatients();
    renderTiming();
    renderConsolidations();
    renderOrders();
  } catch (error) {
    $("#connection-status").className = "pill error";
    $("#connection-status").textContent = "API unavailable";
  } finally {
    state.loading = false;
  }
}

async function approveItem(workflowId, button) {
  button.disabled = true;
  try {
    const response = await fetch(`/api/items/${encodeURIComponent(workflowId)}/approve`, { method: "POST" });
    if (!response.ok) throw new Error("Approval failed");
    await refresh();
  } finally {
    button.disabled = false;
  }
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = $("#submit-order");
  const message = $("#form-message");
  button.disabled = true;
  message.textContent = "Submitting…";
  try {
    const response = await fetch("/api/orders", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        patientId: patientSelect.value,
        addressId: addressSelect.value,
        preset: $("#preset").value,
      }),
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(body.error || "Order submission failed");
    message.textContent = `Started ${body.orderId} (${body.workflowId})`;
    await refresh();
  } catch (error) {
    message.textContent = error.message;
  } finally {
    button.disabled = false;
  }
});

patientSelect.addEventListener("change", populateAddresses);
refresh();
setInterval(refresh, 2000);
