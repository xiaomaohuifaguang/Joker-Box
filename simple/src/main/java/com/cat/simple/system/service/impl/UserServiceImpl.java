package com.cat.simple.system.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cat.common.entity.menu.Menu;
import com.cat.common.utils.*;
import com.cat.common.utils.base64.Base64Utils;
import com.cat.common.utils.googleauth.GoogleAuthUtils;
import com.cat.simple.config.redis.RedisService;
import com.cat.simple.config.security.SecurityUtils;
import com.cat.simple.system.mapper.OrgMapper;
import com.cat.simple.system.mapper.RoleMapper;
import com.cat.simple.system.mapper.UserExtendMapper;
import com.cat.simple.system.mapper.UserMapper;
import com.cat.simple.file.service.FileService;
import com.cat.simple.mail.service.MailService;
import com.cat.simple.system.service.MenuService;
import com.cat.simple.system.service.UserService;
import com.cat.common.entity.CONSTANTS;
import com.cat.common.entity.DTO;
import com.cat.common.entity.Page;
import com.cat.common.entity.auth.*;
import freemarker.template.TemplateException;
import jakarta.annotation.Resource;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static com.cat.common.entity.CONSTANTS.*;

/***
 * 鉴权服务业务层实现
 * @title AuthServiceImpl
 * @description <TODO description class purpose>
 * @author xiaomaohuifaguang
 * @create 2024/6/24 23:01
 **/
@Service
@Slf4j
public class UserServiceImpl implements UserService {



    @Resource
    private UserMapper userMapper;
    @Resource
    private UserExtendMapper userExtendMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private OrgMapper  orgMapper;
    @Resource
    private RedisService redisService;
    @Resource
    private MailService mailService;
    @Resource
    private FileService fileService;
    @Resource
    private MenuService menuService;

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public String getToken(LoginInfo loginInfo) {
        LoginUser loginUser = this.getLoginUser(loginInfo.getUsername());
        if(Objects.isNull(loginUser)) {
            return null;
        }
//        loginUser.setSSO(loginInfo.isSSO());
//        if(loginInfo.isSSO()) {
//            this.makeToken(loginUser);
//        }
        String token = null;
        if(CryptoUtils.verify(loginInfo.getPassword(), loginUser.getPassword())){
            token = this.makeToken(loginUser);
            List<String> tokens;
            String  tokensStr = redisService.get(REDIS_PARENT_TOKEN + loginUser.getUsername(), String.class);
            if(StringUtils.hasText(tokensStr)){
                tokens = JSONArray.parseArray(tokensStr, String.class);
            }else {
                tokens = new ArrayList<>();
            }

            if(tokens.size() == MAX_LOGIN){
                tokens.remove(0);
            }

            tokens.add(token);
            redisService.set(REDIS_PARENT_TOKEN + loginUser.getUsername(), JSONUtils.toJSONString(tokens), tokenExpire);

        }
        return  token;
    }

    @Override
    public long getTokenExpirationTimeLeftMillis(String token) {
        token = token.replace(CONSTANTS.TOKEN_TYPE + " ", "");
        return JwtUtils.getExpirationTimeLeftMillis(token);
    }

    @Override
    public LoginUser getLoginUser(String username) {
        LoginUser loginUser;
        // 数据库读取
        User user = this.getUserByUsername(username); // 获取用户通过username
        if(Objects.isNull(user)){
            return null;
        }
        List<Role> roles = roleMapper.getRolesByUserId(user.getIdStr()); // 获取角色通过userId
//        List<Org> orgs = orgMapper.getOrgsByUserId(user.getIdStr());
        List<Integer> orgIds = orgMapper.getOrgIdsByUserId(user.getIdStr());
        List<Org> orgs = new ArrayList<>();
        orgIds.forEach(id->{
            Org orgInfo = orgMapper.selectById(id);
            if(ObjectUtils.isEmpty(orgInfo)){
                if(id.equals(ORG_PARENT)){
                    orgInfo = new Org().setId(ORG_PARENT).setName(NIUBI_ORG_NAME);
                }
            }
            if(!ObjectUtils.isEmpty(orgInfo)){
                orgs.add(orgInfo);
            }
        });

        loginUser = new LoginUser(user, roles, orgs);
        return loginUser;
    }

