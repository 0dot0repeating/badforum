package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.passwords.ForumPasswordService;
import com.jinotrain.badforum.data.UserSettingViewData;
import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.lambdas.UserSettingInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UserSettingsController extends ForumController
{
    private static Logger logger = LoggerFactory.getLogger(LoginAndRegisterController.class);

    private static List<UserSettingInterface> settingInterfaces;


    @Autowired
    private ForumPasswordService passwordService;


    static
    {
        settingInterfaces = new ArrayList<>();

        settingInterfaces.add(new UserSettingInterface() {
            public String name() { return "Username"; }
            public String id() { return "username"; }
            public String get(ForumUser user) { return user.getUsername(); }
            public String inputType() { return "hidden"; }

            public boolean needsUpdate(ForumUser user, Map<String, String[]> params) { return false; }
        });

        settingInterfaces.add(new UserSettingInterface() {
            public String name() { return "New password"; }
            public String id() { return "newPassword"; }
            public String inputType() { return "password"; }
            public String get(ForumUser user) { return ""; }

            public boolean needsConfirm() { return true; }
            public boolean needsUpdate(ForumUser user, Map<String, String[]> params) { return false; }
        });

        settingInterfaces.add(new UserSettingInterface() {
            public String name() { return "Confirm password"; }
            public String id() { return "confirmPassword"; }
            public String inputType() { return "password"; }
            public String get(ForumUser user) { return ""; }

            public boolean needsConfirm()        { return true; }
            public boolean needsPasswordHasher() { return true; }

            public boolean setWithHasher(ForumUser user, Map<String, String[]> params, ForumPasswordService hasher)
            {
                String newPW     = getParam(params, "newPassword");
                String confirmPW = getParam(params, "confirmPassword");

                if (newPW == null || newPW.length() == 0)
                {
                    return false;
                }

                if (confirmPW == null || confirmPW.length() == 0)
                {
                    throw new IllegalArgumentException("You have to confirm your new password");
                }

                if (!confirmPW.equals(newPW))
                {
                    throw new IllegalArgumentException("Passwords don't match");
                }

                user.setPasshash(hasher.hashPassword(newPW));
                return true;
            }


            public boolean needsUpdate(ForumUser user, Map<String, String[]> params)
            {
                String newPW = getParam(params, "newPassword");
                return newPW != null && newPW.length() > 0;
            }
        });


        settingInterfaces.add(new UserSettingInterface() {
            public String name() { return "Email address"; }
            public String id() { return "email"; }
            public String get(ForumUser user) { return user.getEmail(); }

            public boolean set(ForumUser user, Map<String, String[]> params)
            {
                String newEmail = getParam(params, "email");
                if (newEmail == null) { return false; }

                if (!user.getEmail().equals(newEmail))
                {
                    user.setEmail(newEmail);
                    return true;
                }

                return false;
            }
        });
    }


    private boolean canEditSettings(ForumUser viewUser, ForumUser settingsUser)
    {
        return viewUser.getUsername().equalsIgnoreCase(settingsUser.getUsername())
            || viewUser.hasPermission(UserPermission.MANAGE_USERS);
    }


    private ModelAndView checkPermission(ForumUser viewUser, ForumUser settingsUser)
    {
        if (viewUser == null)
        {
            return errorPage("usersettings_error.html", "NOT_LOGGED_IN", HttpStatus.FORBIDDEN);
        }

        if (settingsUser == null)
        {
            return errorPage("usersettings_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (!canEditSettings(viewUser, settingsUser))
        {
            return errorPage("usersettings_error.html", "NOT_ALLOWED", HttpStatus.FORBIDDEN);
        }

        return null;
    }


    private List<UserSettingViewData> buildSettingData(ForumUser user)
    {
        List<UserSettingViewData> ret = new ArrayList<>();

        for (UserSettingInterface i: settingInterfaces)
        {
            UserSettingViewData viewData = new UserSettingViewData(i.id(), i.name(), i.get(user));
            viewData.inputType    = i.inputType();
            viewData.readonly     = i.readonly();
            viewData.needsConfirm = i.needsConfirm();
            viewData.description  = i.description();

            ret.add(viewData);
        }

        return ret;
    }


    private ModelAndView displaySettings(List<UserSettingViewData> viewData, String username, boolean needsConfirm)
    {
        ModelAndView mav = new ModelAndView("usersettings.html");

        List<UserSettingViewData> protectedViewData = new ArrayList<>();
        List<UserSettingViewData> normalViewData    = new ArrayList<>();

        for (UserSettingViewData v: viewData)
        {
            if (v.needsConfirm) { protectedViewData.add(v); }
            else                { normalViewData.add(v); }
        }

        mav.addObject("protectedSettings", protectedViewData);
        mav.addObject("normalSettings",    normalViewData);
        mav.addObject("showConfirmPassword", needsConfirm && protectedViewData.size() > 0);
        mav.addObject("username", username);

        return mav;
    }


    @Transactional
    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    public ModelAndView viewOwnSettings(HttpServletRequest  request,
                                        HttpServletResponse response)
    {
        ForumUser viewUser = getUserFromRequest(request);

        ModelAndView errorMAV = checkPermission(viewUser, viewUser);
        if (errorMAV != null) { return errorMAV; }

        List<UserSettingViewData> viewData = buildSettingData(viewUser);
        return displaySettings(viewData, viewUser.getUsername(), true);
    }


    @Transactional
    @RequestMapping(value = "/settings/*", method = RequestMethod.GET)
    public ModelAndView viewOtherSettings(HttpServletRequest  request,
                                          HttpServletResponse response)
    {
        ForumUser viewUser = getUserFromRequest(request);

        String settingsUsername = request.getServletPath().substring("/settings/".length());
        ForumUser settingsUser = userRepository.findByUsernameIgnoreCase(settingsUsername);

        ModelAndView errorMAV = checkPermission(viewUser, viewUser);
        if (errorMAV != null) { return errorMAV; }

        List<UserSettingViewData> viewData = buildSettingData(settingsUser);
        boolean needsConfirm = viewUser.getUsername().equalsIgnoreCase(settingsUsername);
        return displaySettings(viewData, settingsUser.getUsername(), needsConfirm);
    }


    @Transactional
    @RequestMapping(value = "/savesettings", method = RequestMethod.GET)
    public ModelAndView waitWhyAreYouTryingToSaveSettingsWithAGETRequestThatIsWrongAndBad(HttpServletRequest request, HttpServletResponse response)
    {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        return viewOwnSettings(request, response);
    }


    @Transactional
    @RequestMapping(value = "/savesettings", method = RequestMethod.POST)
    public ModelAndView saveSettings(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser viewUser = getUserFromRequest(request);

        String    settingsUsername = request.getParameter("username");
        ForumUser settingsUser     = settingsUsername == null ? null : userRepository.findByUsernameIgnoreCase(settingsUsername);

        ModelAndView errorMAV = checkPermission(viewUser, settingsUser);
        if (errorMAV != null) { return errorMAV; }

        // this is just here so that intellij stops complaining
        //  the checkPermission thing already has this check
        if (settingsUser == null) { return null; }

        List<UserSettingViewData>        viewData    = buildSettingData(settingsUser);
        Map<String, UserSettingViewData> viewDataMap = new HashMap<>();
        Map<String, String[]> paramData = request.getParameterMap();

        for (UserSettingViewData d: viewData) { viewDataMap.put(d.id, d); }

        boolean thingsChanged  = false;
        boolean errorsOccured  = false;
        boolean missingConfirm = false;

        boolean needsConfirm      = viewUser.getUsername().equalsIgnoreCase(settingsUsername);
        boolean passwordConfirmed = !needsConfirm;

        if (!passwordConfirmed && paramData.containsKey("currentPassword"))
        {
            String pw = paramData.get("currentPassword")[0];
            passwordConfirmed = passwordService.passwordMatches(pw, settingsUser.getPasshash());
        }

        for (UserSettingInterface i: settingInterfaces)
        {
            if (!i.needsUpdate(settingsUser, paramData)) { continue; }

            if (i.needsConfirm() && !passwordConfirmed)
            {
                errorsOccured  = true;
                missingConfirm = true;
                continue;
            }

            UserSettingViewData settingData = viewDataMap.get(i.id());

            try
            {
                boolean changed;

                if (i.needsPasswordHasher())
                {
                    changed = i.setWithHasher(settingsUser, paramData, passwordService);
                }
                else
                {
                    changed = i.set(settingsUser, paramData);
                }

                thingsChanged = thingsChanged || changed;
                settingData.value   = i.get(settingsUser);
                settingData.message = "Saved";
            }
            catch (IllegalArgumentException e)
            {
                errorsOccured = true;

                settingData.error    = true;
                settingData.message = e.getMessage();
            }
        }

        if (thingsChanged) { userRepository.saveAndFlush(settingsUser); }

        ModelAndView mav = displaySettings(viewData, settingsUser.getUsername(), needsConfirm);
        mav.addObject("savingSettings", true);
        mav.addObject("errorsOccured", errorsOccured);
        mav.addObject("thingsChanged", thingsChanged);
        mav.addObject("missingConfirm", missingConfirm);

        return mav;
    }
}
