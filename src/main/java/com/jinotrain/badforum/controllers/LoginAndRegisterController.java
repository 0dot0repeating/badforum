package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.flooding.FloodCategory;
import com.jinotrain.badforum.components.passwords.ForumPasswordService;
import com.jinotrain.badforum.data.PreAdminKey;
import com.jinotrain.badforum.db.entities.ForumRole;
import com.jinotrain.badforum.db.entities.ForumSession;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.util.DurationFormat;
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

        if (username.equals("you"))
        {
            ret.put("registered", false);
            ret.put("errorCode", "USERNAME_CANT_BE_YOU");
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

        ForumUser existingUser = userRepository.findByUsernameIgnoreCase(username);

        if (existingUser != null)
        {
            ret.put("registered", false);
            ret.put("errorCode", "USERNAME_TAKEN");
            return ret;
        }

        try
        {
            String passhash = passwordService.hashPassword(password);
            ForumUser user = new ForumUser(username, passhash, email);

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

        if (!passwordService.passwordMatches(password, user.getPasshash()))
        {
            ret.put("loggedIn", false);
            ret.put("errorCode", "WRONG_PASSWORD");
            return ret;
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




    @ResponseBody
    @Transactional
    @RequestMapping(value = "/api/register", produces = "application/json")
    public String registerUserViaJSON(HttpServletRequest request)
    {
        String username  = request.getParameter("username");
        String email     = request.getParameter("email");
        String password  = request.getParameter("password");
        String pwConfirm = request.getParameter("confirm");
        String address   = request.getRemoteAddr();

        boolean notFlooding = floodProtectionService.updateIfNotFlooding(FloodCategory.REGISTER, address);

        if (notFlooding)
        {
            Map<String, Object> retMap = registerUser(username, email, password, pwConfirm);
            retMap.remove("session");

            return new JSONObject(retMap).toString();
        }

        JSONObject ret = new JSONObject();
        ret.put("registered", false);
        ret.put("flooding", true);
        return ret.toString();
    }


    @Transactional
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ModelAndView registerUserViaPOST(HttpServletRequest request, HttpServletResponse response)
    {
        String username  = request.getParameter("username");
        String email     = request.getParameter("email");
        String password  = request.getParameter("password");
        String pwConfirm = request.getParameter("confirm");
        String address   = request.getRemoteAddr();

        ModelAndView mav;
        boolean flooding = !floodProtectionService.updateIfNotFlooding(FloodCategory.REGISTER, address);

        if (flooding)
        {
            mav = new ModelAndView("flooding.html");
            Duration floodWindow = floodProtectionService.getFloodWindow(FloodCategory.REGISTER);

            response.setStatus(429); // too many requests
            mav.addObject("floodType", "login");
            mav.addObject("floodWindow", DurationFormat.format(floodWindow));
            return mav;
        }

        Map<String, Object> result = registerUser(username, email, password, pwConfirm);

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
            mav.addObject("email", email);
        }

        return mav;
    }


    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public ModelAndView getRegisterPage()
    {
        return new ModelAndView("register.html");
    }




    @ResponseBody
    @Transactional
    @RequestMapping(value = "/api/login", produces = "application/json")
    public String loginViaJSON(HttpServletRequest request)
    {
        String username   = request.getParameter("username");
        String password   = request.getParameter("password");
        String rememberMe = request.getParameter("rememberMe");
        String address    = request.getRemoteAddr();

        boolean notFlooding = floodProtectionService.updateIfNotFlooding(FloodCategory.LOGIN, address);

        if (notFlooding)
        {
            Map<String, Object> retMap = loginUser(username, password, Boolean.parseBoolean(rememberMe));
            retMap.remove("session");

            return new JSONObject(retMap).toString();
        }

        JSONObject ret = new JSONObject();
        ret.put("loggedIn", false);
        ret.put("flooding", true);
        return ret.toString();
    }


    @Transactional
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ModelAndView loginViaPOST(HttpServletRequest request, HttpServletResponse response)
    {
        String username    = request.getParameter("username");
        String password    = request.getParameter("password");
        boolean rememberMe = Boolean.parseBoolean(request.getParameter("rememberMe"));
        String address     = request.getRemoteAddr();

        ModelAndView mav;
        boolean flooding = !floodProtectionService.updateIfNotFlooding(FloodCategory.LOGIN, address);

        if (flooding)
        {
            mav = new ModelAndView("flooding.html");
            Duration floodWindow = floodProtectionService.getFloodWindow(FloodCategory.LOGIN);

            response.setStatus(429); // too many requests
            mav.addObject("floodType", "login");
            mav.addObject("floodWindow", DurationFormat.format(floodWindow));
            return mav;
        }

        String referer = request.getParameter("previousPage");
        if (referer == null) { referer = request.getHeader("Referer"); }
        if (referer == null) { referer = "/"; }

        Map<String, Object> result = loginUser(username, password, rememberMe);

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
        return new ModelAndView("login.html");
    }




    @ResponseBody
    @Transactional
    @RequestMapping(value = "/api/logout", produces = "application/json")
    public String logoutViaJSON(String sessionID)
    {
        ForumSession session = sessionRepository.findById(sessionID).orElse(null);
        Map<String, Object> retMap = logout(session);
        return new JSONObject(retMap).toString();
    }


    @Transactional
    @RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST}, produces = "text/plain")
    public String logoutViaPOST(HttpServletRequest request, HttpServletResponse response)
    {
        ForumSession session = getForumSession(request, true);
        if (session != null) { logout(session); }

        String referer = request.getHeader("Referer");
        if (referer == null) { referer = ""; }

        return "redirect:" + referer;
    }




    @ResponseBody
    @RequestMapping(value = "/api/checkUsername", method = RequestMethod.GET, produces = "application/json")
    public String validateUsername(String username)
    {
        ForumUser existingUser = userRepository.findByUsernameIgnoreCase(username);

        JSONObject ret = new JSONObject();
        ret.put("tooLong",   username.length() > ForumUser.MAX_USERNAME_LENGTH);
        ret.put("tooShort",  username.length() < ForumUser.MIN_USERNAME_LENGTH);
        ret.put("valid",     username.matches(ForumUser.VALID_USERNAME_REGEX));
        ret.put("available", existingUser == null);
        return ret.toString();
    }
}
