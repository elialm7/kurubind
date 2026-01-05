import com.roelias.kurubind.annotation.Kurubind;


@Kurubind
public record TestUserDto(Long id, String username, String email) {
}