package com.gomech.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tutorial_progress")
@Getter
@Setter
@NoArgsConstructor
public class TutorialProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "completed_tutorials", 
        joinColumns = @JoinColumn(name = "tutorial_progress_id")
    )
    @Column(name = "tutorial_key", nullable = false)
    private List<String> completedTutorials = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    public TutorialProgress(User user) {
        this.user = user;
        this.completedTutorials = new ArrayList<>();
    }

    public void markTutorialAsCompleted(String tutorialKey) {
        if (!completedTutorials.contains(tutorialKey)) {
            completedTutorials.add(tutorialKey);
        }
    }

    public boolean hasTutorialCompleted(String tutorialKey) {
        return completedTutorials.contains(tutorialKey);
    }
}

