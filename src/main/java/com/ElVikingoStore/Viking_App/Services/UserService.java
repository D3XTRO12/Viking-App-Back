package com.ElVikingoStore.Viking_App.Services;

import java.util.*;
import java.util.stream.Collectors;

import com.ElVikingoStore.Viking_App.DTOs.UserDto;
import com.ElVikingoStore.Viking_App.Models.Role;
import com.ElVikingoStore.Viking_App.Models.UserRole;
import com.ElVikingoStore.Viking_App.Repositories.RoleRepo;
import com.ElVikingoStore.Viking_App.Repositories.RoleRepo;
import com.ElVikingoStore.Viking_App.Repositories.UserRoleRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ElVikingoStore.Viking_App.Models.User;
import com.ElVikingoStore.Viking_App.Repositories.UserRepo;

import jakarta.transaction.Transactional;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private UserRoleRepo userRoleRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    public void CustomUsersDetailsService (UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    // Obtener todos los usuarios
    public ArrayList<User> getAllUsers(){
        return (ArrayList<User>) userRepo.findAll();
    }

    // Obtener un usuario por ID
    public Optional<User> getUserById(UUID id) {
        return userRepo.findById(id);
    }
    public List<User> getUsersByRoleId(UUID rolId) {
        List<UserRole> userRoles = userRoleRepo.findByRole_Id(rolId);

        return userRoles.stream()
                .map(userRole -> userRepo.findById(userRole.getUser().getId())
                        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userRole.getUser().getId())))
                .collect(Collectors.toList());
    }

    // Obtener un usuario por DNI
    public User getUserByDni(Integer dni) {
        return userRepo.findByDni(dni)
                .orElseThrow(() -> new NoSuchElementException("User not found with DNI: " + dni));
    }
    public User getUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found with email: " + email));
    }
    // Obtener un usuario por CUIT (para empresas)
    public User getUserByCuit(String cuit) {
        return userRepo.findByCuit(cuit)
                .orElseThrow(() -> new NoSuchElementException("User not found with CUIT: " + cuit));
    }

    @Transactional
    public String saveUserInstance(UserDto userDto) {
        Role role = validateRole(userDto.getRoleId());

        User user = new User();
        user.setName(userDto.getName());
        user.setDni(userDto.getDni());
        user.setUserType(userDto.getUserType());
        user.setAddress(userDto.getAddress());
        user.setPhoneNumber(userDto.getPhoneNumber());
        user.setSecondaryPhoneNumber(userDto.getSecondaryPhoneNumber());
        user.setEmail(userDto.getEmail());
        user.setPassword(encodePassword(userDto.getPassword()));

        // Guardar el usuario
        userRepo.save(user);

        // Crear y guardar el UserRole
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role); // Establecer el rol recuperado
        userRoleRepo.save(userRole);
        return "User created successfully";
    }

    @Transactional
    public UserDto updateUser(UserDto userDto) {
        User existingUser = userRepo.findById(userDto.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Actualizar los campos del usuario
        existingUser.setName(userDto.getName());
        existingUser.setDni(userDto.getDni());
        existingUser.setUserType(userDto.getUserType());
        existingUser.setAddress(userDto.getAddress());
        existingUser.setPhoneNumber(userDto.getPhoneNumber());
        existingUser.setSecondaryPhoneNumber(userDto.getSecondaryPhoneNumber());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setCuit(userDto.getCuit());

        // Actualizar la contraseña si se proporciona una nueva
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            existingUser.setPassword(encodePassword(userDto.getPassword()));
        }

        // Guardar los cambios del usuario
        userRepo.save(existingUser);

        // Actualizar el rol si se proporciona un nuevo roleId
        if (userDto.getRoleId() != null) {
            Role newRole = validateRole(userDto.getRoleId());

            // Buscar el UserRole existente
            Optional<UserRole> existingUserRoleOpt = userRoleRepo.findRoleIdByUserId(existingUser.getId());

            if (existingUserRoleOpt.isPresent()) {
                UserRole existingUserRole = existingUserRoleOpt.get();
                existingUserRole.setRole(newRole);
                userRoleRepo.save(existingUserRole);
            } else {
                // Si no existe un UserRole, creamos uno nuevo
                UserRole newUserRole = new UserRole();
                newUserRole.setUser(existingUser);
                newUserRole.setRole(newRole);
                userRoleRepo.save(newUserRole);
            }
        }

        return convertToDto(existingUser);
    }

    // Metodo auxiliar para codificar la contraseña (si es necesario)
    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }


    // Eliminar un usuario por ID
    public boolean deleteUser(UUID id) {
        try {
            Optional<User> optionalUser = userRepo.findById(id);
            if (optionalUser.isEmpty()) {
                return false; // Usuario no encontrado
            }

            userRepo.deleteById(id);
            return true; // Usuario eliminado exitosamente
        } catch (Exception e) {
            return false; // Error al eliminar
        }
    }
    public boolean hasRoleId(String email, UUID roleId) {
        Optional<User> userOptional = userRepo.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Buscar si el usuario tiene el rol especificado
            Optional<UserRole> userRole = userRoleRepo.findRoleIdByUserId(user.getId());
            return userRole.isPresent() && userRole.get().getRoleId().equals(roleId);
        }
        return false;
    }
    // Método para validar el rol
    private Role validateRole(UUID roleId) {
        return roleRepo.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado con ID: " + roleId));
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setDni(user.getDni());
        dto.setUserType(user.getUserType());
        dto.setAddress(user.getAddress());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setSecondaryPhoneNumber(user.getSecondaryPhoneNumber());
        dto.setEmail(user.getEmail());
        dto.setCuit(user.getCuit());

        // Obtener el roleId del UserRole asociado
        Optional<UserRole> userRoleOpt = userRoleRepo.findRoleIdByUserId(user.getId());
        userRoleOpt.ifPresent(userRole -> dto.setRoleId(userRole.getRole().getId()));

        return dto;
    }
}
