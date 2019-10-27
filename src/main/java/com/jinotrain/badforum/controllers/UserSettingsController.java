package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.components.passwords.ForumPasswordService;
import com.jinotrain.badforum.data.UserRoleStateData;
import com.jinotrain.badforum.data.UserSettingViewData;
import com.jinotrain.badforum.data.UserViewData;
import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.entities.ForumRole;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.lambdas.UserSettingInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Controller
public class UserSettingsController extends ForumController
{
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
            return errorPage("usersettings_error.html", "NOT_LOGGED_IN", HttpStatus.UNAUTHORIZED);
        }

        if (settingsUser == null)
        {
            return errorPage("usersettings_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        if (!canEditSettings(viewUser, settingsUser))
        {
            return errorPage("usersettings_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
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


    private ModelAndView displaySettings(List<UserSettingViewData> viewData, String username, List<UserRoleStateData> userRoles, boolean needsConfirm)
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
        mav.addObject("userRoles", userRoles);

        return mav;
    }


    private List<UserRoleStateData> getUserRoles(ForumUser user)
    {
        List<ForumRole> userRoles = user.getRoles();
        List<UserRoleStateData> ret = new ArrayList<>();

        Set<Long> userRoleIDs = new HashSet<>();
        for (ForumRole role: userRoles) { userRoleIDs.add(role.getId()); }


        List<Object[]> roleData = em.createQuery("SELECT role.name, role.id, role.admin FROM ForumRole role"
                                                         + " WHERE role.defaultRole = false ORDER BY role.priority DESC", Object[].class)
                                          .getResultList();

        int i = 0;

        for (Object[] d: roleData)
        {
            String  name    = (String)d[0];
            boolean present = userRoleIDs.contains((Long)d[1]);
            ret.add(new UserRoleStateData(name, present, (boolean)d[2], i++));
        }

        return ret;
    }


    private void updateRolesFromParams(ForumUser user, Map<String, String[]> params) throws IllegalArgumentException
    {
        Map<String, Boolean> roleStates = new HashMap<>();

        for (Map.Entry<String, String[]> param: params.entrySet())
        {
            String roleName  = param.getKey().toLowerCase();
            String roleState = param.getValue()[0];

            if (roleName.startsWith("role:")) { roleName = roleName.substring("role:".length()); }
            else { continue; }

            roleStates.put(roleName, "1".equals(roleState));
        }

        if (roleStates.isEmpty())
        {
            throw new IllegalArgumentException("No roles specified");
        }

        List<ForumRole> roles = em.createQuery("SELECT role FROM ForumRole role WHERE lower(role.name) IN :roleNames"
                                                       + " AND role.defaultRole = false", ForumRole.class)
                                        .setParameter("roleNames", roleStates.keySet())
                                        .getResultList();

        for (ForumRole role: roles)
        {
            boolean keep = roleStates.get(role.getName().toLowerCase());

            if (keep) { user.addRole(role); }
            else      { user.removeRole(role); }
        }
    }



    @Transactional
    @RequestMapping(value = "/settings")
    public ModelAndView viewOwnSettings(HttpServletRequest  request,
                                        HttpServletResponse response)
    {
        ForumUser viewUser = getUserFromRequest(request);

        ModelAndView errorMAV = checkPermission(viewUser, viewUser);
        if (errorMAV != null) { return errorMAV; }

        List<UserSettingViewData> viewData = buildSettingData(viewUser);
        List<UserRoleStateData> userRoles  = userHasPermission(viewUser, UserPermission.MANAGE_USERS) ? getUserRoles(viewUser) : null;

        return displaySettings(viewData, viewUser.getUsername(), userRoles, true);
    }


    @Transactional
    @RequestMapping(value = "/settings/*")
    public ModelAndView viewOtherSettings(HttpServletRequest  request,
                                          HttpServletResponse response)
    {
        ForumUser viewUser = getUserFromRequest(request);

        String settingsUsername = request.getServletPath().substring("/settings/".length());
        ForumUser settingsUser = userRepository.findByUsernameIgnoreCase(settingsUsername);

        ModelAndView errorMAV = checkPermission(viewUser, viewUser);
        if (errorMAV != null) { return errorMAV; }

        List<UserSettingViewData> viewData = buildSettingData(settingsUser);
        List<UserRoleStateData> userRoles  = userHasPermission(viewUser, UserPermission.MANAGE_USERS) ? getUserRoles(settingsUser) : null;
        boolean needsConfirm = viewUser.getUsername().equalsIgnoreCase(settingsUsername);

        return displaySettings(viewData, settingsUser.getUsername(), userRoles, needsConfirm);
    }


    @Transactional
    @RequestMapping(value = "/savesettings")
    public ModelAndView saveSettings(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("usersettings_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

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

        List<UserRoleStateData> userRoles  = userHasPermission(viewUser, UserPermission.MANAGE_USERS) ? getUserRoles(settingsUser) : null;
        ModelAndView mav = displaySettings(viewData, settingsUser.getUsername(), userRoles, needsConfirm);

        mav.addObject("savingSettings", true);
        mav.addObject("errorsOccured", errorsOccured);
        mav.addObject("thingsChanged", thingsChanged);
        mav.addObject("missingConfirm", missingConfirm);

        return mav;
    }


    // parameter format:
    //  key: role:[role name]
    //  value: 0 for "take away this role", 1 for "give this role"

    @Transactional
    @RequestMapping(value = "/saveuserroles")
    public ModelAndView saveUserRoles(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("userroles_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        ForumUser accessUser = getUserFromRequest(request);

        if (!userHasPermission(accessUser, UserPermission.MANAGE_USERS))
        {
            return errorPage("userroles_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        String modifyUsername = request.getParameter("username");

        if (modifyUsername == null)
        {
            return errorPage("userroles_error.html", "MISSING_USERNAME", HttpStatus.BAD_REQUEST);
        }

        ForumUser modifyUser = userRepository.findByUsernameIgnoreCase(modifyUsername);

        if (modifyUser == null)
        {
            return errorPage("userroles_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        try
        {
            updateRolesFromParams(modifyUser, request.getParameterMap());
        }
        catch (IllegalArgumentException e)
        {
            return errorPage("userroles_error.html", "MISSING_ROLES", HttpStatus.BAD_REQUEST);
        }

        ModelAndView ret = new ModelAndView("userroles_saved.html");
        ret.addObject("username", modifyUser.getUsername());
        return ret;
    }
}
