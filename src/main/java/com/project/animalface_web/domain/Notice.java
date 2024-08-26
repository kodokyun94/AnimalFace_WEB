package com.project.animalface_web.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeNo;

    private String noticeName;
    private String noticeContents;

    private LocalDate date;

    public void updateNameAndContent(String noticeName, String noticeContents) {
        this.noticeName = noticeName;
        this.noticeContents = noticeContents;
    }
}

