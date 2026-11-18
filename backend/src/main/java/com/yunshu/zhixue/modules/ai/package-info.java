/**
 * AI 能力层：模型客户端（OpenAI 兼容协议）、提示词模板、评分与报告生成。
 *
 * <p>无状态、无业务库依赖、不落库；只暴露领域语义接口（gradeAnswer、generateReport 等），
 * 由调用方负责持久化。模型与密钥走 {@link com.yunshu.zhixue.modules.ai.config.AiProperties}。</p>
 */
package com.yunshu.zhixue.modules.ai;
