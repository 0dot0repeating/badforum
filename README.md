# BadForum - CSS is Overrated

It's a forum. It does all the things a forum needs to do. It's raw HTML, with the
bare minimum of JavaScript in the login and registration pages, and the logo was
made in 30 seconds in Inkscape.

This thing was made to learn how some stuff in the Java ecosystem (Spring, Hibernate,
Thymeleaf, JPA) works. This may end up being a not-bad forum at some point, but the
most likely outcome is that, well... it won't. There's already plenty of forums out
there, after all, and they work just fine.


## Getting it running

- Run `gradle copydependencies`. This will take every dependency JAR the forum needs,
  and put it into the `dependencies/` folder.
- Run `gradle assemble`. This builds the JAR file, as usual.
- Run `./run.sh`. This will run the JAR file with the necessary classpath adjustment.

If everything works, you'll see some startup logs, finished off with a line looking
like `DispatcherServlet - Completed initialization in 2654 ms`.


## Setup

By default, the forum runs on port 8081. To change this, create a file called
`badforum.properties`, put it in either the folder the JAR is in or the folder
you're running the JAR in, and put the following in the file:

```text
badforum.port = <port>
```

When you start up the forum, there will be one board - the root board - and a test
thread. There will be no users, not even an administrator, but if you look in the
server's console output, you will see something that looks like this:

```text
-------------------------------------

You currently have no administrators.
   To fix this, create a new user
    with the following password:

  bB2@qjQ@z^tP&aL(BdSROaxMb@AWc9oO

-------------------------------------
```

The message should be self-explanatory. Once you do this, you'll have an account
with the "Global administrator" role. It is *heavily* recommended to change your
password after doing so, as while this password is random, it's still in the server
logs in plaintext.

You now have full admin access to the board. Most of the board-related stuff is
easy enough to find, but as of right now, there are two tools that aren't available
through any links.

- `/settings/<username>` - modify the settings and roles of the given user, or ban/unban them
- `/roles` - create, delete, and modify roles


## Stuff I should probably write up

### Roles and permissions

A role is composed of a name, a priority between -1000 and 1000 (with two exceptions),
and a collection of forum-level permissions. Each user has a list containing the roles
that it has, and each board has a list containing the roles that it holds board-level
permissions for.

A role's permissions can be set to either on, off, or pass-through. On and off are
self-explanatory, but a permission set to pass-through defers to a user's next highest
priority role to determine whether a given permission is on or off for a given user.
If this deference continues until there's no more roles to check, the permission
defaults to "off".

Board-level permissions are determined similarly. For every role a user has, its
permissiosns for the given board are checked, defaulting to pass-through if the
board doesn't specify any permissions for it. However, boards have a default on/off
value for every given board-level permission, which is used if permission checks
end up passing through to the very last role. This default value is also used for
anonymous (aka. not logged in) users, allowing one to (for example) create a board
where you can post without logging in.

For example, if a user has three roles, and permissions are set up as so:

  Role  | Manage users |  Ban users   | Manage roles | Manage boards | Manage detached entities
:------:|:------------:|:------------:|:------------:|:-------------:|:------------------------:
 aeiou  | on           | pass-through | pass-through | pass-through  | on
 xyzzy  | pass-through | off          | pass-through | on            | off
 plugh  | pass-through | on           | pass-through | pass-through  | off
 
The user would be allowed to manage users, manage boards, and managed detached entities,
but not be allowed to ban users or manage roles.

And if board permissions are set up as so:

  Role   | View board   | Post/reply   | Moderate board 
:-------:|:------------:|:------------:|:--------------:
 aeiou   | pass-through | pass-through | pass-through
 xyzzy   | pass-through | pass-through | pass-through
 plugh   | pass-through | on           | pass-through
 Default | on           | off          | off

The user would be allowed to view and post on the board, but not moderate it.


### Default roles

There are two roles that cannot be deleted. These roles are the "Global administrator"
role, and the "All users" role.

The "global administrator" role has a priority of Integer.MAX_INT, and is flagged
internally to have every permission at all times. This cannot be revoked. Without
messing with the forum database, there can be one global administrator at most.

The "all users" role has a priority of Integer.MIN_INT, and is flagged as the default
role. Every single user is added to this role, and cannot be removed from it without
database-level editing.

These roles are the two exceptions to the "-1000 to 1000 priority" rule mentioned above,
and their priorities cannot be changed.


### Priority

As mentioned above, each role has a priority from -1000 to 1000, with the two
exceptions mentioned above. These priorities are critical to establishing a heirarchy
of permissions - without this, any user with the "Manage users" or "Manage roles"
permissions would basically have full access to the forum, and any user with the
"Manage boards" permission would basically have full access to every board.

A user's priority is the highest priority out of all the roles they have. In general,
a user cannot modify whatever another user has created unless they outrank that user.
Specifically:

- A user can only delete/move/rename the posts/threads of users they outrank.
- A user can only modify the permissions of boards created by users they outrnak.
- A user can only modify the settings of users they outrank.
- A user can only ban users they outrank.
- A user can only revoke bans made by users they outrank.
- A user can only modify, grant, and revoke permissions with priority lower than their own.

This means that, for example, moderators cannot interfere with what other moderators
of the same rank can do, and nothing can overrule the global administrator. This also
has the side effect of there only being one global administrator, as you can't grant
roles with priority equal to your own.

This does mean that the "Manage roles" on "All users" only allows users to see
permissions, not modify them.

There is one exception to the outranking rule: if both users' highest ranked role
is the "All users" role, or if both users are anonymous, they count as outranking
each other. Enabling the relevant permissions on all users is guaranteed to be chaos,
but hey, it is what you asked for.