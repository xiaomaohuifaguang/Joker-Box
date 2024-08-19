import com.cat.auth.AuthServerApplication;
import com.cat.auth.mapper.UserMapper;
import com.cat.common.entity.auth.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/***
 * <TODO description class purpose>
 * @title UserTest
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/23 23:10
 **/
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AuthServerApplication.class)
@AutoConfigureWebMvc
public class UserTest {

    @Resource
    private UserMapper userMapper;

    @Test
    public void testSelectById() {
        User user = userMapper.selectById("0000000001");
        System.out.println(user);
    }


}
