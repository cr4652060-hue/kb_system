const $ = (id)=>document.getElementById(id);

async function api(url, opts){
    const r = await fetch(url, opts);
    if(!r.ok){
        const t = await r.text();
        throw new Error(t || ("HTTP " + r.status));
    }
    const ct = r.headers.get("content-type") || "";
    return ct.includes("application/json") ? r.json() : r.text();
}

function esc(s){
    return (s ?? "").toString()
        .replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;")
        .replaceAll('"',"&quot;").replaceAll("'","&#039;");
}

async function loadMe(){
    const me = await api("/api/me");
    $("me").textContent = me.username;
    $("role").textContent = me.role;
    if(me.role !== "ADMIN"){
        $("status").textContent = "⚠ 你不是管理员，无法使用此页面。";
        throw new Error("Not ADMIN");
    }
}

function renderUsers(users){
    const rows = users || [];
    const html = `
  <table>
    <thead>
      <tr>
        <th style="width:80px">ID</th>
        <th style="width:140px">用户名</th>
        <th style="width:100px">角色</th>
        <th>部门</th>
      </tr>
    </thead>
    <tbody>
      ${rows.map(u=>`
        <tr>
          <td>${esc(u.id)}</td>
          <td>${esc(u.username)}</td>
          <td>${esc(u.role)}</td>
          <td>${esc(u.department)}</td>
        </tr>
      `).join("")}
    </tbody>
  </table>`;
    $("table").innerHTML = html;
}

async function refresh(){
    $("status").textContent = "加载账号列表…";
    const users = await api("/api/admin/users");
    renderUsers(users);
    $("status").textContent = `✅ 已加载 ${users.length} 个账号`;
}

async function bootstrap(){
    $("status").textContent = "正在一键生成部门账号…";
    const res = await api("/api/admin/bootstrap-dept-accounts", { method:"POST" });
    $("result").textContent = JSON.stringify(res, null, 2);
    $("status").textContent = `✅ 完成：新增 ${res.createdCount}，跳过 ${res.skippedCount}（已存在）`;
    await refresh();
}

async function resetPwd(){
    const username = $("u").value.trim();
    const newPassword = $("p").value.trim();
    if(!username || !newPassword){
        $("status").textContent = "请输入用户名和新密码。";
        return;
    }
    $("status").textContent = "重置中…";
    const res = await api("/api/admin/users/reset-password", {
        method:"POST",
        headers: { "Content-Type":"application/json" },
        body: JSON.stringify({ username, newPassword })
    });
    $("status").textContent = res.msg || "完成";
}

(async function init(){
    try{
        await loadMe();
        $("bootstrap").onclick = bootstrap;
        $("refresh").onclick = refresh;
        $("reset").onclick = resetPwd;
        await refresh();
    }catch(e){
        $("status").textContent = "初始化失败：" + e.message;
    }
})();
