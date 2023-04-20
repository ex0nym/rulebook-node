package io.exonym.rulebook.gets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/images/working.svg")
public class WorkingServlet extends HttpServlet {

    private final static String WORKING_SVG = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" style=\"margin: auto; background: rgba(0, 0, 0, 0) none repeat scroll 0% 0%; display: block; shape-rendering: auto;\" width=\"48px\" height=\"48px\" viewBox=\"0 0 100 100\" preserveAspectRatio=\"xMidYMid\">\n" +
            "<circle cx=\"50\" cy=\"50\" r=\"30\" stroke-width=\"6\" stroke=\"#797979\" stroke-dasharray=\"47.12388980384689 47.12388980384689\" fill=\"none\" stroke-linecap=\"round\">\n" +
            "  <animateTransform attributeName=\"transform\" type=\"rotate\" dur=\"3.7037037037037033s\" repeatCount=\"indefinite\" keyTimes=\"0;1\" values=\"0 50 50;360 50 50\"></animateTransform>\n" +
            "</circle>\n" +
            "<circle cx=\"50\" cy=\"50\" r=\"23\" stroke-width=\"6\" stroke=\"#cecece\" stroke-dasharray=\"36.12831551628262 36.12831551628262\" stroke-dashoffset=\"36.12831551628262\" fill=\"none\" stroke-linecap=\"round\">\n" +
            "  <animateTransform attributeName=\"transform\" type=\"rotate\" dur=\"3.7037037037037033s\" repeatCount=\"indefinite\" keyTimes=\"0;1\" values=\"0 50 50;-360 50 50\"></animateTransform>\n" +
            "</circle>\n" +
            "</svg>";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(WORKING_SVG);
    }
}
