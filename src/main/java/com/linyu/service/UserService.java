package com.linyu.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.linyu.model.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linyu.model.request.UserCreateRequest;
import com.linyu.model.request.UserUpdateRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


/**
 * 用户服务
 * @author Linyu
*/
public interface UserService extends IService<User> {



    /**
     * 用户注释
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return 用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户脱敏
     * @param user
     * @return
     */
    User getSafetyUser(User user);

    /**
     * 用户注销
     * @param request
     * @return
     */
    int userLogout(HttpServletRequest request);

    /**
     * 管理员创建新用户
     * @param createRequest
     * @return
     */
    boolean createUser(UserCreateRequest createRequest);

    /**
     * 管理员更新用户
     * @param updateRequest
     * @return
     */
//    boolean updateUser(UserUpdateRequest updateRequest);

    /**
     * 管理员删除id
     * @param id
     * @return
     */
    boolean deleteById(Long id);

    /**
     * 分页查询用户
     * @param currentPage
     * @param pageSize
     * @return
     */
    IPage<User> getUsersByPage(int currentPage, int pageSize);

    /**
     * 根据id集合查询用户数据
     * @param ids
     * @return
     */
    List<User> getUsersByIds(List<Long> ids);

    /**
     * 根据标签搜索用户
     * @param tagNameList
     * @return
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 用户信息修改
     * @param user
     * @param loginUser
     * @return
     */
    int updateUser(User user, User loginUser);

    /**
     * 获取当前用户信息
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 是否为管理员
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);

}
