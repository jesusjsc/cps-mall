package com.xm.cloud_gateway.shiro.realm;

import com.xm.cloud_gateway.shiro.token.WeChatToken;
import com.xm.comment_feign.module.user.feign.UserFeignClient;
import com.xm.comment_serialize.module.user.entity.SuPermissionEntity;
import com.xm.comment_serialize.module.user.entity.SuUserEntity;
import com.xm.comment_serialize.module.user.ex.RolePermissionEx;
import com.xm.comment_serialize.module.user.form.GetUserInfoForm;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 前台登录realm
 */
@Component
public class WechatRealm extends AuthorizingRealm {

    @Autowired
    @Lazy
    private UserFeignClient userFeignClient;

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        System.out.println("授权");
        //从 principals获取主身份信息
        //将getPrimaryPrincipal方法返回值转为真实身份类型（在上边的doGetAuthenticationInfo认证通过填充到SimpleAuthenticationInfo中身份类型），
        SuUserEntity userEntity =  (SuUserEntity) principals.getPrimaryPrincipal();

        //根据身份信息获取权限信息
        List<RolePermissionEx> rolePermissionExMsg = userFeignClient.role(userEntity.getId());
        List<RolePermissionEx> rolePermissionExs = rolePermissionExMsg;
        List<String> permissions = new ArrayList<>();
        for (RolePermissionEx rolePermissionEx : rolePermissionExs) {
            for (SuPermissionEntity suPermissionEntity : rolePermissionEx.getSuPermissionEntities()) {
                permissions.add(rolePermissionEx.getName()+":"+suPermissionEntity.getName());
            }
        }
        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
        simpleAuthorizationInfo.addStringPermissions(permissions);
        return simpleAuthorizationInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        System.out.println("身份验证");
        //获取用户的输入的账号.
        String openId = (String)token.getPrincipal();
        GetUserInfoForm form = new GetUserInfoForm();
        form.setOpenId(openId);
        SuUserEntity userInfoMsg = userFeignClient.getUserInfo(form);
        SimpleAuthenticationInfo authenticationInfo = new SimpleAuthenticationInfo(userInfoMsg,"",getName());
        return authenticationInfo;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof WeChatToken;
    }
}
