package in.atail.moneymanager.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthDTO {
    private String email;
    private String password;
    private String token;
}
