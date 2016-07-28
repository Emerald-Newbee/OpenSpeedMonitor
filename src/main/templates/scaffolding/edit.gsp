<!DOCTYPE html>
<html>
    <head>
        <meta name="layout" content="kickstart" />
        <g:set var="entityName" value="\${message(code: '${propertyName}.label', default: '${className}')}" />
        <title><g:message code="default.edit.label" args="[entityName]" /></title>
    </head>
    <body>
        <div id="edit-${propertyName}" class="content scaffold-edit" role="main">
            <g:if test="\${flash.message}">
            <div class="message" role="status">\${flash.message}</div>
            </g:if>
            <g:hasErrors bean="\${${propertyName}}">
            <ul class="errors" role="alert">
                <g:eachError bean="\${${propertyName}}" var="error">
                <li <g:if test="\${error in org.springframework.validation.FieldError}">data-field-id="\${error.field}"</g:if>><div class="alert alert-danger"><g:message error="\${error}"/></div></li>
                </g:eachError>
            </ul>
            </g:hasErrors>
            <g:form resource="\${${propertyName}}" method="PUT" class="form-horizontal">
                <g:hiddenField name="version" value="\${${propertyName}?.version}" />
                <fieldset class="form">
                    <f:all bean="${propertyName}"/>
                </fieldset>
                <div class="form-actions">
                    <g:actionSubmit class="btn btn-primary" action="update" value="\${message(code: 'default.button.update.label', default: 'Update')}" />
                    <g:render template="/_common/modals/deleteSymbolLink"/>
                    <button class="btn" type="reset"><g:message code="default.button.reset.label" default="Reset" /></button>
                </div>
            </g:form>
        </div>
    </body>
</html>
