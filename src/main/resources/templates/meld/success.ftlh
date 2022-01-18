<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Discord Github Melder</title>
  <link rel="stylesheet" href="/webjars/font-awesome/5.15.4/css/all.min.css"/>
  <link rel="stylesheet" href="/css/main.css"/>
  <link rel="stylesheet" href="/css/meld.css"/>
</head>
<body>

<header>
  <h1>Connected!</h1>
</header>
<main class="meld-results">
  <div class="connection-row">
    <div class="logo-cell">
      <div>
        <i class="logo fab fa-discord discord-blurple"></i>
      </div>
      <div>
          ${member.discordUsername}#${member.discordDiscriminator}
      </div>
    </div>
    <div class="cell-arrow">
      <i class="fas fa-arrow-right"></i>
    </div>
    <div class="logo-cell">
      <div>
        <i class="logo fab fa-github github-black"></i>
      </div>
      <div>
          ${member.githubUsername}
      </div>
    </div>
  </div>
  <div class="roles">
      <#list rolesByStatus as status, role>
          <#if status.name() == "ALREADY_ASSIGNED">
            You were already assigned ${role} role on all of the servers.
          <#elseif status.name() == "ADDED_SOME_ASSIGNMENTS">
            You were assigned ${role} role on remaining servers.
          <#elseif status.name() == "NO_ROLES_ASSIGNED">
            <div>
              You were not yet assigned ${role} role since you haven't contributed to the
              following repos:
            </div>
            <div class="repos">
                <#list repos as repo>
                  <div class="repo"><a href="https://github.com/${repo}">${repo}</a></div>
                </#list>
            </div>
          </#if>
      </#list>
  </div>
</main>
</body>
</html>