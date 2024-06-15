package com.linyu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linyu.common.ErrorCode;
import com.linyu.exception.BusinessException;
import com.linyu.model.User;
import com.linyu.model.request.UserCreateRequest;
import com.linyu.model.request.UserUpdateRequest;
import com.linyu.service.UserService;
import com.linyu.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.linyu.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
* @author Linyu

*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    private UserMapper userMapper;

    /**
     *  盐值，用于混淆密码
     */
    private static final String SALT = "linyu";


    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String  planetCode) {
        if(StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if(userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if(planetCode.length() > 5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
        }

        //账户不能包含特殊字符
        //使用正则表达式进行检验
        String validPattern =  ".*[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*()——+|{}【】‘；：”“’。，、？\\\\]+.*";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if(matcher.find()){ //如果包含特殊字符，返回-1
//            return -1;
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户包含特殊字符");
        }


        //密码和校验密码相同
        if(!userPassword.equals(checkPassword)){
//            return -1;
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码和校验密码不相同");
        }


        //账户不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if(count > 0){
//            return -1;
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户已经存在");
        }

        //星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if(count > 0){
//            return -1;
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号已存在");
        }

        //加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode); //设置星球编号
        boolean saveResult = this.save(user);
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        if(StringUtils.isAnyBlank(userAccount, userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if(userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }

        //账户不能包含特殊字符
        //使用正则表达式进行检验
        String validPattern =  ".*[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*()——+|{}【】‘；：”“’。，、？\\\\]+.*";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if(matcher.find()){ //如果包含特殊字符，返回-1
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户包含特殊字符");
        }

        //加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        //查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if(user == null){
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "登录账号或密码有误");
        }

        //用户脱敏
        User safetyUser = getSafetyUser(user);

        //记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return safetyUser;
    }

    /**
     * 用户脱敏
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        return safetyUser;
    }

    /**
     * 用户注销
     */
    @Override
    public int userLogout(HttpServletRequest request) {
        //移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 管理员创建新用户
     * @param createRequest
     * @return
     */
    @Override
    public boolean createUser(UserCreateRequest createRequest) {
        if(StringUtils.isAnyBlank(createRequest.getUserAccount(), createRequest.getUserPassword(),
                createRequest.getCheckPassword(), createRequest.getPlanetCode())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if(checkUserAccountExist(createRequest.getUserAccount())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号已经存在");
        }
        if(checkPlanetCodeExist(createRequest.getPlanetCode())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号已经存在");
        }
        //创建新用户
        User user = new User();
        user.setUsername(createRequest.getUsername());
        user.setUserAccount(createRequest.getUserAccount());
        user.setAvatarUrl(createRequest.getAvatarUrl());
        user.setGender(createRequest.getGender());

        user.setPhone(createRequest.getPhone());
        user.setEmail(createRequest.getEmail());
        user.setUserRole(createRequest.getUserRole());
        user.setPlanetCode(createRequest.getPlanetCode());

        //加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + createRequest.getUserPassword()).getBytes());
        user.setUserPassword(encryptPassword);
        //插入新用户数据
        boolean saveResult = this.save(user);
        return saveResult;
    }

    /**
     * 管理员更新用户数据
     * @param updateRequest
     * @return
     */
    @Override
    public boolean updateUser(UserUpdateRequest updateRequest) {
        if(updateRequest.getId() == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID不能为空");
        }
        //账号校验
        String userAccount = updateRequest.getUserAccount();
        if(StringUtils.isNotBlank(userAccount)){
            //长度校验
            if(userAccount.length() < 4)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
            //特殊字符校验
            String validPattern =  ".*[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*()——+|{}【】‘；：”“’。，、？\\\\]+.*";
            Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
            if(matcher.find()){ //如果包含特殊字符，返回-1
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户包含特殊字符");
            }
            //唯一性校验
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            User existUser = userMapper.selectOne(queryWrapper);
            if(existUser != null && !existUser.getId().equals(updateRequest.getId())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户已经存在");
            }
        }

        //密码校验和加密
        String userPassword = updateRequest.getUserPassword();
        String encryptPassword = null;
        if(StringUtils.isNotBlank(userPassword)){
            if(userPassword.length() < 8){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
            }
            //加密
            encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        }

        //星球编号校验
        String planetCode = updateRequest.getPlanetCode();
        if(StringUtils.isNotBlank(planetCode)){
            if(planetCode.length() > 5){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
            }
            //唯一性校验
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("planetCode", planetCode);
            User existUser = userMapper.selectOne(queryWrapper);
            if(existUser != null && !existUser.getId().equals(updateRequest.getId())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号已经存在");
            }
        }

        //构造更新的条件 根据id查询用户
        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
        userUpdateWrapper.eq("id", updateRequest.getId());
        //准备更新的用户实体
        User user = new User();
        BeanUtils.copyProperties(updateRequest, user);
        //设置加密密码
        if(encryptPassword != null){
            user.setUserPassword(encryptPassword);
        }
        //更新
        int updateCount = userMapper.update(user, userUpdateWrapper);
        return updateCount > 0;
    }

    /**
     * 管理员删除用户
     * @param id
     * @return
     */
    @Override
    public boolean deleteById(Long id) {
        //使用mp的根据id删除方法(逻辑删除）
        int count = userMapper.deleteById(id);
        return count > 0;
    }

    /**
     * 分页查询用户
     * @param currentPage
     * @param pageSize
     * @return
     */
    @Override
    public IPage<User> getUsersByPage(int currentPage, int pageSize) {
        //创建分页查询对象，执行分页查询
        IPage<User> page = new Page<User>(currentPage, pageSize);
        //TODO 可以设置queryWrapper，增加分页查询的条件
        return userMapper.selectPage(page, null);
    }

    /**
     * 根据id集合查询用户数据
     * @param ids
     * @return
     */
    @Override
    public List<User> getUsersByIds(List<Long> ids) {
        List<User> users = userMapper.selectBatchIds(ids);
        return users;
    }

    /**
     * 判断用户账号是否已经存在
     * @param userAccount
     * @return
     */
    public boolean checkUserAccountExist(String userAccount){
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = userMapper.selectCount(queryWrapper);
        return count > 0 ? true : false;
    }

    /**
     * 判断星球编号是否已经存在
     * @param planetCode
     * @return
     */
    public boolean checkPlanetCodeExist(String planetCode){
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        Long count = userMapper.selectCount(queryWrapper);
        return count > 0 ? true : false;
    }
}




