package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.ForumPasswordService;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.db.repositories.ForumUserRepository;
import com.jinotrain.badforum.util.LogHelper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Controller
public class LoginAndRegisterController
{
    private static Logger logger = LoggerFactory.getLogger(LoginAndRegisterController.class);

    @Autowired
    private ForumPasswordService passwordService;

    @Autowired
    private ForumUserRepository userRepository;

    @PersistenceContext
    private EntityManager em;


    private JSONObject registerUser(String username, String email, String password, String pwConfirm)
    {
        JSONObject ret = new JSONObject();

        username  = username  != null ? username  : "";
        email     = email     != null ? email     : "";
        password  = password  != null ? password  : "";
        pwConfirm = pwConfirm != null ? pwConfirm : "";

        if (username.equals("") || password.equals("") || pwConfirm.equals(""))
        {
            List<String> missing = new ArrayList<>();

            if (username.equals(""))  { missing.add("username"); }
            if (password.equals(""))  { missing.add("password"); }
            if (pwConfirm.equals("")) { missing.add("password confirmation"); }

            ret.put("registered", false);
            ret.put("errorCode", "INCOMPLETE");
            ret.put("errorExtra", "Missing " + String.join(", ", missing));
            return ret;
        }

        if (!password.equals(pwConfirm))
        {
            ret.put("registered", false);
            ret.put("errorCode", "PASSWORD_NOT_MATCHING");
            return ret;
        }

        ForumUser existingUser = userRepository.findByUsernameIgnoreCase(username);

        if (existingUser != null)
        {
            ret.put("registered", false);
            ret.put("errorCode", "USERNAME_TAKEN");
            return ret;
        }

        try
        {
            ForumUser user;
            String passhash = passwordService.hashPassword(password);
            user = new ForumUser(username, passhash, email);
            em.persist(user);
        }
        catch (Exception e)
        {
            ret.put("registered", false);
            ret.put("errorCode", "INTERNAL_ERROR");

            LogHelper.dumpException(logger, e);
            return ret;
        }

        ret.put("registered", true);
        ret.put("sessionID", "TODO");
        return ret;
    }


    @Transactional
    @ResponseBody
    @RequestMapping(value = "/api/register", method = {RequestMethod.GET, RequestMethod.HEAD}, produces = "application/json")
    public String registerUserViaJSON(HttpServletRequest request)

    {
        try { request.setCharacterEncoding("UTF-8"); }
        catch (Exception ignore) {}

        String username  = request.getParameter("username");
        String email     = request.getParameter("email");
        String password  = request.getParameter("password");
        String pwConfirm = request.getParameter("confirm");

        return registerUser(username, email, password, pwConfirm).toString();
    }


    @Transactional
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ModelAndView registerUserViaPOST(HttpServletRequest request, HttpServletResponse response)
    {
        try { request.setCharacterEncoding("UTF-8"); }
        catch (Exception ignore) {}

        String username  = request.getParameter("username");
        String email     = request.getParameter("email");
        String password  = request.getParameter("password");
        String pwConfirm = request.getParameter("confirm");

        JSONObject result = registerUser(username, email, password, pwConfirm);
        ModelAndView mav;

        if (result.getBoolean("registered"))
        {
            mav = new ModelAndView("registered.html");

            Cookie sessionCookie = new Cookie("forumSession", result.optString("sessionID"));
            response.addCookie(sessionCookie);
        }
        else
        {
            mav = new ModelAndView("register.html");

            mav.addObject("errorCode",  result.getString("errorCode"));
            mav.addObject("errorExtra", result.optString("errorExtra"));
            mav.addObject("username", username);
            mav.addObject("email", email);
        }

        return mav;
    }


    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public ModelAndView getRegisterPage(HttpServletRequest request,
                                        HttpServletResponse response)
    {
        return new ModelAndView("register.html");
    }


    @RequestMapping(value = "/api/checkUsername", method = RequestMethod.GET)
    @ResponseBody
    public String checkUsernameAvailable(String username)
    {
        ForumUser existingUser = userRepository.findByUsernameIgnoreCase(username);

        JSONObject ret = new JSONObject();
        ret.put("available", existingUser == null);
        return ret.toString();
    }
}
