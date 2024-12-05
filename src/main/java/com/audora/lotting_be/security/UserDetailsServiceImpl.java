// src/main/java/com/audora/lotting_be/security/UserDetailsServiceImpl.java
package com.audora.lotting_be.security;

import com.audora.lotting_be.model.manager.Manager;
import com.audora.lotting_be.repository.ManagerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    ManagerRepository managerRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        Manager manager = managerRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Manager Not Found with username: " + username));

        return UserDetailsImpl.build(manager);
    }
}
