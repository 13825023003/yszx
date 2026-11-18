/**
 * AI 面试模块：面试配置、会话暂存、逐题评分（SSE）、报告生成、面试记录。
 *
 * <p>依赖 interviewbank（读取）与 ai；stats 只读依赖本模块。
 * 发起阶段不落库，逐题评分暂存 Redis，结束时单事务落库（D4-4）。</p>
 */
package com.yunshu.zhixue.modules.interview;
