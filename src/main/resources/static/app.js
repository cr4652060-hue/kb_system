const SEARCH_API = "/api/search";
const ADD_API = "/api/knowledge";
const IMPORT_API = "/api/knowledge/import";
const TPL_URL = "/kb_stystem_template.xlsx";

// Unified JSON request with cookie and no cache (avoiding /api/me with old account)
async function apiJson(url, options) {
    const res = await fetch(url, {
        credentials: "same-origin",
        cache: "no-store",
        ...((options || {}))
    });

    const ct = (res.headers.get("content-type") || "").toLowerCase();
    const text = await res.text();

    // When not logged in, it may return login.html (text/html)
    if (ct.includes("text/html")) {
        throw new Error("not logged in (html)");
    }
    if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${text}`);
    }

    try {
        return JSON.parse(text); // Correctly parse the returned JSON data
    } catch {
        return text; // Return raw text if it's not JSON
    }
}

// ========= 工具 =========
const byId = (id) => document.getElementById(id);

function bindClick(id, handler) {
    const el = byId(id);
    if (el) el.addEventListener("click", handler);
    return el;
}
// ========= UI =========
function setStatus(msg) {
    const el = byId("status");
    if (el) el.textContent = msg || "";
}
function escapeHtml(v) {
    return (v ?? "")
        .toString()
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function highlight(text, q) {
    const src = escapeHtml(text);
    const kw = (q || "").trim();
    if (!kw) return src;
    const reg = new RegExp(kw.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "gi");
    return src.replace(reg, (m) => `<mark>${m}</mark>`);
}
function renderTable(rows, q) { // 登陆并渲染表格
    const box = byId("table");
    if (!box) return;

    if (!Array.isArray(rows) || rows.length === 0) {
        box.innerHTML = `<div class="empty">未找到匹配结果。</div>`;
        return;
    }

    box.innerHTML = `
    <table class="tb">
        <thead>
            <tr>
                <th>分类</th><th>部门</th><th>业务名称</th><th>办理流程</th>
                <th>最新要求下达时间</th><th>最新要求</th><th>案例</th>
                <th>扣罚标准</th><th>制度依据</th><th>关键词</th>
                <th>维护人</th><th>更新时间</th><th>状态</th>
            </tr>
        </thead>
        <tbody>
            ${rows.map(r => `
                <tr>
                    <td>${escapeHtml(r.category)}</td>
                    <td>${escapeHtml(r.department)}</td>
                    <td>${highlight(r.bizName, q)}</td>
                    <td>${highlight(r.process, q)}</td>
                    <td>${escapeHtml(r.latestReqDate)}</td>
                    <td>${highlight(r.latestReq, q)}</td>
                    <td>${highlight(r.caseText, q)}</td>
                    <td>${highlight(r.penalty, q)}</td>
                    <td>${highlight(r.basis, q)}</td>
                    <td>${highlight(r.keywords, q)}</td>
                    <td>${escapeHtml(r.owner)}</td>
                    <td>${escapeHtml(r.updateTime)}</td>
                    <td>${escapeHtml(r.status)}</td>
                </tr>
            `).join("")}
        </tbody>
    </table>`;
}


function roleUpper(me) {
    return ((me && me.role) ? me.role : "").toUpperCase();
}


// ===== 刷新 UI =====
async function refreshMe() {
    try {
        const loginLink = byId("loginLink");
        const adminLink = byId("adminLink");
        const tplBtn = byId("tplBtn");
        const logoutForm = byId("logoutForm");
        const excelCard = byId("excelCard");
        const addBtn = byId("addBtn");

        const me = await fetchMeSafe();
        window.me = me;
        const normRole = normalizeRole(me);

        // 控制显示登录/登出
        if (loginLink) loginLink.style.display = me ? "none" : "inline";
        if (logoutForm) logoutForm.style.display = me ? "inline" : "none";

        // 用户信息
        const eUser = byId("meUser");
        const eRole = byId("meRole");
        const eDept = byId("meDept");
        if (eUser) eUser.textContent = me ? (me.username || "-") : "-";
        if (eRole) eRole.textContent = me ? (me.role || normRole || "-") : "-";
        if (eDept) eDept.textContent = me ? (me.department || "-") : "-";

        // 管理/模板/Excel 权限
        const canAdmin = normRole === "ADMIN" || normRole === "DEPT";
        if (adminLink) adminLink.style.display = canAdmin ? "inline" : "none";
        if (tplBtn) {
            tplBtn.style.display = canDownloadTemplate(me) ? "inline" : "none";
            tplBtn.href = TPL_URL;
        }
        if (excelCard) excelCard.style.display = canUploadExcel(me) ? "block" : "none";
        if (!canUploadExcel(me) && excelCard) {
            const msg = byId("excelMsg");
            if (msg) msg.textContent = "";
        }
        if (addBtn) addBtn.style.display = canAdmin ? "inline" : "none";

    } catch (e) {
        console.error("获取用户信息失败:", e);
        if (byId("loginLink")) byId("loginLink").style.display = "inline";
        if (byId("logoutForm")) byId("logoutForm").style.display = "none";
    }
}

// 执行搜索       renderTable(rows, q);
//     } catch (e) {
//         console.error(e);
//         setStatus("搜索失败：" + e.message);
//     }
// }
async function runSearch() {
    const input = byId("q");
    const q = input ? input.value.trim() : "";
    setStatus(q ? "搜索中…" : "加载最新数据中…");
    try {
        let rows;
        if (!q) {
            // 没有关键字时，默认拉取最新 200 条，方便用户先看到数据
            rows = await apiJson(`/api/knowledge?limit=200`);
            setStatus(`显示最近 ${rows.length} 条记录。`);
        } else {
            const params = new URLSearchParams({ q });
            rows = await apiJson(`${SEARCH_API}?${params.toString()}`);
            setStatus(`找到 ${rows.length} 条结果。`);
        }
        renderTable(rows, q);
    } catch (e) {
        console.error(e);
        setStatus("搜索失败：" + e.message);
    }
}
// 执行搜索
async function runSearch() {
    const input = byId("q");
    const q = input ? input.value.trim() : "";
    setStatus(q ? "搜索中…" : "加载最新数据中…");
    try {
        let rows;
        if (!q) {
            // 没有关键字时，默认拉取最新 200 条，方便用户先看到数据
            rows = await apiJson(`/api/knowledge?limit=200`);
            setStatus(`显示最近 ${rows.length} 条记录。`);
        } else {
            const params = new URLSearchParams({ q });
            rows = await apiJson(`${SEARCH_API}?${params.toString()}`);
            setStatus(`找到 ${rows.length} 条结果。`);
        }

        renderTable(rows, q);
    } catch (e) {
        console.error(e);
        setStatus("搜索失败：" + e.message);
    }
}

function showExcelMessage(text, isError) {
    const msg = byId("excelMsg");
    if (!msg) return;
    msg.textContent = text;
    msg.style.color = isError ? "#cf1322" : "#333";
}

async function importExcel() {
    const fileInput = byId("excelFile");
    if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
        showExcelMessage("请先选择要上传的 Excel 文件。", true);
        return;
    }

    const file = fileInput.files[0];
    const fd = new FormData();
    fd.append("file", file);

    try {
        const res = await fetch(IMPORT_API, {
            method: "POST",
            body: fd,
            credentials: "same-origin",
        });

        if (!res.ok) {
            const text = await res.text();
            throw new Error(text || `HTTP ${res.status}`);
        }

        const data = await res.json();
        showExcelMessage(`导入成功：新增 ${data.added} 条，更新 ${data.updated} 条。`, false);
        runSearch();
    } catch (e) {
        console.error("Excel 导入失败", e);
        showExcelMessage(`导入失败：${e.message}`, true);
    }
}
// 要求：除了普通员工，能登录的都能下载模板
function canDownloadTemplate(me) {
    if (!me) return false; // 未登录不行
    const r = normalizeRole(me);
    return !(r === "EMP" || r === "USER" || r === "STAFF" || r === "EMPLOYEE");
}

function canUploadExcel(me) {
    const r = normalizeRole(me);
    return r === "ADMIN" || r === "DEPT";
}

async function fetchMeSafe() {
    try {
        const res = await fetch("/api/me", {
            method: "GET",
            credentials: "same-origin",     // ✅ 必须带 cookie
            cache: "no-store",
            redirect: "follow",
            headers: { "Accept": "application/json" } // ✅ 强制要 JSON
        });

        const ct = (res.headers.get("content-type") || "").toLowerCase();
        const text = await res.text();

        // 401/403/500 都当成未登录/失败
        if (!res.ok) return null;

        // 返回了 HTML（例如登录页）=> 未登录
        if (ct.includes("text/html") || text.trim().startsWith("<")) return null;

        // 正常 JSON
        return JSON.parse(text);
    } catch (e) {
        return null;
    }
}



// 确保 DOM 加载后再跑
document.addEventListener("DOMContentLoaded", () => {
    refreshMe();
    renderTable([], "");

    bindClick("search", runSearch);

    const searchInput = byId("q");
    if (searchInput) {
        searchInput.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                e.preventDefault();
                runSearch();
            }
        });
    }

    const btnImport = bindClick("btnExcelImport", importExcel);
    if (btnImport) {
        const hint = byId("excelHint");
        if (hint) hint.textContent = "仅管理员/部门维护人可上传，导入完成后自动刷新搜索结果。";
    }
});
