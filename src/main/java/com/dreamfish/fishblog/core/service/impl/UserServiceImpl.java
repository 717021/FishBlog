package com.dreamfish.fishblog.core.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.dreamfish.fishblog.core.entity.User;
import com.dreamfish.fishblog.core.entity.UserExtened;
import com.dreamfish.fishblog.core.exception.BadTokenException;
import com.dreamfish.fishblog.core.mapper.UserMapper;
import com.dreamfish.fishblog.core.repository.UserRepository;
import com.dreamfish.fishblog.core.service.MailService;
import com.dreamfish.fishblog.core.service.SettingsService;
import com.dreamfish.fishblog.core.service.UserService;
import com.dreamfish.fishblog.core.utils.auth.TokenAuthUtils;
import com.dreamfish.fishblog.core.utils.encryption.AESUtils;
import com.dreamfish.fishblog.core.utils.log.ActionLog;
import com.dreamfish.fishblog.core.utils.request.ContextHolderUtils;
import com.dreamfish.fishblog.core.utils.Result;
import com.dreamfish.fishblog.core.utils.ResultCodeEnum;
import com.dreamfish.fishblog.core.utils.StringUtils;
import com.dreamfish.fishblog.core.utils.auth.PublicAuth;
import com.dreamfish.fishblog.core.utils.response.AuthCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static com.dreamfish.fishblog.core.service.AuthService.AUTH_PASSWORD_KEY;

