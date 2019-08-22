package com.jinotrain.badforum.db.entities;

import javax.persistence.*;
import java.util.Collection;

@Entity
@Cacheable
public class ForumBoard
{
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, orphanRemoval = true, mappedBy = "board")
    private Collection<RoleToBoardLink> accessRoles;

    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, mappedBy = "board")
    private Collection<ForumThread> threads;


    @OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY, mappedBy = "parentBoard")
    private Collection<ForumBoard> childBoards;

    @ManyToOne
    @JoinColumn(name = "parentboard_id")
    private ForumBoard parentBoard;


    public ForumBoard() { this(""); }

    public ForumBoard(String name)
    {
        this.name = name;
    }


    public Long getId() { return id; }
}
