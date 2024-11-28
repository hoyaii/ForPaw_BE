package com.hong.forapw.domain.authentication;

import com.hong.forapw.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "visit_tb")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private LocalDateTime date;

    @Builder
    public Visit(User user, LocalDateTime date) {
        this.user = user;
        this.date = date;
    }
}
