package ru.practicum.explorewithme.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "annotation", nullable = false, length = 2000)
    private String annotation;

    @Column(name = "description", length = 7000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Embedded
    private Location location;

    @Column(name = "paid", nullable = false)
    private Boolean paid;

    @Column(name = "participant_limit", nullable = false)
    private Integer participantLimit;

    @Column(name = "request_moderation", nullable = false)
    private Boolean requestModeration;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private EventState state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(name = "confirmed_requests", nullable = false)
    private Integer confirmedRequests;

    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    private Set<ParticipationRequest> requests;

    @ManyToMany(mappedBy = "events")
    private Set<Compilation> compilations;

    @PrePersist
    protected void onCreate() {
        if (paid == null) paid = false;
        if (participantLimit == null) participantLimit = 0;
        if (requestModeration == null) requestModeration = true;
        if (state == null) state = EventState.PENDING;
        if (confirmedRequests == null) confirmedRequests = 0;
    }
}