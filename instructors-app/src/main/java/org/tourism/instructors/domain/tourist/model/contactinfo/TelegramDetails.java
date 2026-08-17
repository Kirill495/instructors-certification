package org.tourism.instructors.domain.tourist.model.contactinfo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TelegramDetails implements ContactInfoDetails {
    private long id;
    private String nickName;
}
