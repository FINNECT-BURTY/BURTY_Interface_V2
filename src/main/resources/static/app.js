const state = {
  token: "",
  userId: "",
  actionType: ""
};

const el = (id) => document.getElementById(id);
const money = (value) => `${Number(value || 0).toLocaleString("ko-KR")}원`;

async function api(path, options = {}) {
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }
  const response = await fetch(path, { ...options, headers });
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }
  const payload = await response.json();
  return payload.data ?? payload;
}

async function bootstrap() {
  setLoading("데모 세션을 준비하고 있습니다.");
  const demo = await api("/api/burty/auth/demo/session", { method: "POST", body: "{}" });
  state.token = demo.accessToken;
  state.userId = demo.userId;
  el("scenarioText").textContent = demo.scenario;
  await loadDashboard();
}

async function loadDashboard() {
  setLoading("현금흐름을 분석하고 있습니다.");
  const query = `userId=${encodeURIComponent(state.userId)}`;
  const [forecast, risk, action, causes, policies, report, calendar] = await Promise.all([
    api(`/api/burty/cashflow/forecast?${query}`),
    api(`/api/burty/cashflow/risk?${query}`),
    api(`/api/burty/cashflow/action?${query}`),
    api(`/api/burty/cashflow-management/risk-causes?${query}`),
    api(`/api/burty/policy/match?${query}`),
    api("/api/burty/ai/consult", {
      method: "POST",
      body: JSON.stringify({ userId: state.userId, question: "이번 달을 무사히 넘기려면 지금 무엇부터 해야 해?" })
    }),
    api(`/api/burty/cashflow-management/calendar?${query}`)
  ]);

  renderRisk(forecast, risk);
  renderAction(action);
  renderCauses(causes);
  renderPolicies(policies);
  renderReport(report);
  renderCalendar(calendar);
  drawChart(forecast.dailyBalances || [], forecast.safetyBalance || 0);
}

function setLoading(message) {
  el("riskTitle").textContent = message;
}

function renderRisk(forecast, risk) {
  const band = document.querySelector(".risk-band");
  band.classList.remove("red", "yellow");
  const level = risk.level || "GREEN";
  if (level === "RED") band.classList.add("red");
  if (level === "YELLOW") band.classList.add("yellow");

  const riskDate = forecast.riskDate ? `${forecast.riskDate} 위험 예상` : "30일 내 위험 낮음";
  el("riskTitle").textContent = level === "GREEN" ? "이번 달은 안정권입니다." : "이번 달 현금흐름 방어가 필요합니다.";
  el("riskReason").textContent = forecast.riskReason || risk.reason || "";
  el("riskLevel").textContent = level;
  el("minBalance").textContent = money(forecast.minimumBalance);
  el("riskDate").textContent = riskDate;
}

function renderAction(action) {
  state.actionType = action.actionType;
  el("actionTitle").textContent = action.title || "현 상태 유지";
  el("actionDesc").textContent = action.description || "";
  el("actionImpact").textContent = money(action.estimatedImprovement);
  el("actionScore").textContent = Number(action.priorityScore || 0).toFixed(1);
}

function renderCauses(causes) {
  el("causeList").innerHTML = (causes || []).slice(0, 5).map((cause) => `
    <li>
      <strong>${escapeHtml(cause.label || cause.causeType)}</strong>
      <span>${money(cause.impactAmount)} · ${escapeHtml(cause.description || "")}</span>
    </li>
  `).join("");
}

function renderPolicies(policies) {
  el("policyList").innerHTML = (policies || []).slice(0, 3).map((policy) => `
    <li>
      <strong>${escapeHtml(policy.policyName)}</strong>
      <span>${escapeHtml(policy.supportType)} · ${escapeHtml(policy.reason)} · ${policy.priorityScore}점</span>
    </li>
  `).join("");
}

function renderReport(report) {
  el("aiSummary").textContent = report.summary || "";
  el("aiActions").innerHTML = (report.recommendedActions || [])
    .map((item) => `<span>${escapeHtml(item)}</span>`)
    .join("");
}

function renderCalendar(days) {
  el("calendar").innerHTML = (days || []).slice(0, 30).map((day) => `
    <div class="day ${day.riskDay ? "risk" : ""}">
      <strong>${formatDate(day.date)}</strong>
      <small class="balance">${money(day.expectedBalance)}</small>
      ${(day.events || []).slice(0, 2).map((event) => `<small>${escapeHtml(event)}</small>`).join("")}
    </div>
  `).join("");
}

function drawChart(points, safetyBalance) {
  const canvas = el("balanceChart");
  const ctx = canvas.getContext("2d");
  const width = canvas.width;
  const height = canvas.height;
  ctx.clearRect(0, 0, width, height);
  ctx.fillStyle = "#ffffff";
  ctx.fillRect(0, 0, width, height);

  if (!points.length) return;

  const balances = points.map((point) => point.balance);
  const min = Math.min(...balances, safetyBalance, 0);
  const max = Math.max(...balances, safetyBalance, 1);
  const pad = 28;
  const x = (index) => pad + (index * (width - pad * 2)) / Math.max(1, points.length - 1);
  const y = (value) => height - pad - ((value - min) * (height - pad * 2)) / Math.max(1, max - min);

  ctx.strokeStyle = "#d8ded8";
  ctx.lineWidth = 1;
  for (let i = 0; i < 4; i++) {
    const gy = pad + i * ((height - pad * 2) / 3);
    ctx.beginPath();
    ctx.moveTo(pad, gy);
    ctx.lineTo(width - pad, gy);
    ctx.stroke();
  }

  ctx.strokeStyle = "#b86b00";
  ctx.setLineDash([6, 6]);
  ctx.beginPath();
  ctx.moveTo(pad, y(safetyBalance));
  ctx.lineTo(width - pad, y(safetyBalance));
  ctx.stroke();
  ctx.setLineDash([]);

  ctx.strokeStyle = "#245f9e";
  ctx.lineWidth = 3;
  ctx.beginPath();
  points.forEach((point, index) => {
    if (index === 0) ctx.moveTo(x(index), y(point.balance));
    else ctx.lineTo(x(index), y(point.balance));
  });
  ctx.stroke();

  ctx.fillStyle = "#117476";
  points.forEach((point, index) => {
    if (index % 5 !== 0 && index !== points.length - 1) return;
    ctx.beginPath();
    ctx.arc(x(index), y(point.balance), 4, 0, Math.PI * 2);
    ctx.fill();
  });
}

async function acceptAction() {
  if (!state.actionType) return;
  await api("/api/burty/cashflow/action/feedback", {
    method: "POST",
    body: JSON.stringify({ userId: state.userId, actionType: state.actionType, feedback: "accept" })
  });
  const proof = await api("/api/burty/security/level2/proof", { method: "POST", body: "{}" });
  await api("/api/burty/cashflow/action/execute", {
    method: "POST",
    headers: { "X-Risk-Proof": proof.riskProof },
    body: JSON.stringify({ userId: state.userId, actionType: state.actionType })
  });
  el("acceptActionBtn").textContent = "수락됨";
  await loadDashboard();
}

function formatDate(value) {
  const date = new Date(`${value}T00:00:00`);
  return `${date.getMonth() + 1}/${date.getDate()}`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");
}

el("refreshBtn").addEventListener("click", loadDashboard);
el("acceptActionBtn").addEventListener("click", acceptAction);
bootstrap().catch((error) => {
  el("riskTitle").textContent = "대시보드를 불러오지 못했습니다.";
  el("riskReason").textContent = error.message;
});
