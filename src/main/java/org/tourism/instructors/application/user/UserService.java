package org.tourism.instructors.application.user;

import java.util.List;
import org.tourism.instructors.api.user.dto.UserDTO;
import org.tourism.instructors.domain.user.User;

public interface UserService {
    List<User> findAll();

    UserDTO findById(int id);

    void createUser(UserDTO dto);

    void updateUser(int id, UserDTO dto);

    void deleteUser(int id);

    void changePassword(String username, String newPassword);
}
