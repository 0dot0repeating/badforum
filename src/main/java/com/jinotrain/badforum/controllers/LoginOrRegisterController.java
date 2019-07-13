package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.services.ForumUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class LoginOrRegisterController
{
    @Autowired
    private ForumUserService forumUserService;

    @RequestMapping(value = "/register",
                    method = {RequestMethod.GET, RequestMethod.HEAD},
                    produces = "text/html")
    public ModelAndView getRegisterPage(HttpServletRequest request,
                                        HttpServletResponse response)
    {
        ModelAndView mav = new ModelAndView("register.html");
        return mav;
    }

    @RequestMapping(value = "/register",
                    method = RequestMethod.POST,
                    produces = "text/html")
    public ModelAndView getRegisterPost(HttpServletRequest request,
                                        HttpServletResponse response)
    {
        try { request.setCharacterEncoding("UTF-8"); }
        catch (Exception e) {}

        ModelAndView mav = new ModelAndView("register2.html");

        String username   = request.getParameter("username");
        String email      = request.getParameter("email");
        String password   = request.getParameter("password");
        String confirm    = request.getParameter("confirm");
        Boolean pwMatches = password.equals(confirm);

        String passhash = forumUserService.passwordEncoder().encode(password);

        mav.addObject("username", username);
        mav.addObject("email",    email);
        mav.addObject("passhash", passhash);

        return mav;
    }
}
