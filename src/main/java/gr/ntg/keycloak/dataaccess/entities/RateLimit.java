package gr.ntg.keycloak.dataaccess.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "rate_limits")
public class RateLimit {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name="sec_counter")
    private int secCounter;

    @Column(name="min_counter")
    private int minCounter;
}
