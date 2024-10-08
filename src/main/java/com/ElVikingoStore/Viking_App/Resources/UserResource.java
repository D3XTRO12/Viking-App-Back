package com.ElVikingoStore.Viking_App.Resources;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import com.ElVikingoStore.Viking_App.Services.UserRoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.ElVikingoStore.Viking_App.Models.User;
import com.ElVikingoStore.Viking_App.DTOs.UserDto;
import com.ElVikingoStore.Viking_App.Services.UserService;

@RestController
@RequestMapping("/api/user")
public class UserResource {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRoleService userRoleService;  // Servicio para manejar roles de usuario

    // Búsqueda de usuarios según tipo o parámetros como DNI, CUIT, o ID
    @GetMapping("/search")
    public ResponseEntity<Object> searchUser(@RequestParam(required = false) UUID id,
                                             @RequestParam(required = false) Integer dni,
                                             @RequestParam(required = false) String cuit,
                                             @RequestParam(required = false) UUID roleId,
                                             @RequestParam(required = false) String email,
                                             @RequestParam(required = false) String query) {
        // JWT debería verificar automáticamente el acceso aquí
        try {
            if (query == null) {
                return ResponseEntity.badRequest().body("Query parameter is required");
            }

            switch (query.toLowerCase()) {
                case "all" -> {
                    List<User> users = userService.getAllUsers();
                    return ResponseEntity.ok(users);
                }
                case "by-id" -> {
                    if (id == null) {
                        return ResponseEntity.badRequest().body("ID is required for 'by-id' query");
                    }
                    Optional<User> userById = userService.getUserById(id);
                    return ResponseEntity.ok(userById);
                }
                case "by-dni" -> {
                    if (dni == null) {
                        return ResponseEntity.badRequest().body("DNI is required for 'by-dni' query");
                    }
                    User userByDni = userService.getUserByDni(dni);
                    return ResponseEntity.ok(userByDni);
                }
                case "by-email" ->{
                    if (email == null) {
                        return ResponseEntity.badRequest().body("Email is required for 'by-email' query");
                    }
                    User userByEmail = userService.getUserByEmail(email);
                    return ResponseEntity.ok(userByEmail);
                }
                case "by-cuit" -> {
                    if (cuit == null) {
                        return ResponseEntity.badRequest().body("CUIT is required for 'by-cuit' query");
                    }
                    User userByCuit = userService.getUserByCuit(cuit);
                    return ResponseEntity.ok(userByCuit);
                }
                case "by-role" -> {
                    if (roleId == null) {
                        return ResponseEntity.badRequest().body("Role ID is required for 'by-roleId' query");
                    }
                    System.out.println("Searching users by roleId: " + roleId); // Agrega esta línea
                    List<User> users = userService.getUsersByRoleId(roleId);
                    return ResponseEntity.ok(users);
                }

                default -> {
                    return ResponseEntity.badRequest().body("Invalid query parameter");
                }
            }
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request: " + e.getMessage());
        }
    }

    // Guardar una nueva instancia de usuario (cliente o staff)
    @PostMapping("/save")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserDto userDto) {
        String response = userService.saveUserInstance(userDto);
        return ResponseEntity.ok(response);
    }

    // Actualizar los detalles de un usuario
    @PutMapping("/update/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable UUID id, @Valid @RequestBody UserDto userDto) {
        try {
            if (!id.equals(userDto.getId())) {
                return ResponseEntity.badRequest().build();
            }

            UserDto updateUser = userService.updateUser(userDto);
            if (updateUser != null) {
                return ResponseEntity.ok(updateUser); // Respuesta 200 OK con el objeto actualizado
            } else {
                return ResponseEntity.notFound().build(); // Retorna 404 si no se encuentra
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build(); // Retorna 500 en caso de error interno
        }
    }


    // Eliminar un usuario por ID
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        try {
            Optional<User> existingUser = userService.getUserById(id);
            if (existingUser == null) {
                return ResponseEntity.notFound().build();
            }

            boolean deleted = userService.deleteUser(existingUser.get().getId());

            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting user");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting user: " + e.getMessage());
        }
    }

    // Método para verificar el permiso del usuario autenticado
    private boolean hasPermission(String requiredPermission) {
        // Obtener el usuario autenticado
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // Obtener el rol y permiso del usuario desde la base de datos
        String userPermission = userRoleService.getUserPermission(username);

        // Verificar si el usuario tiene el permiso requerido
        return userPermission != null && userPermission.equals(requiredPermission);
    }
}
