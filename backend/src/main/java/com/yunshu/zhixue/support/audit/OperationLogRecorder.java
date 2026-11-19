package com.yunshu.zhixue.support.audit;

/**
 * 操作日志记录能力（横切支撑）。
 *
 * <p>由 system 模块提供实现；其他业务模块只依赖本接口，避免反向依赖 system 模块。</p>
 */
public interface OperationLogRecorder {

    String ACTION_CREATE = "新增";
    String ACTION_UPDATE = "修改";
    String ACTION_DELETE = "删除";
    String ACTION_STATUS = "禁用启用";

    /** 记录当前登录管理员的操作。 */
    void recordCurrent(String action, String targetType, String targetName);
}
