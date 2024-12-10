package io.event.ems.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "status_codes", uniqueConstraints = @UniqueConstraint(columnNames = {"entity_type", "status"}))
@NoArgsConstructor
@AllArgsConstructor
public class StatusCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private String status;

    private String description;

}
