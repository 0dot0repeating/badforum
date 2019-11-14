package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.flooding.FloodCategory;
import com.jinotrain.badforum.components.passwords.ForumPasswordService;
import com.jinotrain.badforum.data.PreAdminKey;
import com.jinotrain.badforum.db.entities.ForumRole;
import com.jinotrain.badforum.db.entities.ForumSession;
import com.jinotrain.badforum.db.entities.ForumUser;
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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Controller
public class LoginAndRegisterController extends ForumController
{
    private static Logger logger = LoggerFactory.getLogger(LoginAndRegisterController.class);

    @Autowired
    private ForumPasswordService passwordService;

    @Autowired
    private PreAdminKey preAdminKey;


    private Map<String, Object> registerUser(String username, String password, String pwConfirm)
    {
        Map<String, Object> ret = new HashMap<>();

        username  = username  != null ? username  : "";
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

        if (username.equalsIgnoreCase("anonymous"))
        {
            ret.put("registered", false);
            ret.put("errorCode", "USERNAME_CANT_BE_ANON");
            return ret;
        }

        if (username.length() < ForumUser.MIN_USERNAME_LENGTH)
        {
            ret.put("registered", false);
            ret.put("errorCode", "USERNAME_TOO_SHORT");
            return ret;
        }

        if (username.length() > ForumUser.MAX_USERNAME_LENGTH)
        {
            ret.put("registered", false);
            ret.put("errorCode", "USERNAME_TOO_LONG");
            return ret;
        }

        if (!username.matches(ForumUser.VALID_USERNAME_REGEX))
        {
            ret.put("registered", false);
            ret.put("errorCode", "USERNAME_INVALID");
            return ret;
        }

        if (!password.equals(pwConfirm))
        {
            ret.put("registered", false);
            ret.put("errorCode", "PASSWORD_NOT_MATCHING");
            return ret;
        }

        if (userRepository.countByUsernameIgnoreCase(username) > 0)
        {
            ret.put("registered", false);
            ret.put("errorCode", "USERNAME_TAKEN");
            return ret;
        }

        try
        {
            String passhash = passwordService.hashPassword(password);
            ForumUser user = new ForumUser(username, passhash);

            ForumRole defaultRole = roleRepository.findDefaultRole();
            user.addRole(defaultRole);

            String adminCode = preAdminKey.getKey();

            if (password.equals(adminCode))
            {
                Long id = preAdminKey.getAdminRoleID();
                ForumRole adminRole = roleRepository.getOne(id);
                user.addRole(adminRole);

                ret.put("createdAdmin", true);
                preAdminKey.setKey(null);
            }

            userRepository.saveAndFlush(user);

            ForumSession session = new ForumSession(user);
            sessionRepository.saveAndFlush(session);
            sessionRepository.pruneSessions(user);

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


    private Map<String, Object> loginUser(String username, String password, boolean rememberMe)
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

        ForumUser user = userRepository.findByUsernameIgnoreCase(username);

        if (user == null)
        {
            ret.put("loggedIn", false);
            ret.put("errorCode", "USER_NOT_FOUND");
            return ret;
        }

        String currentHash  = user.getPasshash();
        String upgradedHash = passwordService.checkAndUpgradePassword(password, currentHash);

        if (upgradedHash == null)
        {
            ret.put("loggedIn", false);
            ret.put("errorCode", "WRONG_PASSWORD");
            return ret;
        }

        if (!currentHash.equals(upgradedHash))
        {
            user.setPasshash(upgradedHash);
        }

        try
        {
            ForumSession session = new ForumSession(user, rememberMe ? Duration.ofDays(365*10) : Duration.ofMinutes(30));
            sessionRepository.saveAndFlush(session);
            sessionRepository.pruneSessions(user);

            user.setLastLoginTime(Instant.now());

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


    private Map<String, Object> logout(ForumSession session)
    {
        Map<String, Object> ret = new HashMap<>();

        if (session == null)
        {
            ret.put("loggedOut", false);
            ret.put("errorCode", "INVALID_SESSION_ID");
            return ret;
        }

        sessionRepository.delete(session);
        ret.put("loggedOut", true);
        return ret;
    }



    @Transactional
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ModelAndView registerUserViaPOST(HttpServletRequest request, HttpServletResponse response)
    {
        if (isFlooding(request)) { return floodingPage(FloodCategory.ANY); }

        String username  = request.getParameter("username");
        String password  = request.getParameter("password");
        String pwConfirm = request.getParameter("confirm");
        String address   = request.getRemoteAddr();

        boolean flooding = !floodProtectionService.updateIfNotFlooding(FloodCategory.REGISTER, address);
        if (flooding) { return floodingPage(FloodCategory.REGISTER); }

        Map<String, Object> result = registerUser(username, password, pwConfirm);
        ModelAndView mav;

        if ((boolean)result.get("registered"))
        {
            mav = new ModelAndView("registered.html");

            Cookie sessionCookie = new Cookie("forumSession", (String)result.get("sessionID"));
            response.addCookie(sessionCookie);

            request.setAttribute("forumSession", result.get("session"));
            mav.addObject("createdAdmin", result.getOrDefault("createdAdmin", false));
        }
        else
        {
            mav = new ModelAndView("register.html");

            mav.addObject("errorCode",  result.get("errorCode"));
            mav.addObject("errorExtra", result.getOrDefault("errorExtra", null));
            mav.addObject("username", username);
        }

        return mav;
    }


    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public ModelAndView getRegisterPage(HttpServletRequest request)
    {
        if (isFlooding(request)) { return floodingPage(FloodCategory.ANY); }

        return new ModelAndView("register.html");
    }


    @Transactional
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ModelAndView loginViaPOST(HttpServletRequest request, HttpServletResponse response)
    {
        if (isFlooding(request)) { return floodingPage(FloodCategory.ANY); }

        String username    = request.getParameter("username");
        String password    = request.getParameter("password");
        boolean rememberMe = Boolean.parseBoolean(request.getParameter("rememberMe"));
        String address     = request.getRemoteAddr();

        boolean flooding = !floodProtectionService.updateIfNotFlooding(FloodCategory.LOGIN, address);
        if (flooding) { return floodingPage(FloodCategory.LOGIN); }

        String referer = request.getParameter("previousPage");
        if (referer == null) { referer = request.getHeader("Referer"); }
        if (referer == null) { referer = "/"; }

        Map<String, Object> result = loginUser(username, password, rememberMe);
        ModelAndView mav;

        if ((boolean)result.get("loggedIn"))
        {
            mav = new ModelAndView("redirect:" + referer);

            Cookie sessionCookie = new Cookie("forumSession", (String)result.get("sessionID"));
            response.addCookie(sessionCookie);

            request.setAttribute("forumSession", result.get("session"));
        }
        else
        {
            mav = new ModelAndView("login.html");

            mav.addObject("errorCode",  result.get("errorCode"));
            mav.addObject("errorExtra", result.getOrDefault("errorExtra", null));
            mav.addObject("username", username);
            mav.addObject("realReferer", referer);
        }

        return mav;
    }


    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView getLoginPage(HttpServletRequest request,
                                     HttpServletResponse response)
    {
        if (isFlooding(request)) { return floodingPage(FloodCategory.ANY); }

        return new ModelAndView("login.html");
    }


    @Transactional
    @RequestMapping(value = "/logout", produces = "text/plain")
    public ModelAndView logoutViaPOST(HttpServletRequest request, HttpServletResponse response)
    {
        if (isFlooding(request)) { return floodingPage(FloodCategory.ANY); }

        ForumSession session = getForumSession(request, true);
        if (session != null) { logout(session); }

        String referer = request.getHeader("Referer");
        if (referer == null) { referer = ""; }

        return new ModelAndView("redirect:" + referer);
    }




    @ResponseBody
    @RequestMapping(value = "/api/checkUsername", produces = "application/json")
    public String validateUsername(String username)
    {
        JSONObject ret = new JSONObject();
        ret.put("tooLong",   username.length() > ForumUser.MAX_USERNAME_LENGTH);
        ret.put("tooShort",  username.length() < ForumUser.MIN_USERNAME_LENGTH);
        ret.put("valid",     username.matches(ForumUser.VALID_USERNAME_REGEX));
        ret.put("available", userRepository.countByUsernameIgnoreCase(username) == 0);
        return ret.toString();
    }
}
