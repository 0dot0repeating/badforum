package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.ForumPasswordService;
import com.jinotrain.badforum.db.entities.ForumSession;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.db.repositories.ForumSessionRepository;
import com.jinotrain.badforum.db.repositories.ForumUserRepository;
import com.jinotrain.badforum.util.LogHelper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Controller
public class LoginAndRegisterController
{
    private static Logger logger = LoggerFactory.getLogger(LoginAndRegisterController.class);

    @Autowired
    private ForumPasswordService passwordService;

    @Autowired
    private ForumUserRepository userRepository;

    @Autowired
    private ForumSessionRepository sessionRepository;

    @PersistenceContext
    private EntityManager em;


    private Map<String, Object> registerUser(String username, String email, String password, String pwConfirm)
    {
        Map<String, Object> ret = new HashMap<>();

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

        Optional<ForumUser> existingUser = userRepository.findByUsernameIgnoreCase(username);

        if (existingUser.isPresent())
        {
            ret.put("registered", false);
            ret.put("errorCode", "USERNAME_TAKEN");
            return ret;
        }

        try
        {
            String passhash = passwordService.hashPassword(password);
            ForumUser user = new ForumUser(username, passhash, email);
            userRepository.saveAndFlush(user);

            ForumSession session = new ForumSession(user);
            sessionRepository.saveAndFlush(session);

            ret.put("registered", true);
            ret.put("sessionID", session.getId());
            ret.put("session", session);
            return ret;
        }
        catch (Exception e)
        {
            ret.put("registered", false);
            ret.put("errorCode", "INTERNAL_ERROR");

            LogHelper.dumpException(logger, e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ret;
        }
    }


    private Map<String, Object> loginUser(String username, String password)
    {
        Map<String, Object> ret = new HashMap<>();

        username = username != null ? username : "";
        password = password != null ? password : "";

        if (username.equals(""))
        {
            ret.put("loggedIn", false);
            ret.put("errorCode", "USERNAME_MISSING");
            return ret;
        }

        if (password.equals(""))
        {
            ret.put("loggedIn", false);
            ret.put("errorCode", "PASSWORD_MISSING");
            return ret;
        }

        Optional<ForumUser> possibleUser = userRepository.findByUsernameIgnoreCase(username);

        if (!possibleUser.isPresent())
        {
            ret.put("loggedIn", false);
            ret.put("errorCode", "USER_NOT_FOUND");
            return ret;
        }

        ForumUser user = possibleUser.get();

        if (!passwordService.passwordMatches(password, user.getPasshash()))
        {
            ret.put("loggedIn", false);
            ret.put("errorCode", "WRONG_PASSWORD");
            return ret;
        }

        try
        {
            ForumSession session = new ForumSession(user);
            sessionRepository.saveAndFlush(session);

            ret.put("loggedIn", true);
            ret.put("sessionID", session.getId());
            ret.put("session", session);
            return ret;
        }
        catch (Exception e)
        {
            ret.put("loggedIn", false);
            ret.put("errorCode", "INTERNAL_ERROR");

            LogHelper.dumpException(logger, e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ret;
        }
    }




    @Transactional
    @ResponseBody
    @RequestMapping(value = "/api/register", method = RequestMethod.GET, produces = "application/json")
    public String registerUserViaJSON(String username, String email, String password, String pwConfirm)
    {
        Map<String, Object> retMap = registerUser(username, email, password, pwConfirm);

        if (retMap.containsKey("session"))
        {
            retMap.remove("session");
        }

        return new JSONObject(retMap).toString();
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

        Map<String, Object> result = registerUser(username, email, password, pwConfirm);
        ModelAndView mav;

        if ((boolean)result.get("registered"))
        {
            mav = new ModelAndView("registered.html");

            Cookie sessionCookie = new Cookie("forumSession", (String)result.get("sessionID"));
            response.addCookie(sessionCookie);

            // add object explicitly here, as post-handlers won't have the session ID cookie to read from
            mav.addObject("forumSession", result.get("session"));
        }
        else
        {
            mav = new ModelAndView("register.html");

            mav.addObject("errorCode",  result.get("errorCode"));
            mav.addObject("errorExtra", result.getOrDefault("errorExtra", null));
            mav.addObject("username", username);
            mav.addObject("email", email);
        }

        return mav;
    }


    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public ModelAndView getRegisterPage()
    {
        return new ModelAndView("register.html");
    }




    @Transactional
    @ResponseBody
    @RequestMapping(value = "/api/login", method = RequestMethod.GET, produces = "application/json")
    public String loginViaJSON(String username, String password)
    {
        Map<String, Object> retMap = loginUser(username, password);

        if (retMap.containsKey("session"))
        {
            retMap.remove("session");
        }

        return new JSONObject(retMap).toString();
    }


    @Transactional
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ModelAndView loginViaPOST(HttpServletRequest request, HttpServletResponse response)
    {
        try { request.setCharacterEncoding("UTF-8"); }
        catch (Exception ignore) {}

        String username  = request.getParameter("username");
        String password  = request.getParameter("password");

        Map<String, Object> result = loginUser(username, password);
        ModelAndView mav;

        if ((boolean)result.get("loggedIn"))
        {
            mav = new ModelAndView("loggedIn.html");

            Cookie sessionCookie = new Cookie("forumSession", (String)result.get("sessionID"));
            response.addCookie(sessionCookie);

            // add object explicitly here, as post-handlers won't have the session ID cookie to read from
            mav.addObject("forumSession", result.get("session"));
        }
        else
        {
            mav = new ModelAndView("login.html");

            mav.addObject("errorCode",  result.get("errorCode"));
            mav.addObject("errorExtra", result.getOrDefault("errorExtra", null));
            mav.addObject("username", username);
        }

        return mav;
    }


    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView getLoginPage(HttpServletRequest request,
                                     HttpServletResponse response)
    {
        return new ModelAndView("login.html");
    }




    @ResponseBody
    @RequestMapping(value = "/api/checkUsername", method = RequestMethod.GET, produces = "application/json")
    public String checkUsernameAvailable(String username)
    {
        Optional<ForumUser> existingUser = userRepository.findByUsernameIgnoreCase(username);

        JSONObject ret = new JSONObject();
        ret.put("available", !existingUser.isPresent());
        return ret.toString();
    }
}
