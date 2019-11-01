package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.data.UserPermissionStateData;
import com.jinotrain.badforum.data.UserRoleData;
import com.jinotrain.badforum.db.PermissionState;
import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.entities.ForumRole;
import com.jinotrain.badforum.db.entities.ForumUser;
import com.jinotrain.badforum.util.RandomIDGenerator;
import com.jinotrain.badforum.util.UserBannedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
    private static Logger logger = LoggerFactory.getLogger(UserPermissionsController.class);
    private static Map<String, UserPermission> PERMISSIONS_BY_NAME;

    static
    {
        PERMISSIONS_BY_NAME = new HashMap<>();

        for (UserPermission p: UserPermission.values())
        {
            PERMISSIONS_BY_NAME.put(p.name().toLowerCase(), p);
        }
    }


    private List<UserRoleData> getRolesAndPerms(int maxPriority)
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

            boolean canModify = role.getPriority() < maxPriority;
             ret.add(new UserRoleData(role.getName(), role.getPriority(), role.isAdmin(), role.isDefaultRole(), canModify, permissions, i++));
        }

        return ret;
    }


    private boolean modifyingRolesTooHigh(Set<String> roleNames, int maxPriority)
    {
        int roleMax = em.createQuery("SELECT MAX(role.priority) FROM ForumRole role WHERE LOWER(role.name) IN :roleNames", Integer.class)
                        .setParameter("roleNames", roleNames)
                        .getSingleResult();

        return roleMax >= maxPriority;
    }


    // returns list of lowercase names that are currently used by one role,
    //  but will be used by a different one after renaming
    //
    // any names in this list will move from one role to another if a
    //  DataIntegrityViolationException hasn't been thrown

    private Set<String> checkForNameConflicts(Map<String, String> toRename) throws DataIntegrityViolationException
    {
        if (toRename.isEmpty()) { return new HashSet<>(); }

        Set<String> namesToCheck    = new HashSet<>();
        Set<String> reassignedNames = new HashSet<>();

        for (Map.Entry<String, String> entry: toRename.entrySet())
        {
            namesToCheck.add(entry.getKey().toLowerCase()); // from-name

            String toName = entry.getValue().toLowerCase();
            namesToCheck.add(toName);
            reassignedNames.add(toName);
        }

        Map<String, Integer> conflictCount = new HashMap<>();
        List<String> conflicts = new ArrayList<>();

        Set<String> usedNames = new HashSet<>(em.createQuery("SELECT role.name FROM ForumRole role WHERE LOWER(role.name) IN :roleNames", String.class)
                                                .setParameter("roleNames", namesToCheck).getResultList());

        for (String name: usedNames)
        {
            String endName = toRename.getOrDefault(name, name);
            int count = conflictCount.getOrDefault(endName, 0);
            if (count == 1) { conflicts.add(endName); }

            conflictCount.put(endName, count+1);
        }

        if (!conflicts.isEmpty())
        {
            List<String> conflictStrs = new ArrayList<>();

            for (String name: conflicts)
            {
                conflictStrs.add("\"" + name + "\" (" + conflictCount.get(name) + " uses)");
            }

            throw new DataIntegrityViolationException("Attempted to rename roles to same names: " + String.join(", ", conflictStrs));
        }

        reassignedNames.retainAll(usedNames);
        return reassignedNames;
    }


    private void updatePermissions(Map<String, UserRoleData> permissionData, Map<String, String> toRename, Set<String> toDelete, int maxPriority) throws SecurityException, DataIntegrityViolationException
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

        for (String name: toDelete)
        {
            roleNames.add(name.toLowerCase());
        }

        if (roleNames.isEmpty()) { return; }

        if (modifyingRolesTooHigh(roleNames, maxPriority))
        {
            throw new SecurityException("user attempted to modify roles at or above their own max priority");
        }

        for (UserRoleData roleData: permissionData.values())
        {
            if (roleData.priority != null && roleData.priority >= maxPriority)
            {
                throw new SecurityException("user attempted to raise a role's priority to or above their highest role");
            }
        }

        Set<String> shiftedNames = checkForNameConflicts(toRename);

        // deleting a given role is handled first, then its permissions are updated
        // once all that's done, then roles are renamed, with the default and admin roles exempted from this
        List<ForumRole> roles = roleRepository.findAllByNameIgnoreCaseIn(roleNames);
        Map<String, ForumRole> roleMap = new HashMap<>();

        for (ForumRole role: roles)
        {
            if (role.isAdmin()) { continue; }

            String  roleName    = role.getName().toLowerCase();
            roleMap.put(roleName, role);

            boolean admin       = role.isAdmin();
            boolean defaultRole = role.isDefaultRole();

            if (admin || defaultRole)
            {
                toRename.remove(roleName);
                if (admin) { continue; }
            }

            if (!defaultRole && toDelete.contains(roleName))
            {
                em.createQuery("DELETE FROM UserToRoleLink l WHERE l.role.id = :roleID")
                  .setParameter("roleID", role.getId())
                  .executeUpdate();

                roleRepository.delete(role);
                continue;
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

        // TODO: figure out how to do this in one query
        // the two passes are necessary because the DB enforces unique constraints mid-commit

        Map<String, String>    tempToNewName = new HashMap<>();
        Map<String, ForumRole> tempToRole    = new HashMap<>();
        Set<ForumRole> changed = new HashSet<>();

        for (Map.Entry<String, String> entry: toRename.entrySet())
        {
            String fromName      = entry.getKey();
            String fromNameLower = fromName.toLowerCase();
            String toName        = entry.getValue();

            ForumRole role = roleMap.get(fromNameLower);
            if (role == null) { continue; }

            if (shiftedNames.contains(fromNameLower))
            {
                String tempID;
                do { tempID = RandomIDGenerator.newID(32); }
                while (roleRepository.existsByNameIgnoreCase(tempID));

                role.setName(tempID);
                tempToNewName.put(tempID, toName);
                tempToRole.put(tempID, role);
            }
            else
            {
                role.setName(toName);
            }

            changed.add(role);
        }

        // without this, the DB still goes straight from from-name to to-name and complains about conflicts
        roleRepository.saveAll(changed);
        roleRepository.flush();
        changed.clear();

        for (Map.Entry<String, String> entry: tempToNewName.entrySet())
        {
            String tempID = entry.getKey();
            String toName = entry.getValue();

            ForumRole role = tempToRole.get(tempID);
            role.setName(toName);
            changed.add(role);
        }

        roleRepository.saveAll(changed);
        roleRepository.flush();
    }


    @Transactional
    @RequestMapping("/roles")
    public ModelAndView viewRoles(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!userHasPermission(user, UserPermission.MANAGE_ROLES))
        {
            return errorPage("roles_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        ModelAndView ret = new ModelAndView("roles.html");
        ret.addObject("roleData", getRolesAndPerms(user.getMaxPriority()));
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

            if ("(name)".equals(permName) && !paramVal.isEmpty() && !roleName.equals(paramVal))
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

        if (!userHasPermission(accessUser, UserPermission.MANAGE_ROLES))
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

        try
        {
            updatePermissions(permissionData.permissions, permissionData.toRename, permissionData.toDelete, accessUser.getMaxPriority());
        }
        catch (DataIntegrityViolationException e)
        {
            ModelAndView error = errorPage("roles_error.html", "NAME_CONFLICTS", HttpStatus.CONFLICT);
            error.addObject("errorDetails", e.getMessage());
            return error;
        }
        catch (SecurityException e)
        {
            ModelAndView error = errorPage("roles_error.html", "PRIORITY_TOO_LOW", HttpStatus.UNAUTHORIZED);
            error.addObject("errorDetails", e.getMessage());
            return error;
        }

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


        if (!userHasPermission(user, UserPermission.MANAGE_ROLES))
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

        if (priority >= user.getMaxPriority())
        {
            return errorPage("newrole_error.html", "PRIORITY_TOO_HIGH", HttpStatus.UNAUTHORIZED);
        }

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
