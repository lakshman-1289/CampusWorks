package com.campusworks.payment.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "escrows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Escrow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long payerId;

    @Column(nullable = false)
    private Long payeeId;

    @Column(nullable = false)
    private BigDecimal amount;

    private String status; // HELD, RELEASED, REFUNDED
}


