package com.gomech.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "conversations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_thread", columnNames = {"user_id", "thread_id"})
        })
@Getter
@Setter
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thread_id", nullable = false)
    private String threadId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_conversation_user"))
    private User user;

    public Conversation() {}

    public Conversation(User userId, String threadId) {
        this.user = userId;
        this.threadId = threadId;
    }
}