    @Override
    public LoginUser getLoginUser(String clientName, Integer clientId) {
        User userByClient = getUserByClient(clientName, clientId);
        if(ObjectUtils.isEmpty(userByClient)){
            return null;
        }
        String username = userByClient.getUsername();
        return getLoginUser(username);
    }


    @Override
    public LoginUser getLoginUserByToken(String token) {
        token = token.replace(CONSTANTS.TOKEN_TYPE + " ", "");
        Map<String, Object> decrypt = JwtUtils.decrypt(token);
        if (ObjectUtils.isEmpty(decrypt)) return null;
        String userId = (String) decrypt.get("userId");
        String username = (String) decrypt.get("username");
        String password = (String) decrypt.get("password");

        String  tokensStr = redisService.get(REDIS_PARENT_TOKEN + username, String.class);
        if(StringUtils.hasText(tokensStr)){
            List<String> tokens = JSONArray.parseArray(tokensStr, String.class);
            if(!tokens.contains(token)) return null;
        }else {
            return null;
        }

//        boolean isSSO = (boolean) decrypt.get("isSSO");
        LoginUser loginUser = this.getLoginUser(username);

        return userId.equals(loginUser.getUserId()) && password.equals(loginUser.getPassword()) ? loginUser : null;
    }



    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public User getUserByClient(String clientName, Integer clientId) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getClientName, clientName).eq(User::getClientId, clientId));
    }

    @Override
    public List<Role> getRoleByUserId(String userId) {
        return roleMapper.getRolesByUserId(userId);
    }

    @Override
    public UserInfo getUserInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        UserInfo userInfo = new UserInfo();
        assert loginUser != null;
        BeanUtils.copyProperties(loginUser, userInfo);
        userInfo.setUserId(loginUser.getUserId());
        userInfo.getRoles().forEach(role -> {
            if (Objects.equals(role.getId(), CONSTANTS.ROLE_ADMIN_CODE)) {
                userInfo.setAdmin(true);
            }
            if(role.getAdmin().equals("1")){
                userInfo.setAdmin(true);
            }
        });
        if (!ObjectUtils.isEmpty(userInfo)) {
            UserExtend userExtend = userExtendMapper.selectById(loginUser.getUserId());
            if (userExtend != null) {
                userInfo.setSex(userExtend.getSex());
                userInfo.setMail(userExtend.getMail());
                userInfo.setPhone(userExtend.getPhone());
            }
        }

        userInfo.setAuthPaths(menuService.queryAllPathByAuth());

        return userInfo;
    }

    @Override
    public void code(String mail) throws TemplateException, MessagingException, IOException {
        mailService.sendCode(mail);
    }

    @Override
    @Transactional
    public DTO<?> register(RegisterUserInfo registerUserInfo, boolean verifyMainAndCode) {
        if (!RegexUtils.validate(registerUserInfo.getUsername(), RegexUtils.ACCOUNT_REGEX)
                || !RegexUtils.validate(registerUserInfo.getPassword(), RegexUtils.PASSWORD_REGEX)
                || (verifyMainAndCode && !RegexUtils.validate(registerUserInfo.getMail(), RegexUtils.EMAIL_REGEX))) {
            return DTO.error("格式错误");
        }

        User userByUsername = getUserByUsername(registerUserInfo.getUsername());
        if (!ObjectUtils.isEmpty(userByUsername)) {
            return DTO.error("用户名已存在");
        }

        if(verifyMainAndCode){
            String codeCache = redisService.get(CONSTANTS.REDIS_PARENT_MAIL_CODE + registerUserInfo.getMail(), String.class);
            if (!StringUtils.hasText(codeCache) || !codeCache.equals(registerUserInfo.getCode())) {
                return DTO.error("验证码不正确");
            }
        }


        User user = new User();
        BeanUtils.copyProperties(registerUserInfo, user);
        user.setPassword(CryptoUtils.encrypt(user.getPassword()));
        userMapper.insert(user);

        userByUsername = getUserByUsername(registerUserInfo.getUsername());
        UserExtend userExtend = new UserExtend().setUserId(userByUsername.getId());
        BeanUtils.copyProperties(registerUserInfo, userExtend);
        userExtend.setSex(StringUtils.hasText(userExtend.getSex()) && (userExtend.getSex().equals("男") || userExtend.getSex().equals("女")) ? userExtend.getSex() : "未知");
        userExtendMapper.insert(userExtend);
        roleMapper.defaultRole(userByUsername.getId());
        return DTO.success();
    }

    @Override
    public Page<User> queryPage(UserPageParam pageParam) {
        Page<User> page = new Page<>(pageParam);
        page = userMapper.selectPage(page, pageParam);
        page.getRecords().forEach(u -> {
            UserExtend userExtend = userExtendMapper.selectById(u.getId());
            u.setUserExtend(userExtend);
        });
        return page;
    }

    @Override
    public DTO<?> delete(String userId) {
        User user = userMapper.selectById(userId);
        if (ObjectUtils.isEmpty(user)) {
            return DTO.error("用户不存在");
        }

        List<Role> roles = roleMapper.getRolesByUserId(userId);
        List<Integer> roleIds = roles.stream().map(Role::getId).toList();
        if (roleIds.contains(CONSTANTS.ROLE_ADMIN_CODE)) {
            return DTO.error("大胆,该用户为管理员");
        }
        userMapper.deleteById(userId);
        clearUserCache();
        return DTO.success();
    }

    @Override
    public User getUserInfo(String userId) {
        User user = userMapper.selectById(userId);
        if (!ObjectUtils.isEmpty(user)) {
            UserExtend userExtend = userExtendMapper.selectById(userId);
            user.setUserExtend(ObjectUtils.isEmpty(userExtend) ? new UserExtend() : userExtend);
        }
        return user;
    }

    @Override
    @Transactional
    public DTO<?> addRole(String userId, String roleId) {

//        if(userMapper.userCountByRole(String.valueOf(CONSTANTS.ROLE_ADMIN_CODE)) > 0){
//            return DTO.error("超级管理员仅限一个用户");
//        }

        User user = userMapper.selectById(userId);
        if (ObjectUtils.isEmpty(user)) {
            return DTO.error("用户不存在");
        }

        if (!roleMapper.exists(new LambdaQueryWrapper<Role>().eq(Role::getId, roleId))) {
            return DTO.error("角色不存在");
        }

        List<Integer> list = getRoleByUserId(userId).stream().map(Role::getId).toList();
        if (list.contains(Integer.parseInt(roleId))) {
            return DTO.error("角色已绑定,无需重复绑定");
        }

        userMapper.insertUserAndRole(userId, roleId, LocalDateTime.now());
        clearUserCache();
        return DTO.success();
    }

    @Override
    public DTO<?> deleteRole(String userId, String roleId) {
        if (roleId.equals(String.valueOf(CONSTANTS.ROLE_ADMIN_CODE)) && userId.equals("1")) {
            return DTO.error("大傻春，你要干什么");
        }
        if (roleId.equals(String.valueOf(CONSTANTS.ROLE_EVERYONE_CODE))) {
            return DTO.error("默认角色不建议修改");
        }
        userMapper.removeUserAndRole(userId, roleId);
        return DTO.success();
    }

    @Override
    @Transactional
    public DTO<?> resetPassword(String userId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, userId));
        if (ObjectUtils.isEmpty(user)) {
            return DTO.error("用户不存在");
        }
        int update = userMapper.update(new LambdaUpdateWrapper<User>().set(User::getPassword, CryptoUtils.encrypt(CONSTANTS.DEFAULT_PASSWORD)).eq(User::getId, userId));
