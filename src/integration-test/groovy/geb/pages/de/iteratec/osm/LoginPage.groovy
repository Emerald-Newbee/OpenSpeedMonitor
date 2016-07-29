package geb.pages.de.iteratec.osm

import geb.pages.de.iteratec.osm.result.EventResultDashboardPage

class LoginPage extends I18nGebPage {


    static url = getUrl("/login/auth")

    static at = {
        title == getI18nMessage("springSecurity.login.title")
    }


    static content = {
        loginForm { $("#loginForm") }

        username { loginForm.find("#username") }

        password { loginForm.find("#password") }

        submitButton(to: [LoginPage, EventResultDashboardPage]) { loginForm.find("input", type: "submit") }

        errorMessageBox { $("div.alert") }
    }
}