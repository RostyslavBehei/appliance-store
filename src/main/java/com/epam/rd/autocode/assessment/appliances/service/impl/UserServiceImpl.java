package com.epam.rd.autocode.assessment.appliances.service.impl;

import com.epam.rd.autocode.assessment.appliances.dto.client.ClientProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.employee.EmployeeProfileResponse;
import com.epam.rd.autocode.assessment.appliances.dto.user.*;
import com.epam.rd.autocode.assessment.appliances.exception.AlreadyExistsException;
import com.epam.rd.autocode.assessment.appliances.exception.NotFoundException;
import com.epam.rd.autocode.assessment.appliances.model.Client;
import com.epam.rd.autocode.assessment.appliances.model.Employee;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.model.enums.Role;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserByEmail(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User with email " + userEmail + " not found"));

        if (user instanceof Client) {
            return ClientProfileResponse.fromEntity((Client) user);
        } else if (user instanceof Employee) {
            return EmployeeProfileResponse.fromEntity((Employee) user);
        }

        throw new IllegalStateException("Unknow user type");
    }

    @Override
    @Transactional
    public void updateUser(String userEmail, UserProfileUpdateRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User with email " + userEmail + " not found"));

        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.middleName() != null) user.setMiddleName(request.middleName());
        if (request.birthday() != null) user.setBirthday(request.birthday());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (isAdmin) {
            if (request.role() != null) user.setRole(request.role());
            if (user instanceof Client client) {
                if (request.card() != null) client.setCart(request.card());
            }
            if (user instanceof Employee employee) {
                if (request.department() != null) employee.setDepartment(request.department());
            }
            if (request.enable() != null) user.setEnabled(request.enable());
        }

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {

    }

    @Override
    @Transactional
    public void updatePassword(String currentEmail, UserPasswordChangeRequest request) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new NotFoundException("User with email " + currentEmail + " not found"));

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords don't match");
        }

        if (!passwordEncoder.matches(request.oldPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("Old password doesn't match");
        }

        if (passwordEncoder.matches(request.newPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException("New password cannot by same");
        }

        currentUser.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(currentUser);
    }

    @Override
    @Transactional
    public void resetPassword(String token, UserPasswordResetRequest request) {

    }

    @Override
    @Transactional
    public void updateEmail(String currentEmail, UserEmailChangeRequest request) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new NotFoundException("User with email " + currentEmail + " not found"));

        if (!passwordEncoder.matches(request.password(), currentUser.getPassword())) {
            throw new IllegalArgumentException("Incorrect password");
        }

        if (userRepository.findByEmail(request.newEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email " + currentEmail + " already exists");
        }

        currentUser.setEmail(request.newEmail());
        userRepository.save(currentUser);
    }

    @Override
    @Transactional
    public void deleteUserById(Long id) {
        if (userRepository.findById(id).isPresent()) {
            userRepository.deleteById(id);
        } else {
            throw new NotFoundException("User with id " + id + " not found");
        }
    }

    @Override
    @Transactional
    public void deleteUserByEmail(String userEmail) {
        if (userRepository.findByEmail(userEmail).isPresent()) {
            userRepository.deleteByEmail(userEmail);
        } else {
            throw new NotFoundException("User with email " + userEmail + " not found");
        }
    }
}
