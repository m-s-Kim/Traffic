package com.commerce.backend.controller;

import com.commerce.backend.dto.SignUpUser;
import com.commerce.backend.entity.User;
import com.commerce.backend.jwt.JwtUtil;
import com.commerce.backend.service.JwtBlacklistService;
import com.commerce.backend.service.UserDetailService;
import com.commerce.backend.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailService userDetailService;
    private final JwtBlacklistService jwtBlacklistService;

    @Autowired
    public UserController(UserService userService, AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserDetailService userDetailService, JwtBlacklistService jwtBlacklistService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailService = userDetailService;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    /**
     * 회원가입
     *
     * @param signUpUser
     * @return user
     */
    @PostMapping("/signUp")
    public ResponseEntity<User> createUser(@RequestBody SignUpUser signUpUser) {
        User user = userService.createUser(signUpUser);
        return ResponseEntity.ok(user);
    }

    @GetMapping("")
    public ResponseEntity<List<User>> getUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }


    @PostMapping("/{userid}")
    public ResponseEntity<Void> deleteUser(@PathVariable long userid) {
        userService.deleteUser(userid);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        UserDetails userDetails = userDetailService.loadUserByUsername(username);

        String token = jwtUtil.generateToken(userDetails.getUsername());
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(3600);
        cookie.setSecure(false); // 로컬 개발 환경에서는 false로 유지

        response.addHeader("Set-Cookie", "token=" + token + "; HttpOnly; Path=/; Max-Age=3600; SameSite=None; Secure");

        response.addCookie(cookie);
        return token;
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 쿠키 삭제
        response.addCookie(cookie);
    }




    @PostMapping("/logout/all")
    public void logout(@RequestParam(required = false) String requestToken, @CookieValue(value = "token", required = false) String cookieToken, HttpServletRequest request, HttpServletResponse response) {
        String token = null;
        String bearerToken = request.getHeader("Authorization");
        if (requestToken != null) {
            token = requestToken;
        } else if (cookieToken != null) {
            token = cookieToken;
        } else if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            token = bearerToken.substring(7);
        }
        Instant instant = new Date().toInstant();
        LocalDateTime expirationTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        String username = jwtUtil.getUsernameFromToken(token);
        jwtBlacklistService.blacklistToken(token, expirationTime, username);
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 쿠키 삭제
        response.addCookie(cookie);
    }

    @PostMapping("/token/validation")
    @ResponseStatus(HttpStatus.OK)
    public void jwtValidate(@RequestParam String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token is not validation");
        }
    }




}