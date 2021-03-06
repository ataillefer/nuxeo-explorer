<@extends src="base.ftl">
<@block name="title">All Operations</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>${operations?size} Operations</h1>
<div class="tabscontent">
  <table id="contentTable" class="tablesorter">
    <thead>
    <tr>
      <th><@tableFilterArea "operation"/></th>
    </tr>
    </thead>
  <tbody>
  <#list operations as operation>
    <tr>
      <td>
        <div>
          <h4><a title="Operation Label" href="${Root.path}/${distId}/viewOperation/${operation.name}" class="itemLink">${operation.label?html}</a></h4>
          <div class="itemDetail">
            <span title="Category" class="sticker">${operation.category?html}</span>
            <span title="Operation ID">${operation.name?html}</span>
            <#if operation.aliases>
              <div>Alias <code><#list operation.aliases as alias> ${alias} </#list></code></div>
            </#if>
            <#if operation.requires>
            <span>Require <span class="sticker">${operation.requires}</span></span>
            </#if>
            <#if operation.since>
            <span>Since <span class="sticker">${operation.since}</span></span>
            </#if>
          </div>
        </div>
      </td>
    </tr>
  </#list>
  </tbody>
  </table>
</div>

</@block>

<@block name="footer_scripts">
  <@tableSortFilterScript "#contentTable" "[0,0]" />
</@block>

</@extends>