/**
 * 用户管理与操作服务
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper = null;
    @Autowired
    private UserRepository userRepository = null;

    @Autowired
    private MailService mailService = null;
    @Autowired
    private SettingsService settingsService = null;

    private static  final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    //
    //公用方法
    //================================================
    //

    /**
     * 检查用户是否存在
     * @param id 用户id
     * @return 返回用户是否存在
     */
    @Override
    public boolean isUserExistes(int id) {
        Integer i = userMapper.isUserIdExists(id);
        return i != null && i > 0;
    }

    /**
     * 根据用户 ID 获取用户实体
     * @param id 用户 ID
     * @return 用户实体
     */
    @Override
    @Cacheable(value = "blog-user-cache", key = "'user_full_'+#p0")
    public UserExtened findUser(int id) {
        return userMapper.findFullById(id);
    }

    /**
     * 通过第三方ID找用户
     * @param type 第三方类型
     * @param id 第三方ID
     * @return 用户实体
     */
    @Override
    public UserExtened findUserByThirdId(String type, String id) {
        return userMapper.findFullByThirdId(type + "_" + id);
    }

    /**
     * 通过Email查找用户
     * @param email Email
     * @return 用户实体
     */
    @Override
    public UserExtened findUserByEmail(String email) {
        return userMapper.findFullByEmail(email);
    }

    /**
     * 获取用户友好名字
     * @param id
     * @return
     */
    @Override
    public String getUserNameAutoById(Integer id) {
        String u = userMapper.getUserFriendlyNameById(id);
        if(StringUtils.isBlank(u))
            u = userMapper.getUserNameById(id);
        return u;
    }

    /**
     * 带认证删除用户（公开需认证）
     * @param userId 用户 ID
     * @return 返回请求结果
     */
    @Override
    public Result deleteUser(int userId) {

        if (!userRepository.existsById(userId))
            return Result.failure(ResultCodeEnum.NOT_FOUNT);
        if(userId == User.LEVEL_ADMIN)
            return Result.failure(ResultCodeEnum.UNAUTHORIZED.getCode(),"无法删除根管理员");
        if(userMapper.getUserLevelById(userId) <= User.LEVEL_ADMIN)
            return Result.failure(ResultCodeEnum.UNAUTHORIZED.getCode(),"无权限删除指定用户");
        if(userId == PublicAuth.authGetUseId(ContextHolderUtils.getRequest()))
            return Result.failure(ResultCodeEnum.UNAUTHORIZED.getCode(),"无法删除自己");

        //日志
        ActionLog.logUserAction("注销用户：" + userId, ContextHolderUtils.getRequest());

        deleteUserInternal(userId);
        return Result.success();
    }

    /**
     * 添加新用户（公开需认证，无激活）
     * @param user 用户实体
     * @return 返回新添加的用户实体信息
     */
    @Override
    public Result addUser(UserExtened user) {

        String userName = user.getName();
        String password = user.getPasswd();

        //Force set value
        user.setOldLevel(User.LEVEL_WRITER);
        user.setLevel(User.LEVEL_WRITER);
        user.setUserFrom("here");
        user.setPrivilege(0);
        user.setActived(true);

        if(StringUtils.isBlank(userName) || StringUtils.isBlank(password))
            return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(),"用户名或密码不能为空");

        if (userRepository.existsByName(userName))
            return Result.failure(ResultCodeEnum.FAILED_RES_ALREADY_EXIST.getCode(),"指定用户名已存在");

        user.setPasswd(AESUtils.encrypt(password + "$" + userName, AUTH_PASSWORD_KEY));

        UserExtened newUser = addUserInternal(user);
        //日志
        ActionLog.logUserAction("新建用户：" + newUser.getId() + " (" + newUser.getFriendlyName() + ")", ContextHolderUtils.getRequest());
        return Result.success(newUser);
    }

    /**
     * 注册新用户，（公开）需激活
     * @param user 用户实体
     * @return 返回请求结果
     */
    @Override
    public Result addUserSignUp(UserExtened user) {

        if(!Boolean.parseBoolean(settingsService.getSettingCache("AllowRegister")))
            return Result.failure(ResultCodeEnum.FAILED_NOT_ALLOW.getCode(),"管理员设置禁止注册");

        String email = user.getEmail();
        String password = user.getPasswd();

        user.setName(email);
        user.setOldLevel(User.LEVEL_GUEST);
        user.setLevel(User.LEVEL_GUEST);
        user.setUserFrom("here");
        user.setActived(false);//需要激活
        user.setPrivilege(0);
        user.setGender("无");
        user.setActiveToken(AESUtils.encrypt(email + "_active", "UA12USER_ACTIVE"));

        if(StringUtils.isBlank(email) || StringUtils.isBlank(password))
            return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(),"邮箱或密码不能为空");
        if (!StringUtils.isEmail(email))
            return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(),"邮箱无效");
        if (userRepository.existsByName(email))
            return Result.failure(ResultCodeEnum.FAILED_RES_ALREADY_EXIST.getCode(),"该邮箱已被占用，请使用其他邮箱");

        user.setPasswd(AESUtils.encrypt(password + "$" + email, AUTH_PASSWORD_KEY));

        //发送激活邮件
        if(sendUserActiveMail(email, user.getActiveToken())) {
            user = addUserInternal(user);
            //日志
            ActionLog.logUserAction("注册用户：" + email, ContextHolderUtils.getRequest());
        }
        else logger.warn("Send active mail failed ! Mail : " + email);

        return Result.success(user);
    }

    /**
     * 更新用户封禁状态（公开需认证）
     * @param userId 用户 ID
     * @param ban 是否封禁
     * @return 返回请求结果
     */
    @Override
    @CacheEvict(value = "blog-user-cache", key = "'user_full_'+#p0")
    public Result userUpdateBan(int userId, boolean ban) {

        if(ban) {
            if(userId == User.LEVEL_ADMIN)
                return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"无法对根管理员操作");
            int currentUserId = PublicAuth.authGetUseId(ContextHolderUtils.getRequest());
            if(userId == currentUserId)
                return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"无法对自己进行操作");
            int targetUserLevel = userMapper.getUserLevelById(userId);
            if((targetUserLevel <= User.LEVEL_ADMIN && targetUserLevel != User.LEVEL_LOCKED)
                    && userMapper.getUserLevelById(currentUserId) > User.LEVEL_ADMIN)
                return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"无权限对指定用户操作");

            ActionLog.logUserAction("封禁用户：" + userId, ContextHolderUtils.getRequest());
            userMapper.updateUserLevel(userId, User.LEVEL_LOCKED);
        }
        else {
            if(userId == User.LEVEL_ADMIN)
                return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"无法对根管理员操作");
            int currentUserId = PublicAuth.authGetUseId(ContextHolderUtils.getRequest());
            if(userId == currentUserId)
                return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"无法对自己进行操作");
            int targetUserLevel = userMapper.getUserLevelById(userId);
            if((targetUserLevel <= User.LEVEL_ADMIN && targetUserLevel != User.LEVEL_LOCKED)
                    && userMapper.getUserLevelById(currentUserId) > User.LEVEL_ADMIN)
                return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"无权限对指定用户操作");

            ActionLog.logUserAction("解封用户：" + userId, ContextHolderUtils.getRequest());
            userMapper.updateUserLevelSetToOld(userId);
        }
        return Result.success();
    }

    /**
     * 更新用户权限（公开需认证）
     * @param userId 用户 ID
     * @param newPrivilege 用户新权限
     * @return 返回请求结果
     */
    @Override
    @CacheEvict(value = "blog-user-cache", key = "'user_full_'+#p0")
    public Result userUpdatePrivilege(int userId, int newPrivilege) {

        //认证
        HttpServletRequest request = ContextHolderUtils.getRequest();
        if(PublicAuth.authCheckIncludeLevelAndPrivileges(request, User.LEVEL_WRITER, newPrivilege) < AuthCode.SUCCESS)
            return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"当前用户无权限赋予其他用户权限");

        int oldPrivilege = userMapper.getUserPrivilegeById(userId);
        int revokePrivilege = (~newPrivilege) & oldPrivilege;

        if(PublicAuth.authCheckIncludeLevelAndPrivileges(request, User.LEVEL_WRITER, revokePrivilege) < AuthCode.SUCCESS)
            return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"当前用户无权限撤销其他用户有而自己没有的权限");

        if(userId == PublicAuth.authGetUseId(ContextHolderUtils.getRequest()))
            return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"无法对自己进行操作");
        if(userMapper.getUserLevelById(userId) <= User.LEVEL_ADMIN)
            return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"无权限对指定用户操作");

        ActionLog.logUserAction("更新用户权限：" + userId + " 新权限：" + newPrivilege, ContextHolderUtils.getRequest());

        userMapper.updateUserPrivilege(userId, newPrivilege);
        return Result.success();
    }

    @Override
    @CacheEvict(value = "blog-user-cache", key = "'user_full_'+#p0")
    public Result userUpdateLevel(int userId, int level) {
        if(level < 0 || level > User.LEVEL_MAX) return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(),"用户组参数有误");
        if(userId == User.LEVEL_ADMIN) return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"无法对根管理员操作");
        int currentUserId = PublicAuth.authGetUseId(ContextHolderUtils.getRequest());
        if(userId == currentUserId)
            return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"无法对自己进行操作");
        if(userMapper.getUserLevelById(currentUserId) > level)
            return Result.failure(ResultCodeEnum.FORIBBEN.getCode(),"无法将用户调整至权限比自己高的用户组");

        ActionLog.logUserAction("更新用户用户组：" + userId + " 新组：" + level, ContextHolderUtils.getRequest());

        userMapper.updateUserLevel(userId, level);
        userMapper.updateUserLevelOld(userId, level);
        return Result.success();
    }

    //
    //INTERNAL
    //==============================================
    //

    @Override
    @CacheEvict(value = "blog-user-cache", key = "'user_full_'+#p0")
    public void deleteUserInternal(int userId) { userRepository.deleteById(userId); }
    @Override
    @CacheEvict(value = "blog-user-cache", key = "'user_full_'+#p0.id")
    public UserExtened addUserInternal(UserExtened user) { return userRepository.saveAndFlush(user); }
    @Override
    @CacheEvict(value = "blog-user-cache", key = "'user_full_'+#p0")
    public UserExtened updateUserInternal(UserExtened user) { return userRepository.saveAndFlush(user); }

    /**
     * 更新 用户 ID
     * @param oldId 用户 ID
     * @param newId 用户新 ID
     */
    @Override
    public void updateUserId(Integer oldId, Integer newId) { userMapper.updateUserId(oldId, newId); }

    @Override
    public boolean isUserExistsByEmail(String email) {
        return userRepository.existsByName(email);
    }

    /**
     * 激活用户
     * @param token 邮箱链接里的TOKEN
     * @return 返回是否成功
     */
    @Override
    public boolean activeUser(String token) {

        String[] data;
        try {
            data = TokenAuthUtils.decodeTokenAndGetData(token, USER_ACTIVE_TOKEN_KEY, "&");
        }catch (BadTokenException e){
            return false;
        }

        if(data.length < 2) return false;

        String atk =  userMapper.getActiveTokenByUserName(data[0]);
        if(atk.equals(data[1]) || atk.equals(data[1] + "=")){
            userMapper.updateUserActiveByName(data[0], true);
            return true;
        }
        return false;
    }

    /**
     * 测试用户找回密码token是否正确
     * @param token 找回密码token
     * @return 返回结果
     */
    @Override
    public Result testUserChangePasswordToken(String token) {
        if(StringUtils.isBlank(token)) return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(), "Bad recover password token");
        String[] data;
        try{
            data = TokenAuthUtils.decodeTokenAndGetData(token, USER_ACTIVE_TOKEN_KEY, "&");
        }catch (BadTokenException e) {
            return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(), "Bad recover password token");
        }
        if(data.length < 2 || !StringUtils.isInteger(data[0])) return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(), "Bad recover password token");
        return Result.success();
    }

    /**
     * 通过找回密码token更改用户密码
     * @param passwords { token: 找回密码token, newPassword: 新密码 }
     * @return 返回结果
     */
    @Override
    public Result updateUserPasswordRecover(JSONObject passwords) {

        //检查参数
        String newPassword = passwords.getString("newPassword");
        String token = passwords.getString("token");
        if(StringUtils.isBlank(token)) return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(), "Bad recover password token");
        if(StringUtils.isBlank(newPassword)) return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(), "新密码不可为空！");

        //解密Token
        String[] data;
        try {
            data = TokenAuthUtils.decodeTokenAndGetData(token, USER_ACTIVE_TOKEN_KEY, "&");
        }catch (BadTokenException e) {
            return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(), "Bad recover password token");
        }

        if(data.length < 2 || !StringUtils.isInteger(data[0]))  return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(), "Bad recover password token");

        int uid = Integer.parseInt(data[0]);
        String atk =  userMapper.getActiveTokenById(uid);
        if(atk.equals(data[1]) || atk.equals(data[1] + "=")){
            userMapper.updateUserPassword(uid, AESUtils.encrypt(newPassword + "$" + userMapper.getUserNameById(uid), AUTH_PASSWORD_KEY));
            return Result.success();
        } else return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(), "Bad active token");
    }

    //
    //MAIL AND MESSAGES
    //================================================
    //

    @Value("${fishblog.fish-front-address}")
    private String baseSiteAddress = "";

    /**
     * 发送找回密码邮件
     * @param email 邮箱
     * @return 返回是否成功
     */
    @Override
    public boolean sendRepasswordMessage(String email) {
        if (!StringUtils.isEmail(email)) return false;
        HttpServletRequest request = ContextHolderUtils.getRequest();
        List<User> oldUsers = userMapper.findByUserName(email);
        if(oldUsers.size() == 0)
            return false;
        User oldUser = oldUsers.get(0);
        String tkn = AESUtils.encrypt(email + "_active", "UA12USER_RECPASSWD");
        userMapper.updateUserActiveToken(oldUser.getId(), tkn);

        String token = TokenAuthUtils.genToken(0, oldUser.getId() + "&" + tkn, "", USER_ACTIVE_TOKEN_KEY);
        String link = baseSiteAddress + "/user/center/rec-password/?token=" + token + "/";
        try {
            mailService.sendHtmlMail(email, "恢复在 ALONE SPACE 上的账号密码", "<html><head></head><body><h3>您好，" + email + "</h3>请点击以下链接恢复您的账号密码：<br/><a href=\"" + link + "\">恢复账号密码</a></body></html> ");
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            logger.debug("Send mail failed : " + email + " Error: " + e.toString());
        }

        return false;
    }

    /**
     * 发送激活邮件
     * @param mail 返回一个正在激活的TOKEN
     */
    private boolean sendUserActiveMail(String mail, String tkn){

        HttpServletRequest request = ContextHolderUtils.getRequest();

        String token = TokenAuthUtils.genToken(0, mail + "&" + tkn, "", USER_ACTIVE_TOKEN_KEY);
        String link = baseSiteAddress + "/user/center/active/?token=" + token;
        try {
            mailService.sendHtmlMail(mail, "激活在 ALONE SPACE 上注册的账号", "<html><head></head><body><h3>您好，" + mail + "</h3>您已成功在 ALONE SPACE 上注册成为会员，只差最后一步了！请点击以下链接激活您的账号：<br/><a href=\"" + link + "\">激活账号</a></body></html> ");
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            logger.debug("Send mail failed : " + mail + " Error: " + e.toString());
        }
        return false;
    }

    //
    //USERS MANAGEMENT
    //================================================
    //

    /**
     * 更新 用户密码
     * @param passwords 密码参数
     * @return 返回是否成功
     */
    @Override
    public Result updateUserPassword(Integer userId, JSONObject passwords) {

        //验证是否是当前用户
        HttpServletRequest request = ContextHolderUtils.getRequest();
        Integer currentUserId = PublicAuth.authGetUseId(request);
        if(currentUserId.intValue() != userId)
            return Result.failure(ResultCodeEnum.UNAUTHORIZED.getCode(), "试图执行未授权操作");

        String userName = userMapper.getUserNameById(userId);
        String oldPassword = AESUtils.encrypt(passwords.getString("oldPassword") + "$" + userName, AUTH_PASSWORD_KEY);
        String newPassword = AESUtils.encrypt(passwords.getString("newPassword") + "$" + userName, AUTH_PASSWORD_KEY);

        if(StringUtils.isBlank(oldPassword) || StringUtils.isBlank(newPassword))
            return Result.failure(ResultCodeEnum.BAD_REQUEST.getCode(), "密码参数为空");

        String oldPasswordReal = userMapper.getUserPasswordById(userId);
        if(!oldPassword.equals(oldPasswordReal))
            return Result.failure(ResultCodeEnum.FAILED_AUTH.getCode(), "旧密码错误");

        userMapper.updateUserPassword(userId, newPassword);
        return Result.success();
    }

    /**
     * 更新用户信息
     * @param user 用户信息
     * @return 返回是否成功
     */
    @Override
    @CacheEvict(value = "blog-user-cache", key = "'user_full_'+#p0.id")
    public Result updateUser(UserExtened user) {

        HttpServletRequest request = ContextHolderUtils.getRequest();
        int userID = PublicAuth.authGetUseId(request);
        if(userID < AuthCode.SUCCESS) return Result.failure(ResultCodeEnum.UNAUTHORIZED);
        if(userID != user.getId()) return Result.failure(ResultCodeEnum.FORIBBEN.getCode(), "当前用户无法修改其他用户个人信息");

        UserExtened userOld = userMapper.findFullById(user.getId());
        if(userOld == null) return Result.failure(ResultCodeEnum.NOT_FOUNT.getCode(), "未找到指定用户");

        user.setPrivilege(userOld.getPrivilege());
        user.setLevel(userOld.getLevel());
        user.setOldLevel(userOld.getOldLevel());
        user.setUserFrom(userOld.getUserFrom());
        user.setPasswd(userOld.getPasswd());
        user.setName(userOld.getName());

        return Result.success(updateUserInternal(user));
    }

    /**
     * 获取所有用户（带分页）（公开需认证）
     * @param pageIndex 页码
     * @param pageSize 页大小
     * @return 分页结果数据
     */
    @Override
    public Result getUsersWithPageable(Integer pageIndex, Integer pageSize, Boolean includeTourist) {
        if(includeTourist) return Result.success(userRepository.findAll(PageRequest.of(pageIndex, pageSize)));
        else  return Result.success(userRepository.findByLevelLessThanEqual(User.LEVEL_WRITER, PageRequest.of(pageIndex, pageSize)));
    }
}
