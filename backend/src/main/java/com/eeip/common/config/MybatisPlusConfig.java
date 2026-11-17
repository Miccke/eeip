package com.eeip.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.eeip.common.datascope.EeipDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.eeip.common.tenant.EeipTenantLineHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // ① 多租户：最先执行，为所有业务表追加 tenant_id 条件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new EeipTenantLineHandler()));
        // ② 数据权限：在租户隔离的基础上再叠加部门/本人范围
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new EeipDataPermissionHandler()));
        // ③ 乐观锁：实体上的 @Version 需要该拦截器才会生效（否则 updateById 会因缺少
        //    MP_OPTLOCK_VERSION_ORIGINAL 参数而抛 BindingException）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // ④ 分页：放在最后，基于上面改写后的 SQL 计算总数
        PaginationInnerInterceptor page = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        page.setMaxLimit(200L);
        interceptor.addInnerInterceptor(page);
        return interceptor;
    }
}
