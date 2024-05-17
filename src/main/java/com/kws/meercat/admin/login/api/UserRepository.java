package com.kws.meercat.admin.login.api;

import com.kws.meercat.admin.login.vo.User;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    public User findUserByEmail(String email) {
        User user = new User(email,"123456");
        user.setFullName("James Bond");
        return user;
    }
}
