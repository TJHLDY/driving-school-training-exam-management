package com.example.drivingschool.security;

import com.example.drivingschool.entity.UserAccount;
import com.example.drivingschool.mapper.AccountMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AccountMapper accountMapper;

    public DatabaseUserDetailsService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = accountMapper.findByUsername(username);
        if (account == null) {
            throw new UsernameNotFoundException("账号不存在");
        }
        Set<String> roles = new LinkedHashSet<>(accountMapper.findRoleCodesByUserId(account.getUserId()));
        return new LoginUser(account.getUserId(), account.getUsername(), account.getPasswordHash(),
                roles, Boolean.TRUE.equals(account.getEnabled()));
    }
}
