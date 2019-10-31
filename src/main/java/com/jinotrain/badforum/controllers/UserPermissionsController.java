package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.data.UserPermissionStateData;
import com.jinotrain.badforum.data.UserRoleData;
import com.jinotrain.badforum.db.PermissionState;
import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.entities.ForumRole;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.util.UserBannedException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Controller
public class UserPermissionsController extends ForumController
{
    private static Map<String, UserPermission> PERMISSIONS_BY_NAME;

    static
    {
        PERMISSIONS_BY_NAME = new HashMap<>();

        for (UserPermission p: UserPermission.values())
        {
            PERMISSIONS_BY_NAME.put(p.name().toLowerCase(), p);
        }
    }


    private List<UserRoleData> getRolesAndPerms()
    {
        List<UserRoleData> ret = new ArrayList<>();
        int i = 0;

        for (ForumRole role: roleRepository.findAll(Sort.by(Sort.Direction.DESC, "priority")))
        {
            List<UserPermissionStateData> permissions = new ArrayList<>();

            for (UserPermission p: UserPermission.values())
            {
                permissions.add(new UserPermissionStateData(p, role.getPermission(p)));
            }

             ret.add(new UserRoleData(role.getName(), role.getPriority(), role.isAdmin(), role.isDefaultRole(), permissions, i++));
        }

        return ret;
    }


    private void updatePermissions(Map<String, UserRoleData> permissionData, Map<String, String> toRename, Set<String> toDelete)
    {
        Set<String> roleNames = new HashSet<>();

        for (String name: permissionData.keySet())
        {
            roleNames.add(name.toLowerCase());
        }

        for (String name: toRename.keySet())
        {
            roleNames.add(name.toLowerCase());
        }

        roleNames.addAll(toDelete);

        List<ForumRole> roles = em.createQuery("SELECT role FROM ForumRole role WHERE lower(role.name) IN :roleNames", ForumRole.class)
                                  .setParameter("roleNames", roleNames)
                                  .getResultList();

        for (ForumRole role: roles)
        {
            if (role.isAdmin()) { continue; }

            String roleName = role.getName().toLowerCase();

            if (!role.isDefaultRole())
            {
                if (toDelete.contains(roleName))
                {
                    em.createQuery("DELETE FROM UserToRoleLink l WHERE l.role.id = :roleID")
                      .setParameter("roleID", role.getId())
                      .executeUpdate();

                    roleRepository.delete(role);
                    continue;
                }

                String newName = toRename.get(roleName);
                if (newName != null) { role.setName(newName); }
            }

            UserRoleData data = permissionData.get(roleName);

            for (UserPermissionStateData state: data.permissions)
            {
                role.setPermission(state.perm, state.state);
            }

            if (data.priority != null)
            {
                role.setPriority(data.priority);
            }
        }
    }


