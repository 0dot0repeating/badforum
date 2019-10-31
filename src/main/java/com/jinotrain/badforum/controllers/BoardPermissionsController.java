package com.jinotrain.badforum.controllers;

import com.jinotrain.badforum.data.BoardRoleData;
import com.jinotrain.badforum.data.BoardPermissionStateData;
import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.PermissionState;
import com.jinotrain.badforum.db.entities.ForumBoard;
import com.jinotrain.badforum.db.entities.ForumRole;
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
import java.util.*;

@Controller
public class BoardPermissionsController extends ForumController
{
    private static final Map<String, BoardPermission> PERMISSIONS_BY_NAME;

    static
    {
        PERMISSIONS_BY_NAME = new HashMap<>();

        for (BoardPermission p: BoardPermission.values())
        {
            PERMISSIONS_BY_NAME.put(p.name().toLowerCase(), p);
        }
    }


    private List<BoardRoleData> getAllBoardPermissions(ForumBoard board)
    {
        List<BoardRoleData> ret = new ArrayList<>();
        List<ForumRole> roles = roleRepository.findAll();

        roles.sort(Comparator.comparing(ForumRole::getPriority).reversed());

        for (ForumRole role: roles)
        {
            ret.add(new BoardRoleData(role.getName(), role.isAdmin(), role.getBoardPermissions(board)));
        }

        ret.add(new BoardRoleData(null, false, board.getGlobalPermissions()));

        return ret;
    }


    private void updateBoardPermissions(ForumBoard board, List<BoardRoleData> permissions)
    {
        List<String> roleNames = new ArrayList<>();

        for (BoardRoleData perms: permissions)
        {
            if (perms.roleName != null)
            {
                roleNames.add(perms.roleName.toLowerCase());
            }
        }

        List<ForumRole> roles = em.createQuery("SELECT role FROM ForumRole role WHERE lower(role.name) IN :roleNames", ForumRole.class)
                                  .setParameter("roleNames", roleNames)
                                  .getResultList();

        Map<String, ForumRole> roleMap = new HashMap<>();
        for (ForumRole role: roles) { roleMap.put(role.getName().toLowerCase(), role); }

        for (BoardRoleData perms: permissions)
        {
            List<BoardPermissionStateData> permValues = perms.permissions;
            String roleName = perms.roleName;

            if (roleName == null)
            {
                for (BoardPermissionStateData p: permValues)
                {
                    board.setGlobalPermission(p.perm, p.state == PermissionState.ON);
                }
            }
            else
            {
                ForumRole role = roleMap.get(roleName.toLowerCase());
                if (role == null || role.isAdmin()) { continue; }

                if (allPermsAreKeep(role, board, permValues))
                {
                    role.clearBoardPermissions(board);
                }

                for (BoardPermissionStateData p: permValues)
                {
                    role.setBoardPermission(board, p.perm, p.state);
                }
            }
        }
    }


    private boolean allPermsAreKeep(ForumRole role, ForumBoard board, List<BoardPermissionStateData> newPerms)
    {
        Map<BoardPermission, PermissionState> perms = role.getBoardPermissions(board);

        for (BoardPermissionStateData p: newPerms)
        {
             perms.put(p.perm, p.state);
        }

        for (PermissionState state: perms.values())
        {
            if (state != PermissionState.KEEP)
            {
                return false;
            }
        }

        return true;
    }


    @Transactional
    @RequestMapping(value="/board/*/permissions")
    public ModelAndView viewBoardPermissions(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!userHasPermission(user, UserPermission.MANAGE_BOARDS))
        {
            return errorPage("boardpermissions_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        String requestURL = request.getServletPath();
        int boardNumStart = "/board/".length();
        int boardNumEnd   = requestURL.length() - "/permissions".length();

        ForumBoard board;

        try
        {
            long boardNumber = Long.valueOf(requestURL.substring(boardNumStart, boardNumEnd));
            board = boardRepository.findByIndex(boardNumber);
        }
        catch (NumberFormatException e)
        {
            return errorPage("boardpermissions_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        List<BoardRoleData> permissionViewData = getAllBoardPermissions(board);

        ModelAndView ret = new ModelAndView("boardpermissions.html");
        ret.addObject("boardIndex", board.getIndex());
        ret.addObject("boardName",  board.getName());
        ret.addObject("permissionData", permissionViewData);
        return ret;
    }


    // request parameters come in the form:
    //  key:   [permission enum name]:[role name] (eg. POST:All Users)
    //  value: -1 for off, 0 for keep, 1 for on
    //
    // for global permissions, the key is just the permission enum name
    //
    // keys are case insensitive

    private List<BoardRoleData> buildPermissionsFromParams(Map<String, String[]> requestParams)
    {
        Map<String, BoardRoleData> permissions = new HashMap<>();

        for (Map.Entry<String, String[]> params: requestParams.entrySet())
        {
            String key = params.getKey();
            String val = params.getValue()[0];

            int splitPoint = key.indexOf(':');

            BoardPermission permission;
            BoardRoleData permData;

            // possibly global permission
            if (splitPoint == -1)
            {
                permission = PERMISSIONS_BY_NAME.get(key.toLowerCase());
                if (permission == null) { continue; }

                permData = permissions.computeIfAbsent(null, BoardRoleData::new);
            }
            else
            {
                String permName = key.substring(0, splitPoint).toLowerCase();
                String roleName = key.substring(splitPoint+1).toLowerCase();

                permission = PERMISSIONS_BY_NAME.get(permName);
                if (permission == null) { continue; }

                permData = permissions.computeIfAbsent(roleName, BoardRoleData::new);
            }

            PermissionState state;

            switch (val)
            {
                case "-1": state = PermissionState.OFF;     break;
                case "1":  state = PermissionState.ON;      break;
                default:   state = PermissionState.KEEP;    break;
            }

            permData.permissions.add(new BoardPermissionStateData(permission, state));
        }

        return new ArrayList<>(permissions.values());
    }


    @Transactional
    @RequestMapping(value="/board/savepermissions", method=RequestMethod.POST)
    public ModelAndView saveBoardPermissions(HttpServletRequest request, HttpServletResponse response)
    {
        ForumUser user;
        try { user = getUserFromRequest(request); }
        catch (UserBannedException e) { return bannedPage(e); }

        if (!userHasPermission(user, UserPermission.MANAGE_BOARDS))
        {
            return errorPage("boardpermissions_error.html", "NOT_ALLOWED", HttpStatus.UNAUTHORIZED);
        }

        ForumBoard board;

        try
        {
            long boardNumber = Long.valueOf(request.getParameter("boardIndex"));
            board = boardRepository.findByIndex(boardNumber);
        }
        catch (NumberFormatException e)
        {
            return errorPage("boardpermissions_error.html", "NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        updateBoardPermissions(board, buildPermissionsFromParams(request.getParameterMap()));

        ModelAndView ret = new ModelAndView("boardpermissions_saved.html");
        ret.addObject("boardIndex", board.getIndex());
        ret.addObject("boardName", board.getName());
        return ret;
    }
}