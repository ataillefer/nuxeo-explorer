<#if Context.principal.isAnonymous()>
  <a href="${Root.path}/apidocLogin" id="login">Login</a>
<#else>
  <span id="logstate">${Context.principal.name}</span>
  <a href="/${Root.webappName}/logout" id="logout">Logout</a>
</#if>