    @Transactional
    @RequestMapping("/roles")
    public ModelAndView viewRoles(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!userHasPermission(user, UserPermission.MANAGE_USERS))
        {
            return errorPage("roles_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        ModelAndView ret = new ModelAndView("roles.html");
        ret.addObject("roleData", getRolesAndPerms());
        ret.addObject("permissions", UserPermission.values());
        return ret;
    }


    private class PermissionReturnData
    {
        Map<String, UserRoleData> permissions;
        Map<String, String> toRename;
        Set<String> toDelete;

        PermissionReturnData(Map<String, UserRoleData> permissions, Map<String, String> toRename, Set<String> toDelete)
        {
            this.permissions = permissions;
            this.toRename = toRename;
            this.toDelete = toDelete;
        }
    }

    // parameter format is similar to saving board permissions, and is as follows:
    //  key: [permission name]:[role name]
    //  value: -1 for "disable", 0 for "keep", 1 for enable
    //
    // alternatively, to set the priority of a role:
    //  key: (priority):[role name]
    //  value: new priority, between -1000 and 1000 inclusive

    private PermissionReturnData buildPermissionsFromParams(Map<String, String[]> params) throws IllegalArgumentException
    {
        Map<String, UserRoleData> permData = new HashMap<>();
        Map<String, String> renameData = new HashMap<>();
        Set<String> deleteData = new HashSet<>();

        List<String> malformedPriorities = new ArrayList<>();
        boolean errored = false;

        for (Map.Entry<String, String[]> param: params.entrySet())
        {
            String paramKey = param.getKey();
            String paramVal = param.getValue()[0];

            int delimIndex = paramKey.indexOf(':');
            if (delimIndex == -1) { continue; }

            String permName     = paramKey.substring(0, delimIndex).toLowerCase();
            String roleNameCase = paramKey.substring(delimIndex+1);
            String roleName     = roleNameCase.toLowerCase();

            if ("(delete)".equals(permName) && "do it".equals(paramVal))
            {
                deleteData.add(roleName);
                continue;
            }

            if ("(name)".equals(permName) && !paramVal.isEmpty())
            {
                renameData.put(roleName, paramVal);
                continue;
            }

            if ("(priority)".equals(permName))
            {
                if (errored)
                {
                    try { Integer.valueOf(paramVal); }
                    catch (NumberFormatException e) { malformedPriorities.add(roleNameCase); }

                    continue;
                }

                UserRoleData roleData = permData.computeIfAbsent(roleName, UserRoleData::new);

                try
                {
                    int priority = Integer.valueOf(paramVal);
                    roleData.priority = Math.max(-1000, Math.min(priority, 1000));
                }
                catch (NumberFormatException e)
                {
                    malformedPriorities.add(roleNameCase);
                    errored = true;
                }

                continue;
            }

            if (errored) { continue; }

            UserPermission perm = PERMISSIONS_BY_NAME.get(permName);
            if (perm == null) { continue; }

            UserRoleData roleData = permData.computeIfAbsent(roleName, UserRoleData::new);

            PermissionState state;

            switch (paramVal)
            {
                case "-1": state = PermissionState.OFF;  break;
                case "1":  state = PermissionState.ON;   break;
                default:   state = PermissionState.KEEP; break;
            }

            roleData.permissions.add(new UserPermissionStateData(perm, state));
        }

        if (errored)
        {
            String msg = "Malformed priorities on: \"" + String.join("\", \"", malformedPriorities) + "\"";
            throw new IllegalArgumentException(msg);
        }

        return new PermissionReturnData(permData, renameData, deleteData);
    }


    @Transactional
    @RequestMapping(value = "/saveroles")
    public ModelAndView saveRoleSettings(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("roles_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        ForumUser accessUser;
        try { accessUser = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!userHasPermission(accessUser, UserPermission.MANAGE_USERS))
        {
            return errorPage("roles_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        PermissionReturnData permissionData;

        try
        {
            permissionData = buildPermissionsFromParams(request.getParameterMap());
        }
        catch (IllegalArgumentException e)
        {
            ModelAndView error = errorPage("roles_error.html", "MALFORMED_PRIORITY", HttpStatus.BAD_REQUEST);
            error.addObject("errorDetails", e.getMessage());
            return error;
        }

        updatePermissions(permissionData.permissions, permissionData.toRename, permissionData.toDelete);
        return new ModelAndView("roles_saved.html");
    }


    // parameters:
    //  roleName: role name
    //  priority: priority
    //  <UserPermission.name()>: -1 for off, 0 for keep, 1 for on

    @Transactional
    @RequestMapping(value = "/newrole")
    public ModelAndView createNewRole(HttpServletRequest request, HttpServletResponse response)
    {
        if (!request.getMethod().equals("POST"))
        {
            return errorPage("newrole_error.html", "POST_ONLY", HttpStatus.METHOD_NOT_ALLOWED);
        }

        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }


        if (!userHasPermission(user, UserPermission.MANAGE_USERS))
        {
            return errorPage("newrole_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        String roleName  = null;
        Integer priority = null;
        Map<UserPermission, PermissionState> permissions = new HashMap<>();

        for (Map.Entry<String, String[]> param: request.getParameterMap().entrySet())
        {
            String key = param.getKey().toLowerCase();
            String val = param.getValue()[0];

            if (key.isEmpty() || val.isEmpty()) { continue; }

            if ("rolename".equals(key))
            {
                roleName = val;
                continue;
            }

            if ("priority".equals(key))
            {
                try
                {
                    priority = Math.max(-1000, Math.min(Integer.valueOf(val), 1000));
                }
                catch (NumberFormatException e)
                {
                    return errorPage("newrole_error.html", "MALFORMED_PRIORITY", HttpStatus.BAD_REQUEST);
                }
                continue;
            }

            UserPermission perm = PERMISSIONS_BY_NAME.get(key);

            if (perm != null)
            {
                PermissionState state;

                switch (val)
                {
                    case "-1": state = PermissionState.OFF;  break;
                    case "1":  state = PermissionState.ON;   break;
                    default:   state = PermissionState.KEEP; break;
                }

                permissions.put(perm, state);
            }
        }

        if (roleName == null || roleName.isEmpty())
        {
            return errorPage("newrole_error.html", "MISSING_NAME", HttpStatus.BAD_REQUEST);
        }

        if (roleRepository.existsByNameIgnoreCase(roleName))
        {
            return errorPage("newrole_error.html", "ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        if (priority == null) { priority = 0; }

        ForumRole newRole = new ForumRole(roleName, priority);

        for (Map.Entry<UserPermission, PermissionState> param: permissions.entrySet())
        {
            newRole.setPermission(param.getKey(), param.getValue());
        }

        roleRepository.saveAndFlush(newRole);

        ModelAndView ret = new ModelAndView("newrole.html");
        ret.addObject("roleName", newRole.getName());
        return ret;
    }
}
