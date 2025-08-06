package com.backend.model.waitingList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WaitingList {

    @Id
    private String id;
    private String email;
    private String name;
}
