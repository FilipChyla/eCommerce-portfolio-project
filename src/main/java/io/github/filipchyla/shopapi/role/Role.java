package io.github.filipchyla.shopapi.role;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    @Enumerated(EnumType.STRING)
    private RoleName name;

    public GrantedAuthority toAuthority() {
        return new SimpleGrantedAuthority("ROLE_" + name);
    }
}