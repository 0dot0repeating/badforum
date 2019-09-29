package com.jinotrain.badforum.db.entities;

import com.jinotrain.badforum.db.BoardPermission;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Cacheable
@Table(name="forum_boards")
@SequenceGenerator(name="SEQ_BOARDS")
@NamedQuery(name="ForumBoard.getThreadCount",       query="SELECT COUNT(t) FROM ForumThread t WHERE t.board.id = :boardID")
@NamedQuery(name="ForumBoard.multipleThreadCount",  query="SELECT COUNT(t) FROM ForumThread t WHERE t.board.id IN :boardIDs")
@NamedQuery(name="ForumBoard.getPostCount",         query="SELECT COUNT(p) FROM ForumPost p WHERE p.thread.board.id = :boardID")
@NamedQuery(name="ForumBoard.multiplePostCount",    query="SELECT COUNT(p) FROM ForumPost p WHERE p.thread.board.id IN :boardIDs")
@NamedQuery(name="ForumBoard.threadsInUpdateOrder", query="SELECT t FROM ForumThread t WHERE t.board.id = :boardID ORDER BY t.lastUpdate DESC")
public class ForumBoard
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="SEQ_BOARDS")
    private Long id;

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

    private boolean anyoneCanView = true;
    private boolean anyoneCanPost = false;


    @SuppressWarnings("unused")
    ForumBoard() {}

    public ForumBoard(String name)
    {
        this.name = name;
        this.accessRoles = new HashSet<>();
        this.childBoards = new HashSet<>();
        this.threads     = new HashSet<>();
    }


    public Long getId() { return id; }

    public String getName()            { return name; }
    public void   setName(String name) { this.name = name; }

    public boolean isRootBoard() { return rootBoard; }
    public void    setRootBoard(boolean rootBoard) { this.rootBoard = rootBoard; }

    public Collection<ForumThread> getThreads()    { return threads; }
    public Collection<ForumBoard> getChildBoards() { return childBoards; }


    public boolean getGlobalPermission(BoardPermission type)
    {
        switch (type)
        {
            case VIEW: return anyoneCanView;
            case POST: return anyoneCanPost;
            default: throw new UnsupportedOperationException("Board-level permission " + type.name() + " not implemented in ForumBoard");
        }
    }


    public void setGlobalPermission(BoardPermission type, boolean state)
    {
        switch (type)
        {
            case VIEW: anyoneCanView = state; break;
            case POST: anyoneCanPost = state; break;
            default: throw new UnsupportedOperationException("Board-level permission " + type.name() + " not implemented in ForumBoard");
        }
    }

}