//        if (update == 1) {
//            removeUserCache(user.getUsername());
//        }
        clearUserCache();
        return DTO.success();
    }

    @Override
    public boolean avatarUpload(MultipartFile file) throws IOException {
        return fileService.uploadAvatar(file).flag;
    }

    @Override
    public void avatarUpload(String url, String userId) throws IOException {
        MultipartFile multipartFile = IOUtils.downloadUrlToMultipartFile(url);
        fileService.uploadAvatar(multipartFile, userId);
    }

    @Override
    public void avatar(String userId) throws IOException {
        fileService.downloadAvatar(userId);
    }

    @Override
    public DTO<?> changePassword(String oldPassword, String newPassword) throws IOException {

        if (!CryptoUtils.verify(oldPassword, SecurityUtils.getLoginUser().getPassword())) {
            return DTO.error("原密码错误");
        }

        if (!RegexUtils.validate(newPassword, RegexUtils.PASSWORD_REGEX)) {
            return DTO.error("新密码格式错误");
        }

        if (oldPassword.equals(newPassword)) {
            return DTO.error("新密码不能与旧密码相同");
        }

        int update = userMapper.update(new LambdaUpdateWrapper<User>().set(User::getPassword, CryptoUtils.encrypt(newPassword)).eq(User::getId, SecurityUtils.getLoginUser().getUserId()));
//        if (update == 1) {
//            removeUserCache(SecurityUtils.getLoginUser().getUsername());
//        }
        clearUserCache();
        return update == 1 ? DTO.success() : DTO.error("更新失败");
    }


    @Override
    public void clearUserCache() {
    }




    @Override
    public String makeToken(LoginUser loginUser) {
        if (loginUser.getType().equals(CONSTANTS.USER_TYPE_SERVER)) {
            return JwtUtils.encrypt(new HashMap<>() {{
                put("userId", loginUser.getUserId());
                put("username", loginUser.getUsername());
                put("password", loginUser.getPassword());
//                put("isSSO", loginUser.isSSO());
            }}, 0);
        }


        return JwtUtils.encrypt(new HashMap<>() {{
            // 随机插入 随机字符串
            for (int start = 0; start < new Random().nextInt(2); start++) {
                put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            }
            put("userId", loginUser.getUserId());
            // 随机插入 随机字符串
            for (int start = 0; start < new Random().nextInt(2); start++) {
                put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            }
            put("username", loginUser.getUsername());
            // 随机插入 随机字符串
            for (int start = 0; start < new Random().nextInt(2); start++) {
                put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            }
            // 随机插入 随机字符串
            put("password", loginUser.getPassword());
            // 随机插入 随机字符串
            for (int start = 0; start < new Random().nextInt(2); start++) {
                put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            }
            // 随机插入 随机字符串
//            put("isSSO", loginUser.isSSO());
            // 随机插入 随机字符串
            for (int start = 0; start < new Random().nextInt(2); start++) {
                put(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            }
        }}, tokenExpire);
    }

    @Override
    public String getTokenBySSO(String clientName, String id) {
        String token = redisService.get(REDIS_SSO + clientName + ":" + id, String.class);
        redisService.set(REDIS_SSO + clientName + ":" + id, "", 1);
        return token;
    }

    @Override
    public boolean exist(String username) {
        return userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public Map<String, String> generateQRCodeImage() throws Exception {
        Map<String,String> map = new HashMap<>();
        String secret = GoogleAuthUtils.generateSecret();
        map.put("secret", secret);
        String base64 = Base64Utils.encodeImageToBase64(
                GoogleAuthUtils
                        .generateQRCodeImage(secret, SecurityUtils.getLoginUser().getUsername(), applicationName)
        );
        map.put("base64", base64);
        return map;
    }

    @Override
    public DTO<?> updateUserInfo(UserInfo userInfo) {
        String userId = SecurityUtils.getLoginUser().getUserId();
        User userByUsername = userMapper.selectById(userId);
        if(ObjectUtils.isEmpty(userByUsername)) return DTO.error("账号不存在");
        UserExtend userExtend = userExtendMapper.selectById(userId);

        if(ObjectUtils.isEmpty(userExtend)){
            userExtend = new UserExtend().setUserId(Integer.valueOf(userId));
            userExtendMapper.insert(userExtend);

        }

        if(!userByUsername.getUsername().equals(userInfo.getUsername())){
            if(!RegexUtils.validate(userInfo.getUsername(), RegexUtils.ACCOUNT_REGEX)){
                return DTO.error("账号不符合规范");
            }
            if(exist(userInfo.getUsername())){
                return DTO.error("账号已存在");
            }
            userByUsername.setUsername(userInfo.getUsername());

        }

        userByUsername.setNickname(userInfo.getNickname());
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper.set("username",userByUsername.getUsername());
        userUpdateWrapper.set("nickname",userByUsername.getNickname());
        userUpdateWrapper.eq("id", userId);
        userMapper.update(userUpdateWrapper);

        userExtend.setPhone(userInfo.getPhone());
        userExtend.setSex(userInfo.getSex());
        UpdateWrapper<UserExtend> userExtendUpdateWrapper = new UpdateWrapper<>();
        userExtendUpdateWrapper.set("phone",userExtend.getPhone());
        userExtendUpdateWrapper.set("sex",userExtend.getSex());
        userExtendUpdateWrapper.eq("user_id",userId);
        userExtendMapper.update(userExtendUpdateWrapper);

        clearUserCache();

        return DTO.success();
    }

    @Override
    public List<Org> getOrgByUserId(String userId) {
        List<Integer> orgIds = orgMapper.getOrgIdsByUserId(userId);
        List<Org> orgs = new ArrayList<>();
        orgIds.forEach(id->{
            Org orgInfo = orgMapper.selectById(id);
            if(ObjectUtils.isEmpty(orgInfo)){
                if(id.equals(ORG_PARENT)){
                    orgInfo = new Org().setId(ORG_PARENT).setName(NIUBI_ORG_NAME);
                }
            }
            if(!ObjectUtils.isEmpty(orgInfo)){
                orgs.add(orgInfo);
            }
        });
        return orgs;
    }

    @Override
    public DTO<?> addOrg(String userId, String orgId) {
        User user = userMapper.selectById(userId);
        if (ObjectUtils.isEmpty(user)) {
            return DTO.error("用户不存在");
        }

        if (!orgId.equals(String.valueOf(ORG_PARENT)) && !orgMapper.exists(new LambdaQueryWrapper<Org>().eq(Org::getId, orgId))) {
            return DTO.error("机构不存在");
        }

//        List<Integer> list = getOrgByUserId(userId).stream().map(Org::getId).toList();
        List<Integer> list = orgMapper.getOrgIdsByUserId(userId);
        if (list.contains(Integer.parseInt(orgId))) {
            return DTO.error("角色已绑定,无需重复绑定");
        }

        userMapper.insertUserAndOrg(userId, orgId, LocalDateTime.now());
        clearUserCache();
        return DTO.success();
    }

    @Override
    public DTO<?> deleteOrg(String userId, String orgId) {

        userMapper.removeUserAndOrg(userId, orgId);
        return DTO.success();
    }

    @Override
    public List<User> selectorUserWithInfo(String search) {

        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getNickname)
                .like(User::getUsername, search)
                .or()
                .like(User::getNickname, search)
                .last("limit 10")
        );
    }

    @Override
    public List<User> selectorInitByIds(List<Integer> ids) {

        if(CollectionUtils.isEmpty(ids)){
            return Collections.emptyList();
        }

        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getNickname)
                .in(User::getId, ids)
        );
    }


}
