package io.exonym.rulebook.context;

import io.exonym.lite.authenticators.AuthenticateServlet;
import io.exonym.lite.authenticators.StandardAuthenticator;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Authentication for the Admin section based on No Email Binding PREFIX 'I' for independent
 */
@WebServlet("/authenticate")
public class IAuthenticateServlet extends AuthenticateServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected StandardAuthenticator openAuthenticator() {
        return IAuthenticator.getInstance();

    }
}
