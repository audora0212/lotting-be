// src/main/java/com/audora/lotting_be/model/manager/Manager.java
package com.audora.lotting_be.model.manager;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Set;

@Entity
@Data
@Table(name = "managers")
public class Manager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    // 역할을 확장할 수 있도록 Set으로 정의
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "manager_roles", joinColumns = @JoinColumn(name = "manager_id"))
    @Column(name = "role")
    private Set<String> roles;
}
