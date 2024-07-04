package com.linyu.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.linyu.common.BaseResponse;
import com.linyu.common.ErrorCode;
import com.linyu.common.ResultUtils;
import com.linyu.exception.BusinessException;
import com.linyu.model.User;
import com.linyu.model.request.UserCreateRequest;
import com.linyu.model.request.UserLoginRequest;
import com.linyu.model.request.UserRegisterRequest;
import com.linyu.model.request.UserUpdateRequest;
import com.linyu.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.linyu.constant.UserConstant.ADMIN_ROLE;
import static com.linyu.constant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/user")
@Slf4j
@Api(tags = "用户中心接口")
//@CrossOrigin(origins = {"http://localhost:5173/"}, allowCredentials = "true")
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @ApiOperation("用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        if(userRegisterRequest == null){
            //return ResultUtils.error(ErrorCode.PARAMS_ERROR);
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if(StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        if(userLoginRequest == null){
//            return null;
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if(StringUtils.isAnyBlank(userAccount, userPassword)){
//            return null;
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request){
        if(request == null){
//            return null;
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 查询
     * 管理员权限
     * 模糊查询
     * @param username
     * @param request
     * @return
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username, HttpServletRequest request){
        if(!isAdmin(request)){
//            return new ArrayList<>();
//            return null;
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if(StringUtils.isNotBlank(username)){
            queryWrapper.like("username", username);
        }
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> {
            user.setUserPassword(null);
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 获取当前用户的登录态
     * @param request
     * @return
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if(currentUser == null){
//            return null;
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = currentUser.getId();
        // TODO 校验用户是否合法
        User user = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    /**
     * 逻辑删除用户
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody long id, HttpServletRequest request){
        if(!isAdmin(request)){
//            return false;
            throw new BusinessException(ErrorCode.NOT_AUTH);
        }
        if(id <= 0){
//            return false;
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 管理员创建新用户
     * @param createRequest
     * @param request
     * @return
     */
    @PostMapping("/create")
    public BaseResponse<Boolean> createUser(@RequestBody UserCreateRequest createRequest, HttpServletRequest request){
        //权限校验，确保是管理员操作
        if(!isAdmin(request)){
            throw new BusinessException(ErrorCode.NOT_AUTH);
        }
        if(createRequest == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        boolean result = userService.createUser(createRequest);
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新用户信息
     * @param updateRequest
     * @param request
     * @return
     */
//    @PostMapping("/update")
//    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest updateRequest, HttpServletRequest request){
//        //权限校验，确保是管理员操作
//        if(!isAdmin(request)){
//            throw new BusinessException(ErrorCode.NOT_AUTH);
//        }
//        if(updateRequest == null){
//            throw new BusinessException(ErrorCode.NULL_ERROR);
//        }
//        boolean result = userService.updateUser(updateRequest);
//        return ResultUtils.success(result);
//    }

    /**
     * 管理员删除用户信息
     * 和deleteUser方法实现功能一致
     * @param id
     * @param request
     * @return
     */
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteUser(@PathVariable Long id, HttpServletRequest request){
        //权限校验，确保是管理员操作
        if(!isAdmin(request)){
            throw new BusinessException(ErrorCode.NOT_AUTH);
        }
        if(id == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        boolean result = userService.deleteById(id);
        return ResultUtils.success(result);
    }

    /**
     * 根据id集合查询用户数据
     * @param ids
     * @param request
     * @return
     */
    @GetMapping("/getUsersByIds")
    public BaseResponse<List<User>> getUsersByIds(@RequestParam("ids") List<Long> ids, HttpServletRequest request){
        //权限校验，确保是管理员操作
        if(!isAdmin(request)){
            throw new BusinessException(ErrorCode.NOT_AUTH);
        }
        List<User> userList = userService.getUsersByIds(ids);
        //脱敏处理
        List<User> list = userList.stream().map(user -> {
            user.setUserPassword(null);
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 分页查询用户数据1
     * @param currentPage
     * @param pageSize
     * @param request
     * @return
     */
    @GetMapping("/getUsersByPage")
    public BaseResponse<IPage<User>> getUsersByPage(
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "3") int pageSize,
            HttpServletRequest request){
        //权限校验，确保是管理员操作
        if(!isAdmin(request)){
            throw new BusinessException(ErrorCode.NOT_AUTH);
        }
        //分页查询
        IPage<User> usersByPage = userService.getUsersByPage(currentPage, pageSize);
        //脱敏处理
        List<User> list = usersByPage.getRecords().stream().map(user -> {
            user.setUserPassword(null);
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        //将脱敏结果设置给返回值
        usersByPage.setRecords(list);
        return ResultUtils.success(usersByPage);
    }

    /**
     * 分页查询用户数据2
     * @param currentPage
     * @param pageSize
     * @param request
     * @return
     */
    @GetMapping("/{currentPage}/{pageSize}")
    public BaseResponse<IPage<User>> getByPage(@PathVariable int currentPage, @PathVariable int pageSize
            , HttpServletRequest request){
        //权限校验，确保是管理员操作
        if(!isAdmin(request)){
            throw new BusinessException(ErrorCode.NOT_AUTH);
        }
        //分页查询
        IPage<User> usersByPage = userService.getUsersByPage(currentPage, pageSize);
        //脱敏处理
        List<User> list = usersByPage.getRecords().stream().map(user -> {
            user.setUserPassword(null);
            return userService.getSafetyUser(user);
        }).collect(Collectors.toList());
        //将脱敏结果设置给返回值
        usersByPage.setRecords(list);
        return ResultUtils.success(usersByPage);
    }

    @ApiOperation("根据标签查询用户")
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList){
        if(CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    /**
     * 推荐页面
     * @param request
     * @return
     */
    @ApiOperation("推荐用户信息")
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize,long pageNum, HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        String redisKey = String.format("rain:user:recommend:%s", loginUser.getId());
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //如果有缓存，直接读取
        Page<User> userPage = (Page<User>) valueOperations.get(redisKey);
        if(userPage != null){
            return ResultUtils.success(userPage);
        }
        //无缓存，查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        //写缓存，30s过期
        try {
            valueOperations.set(redisKey, userPage, 30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.info("redis set key error", e);
        }
        return ResultUtils.success(userPage);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request){
        //验证参数是否为空
        if(user == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        //鉴权
        User loginUser = userService.getLoginUser(request);
        int result = userService.updateUser(user, loginUser);
        return ResultUtils.success(result);
    }

    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num <= 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.matchUsers(num, user));
    }
        /**
     * 是否是管理员
     * @param request
     * @return
     */
    public boolean isAdmin(HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            log.info("用户态USER_LOGIN_STATE不存在于会话中");
        }

        User user = (User) userObj;
        if(user == null || user.getUserRole() != ADMIN_ROLE){
            return false;
        }
        return true;
    }


}
