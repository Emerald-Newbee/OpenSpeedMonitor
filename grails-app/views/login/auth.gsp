<g:set var="lang" value="${session.'org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE'}"/>
<html>
<head>
	<title><g:message code="springSecurity.login.title" locale="${lang}"/></title>
	<meta name="layout" content="kickstart" />

	<g:set var="layout_nomainmenu"		value="${true}" scope="request"/>
	<g:set var="layout_nosecondarymenu"	value="${true}" scope="request"/>
</head>

<body>

<section id="login" class="first">
<div class="row">
	<div class="span3"></div>
	<div class="span6">
		<h3> <g:message code="springSecurity.login.header" locale="${lang}"/> </h3>
		<form id='loginForm' class='form-horizontal' action='${postUrl}' method='POST' autocomplete='off'>
			<fieldset class="form">
				<div class="control-group fieldcontain">
					<label for='username' class="control-label"><g:message code="springSecurity.login.username.label" locale="${lang}"/>:</label>
					<div class="controls">
						<input type='text' class='span4' name='username' id='username'/>
					</div>
				</div>
	
				<div class="control-group fieldcontain">
					<label for='password' class="control-label"><g:message code="springSecurity.login.password.label" locale="${lang}"/>:</label>
					<div class="controls">
						<input type='password' class='span4' name='password' id='password'/>
					</div>
				</div>
				
				<div id="remember_me_holder" class="control-group fieldcontain">
					<label for='remember_me' class="control-label"><g:message code="springSecurity.login.remember.me.label" locale="${lang}"/></label>
					<div class="controls">
						<bs:checkBox class="span4" name="${rememberMeParameter}" value="${hasCookie}" />
					</div>
				</div>
			</fieldset>
			<div class="controls">
				<input type='submit' id="submit" class="btn btn-success" value='${message([code:'springSecurity.login.button', locale:lang])}'/>
			</div>
		</form>
	</div>
	<div class="span3"></div>
</div>
</section>

<script type='text/javascript'>
	<!--
	(function() {
		document.forms['loginForm'].elements['username'].focus();
	})();
	// -->
</script>

</body>
</html>
