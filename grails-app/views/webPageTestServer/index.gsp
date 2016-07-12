
<%@ page import="de.iteratec.osm.measurement.environment.WebPageTestServer" %>
<!doctype html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="kickstart" />
    <g:set var="entityName" value="${message(code: 'webPageTestServer.label', default: 'WebPageTestServer')}" />
    <title><g:message code="default.list.label" args="[entityName]" /></title>
</head>

<body>

<section id="list-webPageTestServer" class="first">

    <table class="table table-bordered">
        <thead>
        <tr>

            <g:sortableColumn property="label" title="${message(code: 'webPageTestServer.label.label', default: 'Label')}" />

            <g:sortableColumn property="proxyIdentifier" title="${message(code: 'webPageTestServer.proxyIdentifier.label', default: 'Proxy Identifier')}" />

            <g:sortableColumn property="dateCreated" title="${message(code: 'webPageTestServer.dateCreated.label', default: 'Date Created')}" />

            <g:sortableColumn property="lastUpdated" title="${message(code: 'webPageTestServer.lastUpdated.label', default: 'Last Updated')}" />

            <g:sortableColumn property="active" title="${message(code: 'webPageTestServer.active.label', default: 'Active')}" />

            <g:sortableColumn property="description" title="${message(code: 'webPageTestServer.description.label', default: 'Description')}" />

        </tr>
        </thead>
        <tbody>
        <g:each in="${webPageTestServerList}" status="i" var="webPageTestServer">
            <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">

                <td><g:link action="show" id="${webPageTestServer.id}">${fieldValue(bean: webPageTestServer, field: "label")}</g:link></td>

                <td>${fieldValue(bean: webPageTestServer, field: "proxyIdentifier")}</td>

                <td><g:formatDate date="${webPageTestServer.dateCreated}" /></td>

                <td><g:formatDate date="${webPageTestServer.lastUpdated}" /></td>

                <td><g:formatBoolean boolean="${webPageTestServer.active}" /></td>

                <td>${fieldValue(bean: webPageTestServer, field: "description")}</td>

            </tr>
        </g:each>
        </tbody>
    </table>
    <div class="pagination">
        <bs:paginate total="${webPageTestServerCount ?: 0}" />
    </div>
</section>

</body>

</html>