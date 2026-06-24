package com.example.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.server.entity.User;
import com.example.server.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class UserController {

    @Autowired(required = false)
    private UserMapper userMapper;

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (userMapper == null) {
                throw new RuntimeException("UserMapper is not available");
            }
            if (user == null || isBlank(user.getUsername()) || isBlank(user.getPassword())) {
                result.put("code", 400);
                result.put("msg", "Username and password are required");
                return result;
            }

            QueryWrapper<User> query = new QueryWrapper<>();
            query.eq("username", user.getUsername());
            if (userMapper.selectCount(query) > 0) {
                result.put("code", 400);
                result.put("msg", "Username already exists");
                return result;
            }

            if (isBlank(user.getNickname())) {
                user.setNickname("User" + System.currentTimeMillis());
            }
            user.setRole("USER");
            userMapper.insert(user);

            result.put("code", 200);
            result.put("msg", "Register success");
            result.put("data", user);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "Register failed: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User loginUser) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (userMapper == null) {
                throw new RuntimeException("UserMapper is not available");
            }
            if (loginUser == null || isBlank(loginUser.getUsername()) || isBlank(loginUser.getPassword())) {
                result.put("code", 400);
                result.put("msg", "Username and password are required");
                return result;
            }

            QueryWrapper<User> query = new QueryWrapper<>();
            query.eq("username", loginUser.getUsername());
            User dbUser = userMapper.selectOne(query);

            boolean passwordMatches = demoMode || loginUser.getPassword().equals(dbUser.getPassword());
            if (dbUser == null || !passwordMatches) {
                result.put("code", 401);
                result.put("msg", "Invalid username or password");
            } else {
                result.put("code", 200);
                result.put("msg", "Login success");
                result.put("token", "user_" + dbUser.getId());
                result.put("userInfo", dbUser);
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "Login failed: " + e.getMessage());
        }
        return result;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
