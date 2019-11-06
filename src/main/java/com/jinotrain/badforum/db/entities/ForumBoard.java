package com.jinotrain.badforum.db.entities;

import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.PermissionState;

import javax.persistence.*;
import java.util.*;

@Entity
@Cacheable
@Table(name="forum_boards")
@NamedQuery(name="ForumBoard.getThreadCount",       query="SELECT COUNT(t) FROM ForumThread t WHERE t.board.id = :boardID AND t.moved = false")
@NamedQuery(name="ForumBoard.multipleThreadCount",  query="SELECT COUNT(t) FROM ForumThread t WHERE t.board.id IN :boardIDs AND t.moved = false")
@NamedQuery(name="ForumBoard.getPostCount",         query="SELECT COUNT(p) FROM ForumPost p WHERE p.thread.board.id = :boardID AND p.split = false AND p.deleted = false")
@NamedQuery(name="ForumBoard.multiplePostCount",    query="SELECT COUNT(p) FROM ForumPost p WHERE p.thread.board.id IN :boardIDs AND p.split = false AND p.deleted = false")
public class ForumBoard
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true)
    private long index;

    @Column(unique = true, nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.PERSIST, fetch=FetchType.LAZY, orphanRemoval = true, mappedBy = "board")
    private Set<RoleToBoardLink> accessRoles;

    @OneToMany(cascade = CascadeType.PERSIST, fetch=FetchType.LAZY, mappedBy = "board")
    private Set<ForumThread> threads;

    @OneToMany(cascade = CascadeType.PERSIST, fetch=FetchType.LAZY, mappedBy = "parentBoard")
    private Set<ForumBoard> childBoards;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentboard_id")
    private ForumBoard parentBoard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private ForumUser creator;


    private boolean rootBoard = false;

    private boolean anyoneCanView     = true;
    private boolean anyoneCanPost     = false;
    private boolean anyoneCanModerate = false;


    @SuppressWarnings("unused")
    ForumBoard() {}

    public ForumBoard(long index, String name)
    {
        this(index, name, null);
    }

    public ForumBoard(long index, String name, ForumUser creator)
    {
        this.index   = index;
        this.name    = name;
        this.creator = creator;

        this.accessRoles = new HashSet<>();
        this.childBoards = new HashSet<>();
        this.threads     = new HashSet<>();
    }


    public Long getId()     { return id; }
    public long getIndex()  { return index; }

    public String getName()            { return name; }
    public void   setName(String name) { this.name = name; }

    public boolean isRootBoard() { return rootBoard; }
    public void    setRootBoard(boolean rootBoard) { this.rootBoard = rootBoard; }

    public ForumBoard getParentBoard()                       { return parentBoard; }
    public void       setParentBoard(ForumBoard parentBoard) { this.parentBoard = parentBoard; }

    public Collection<ForumThread> getThreads()    { return threads; }
    public void addThread(ForumThread thread)      { this.threads.add(thread); }
    public void removeThread(ForumThread thread)   { this.threads.remove(thread); }

    public Collection<ForumBoard> getChildBoards() { return childBoards; }
    public void addChildBoard(ForumBoard board)    { this.childBoards.add(board); }
    public void removeChildBoard(ForumBoard board) { this.childBoards.remove(board); }

    public ForumUser getCreator() { return creator; }


    public boolean getGlobalPermission(BoardPermission type)
    {
        switch (type)
        {
            case VIEW:     return anyoneCanView;
            case POST:     return anyoneCanPost;
            case MODERATE: return anyoneCanModerate;
            default: throw new UnsupportedOperationException("Board-level permission " + type.name() + " not implemented in ForumBoard");
        }
    }


    public void setGlobalPermission(BoardPermission type, boolean state)
    {
        switch (type)
        {
            case VIEW:     anyoneCanView     = state; break;
            case POST:     anyoneCanPost     = state; break;
            case MODERATE: anyoneCanModerate = state; break;
            default: throw new UnsupportedOperationException("Board-level permission " + type.name() + " not implemented in ForumBoard");
        }
    }


    public Map<BoardPermission, PermissionState> getGlobalPermissions()
    {
        Map<BoardPermission, PermissionState> ret = new HashMap<>();

        for (BoardPermission p: BoardPermission.values())
        {
            ret.put(p, getGlobalPermission(p) ? PermissionState.ON : PermissionState.OFF);
        }

        return ret;
    }
}
