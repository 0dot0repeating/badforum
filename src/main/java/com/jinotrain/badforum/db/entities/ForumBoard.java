package com.jinotrain.badforum.db.entities;

import com.jinotrain.badforum.db.BoardPermission;
import com.jinotrain.badforum.db.PermissionState;

import javax.persistence.*;
import java.util.*;

@Entity
@Cacheable
@Table(name="forum_boards")
@NamedQuery(name="ForumBoard.getThreadCount",       query="SELECT COUNT(t) FROM ForumThread t WHERE t.board.id = :boardID")
@NamedQuery(name="ForumBoard.multipleThreadCount",  query="SELECT COUNT(t) FROM ForumThread t WHERE t.board.id IN :boardIDs")
@NamedQuery(name="ForumBoard.getPostCount",         query="SELECT COUNT(p) FROM ForumPost p WHERE p.thread.board.id = :boardID")
@NamedQuery(name="ForumBoard.multiplePostCount",    query="SELECT COUNT(p) FROM ForumPost p WHERE p.thread.board.id IN :boardIDs")
@NamedQuery(name="ForumBoard.threadsInUpdateOrder", query="SELECT t FROM ForumThread t WHERE t.board.id = :boardID ORDER BY t.lastUpdate DESC")
public class ForumBoard
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true)
    private long index;

    @Column(unique = true, nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval = true, mappedBy = "board")
    private Set<RoleToBoardLink> accessRoles;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, mappedBy = "board")
    private Collection<ForumThread> threads;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, mappedBy = "parentBoard")
    private Collection<ForumBoard> childBoards;

    @ManyToOne
    @JoinColumn(name = "parentboard_id")
    private ForumBoard parentBoard;


    private boolean rootBoard = false;

    private boolean anyoneCanView     = true;
    private boolean anyoneCanPost     = false;
    private boolean anyoneCanModerate = false;


    @SuppressWarnings("unused")
    ForumBoard() {}

    public ForumBoard(long index, String name)
    {
        this.index = index;
        this.name = name;
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

    public Collection<ForumThread> getThreads()    { return threads; }
    public void addThread(ForumThread thread)    { this.threads.add(thread); }
    public void removeThread(ForumThread thread) { this.threads.remove(thread); }

    public Collection<ForumBoard> getChildBoards() { return childBoards; }
    public void addChildBoard(ForumBoard board)    { this.childBoards.add(board); }
    public void removeChildBoard(ForumBoard board) { this.childBoards.remove(board); }


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
