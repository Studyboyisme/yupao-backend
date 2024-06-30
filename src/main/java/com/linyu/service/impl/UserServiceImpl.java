package com.linyu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.linyu.common.ErrorCode;
import com.linyu.exception.BusinessException;
import com.linyu.model.User;
import com.linyu.model.request.UserCreateRequest;
import com.linyu.model.request.UserUpdateRequest;
import com.linyu.service.UserService;
import com.linyu.mapper.UserMapper;
import com.linyu.utils.AlgorithmUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.linyu.constant.UserConstant.ADMIN_ROLE;
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
        safetyUser.setTags(originUser.getTags());
        safetyUser.setProfile(originUser.getProfile());
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
//    @Override
//    public boolean updateUser(UserUpdateRequest updateRequest) {
//        if(updateRequest.getId() == null){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ID不能为空");
//        }
//        //账号校验
//        String userAccount = updateRequest.getUserAccount();
//        if(StringUtils.isNotBlank(userAccount)){
//            //长度校验
//            if(userAccount.length() < 4)
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
//            //特殊字符校验
//            String validPattern =  ".*[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*()——+|{}【】‘；：”“’。，、？\\\\]+.*";
//            Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
//            if(matcher.find()){ //如果包含特殊字符，返回-1
//                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户包含特殊字符");
//            }
//            //唯一性校验
//            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//            queryWrapper.eq("userAccount", userAccount);
//            User existUser = userMapper.selectOne(queryWrapper);
//            if(existUser != null && !existUser.getId().equals(updateRequest.getId())){
//                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户已经存在");
//            }
//        }
//
//        //密码校验和加密
//        String userPassword = updateRequest.getUserPassword();
//        String encryptPassword = null;
//        if(StringUtils.isNotBlank(userPassword)){
//            if(userPassword.length() < 8){
//                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
//            }
//            //加密
//            encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
//        }
//
//        //星球编号校验
//        String planetCode = updateRequest.getPlanetCode();
//        if(StringUtils.isNotBlank(planetCode)){
//            if(planetCode.length() > 5){
//                throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
//            }
//            //唯一性校验
//            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//            queryWrapper.eq("planetCode", planetCode);
//            User existUser = userMapper.selectOne(queryWrapper);
//            if(existUser != null && !existUser.getId().equals(updateRequest.getId())){
//                throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号已经存在");
//            }
//        }
//
//        //构造更新的条件 根据id查询用户
//        UpdateWrapper<User> userUpdateWrapper = new UpdateWrapper<>();
//        userUpdateWrapper.eq("id", updateRequest.getId());
//        //准备更新的用户实体
//        User user = new User();
//        BeanUtils.copyProperties(updateRequest, user);
//        //设置加密密码
//        if(encryptPassword != null){
//            user.setUserPassword(encryptPassword);
//        }
//        //更新
//        int updateCount = userMapper.update(user, userUpdateWrapper);
//        return updateCount > 0;
//    }

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
     * 根据标签搜索用户(内存过滤版)
     * @param tagNameList
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if(CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        for(String tagList : tagNameList){
//            queryWrapper = queryWrapper.like("tags", tagList);
//        }
//        List<User> userList = userMapper.selectList(queryWrapper);
//        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());

        //1 先查询所有用户
        QueryWrapper queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        //对userList中的每一个user进行判断
        return userList.stream().filter(user -> {
            String tagStr = user.getTags();
//            if(StringUtils.isBlank(tagStr)){
//                return false;
//            }
            //反序列化
            Set<String> tempTagNameSet = gson.fromJson(tagStr, new TypeToken<Set<String>>(){}.getType());
            //Optional 可选类
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            for(String tagName : tagNameList){
                if(!tempTagNameSet.contains(tagName)){ //如果有任何一个标签不存在
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());

//        if (CollectionUtils.isEmpty(tagNameList)){
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        return sqlSearch(tagNameList);   //先 sql query time = 5982 后 memory query time = 5606
//        return memorySearch(tagNameList);    // 先 memory query time = 5938 后 sql query time = 5956 （清过缓存）
    }

    @Override
    public int updateUser(User user, User loginUser) {
        Long userId = user.getId();
        if(userId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //TODO 补充校验，如果用户没有传任何要更新的值，就直接报错，不执行update语句
        //如果是管理员，允许更新任意用户
        //如果不是管理员，只允许更新自己的信息
        if(!isAdmin(loginUser) && userId != loginUser.getId()){
            throw new BusinessException(ErrorCode.NOT_AUTH);
        }
        User userold = userMapper.selectById(userId);
        if(userold == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userMapper.updateById(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if(request == null){
            return null;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if(userObj == null){
            throw new BusinessException(ErrorCode.NOT_AUTH);
        }
        return (User) userObj;
    }

    /**
     * 是否为管理员
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null || user.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public boolean isAdmin(User loginUser) {
        return loginUser != null && loginUser.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public List<User> matchUsers(long num, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //只查询固定列，提高查询速度
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list(queryWrapper);
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        //使用gson将json类型的tags转化为Java实体
        //TypeToken是gson提供的数据类型转化器，支持各种数据类型集合的转化
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        //用户列表的下标 -> 相似度
        List<Pair<User, Long>> list = new ArrayList<>();
        //依次计算所有用户和当前用户的相似度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            //无标签或者为当前用户自己
            //注意user.getId方法的返回值是Long类型，使用equals方法比较是否相等
            if(StringUtils.isBlank(userTags) || user.getId().equals(loginUser.getId())){
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            //计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }
        //按编辑距离由小到大排序
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int)(a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        //原本顺序的userId列表
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList);
        //1， 3， 2
        //User1, User2, User3
        //1 => User1, 2 => User2, 3 => User3
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(user -> getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));
        List<User> finalUserList = new ArrayList<>();
        for(Long userId: userIdList){
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }

    /**
     *     sql运行查询
     * @param tagNameList
     * @return
     */
    public List<User> sqlSearch(List<String> tagNameList){
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        long starTime = System.currentTimeMillis();
        //拼接tag
        // like '%Java%' and like '%Python%'
        for (String tagList : tagNameList) {
            queryWrapper = queryWrapper.like("tags", tagList);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        log.info("sql query time = " + (System.currentTimeMillis() - starTime));
        return  userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     *     查询，内存运行筛选
     * @param tagNameList
     * @return
     */
    public List<User> memorySearch(List<String> tagNameList){

        //1.先查询所有用户
        QueryWrapper queryWrapper = new QueryWrapper<>();
        long starTime = System.currentTimeMillis();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        //2.判断内存中是否包含要求的标签
        userList.stream().filter(user -> {
            String tagstr = user.getTags();
            if (StringUtils.isBlank(tagstr)){
                return false;
            }
            Set<String> tempTagNameSet =  gson.fromJson(tagstr,new TypeToken<Set<String>>(){}.getType());
            for (String tagName : tagNameList){
                if (!tempTagNameSet.contains(tagName)){
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
        log.info("memory query time = " + (System.currentTimeMillis() - starTime));
        return  userList;
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




