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



// ========= UI =========
function setStatus(msg) {
    const el = $("status");
    if (el) el.textContent = msg || "";
}

function renderTable(rows, q) {// 登陆并渲染表格
    const box = $("table");
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







// ===== 刷新 UI =====
function $(id) { return document.getElementById(id); }

function roleUpper(me) {
    return ((me && me.role) ? me.role : "").toUpperCase();
}



async function refreshMe() {
    try {
        const loginLink = document.getElementById("loginLink");
        const adminLink = document.getElementById("adminLink");
        const tplBtn = document.getElementById("tplBtn");
        const logoutForm = document.getElementById("logoutForm");

        // 请求用户信息
        const res = await fetch("/api/me");
        const me = await res.json();
        window.me = me;

        console.log("User data:", window.me);  // 查看获取的用户数据

        // 控制显示登录链接
        if (loginLink) {
            loginLink.style.display = me ? "none" : "inline"; // 如果有用户信息则隐藏登录链接
        }

        // 控制显示登出按钮
        if (logoutForm) {
            logoutForm.style.display = me ? "inline" : "none"; // 如果有用户信息则显示登出按钮
        }

        // 清空用户信息
        const eUser = document.getElementById("meUser");
        const eRole = document.getElementById("meRole");
        const eDept = document.getElementById("meDept");

        if (eUser) eUser.textContent = me ? me.username : "-";
        if (eRole) eRole.textContent = me ? me.role : "-";
        if (eDept) eDept.textContent = me ? me.department : "-";

        // 显示管理员链接
        if (adminLink) {
            adminLink.style.display = me && (me.role === 'ADMIN' || me.role === 'DEPT') ? "inline" : "none";
        }

        // 显示下载按钮
        if (tplBtn) {
            tplBtn.style.display = me && (me.role === 'ADMIN' || me.role === 'DEPT') ? "inline" : "none";
        }

    } catch (e) {
        console.error("获取用户信息失败:", e);

        // 处理错误，例如提示用户登录
    }
}



// 确保 DOM 加载后再跑
document.addEventListener("DOMContentLoaded", () => {
    refreshMe();
});
const searchButton = document.getElementById("search");
if (searchButton) {
    searchButton.addEventListener("click", runSearch);
}

const searchInput = document.getElementById("q");
if (searchInput) {
    searchInput.addEventListener("keypress", (e) => {
        if (e.key === "Enter") runSearch();
    });
}



// 要求：除了普通员工，能登录的都能下载模板
function canDownloadTemplate(me) {
    if (!me) return false; // 未登录不行
    const r = roleUpper(me);
    return !(r === "EMP" || r === "USER" || r === "STAFF" || r === "EMPLOYEE");
}

function canUploadExcel(me) {
    const r = roleUpper(me);
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

