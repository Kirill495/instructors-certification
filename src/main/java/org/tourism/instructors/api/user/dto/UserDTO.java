package org.tourism.instructors.api.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDTO {
    private Integer id;
    private String name;
    private String password;
    private String role;

    public boolean isNewItem() {
        return id == null;
    }
}