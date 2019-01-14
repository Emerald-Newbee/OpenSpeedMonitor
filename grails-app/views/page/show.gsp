<%@ page import="grails.converters.JSON; de.iteratec.osm.csi.Page" %>
<!doctype html>
<html>

<head>
    <g:set var="entityName" value="${message(code: 'page.label', default: 'Page')}" scope="request"/>
	<parameter name="layout_nosecondarymenu" value="${true}"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="layoutOsm"/>
    <title><g:message code="default.show.label" args="[entityName]"/></title>
</head>

<body>
<g:render template="/_menu/submenubarWithoutDelete"/>
<section id="show-page" class="first">

	<table class="table">
		<tbody>
		
			<tr class="prop">
				<td valign="top" class="name"><g:message code="page.name.label" default="Name" /></td>
				
				<td valign="top" class="value">${fieldValue(bean: pageInstance, field: "name")}</td>

			</tr>

		</tbody>
	</table>
</section>

</body>

</html>
