<%@ page import="de.iteratec.osm.measurement.schedule.ConnectivityProfile" %>
<!doctype html>
<html>

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="layoutOsm" />
        <g:set var="entityName" value="${message(code: 'connectivityProfile.label', default: 'ConnectivityProfile')}" scope="request"/>
        <title><g:message code="default.create.label" args="[entityName]" /></title>
    </head>

    <body>
        <h1><g:message code="default.create.label" args="[entityName]" /></h1>
        <section id="create-connectivityProfile" class="card">

            <g:hasErrors bean="${connectivityProfileInstance}">
            <div class="alert alert-danger">
                <g:renderErrors bean="${connectivityProfileInstance}" as="list" />
            </div>
            </g:hasErrors>

            <g:form action="save" class="form-horizontal" >
                <fieldset class="form-horizontal">
                    <g:render template="form"/>
                </fieldset>
                <div>
                    <g:submitButton name="create" class="btn btn-primary" value="${message(code: 'default.button.create.label', default: 'Create')}" />
                    <button class="btn btn-default" type="reset"><g:message code="default.button.reset.label" default="Reset" /></button>
                </div>
            </g:form>

        </section>

    </body>

</html>
