package com.campusworks.admin.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "disputes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long taskId;
    private Long complainantId;
    private Long defendantId;
    private String description;
    private String status; // OPEN, RESOLVED, REJECTED
}


