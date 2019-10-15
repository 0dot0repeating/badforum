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

    private Boolean enabled;

    private Instant creationTime;
    private Instant lastLoginTime;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval = true, mappedBy = "user")
    private Collection<UserToRoleLink> roleLinks;


    public String getUsername() { return username; }

    public String getPasshash()       { return passhash; }
    public void setPasshash(String s) { passhash = s; }

    public String getEmail()       { return email; }
    public void setEmail(String e) { email = e; }

    public Boolean getEnabled()       { return enabled; }
    public void setEnabled(Boolean e) { enabled = e; }

    public Instant getCreationTime() { return creationTime; }

    public Instant getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(Instant d) { lastLoginTime = d; }


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
        this.enabled       = true;
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
            PermissionState state = role.hasPermission(type);

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
}
