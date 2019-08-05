package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.util.Collection;

@Entity
public class ForumBoard
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    protected long id;

    @Column(unique = true, nullable = false)
    protected String name;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, mappedBy = "board")
    protected Collection<RoleToBoardLink> accessRoles;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, mappedBy = "board")
    protected Collection<ForumThread> threads;


    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, mappedBy = "parentBoard")
    protected Collection<ForumBoard> childBoards;

    @ManyToOne
    @JoinColumn(name = "parentboard_id")
    protected ForumBoard parentBoard;


    public ForumBoard() { this(""); }

    public ForumBoard(String name)
    {
        this.name = name;
    }
}
