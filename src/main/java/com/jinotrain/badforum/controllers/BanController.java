package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.entities.ForumBoard;
import com.jinotrain.badforum.db.entities.ForumPost;
import com.jinotrain.badforum.db.entities.ForumThread;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.util.UserBannedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Controller
public class BanController extends ForumController
{
    private Instant nowPlusDaysHours(int days, int hours)
    {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime then = now.plusDays(days).plusHours(hours);
        return then.atZone(ZoneId.systemDefault()).toInstant();
    }

    private Integer safeValueOf(String str)
    {
        try { return Integer.valueOf(str); }
        catch (NumberFormatException e) { return null; }
    }


    // params:
    //  - username: self-explanatory, case-insensitive
    //  - days, hours: self-explanatory, and if both are 0, interpret as "permanent"
    //  - reason: optional, put your ban reason here
    //  - postIndex: optional, the post you banned them for (will be marked with "USER WAS BANNED FOR THIS POST")

    @Transactional
    @RequestMapping(value = "/banuser")
    public ModelAndView banUser(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("banuser_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!userHasPermission(user, UserPermission.MANAGE_USERS))
        {
            return errorPage("banuser_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        String banUsername  = request.getParameter("username");
        String banDaysRaw   = request.getParameter("days");
        String banHoursRaw  = request.getParameter("hours");
        String postIndexRaw = request.getParameter("postIndex");

        List<String> errorItems = new ArrayList<>();
        if (banUsername  == null || banUsername.isEmpty()) { errorItems.add("username"); }
        if (banDaysRaw   == null || banDaysRaw.isEmpty())  { errorItems.add("days"); }
        if (banHoursRaw  == null || banHoursRaw.isEmpty()) { errorItems.add("hours"); }

        if (!errorItems.isEmpty())
        {
            ModelAndView ret = errorPage("banuser_error.html", "MISSING_PARAMETERS", HttpStatus.BAD_REQUEST);
            String errorMsg = "missing parameters: (" + String.join(", ", errorItems) + ")";
            ret.addObject("errorDetails", errorMsg);
            return ret;
        }

        Integer banDays   = safeValueOf(banDaysRaw);
        Integer banHours  = safeValueOf(banHoursRaw);
        Integer postIndex = safeValueOf(postIndexRaw);

        if (banDays   == null || banDays < 0)                   { errorItems.add("days (must be positive integer)"); }
        if (banHours  == null || banHours < 0 || banHours > 23) { errorItems.add("hours (must be integer between 0-23)"); }
        if (postIndex == null && postIndexRaw != null && !postIndexRaw.isEmpty()) { errorItems.add("postIndex (must be valid post index, blank, or null)"); }

        if (!errorItems.isEmpty())
        {
            ModelAndView ret = errorPage("banuser_error.html", "MALFORMED_PARAMETERS", HttpStatus.BAD_REQUEST);
            String errorMsg = "following parameters are not integers or out of range: (" + String.join(", ", errorItems) + ")";
            ret.addObject("errorDetails", errorMsg);
            return ret;
        }

        ForumUser banUser = userRepository.findByUsernameIgnoreCase(banUsername);

        if (banUser == null)
        {
            return errorPage("banuser_error.html", "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (banUser.isBanned())
        {
            return errorPage("banuser_error.html", "ALREADY_BANNED", HttpStatus.CONFLICT);
        }

        ForumPost banPost     = postIndex == null ? null : postRepository.findByIndex(postIndex);
        String    banReason   = request.getParameter("reason");
        if (banReason != null && banReason.isEmpty()) { banReason = null; }

        //noinspection ConstantConditions
        Instant bannedUntil = (banDays > 0 || banHours > 0) ? nowPlusDaysHours(banDays, banHours) : null;

        banUser.ban(bannedUntil, banReason);

        if (banPost != null)
        {
            banPost.userWasBannedForThisPost(banReason);
        }

        ModelAndView ret = new ModelAndView("banuser.html");
        ret.addObject("banUsername", banUser.getUsername());
        ret.addObject("bannedUntil", bannedUntil);
        ret.addObject("banReason", banReason);

        if (banPost != null && banPost.getThread() != null)
        {
            ForumThread banThread = banPost.getThread();
            ret.addObject("banThreadIndex", banThread.getIndex());
            ret.addObject("banThreadTopic", banThread.getTopic());

            if (banThread.getBoard() != null)
            {
                ForumBoard banBoard = banThread.getBoard();
                ret.addObject("banBoardIndex", banBoard.getIndex());
                ret.addObject("banBoardName", banBoard.getName());
            }
        }

        return ret;
    }


    @Transactional
    @RequestMapping(value = "/unbanuser")
    public ModelAndView unbanUser(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("unbanuser_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!userHasPermission(user, UserPermission.MANAGE_USERS))
        {
            return errorPage("unbanuser_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        String unbanUsername = request.getParameter("username");

        if (unbanUsername == null || unbanUsername.isEmpty())
        {
            return errorPage("unbanuser_error.html", "MISSING_USERNAME", HttpStatus.BAD_REQUEST);
        }

        ForumUser unbanUser = userRepository.findByUsernameIgnoreCase(unbanUsername);

        if (unbanUser == null)
        {
            return errorPage("unbanuser_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (!unbanUser.isBanned())
        {
            return errorPage("unbanuser_error.html", "NOT_BANNED", HttpStatus.CONFLICT);
        }

        unbanUser.unban();

        ModelAndView ret = new ModelAndView("unbanuser.html");
        ret.addObject("unbanUsername", unbanUser.getUsername());
        return ret;
    }


    @Transactional
    @RequestMapping(value = "/post/*/ban")
    public ModelAndView prepareBanForPost(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!userHasPermission(user, UserPermission.MANAGE_USERS))
        {
            return errorPage("banuser_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        String requestURL   = request.getServletPath();
        String postIndexRaw = requestURL.substring("/post/".length(), requestURL.length() - "/ban".length());
        long   postIndex;

        try
        {
            postIndex = Long.valueOf(postIndexRaw);
        }
        catch (NumberFormatException e)
        {
            return errorPage("banuser_error.html", "MALFORMED_INDEX", HttpStatus.BAD_REQUEST);
        }

        ForumPost banPost = postRepository.findByIndex(postIndex);

        if (banPost == null)
        {
            return errorPage("banuser_error.html", "POST_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        ForumUser   banUser = banPost.getAuthor();
        ForumThread thread  = banPost.getThread();

        if (banUser == null)
        {
            return errorPage("banuser_error.html", "NO_AUTHOR", HttpStatus.NOT_FOUND);
        }

        ModelAndView ret = new ModelAndView("banforpost.html");
        ret.addObject("banUsername", banUser.getUsername());
        ret.addObject("postIndex", postIndex);
        ret.addObject("threadTopic", thread == null ? null : thread.getTopic());
        return ret;
    }
}
