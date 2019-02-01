<!-- NOTE: the renderDialog for the "Register" modal dialog MUST be placed outside the NavBar (at least for Bootstrap 2.1.1): see bottom of main.gsp -->
<g:set var="lang" value="${session.'org.springframework.web.servlet.i18n.SessionLocaleResolver.LOCALE'}"/>
<sec:ifNotLoggedIn>
	<g:if test="${grailsApplication.config.getProperty('grails.mail.disabled')?.toLowerCase() == "true"}">
		<li>
			<g:link controller="login" action="auth"><i class="fas fa-sign-in-alt" aria-hidden="true"></i>
				<g:message code="security.signin.label" default="Log in"/>
			</g:link>
		</li>
	</g:if>
	<g:else>
		<li class="dropdown">
			<a class="dropdown-toggle" href="#">
				<i class="fas fa-sign-in-alt" aria-hidden="true"></i>
				<g:message code="security.signin.label" default="Log in" locale="${lang}"/>
				<b class="caret"></b>
			</a>
			<ul class="dropdown-menu">

				%{--measurement --------------------------------------------------------}%

				<li class="controller">
					<g:link controller="login" action="auth"><i class="fas fa-sign-in-alt" aria-hidden="true"></i> <g:message
							code="security.signin.label" default="Log in"/></g:link>
				</li>
				<g:if test="${grailsApplication.config.getProperty('grails.mail.disabled')?.toLowerCase() == "false"}">
					<li class="controller">
						<g:link controller="register" action="register"><i class="fas fa-book"
																		   aria-hidden="true"></i> <g:message
								code="security.register.label" default="Register"/></g:link>
					</li>
				</g:if>
			</ul>
		</li>
	</g:else>
</sec:ifNotLoggedIn>
<sec:ifLoggedIn>
	<li class="dropdown">
			<a class="dropdown-toggle" href="#">
				<i class="fas fa-user-circle"></i>
				<sec:username/> <b class="caret"></b>
			</a>
			<ul class="dropdown-menu">


				%{--Logout --------------------------------------------------------}%

				<li class="controller">
					<g:link controller="logout" action="index"><i class="fas fa-sign-out-alt" aria-hidden="true"></i>
						<g:message code="security.signout.label" default="Log out"/></g:link>
				</li>

				<sec:ifAnyGranted roles="ROLE_SUPER_ADMIN">
					%{--csi --------------------------------------------------------}%
					<li class="controller">
						<g:link controller="user" action="index"><i class="fas fa-user"
																	aria-hidden="true"></i> <g:message
								code="user.label" default="User"/></g:link>
					</li>

					<li class="controller">
						<g:link controller="registrationCode" action="index"><i class="fas fa-key"
																				aria-hidden="true"></i> <g:message
								code="registrationCode.label" default="RegistrationCode"/></g:link>
					</li>
				</sec:ifAnyGranted>

			</ul>
	</li>
</sec:ifLoggedIn>


