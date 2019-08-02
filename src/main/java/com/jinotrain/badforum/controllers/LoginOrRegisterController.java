package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.ForumPasswordService;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.db.repositories.ForumUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LoginOrRegisterController
{
    @Autowired
    private ForumPasswordService passwordService;

    @Autowired
    private ForumUserRepository userRepository;

    @PersistenceContext
    private EntityManager em;


    @RequestMapping(value = "/register",
                    method = {RequestMethod.GET, RequestMethod.HEAD},
                    produces = "text/html")
    public ModelAndView getRegisterPage(HttpServletRequest request,
                                        HttpServletResponse response)
    {
        return getRegisterPage(request, response, null);
    }


    private ModelAndView getRegisterPage(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Map<String, String> keys)
    {
        ModelAndView mav = new ModelAndView("register.html");
        if (keys != null) { mav.addAllObjects(keys); }

        return mav;
    }


    @Transactional
    @RequestMapping(value = "/register",
                    method = RequestMethod.POST,
                    produces = "text/html")
    public ModelAndView getRegisterPost(HttpServletRequest request,
                                        HttpServletResponse response)
    {
        try { request.setCharacterEncoding("UTF-8"); }
        catch (Exception e) {}

        String username   = request.getParameter("username");
        String email      = request.getParameter("email");
        String password   = request.getParameter("password");
        String confirm    = request.getParameter("confirm");

        boolean available = userRepository.findByUsernameIgnoreCase(username) == null;
        boolean pwMatches = password.equals(confirm);

        String errorMessage = null;

        if      (!available) { errorMessage = "Username already taken"; }
        else if (!pwMatches) { errorMessage = String.format("Passwords don't match (%s) (%s)", password, confirm); }

        if (errorMessage != null)
        {
            Map<String, String> errorKeys = new HashMap<>();

            errorKeys.put("errorMessage",   errorMessage);
            errorKeys.put("username",       username);
            errorKeys.put("email",          email);

            return getRegisterPage(request, response, errorKeys);
        }


        String passhash = passwordService.hashPassword(password);
        ForumUser user = new ForumUser(username, passhash);

        user.setEmail(email);
        em.persist(user);

        ModelAndView mav = new ModelAndView("register2.html");

        mav.addObject("username", username);
        mav.addObject("email",    email);
        mav.addObject("passhash", passhash);

        return mav;
    }


    @ResponseBody
    @RequestMapping(value = "/register/check/username",
                    method = {RequestMethod.GET, RequestMethod.POST},
                    produces = "application/json")
    public String checkUsername(@RequestParam("username") String username)
    {
        boolean unavailable = userRepository.findByUsernameIgnoreCase(username) != null;
        return String.format("{\"unavailable\": %s}", unavailable ? "true" : "false");
    }
}
