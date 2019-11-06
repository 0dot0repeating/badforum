package com.jinotrain.badforum.db.entities;

import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.UserPermission;
import com.jinotrain.badforum.db.PermissionState;

import javax.persistence.*;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.*;
import java.time.Instant;

@Entity
@Cacheable
@Table(name="forum_users")
public class ForumUser
{
    public static final int    MIN_USERNAME_LENGTH = 4;
    public static final int    MAX_USERNAME_LENGTH = 32;
    public static final String VALID_USERNAME_REGEX = "[a-zA-Z0-9_\\-]{" + MIN_USERNAME_LENGTH + "," + MAX_USERNAME_LENGTH + "}";

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    protected Long id;

    @Column(unique = true, nullable = false, length = MAX_USERNAME_LENGTH)
    @Size(min = MIN_USERNAME_LENGTH, max = MAX_USERNAME_LENGTH)
    @Pattern(regexp = VALID_USERNAME_REGEX)
    private String username;

    @Column(nullable = false)
    private String passhash;

    private String email;

    private Instant creationTime;
    private Instant lastLoginTime;

    private Boolean banned      = false;
    private Instant bannedUntil = null;
    private String  banReason   = null;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval = true, mappedBy = "user")
    private Set<UserToRoleLink> roleLinks;


    public Long   getId()       { return id; }
    public String getUsername() { return username; }

    public String getPasshash()       { return passhash; }
    public void setPasshash(String s) { passhash = s; }

    public String getEmail()       { return email; }
    public void setEmail(String e) { email = e; }

    public Instant getCreationTime() { return creationTime; }

    public Instant getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(Instant t) { lastLoginTime = t; }

    public boolean isBanned()       { return banned; }
    public Instant getBannedUntil() { return bannedUntil; }
    public String  getBanReason()   { return banReason; }


    @SuppressWarnings("unused")
    ForumUser() {}

    public ForumUser(String name, String passhash)
    {
        this(name, passhash, null);
    }

    public ForumUser(String name, String passhash, String email)
    {
        this.username      = name;
        this.passhash      = passhash;
        this.email         = email;
        this.roleLinks     = new HashSet<>();
        this.creationTime  = Instant.now();
        this.lastLoginTime = this.creationTime;
    }


    public List<ForumRole> getRoles()
    {
        List<ForumRole> roles = new ArrayList<>();

        for (UserToRoleLink link: roleLinks)
        {
            roles.add(link.getRole());
        }

        roles.sort(Comparator.comparing(ForumRole::getPriority).reversed());
        return roles;
    }


    public boolean hasRole(ForumRole role)
    {
        Long roleID = role.getId();

        for (UserToRoleLink link: roleLinks)
        {
            ForumRole linkRole = link.getRole();
            if (role == linkRole || roleID.equals(linkRole.getId())) { return true; }
        }

        return false;
    }


    public void addRole(ForumRole role)
    {
        if (!hasRole(role))
        {
            UserToRoleLink newLink = new UserToRoleLink(this, role);
            roleLinks.add(newLink);
        }
    }


    public void removeRole(ForumRole role)
    {
        Long roleID = role.getId();

        Iterator<UserToRoleLink> iter = roleLinks.iterator();

        while (iter.hasNext())
        {
            UserToRoleLink link = iter.next();
            ForumRole linkRole = link.getRole();

            if (role == linkRole || roleID.equals(linkRole.getId()))
            {
                iter.remove();
                return;
            }
        }
    }



    public boolean hasPermission(UserPermission type)
    {
        for (ForumRole role: getRoles())
        {
            PermissionState state = role.getPermission(type);

            if (state == PermissionState.OFF) { return false; }
            if (state == PermissionState.ON)  { return true; }
        }

        return false;
    }


    public boolean hasBoardPermission(ForumBoard board, BoardPermission type)
    {
        for (ForumRole role: getRoles())
        {
            PermissionState state = role.getBoardPermission(board, type);

            if (state == PermissionState.OFF) { return false; }
            if (state == PermissionState.ON)  { return true; }
        }

        return board.getGlobalPermission(type);
    }


    public void ban(Instant until, String reason)
    {
        banned      = true;
        bannedUntil = until;
        banReason   = reason;
    }


    public void unban()
    {
        banned      = false;
        bannedUntil = null;
        banReason   = null;
    }


    @PostLoad
    public void clearBanIfExpired()
    {
        if (bannedUntil != null && bannedUntil.isBefore(Instant.now()))
        {
            unban();
        }
    }


    public int getMaxPriority()
    {
        int maxPriority = Integer.MIN_VALUE;

        for (ForumRole role: getRoles())
        {
            int p = role.getPriority();
            if (p > maxPriority) { maxPriority = p; }
        }

        return maxPriority;
    }


    public static boolean userHasBoardPermission(ForumUser user, ForumBoard board, BoardPermission permission)
    {
        if (user == null) { return board.getGlobalPermission(permission); }
        return user.hasBoardPermission(board, permission);
    }


    public static boolean userHasPermission(ForumUser user, UserPermission permission)
    {
        if (user == null) { return false; }
        return user.hasPermission(permission);
    }


    public static boolean userOutranks(ForumUser user, ForumUser other)
    {
        if (other == null) { return true; }
        if (user  == null) { return false; }
        return user.getMaxPriority() > other.getMaxPriority();
    }


    public static boolean userOutranksOrIs(ForumUser user, ForumUser other)
    {
        if (other == null) { return true; }
        if (user  == null) { return false; }

        // for some reason, direct property access can be null even if calling its
        // respective getter returns the expected value. I have no fucking idea why.
        if (user.getId().equals(other.getId())) { return true; }
        return user.getMaxPriority() > other.getMaxPriority();
    }


    public static boolean sameUser(ForumUser user, ForumUser other)
    {
        if (user == null || other == null) { return false; }
        if (user == other) { return true; }

        Long id1 = user.getId();
        Long id2 = other.getId();
        if (id1 == null || id2 == null) { return false; }
        return id1.equals(id2);
    }
}
