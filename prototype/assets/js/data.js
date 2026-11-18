/* ============================================================
   云枢智学 · 原型模拟数据（纯前端，无服务端依赖）
   ============================================================ */
(function (global) {
  'use strict';

  /* ---------- 工具 ---------- */
  let seed = 7;
  const rand = () => { seed = (seed * 9301 + 49297) % 233280; return seed / 233280; };
  const between = (a, b) => Math.round(a + rand() * (b - a));

  function makeQuestion(id, type, difficulty, stem, opts, answer, analysis, tags, extra) {
    const options = (opts || []).map((text, i) => ({ key: String.fromCharCode(65 + i), text }));
    return Object.assign({
      id, type, difficulty, stem, options, answer,
      analysis: analysis || '',
      tags: tags || [],
      people: between(120, 3860),
      accuracy: between(52, 94),
      status: 'published'
    }, extra || {});
  }

  function stage(id, name, icon, questions) {
    return { id, name, icon, questions };
  }

  /* ============================================================
     学科 / 阶段 / 刷题题目
     ============================================================ */
  const SUBJECTS = [
    {
      id: 'agent', name: 'AI应用与智能体开发', icon: '🤖', color: '#6366f1', soft: '#eef2ff',
      desc: '从大模型基础到 Agent 工程化落地，掌握 Prompt、RAG、工具编排与多智能体协作。',
      stages: [
        stage('agent-1', '大模型基础与 Prompt 工程', '🧠', [
          makeQuestion('agent-1-1', 'single', 'easy',
            '在 Transformer 架构中，Self-Attention（自注意力）的核心作用是？',
            ['捕捉序列中不同位置之间的依赖关系', '对输入特征做非线性变换', '显著降低模型的参数量', '把文本转换成固定维度的向量'],
            'A',
            'Self-Attention 通过计算序列中每个位置与其他位置的相关性权重，聚合全局上下文信息，从而捕捉长距离依赖。非线性变换由 FFN 完成，降参数量与文本向量化均不是其主要作用。',
            ['Transformer', '注意力机制']),
          makeQuestion('agent-1-2', 'multiple', 'medium',
            '以下哪些做法可以有效降低大模型「幻觉」发生的概率？',
            ['引入 RAG 检索增强，基于外部知识作答', '要求模型在关键结论后给出引用来源', '把 Temperature 调高以增加多样性', '对结构化输出做 schema 校验与重试'],
            'ABD',
            'RAG 提供可溯源的事实依据、引用机制提升可核查性、结构化校验能拦截不合规输出，均能降低幻觉。提高 Temperature 会让采样更随机，反而增加幻觉风险。',
            ['幻觉治理', 'RAG', 'Prompt']),
          makeQuestion('agent-1-3', 'judge', 'easy',
            '在 Prompt 中提供少量示例（Few-shot）通常可以提升模型在特定输出格式上的稳定性。',
            ['正确', '错误'], 'A',
            'Few-shot 通过示例向模型传达任务模式与输出格式，对格式遵循和风格一致性有明显帮助。示例应精选、避免相互矛盾。',
            ['Prompt 工程', 'Few-shot']),
          makeQuestion('agent-1-4', 'single', 'medium',
            '关于大模型采样参数 Temperature，下列说法正确的是？',
            ['Temperature 越高，输出越确定', 'Temperature 越低，输出越发散', 'Temperature 越高，输出的随机性越强', 'Temperature 与采样过程无关'],
            'C',
            'Temperature 作用于 logits 缩放：值越高，概率分布越平滑，采样随机性越强；值越低，分布越尖锐，输出越确定、重复性越高。',
            ['采样参数', '模型基础']),
          makeQuestion('agent-1-5', 'short', 'medium',
            '请简述 RAG（检索增强生成）的基本流程，并说明它相比直接让大模型作答有哪些优势。',
            null, null,
            '标准 RAG 流程分为「文档入库」与「在线问答」两条链路。入库：文档解析 → 分块（Chunking）→ 向量化 → 写入向量库。在线：用户问题改写/向量化 → 相似度检索 Top-K → 重排（可选）→ 拼装上下文 → LLM 生成答案。优势：知识可更新且无需重训模型、答案可溯源降低幻觉、可接入私有数据、成本低于微调。',
            ['RAG', '系统设计'],
            {
              reference: 'RAG = 检索 + 生成。离线阶段把文档切块并向量化入库；在线阶段先把问题向量化，召回 Top-K 相关片段，经过重排后拼接到 Prompt 中，由大模型基于检索结果生成答案。优势包括：知识实时可更新、可引用来源降低幻觉、支持私有/垂直领域数据、相比微调成本更低。',
              keyPoints: ['文档切块与向量入库', '问题向量化并召回 Top-K', '重排与上下文拼装', '可溯源、可更新、成本低']
            }),
          makeQuestion('agent-1-6', 'multiple', 'hard',
            '在 Function Calling 场景下，以下哪些做法可以提升工具调用的可靠性？',
            ['为每个工具编写清晰、无歧义的功能描述与参数说明', '限制单轮可调用工具的数量与权限范围', '允许模型在参数缺失时自由编造参数值', '对工具执行结果做校验，并在失败时支持重试'],
            'ABD',
            '清晰的工具描述帮助模型正确选择工具；限制工具数量与权限降低误调用风险；结果校验与重试提升最终成功率。让模型编造参数是典型反模式，应通过追问或返回错误让模型修正。',
            ['Function Calling', 'Agent 工程']),
          makeQuestion('agent-1-7', 'judge', 'medium',
            '只要大模型的上下文窗口足够大，就一定能准确利用窗口内的全部信息。',
            ['正确', '错误'], 'B',
            '存在「Lost in the Middle」现象：模型对上下文中部信息的利用率明显低于首尾。上下文越长，注意力越容易稀释，因此需要合理切分、重排与压缩，而不是简单堆砌。',
            ['上下文工程', '长上下文']),
          makeQuestion('agent-1-8', 'single', 'easy',
            '以下哪个指标最能直接衡量 RAG 系统中「检索」环节的质量？',
            ['Recall@K（召回率）', 'Perplexity（困惑度）', 'BLEU', '训练损失'],
            'A',
            'Recall@K 衡量前 K 条检索结果中是否覆盖了相关文档，直接反映检索效果。Perplexity 用于语言模型评估，BLEU 评估生成文本与参考文本的重合度。',
            ['RAG', '评测指标'])
        ]),
        stage('agent-2', 'Function Calling 与工具编排', '🛠️', [
          makeQuestion('agent-2-1', 'single', 'medium',
            '在 Function Calling 中，tools 定义里的 JSON Schema 主要作用是？',
            ['描述工具的入参结构与约束，约束模型生成合法参数', '存储工具的真实业务逻辑', '替代系统提示词', '加密工具返回结果'],
            'A',
            'JSON Schema 描述函数名、参数类型、必填项与枚举约束，是模型生成结构化调用参数的依据；真实业务逻辑仍由服务端实现。',
            ['Function Calling', 'JSON Schema']),
          makeQuestion('agent-2-2', 'multiple', 'medium',
            '一个可上线的 Agent 工具执行层，通常需要具备哪些能力？',
            ['超时控制与重试', '参数校验与权限控制', '把异常堆栈原样返回给模型', '执行日志与可观测性'],
            'ABD',
            '超时重试保证稳定性，参数校验与权限控制保证安全，日志与可观测性便于排障。异常堆栈应转为模型可理解的错误说明，原样返回既不安全也不利于模型修正。',
            ['Agent 工程', '稳定性']),
          makeQuestion('agent-2-3', 'judge', 'easy',
            '工具返回结果过大时，可以先做摘要或截断再回填给模型，以控制上下文长度。',
            ['正确', '错误'], 'A',
            '工具结果直接回填容易挤爆上下文并稀释关键信息，应先做结构化提取、摘要或分页读取。',
            ['上下文工程']),
          makeQuestion('agent-2-4', 'short', 'hard',
            '请设计一个「查询订单并处理退款」的 Agent 工具调用流程，说明你会如何划分工具、以及如何处理失败与越权风险。',
            null, null,
            '应拆分为 query_order、check_refund_policy、create_refund 三个粒度清晰的工具。query_order 只读；check_refund_policy 校验时效与状态；create_refund 属于写操作，需要二次确认与幂等键。失败处理：区分可重试（超时、限流）与不可重试（参数错误、业务规则不满足），把错误转成结构化说明返回模型。越权风险：工具级权限校验、用户身份透传、写操作要求人工确认，并记录完整审计日志。',
            ['Agent 设计', '工具编排'],
            {
              reference: '拆分只读查询与写操作工具，写操作增加确认与幂等；失败按可重试/不可重试分类处理；通过身份透传、工具级权限与审计日志控制越权风险。',
              keyPoints: ['工具按读写职责拆分', '写操作二次确认 + 幂等', '失败分类与重试策略', '权限校验与审计日志']
            })
        ]),
        stage('agent-3', 'RAG 知识库构建', '📚', [
          makeQuestion('agent-3-1', 'single', 'medium',
            '文档切块（Chunking）时设置 overlap（重叠）的主要目的是？',
            ['减少跨块语义被截断造成的信息丢失', '减小向量维度', '加快文档上传速度', '避免文档重复入库'],
            'A',
            '相邻块保留部分重叠内容，可缓解句子或段落在边界被切断导致的语义缺失，提升召回完整性。',
            ['RAG', '文档处理']),
          makeQuestion('agent-3-2', 'multiple', 'hard',
            '以下哪些手段可以提升 RAG 的召回质量？',
            ['混合检索（向量 + 关键词 BM25）', '查询改写 / 多查询扩展', '只增大 Top-K 且不做重排', '引入 Rerank 重排模型'],
            'ABD',
            '混合检索兼顾语义与精确匹配，查询改写提升问题表达覆盖度，Rerank 能显著改善最终排序。单纯增大 Top-K 会引入噪声并推高成本。',
            ['RAG', '检索优化']),
          makeQuestion('agent-3-3', 'judge', 'medium',
            '向量检索的结果一定优于关键词检索，因此在 RAG 中可以完全去掉 BM25。',
            ['正确', '错误'], 'B',
            '向量检索擅长语义相似，但对专有名词、编号、错误码等精确匹配场景可能失效。工程上通常采用混合检索并做融合排序。',
            ['RAG', '混合检索'])
        ]),
        stage('agent-4', '多智能体系统设计', '🕸️', [
          makeQuestion('agent-4-1', 'single', 'medium',
            '在多智能体协作中，Supervisor（调度者）模式的核心职责是？',
            ['负责任务拆解、角色分派与结果汇总', '替代所有子智能体完成推理', '只负责日志记录', '直接对外提供 HTTP 接口'],
            'A',
            'Supervisor 负责规划与调度：拆解任务、分派给专职 Agent、收集结果并决定是否继续迭代，避免子 Agent 之间无序通信。',
            ['多智能体', '架构模式']),
          makeQuestion('agent-4-2', 'multiple', 'medium',
            '多智能体系统相比单体 Agent，通常会带来哪些挑战？',
            ['上下文与状态管理更复杂', '调用成本与延迟上升', '错误传播与调试难度增加', '完全消除幻觉'],
            'ABC',
            '多 Agent 引入通信开销、状态同步与错误级联问题，成本和延迟上升，可观测性要求更高。它并不能消除幻觉。',
            ['多智能体', '工程权衡']),
          makeQuestion('agent-4-3', 'judge', 'easy',
            '把任务拆给多个 Agent 一定能提升效果，因此应尽量增加 Agent 数量。',
            ['正确', '错误'], 'B',
            '智能体数量应根据任务复杂度权衡。粒度过细会带来通信与协调成本，反而降低整体效果和稳定性。',
            ['多智能体', '设计原则'])
        ])
      ]
    },
    {
      id: 'llm', name: 'AI大模型开发', icon: '🧬', color: '#8b5cf6', soft: '#f5f3ff',
      desc: '深入 Transformer、预训练与微调，掌握大模型训练、评测与推理部署的完整链路。',
      stages: [
        stage('llm-1', 'Transformer 与注意力机制', '⚡', [
          makeQuestion('llm-1-1', 'single', 'easy',
            'Multi-Head Attention 中「多头」的主要意义是？',
            ['让模型在不同子空间学习多样化的注意力模式', '成倍增加序列长度', '替代前馈网络', '减少位置编码需求'],
            'A',
            '多个注意力头在不同投影子空间中并行计算，可捕捉语法、语义、位置等不同类型的关联，最后拼接融合。',
            ['Transformer']),
          makeQuestion('llm-1-2', 'multiple', 'medium',
            '以下哪些属于 Transformer 相比 RNN 的优势？',
            ['支持并行计算', '长距离依赖建模更直接', '天然具备序列顺序信息', '计算复杂度不随序列长度增长'],
            'AB',
            'Transformer 可并行处理整个序列，任意两位置之间只需一步注意力即可建立联系。但它本身不包含顺序信息（需位置编码），且注意力复杂度随序列长度平方增长。',
            ['Transformer', '模型基础']),
          makeQuestion('llm-1-3', 'judge', 'medium',
            '位置编码的作用是向模型注入 token 在序列中的顺序信息。',
            ['正确', '错误'], 'A',
            '自注意力本身具有置换不变性，必须通过绝对或相对位置编码（如 RoPE、ALiBi）引入顺序信息。',
            ['位置编码'])
        ]),
        stage('llm-2', '模型微调 LoRA / QLoRA', '🎛️', [
          makeQuestion('llm-2-1', 'single', 'medium',
            'LoRA 微调的核心思想是？',
            ['冻结原模型权重，仅训练低秩分解的旁路矩阵', '直接全量更新所有参数', '只训练词嵌入层', '删除部分 Transformer 层'],
            'A',
            'LoRA 将权重增量 ΔW 分解为两个低秩矩阵 A·B，训练时只更新这两个小矩阵，显著降低显存与存储开销，并可插拔切换。',
            ['微调', 'LoRA']),
          makeQuestion('llm-2-2', 'multiple', 'hard',
            '使用 LoRA 微调时，以下哪些因素会显著影响效果？',
            ['秩 r 的取值', '学习率与训练轮数', '训练数据的质量与分布', '把 temperature 设为 0'],
            'ABC',
            '秩决定可训练容量，学习率与轮数影响收敛与过拟合，数据质量直接决定上限。temperature 是推理采样参数，与训练无关。',
            ['微调', '超参数']),
          makeQuestion('llm-2-3', 'judge', 'medium',
            'QLoRA 通过 4bit 量化基座模型，可以在更小的显存下完成微调，但可能带来一定的精度损失。',
            ['正确', '错误'], 'A',
            'QLoRA 使用 NF4 量化基座并配合 LoRA 训练，大幅降低显存占用，代价是可能存在轻微效果损失，需要通过评测验证。',
            ['QLoRA', '量化'])
        ]),
        stage('llm-3', '推理优化与部署', '🚀', [
          makeQuestion('llm-3-1', 'single', 'medium',
            'KV Cache 的主要作用是？',
            ['缓存历史 token 的 Key/Value，避免重复计算', '压缩模型权重', '加速数据预处理', '缓存用户会话'],
            'A',
            '自回归生成时，历史 token 的 K/V 可复用，KV Cache 避免每步重算全部注意力，大幅降低推理延迟，但会占用显存。',
            ['推理优化']),
          makeQuestion('llm-3-2', 'multiple', 'hard',
            '以下哪些技术可以降低大模型推理的显存占用？',
            ['权重量化（INT8 / INT4）', 'PagedAttention 管理 KV Cache', '连续批处理（Continuous Batching）', '增大 batch size'],
            'ABC',
            '量化降低权重显存，PagedAttention 减少 KV Cache 碎片与浪费，连续批处理提升吞吐。单纯增大 batch 会增加显存占用。',
            ['推理优化', '部署']),
          makeQuestion('llm-3-3', 'judge', 'easy',
            '首 token 延迟（TTFT）和每 token 输出延迟（TPOT）是衡量推理体验的两个关键指标。',
            ['正确', '错误'], 'A',
            'TTFT 影响用户感知的响应速度，TPOT 影响流式输出的流畅度，两者需结合吞吐量共同评估。',
            ['评测指标'])
        ])
      ]
    },
    {
      id: 'embodied', name: 'AI具身智能机器人开发', icon: '🦾', color: '#0ea5e9', soft: '#f0f9ff',
      desc: '面向真实物理世界的智能体，覆盖运动学、感知、强化学习与机器人系统集成。',
      stages: [
        stage('emb-1', '机器人运动学与坐标系', '📐', [
          makeQuestion('emb-1-1', 'single', 'medium',
            '机器人正运动学（Forward Kinematics）解决的问题是？',
            ['已知关节角，求末端执行器的位姿', '已知末端位姿，求关节角', '求解关节力矩', '规划无碰撞路径'],
            'A',
            '正运动学由关节变量推导末端位姿；逆运动学则相反。路径规划与动力学分别属于其他环节。',
            ['运动学']),
          makeQuestion('emb-1-2', 'multiple', 'medium',
            '描述刚体在三维空间中的位姿通常需要哪些信息？',
            ['位置（平移）', '姿态（旋转）', '质量', '速度'],
            'AB',
            '位姿 = 位置 + 姿态，通常用 3×1 平移向量与旋转矩阵/四元数表示。质量与速度属于动力学状态。',
            ['位姿表示']),
          makeQuestion('emb-1-3', 'judge', 'easy',
            '逆运动学可能存在多解，也可能无解。',
            ['正确', '错误'], 'A',
            '同一末端位姿可能对应多组关节角（如肘部上/下），也可能因超出工作空间而无解。',
            ['运动学'])
        ]),
        stage('emb-2', '强化学习与模仿学习', '🎮', [
          makeQuestion('emb-2-1', 'single', 'medium',
            '在强化学习中，奖励函数设计不当最可能导致什么问题？',
            ['智能体学到奖励漏洞（Reward Hacking）', '训练速度必然变慢', '模型参数量增加', '环境无法仿真'],
            'A',
            '奖励是行为的唯一导向，设计偏差会让智能体钻空子获得高分却不符合真实目标，即奖励劫持。',
            ['强化学习']),
          makeQuestion('emb-2-2', 'multiple', 'hard',
            '模仿学习相比强化学习的主要优势包括？',
            ['可利用专家示范快速获得策略', '训练过程更稳定', '完全不依赖数据', '更容易处理稀疏奖励任务'],
            'ABD',
            '模仿学习直接从示范中学习，避免探索成本与稀疏奖励难题，训练更稳定；但它依赖高质量示范数据。',
            ['模仿学习']),
          makeQuestion('emb-2-3', 'judge', 'medium',
            'Sim2Real 的核心难点之一是仿真环境与真实世界之间的物理差异。',
            ['正确', '错误'], 'A',
            '仿真与真实的动力学、传感器噪声、延迟存在差异，需要通过域随机化、系统辨识等手段缩小差距。',
            ['Sim2Real'])
        ])
      ]
    },
    {
      id: 'embedded', name: 'AI嵌入式_机器人开发', icon: '🔧', color: '#14b8a6', soft: '#f0fdfa',
      desc: '从 MCU、RTOS 到边缘推理，构建低功耗、实时可靠的嵌入式智能设备。',
      stages: [
        stage('embd-1', '嵌入式 C 与 RTOS', '🧩', [
          makeQuestion('embd-1-1', 'single', 'medium',
            'RTOS 中「优先级反转」通常通过什么机制缓解？',
            ['优先级继承或优先级天花板', '增大时钟频率', '关闭所有中断', '增加任务栈大小'],
            'A',
            '高优先级任务等待被低优先级任务占用的共享资源时，低优先级任务临时继承高优先级，避免被中优先级任务抢占导致长期阻塞。',
            ['RTOS', '并发']),
          makeQuestion('embd-1-2', 'multiple', 'medium',
            '以下哪些属于 RTOS 中常见的任务间通信机制？',
            ['信号量', '消息队列', '互斥锁', 'DMA'],
            'ABC',
            '信号量、消息队列、互斥锁均为 RTOS 通信/同步原语；DMA 是内存与外设间的数据传输机制。',
            ['RTOS']),
          makeQuestion('embd-1-3', 'judge', 'easy',
            '在中断服务程序中应尽量缩短执行时间，把耗时逻辑放到任务中处理。',
            ['正确', '错误'], 'A',
            'ISR 过久会阻塞其他中断、增加响应抖动。常见做法是 ISR 中只做标记或用队列投递，由任务完成耗时处理。',
            ['中断'])
        ]),
        stage('embd-2', '边缘 AI 推理部署', '📟', [
          makeQuestion('embd-2-1', 'single', 'medium',
            '在 MCU 上部署神经网络时，模型量化最主要的收益是？',
            ['减少模型体积与内存带宽，提升推理速度', '提高模型精度', '增加可训练参数', '消除对算力的需求'],
            'A',
            'INT8 量化可将权重从 32 位压缩到 8 位，显著降低 Flash 与 RAM 占用并加速推理，通常需要校准以控制精度损失。',
            ['边缘推理', '量化']),
          makeQuestion('embd-2-2', 'multiple', 'hard',
            '为 MCU 选择推理框架时，需要重点评估哪些因素？',
            ['算子支持范围', '内存占用与模型体积', '是否支持目标芯片的加速指令', '框架的 GitHub star 数'],
            'ABC',
            '算子覆盖决定模型能否落地，内存与体积受限于 MCU 资源，芯片加速支持影响性能。star 数仅作参考。',
            ['边缘推理', '选型']),
          makeQuestion('embd-2-3', 'judge', 'medium',
            '边缘 AI 部署时，模型精度与推理延迟之间通常存在权衡。',
            ['正确', '错误'], 'A',
            '量化、剪枝、蒸馏等手段在降低计算量的同时可能损失精度，需要根据业务容忍度做权衡与评测。',
            ['边缘推理'])
        ])
      ]
    },
    {
      id: 'testing', name: 'AI测试', icon: '🧪', color: '#f59e0b', soft: '#fffbeb',
      desc: '覆盖测试基础、自动化与 AI 模型质量评测，保障 AI 产品的可靠性与一致性。',
      stages: [
        stage('test-1', '测试基础与用例设计', '📝', [
          makeQuestion('test-1-1', 'single', 'easy',
            '在等价类划分法中，以下哪个属于「无效等价类」？',
            ['输入年龄为 -5 岁', '输入年龄为 20 岁', '输入年龄为 65 岁', '输入年龄为 0 岁'],
            'A',
            '年龄的合法区间通常为 0~150，-5 超出边界属于无效等价类，用于验证系统对非法输入的拦截。',
            ['测试方法']),
          makeQuestion('test-1-2', 'multiple', 'medium',
            '关于边界值分析，下列说法正确的有？',
            ['重点测试取值区间的边界', '通常结合等价类划分使用', '只需要测试区间中点', '可发现大量边界相关缺陷'],
            'ABD',
            '大量缺陷发生在边界附近，边界值分析取 min、min±1、max±1 等点，常与等价类配合。只测中点无法覆盖边界问题。',
            ['测试方法']),
          makeQuestion('test-1-3', 'judge', 'medium',
            '回归测试的主要目的是验证新改动没有破坏既有功能。',
            ['正确', '错误'], 'A',
            '回归测试在变更后重跑既有用例，确认原有功能未被引入的缺陷影响。',
            ['测试分类'])
        ]),
        stage('test-2', 'AI 模型测试与评测', '📊', [
          makeQuestion('test-2-1', 'single', 'medium',
            '评估大模型问答质量时，以下哪项最需要「人工/LLM 裁判」参与？',
            ['回答的事实准确性与完整性', '接口响应状态码', '请求耗时', '返回 JSON 字段数量'],
            'A',
            '事实准确性与完整性属于开放性语义质量，难以用固定规则判定，通常采用人工标注或 LLM-as-a-Judge 评估。',
            ['模型评测']),
          makeQuestion('test-2-2', 'multiple', 'hard',
            '构建 AI 应用测试集时，应重点关注？',
            ['覆盖典型业务场景', '包含对抗性与边界输入', '只保留模型答对的样本', '标注清晰的期望结果'],
            'ABD',
            '测试集应覆盖典型场景、对抗与边界输入，并有明确期望；只保留答对样本会让测试失去区分度。',
            ['模型评测']),
          makeQuestion('test-2-3', 'judge', 'easy',
            '大模型的输出具有不确定性，因此测试应当关注多次采样下的稳定性。',
            ['正确', '错误'], 'A',
            '同一输入可能产生不同输出，需要通过多次采样、温度控制与一致性指标来评估稳定性。',
            ['模型评测'])
        ])
      ]
    },
    {
      id: 'ops', name: 'AI运维', icon: '⚙️', color: '#10b981', soft: '#ecfdf5',
      desc: '掌握容器化、GPU 集群与模型服务运维，让 AI 系统稳定高效运行。',
      stages: [
        stage('ops-1', 'Linux 与容器化', '🐧', [
          makeQuestion('ops-1-1', 'single', 'easy',
            'Docker 镜像分层（Layer）机制带来的主要好处是？',
            ['层可复用，减少存储与传输开销', '提升容器内 CPU 性能', '自动加密镜像', '替代 Kubernetes'],
            'A',
            '镜像由只读层叠加而成，相同层可在镜像与容器间共享，配合缓存显著降低构建与分发成本。',
            ['Docker']),
          makeQuestion('ops-1-2', 'multiple', 'medium',
            '以下哪些属于 Kubernetes 的核心资源对象？',
            ['Pod', 'Deployment', 'Service', 'Dockerfile'],
            'ABC',
            'Pod 是最小调度单元，Deployment 管理副本与滚动更新，Service 提供稳定访问入口。Dockerfile 是镜像构建文件。',
            ['K8s']),
          makeQuestion('ops-1-3', 'judge', 'medium',
            '容器的本质是利用 Linux Namespace 和 Cgroups 实现的进程隔离与资源限制。',
            ['正确', '错误'], 'A',
            'Namespace 提供视图隔离，Cgroups 限制 CPU/内存等资源，容器共享宿主内核，因此比虚拟机更轻量。',
            ['容器原理'])
        ]),
        stage('ops-2', 'GPU 集群与模型服务运维', '🖥️', [
          makeQuestion('ops-2-1', 'single', 'medium',
            'GPU 显存不足（OOM）时，以下哪种做法最不可取？',
            ['无限调大 batch size 以提升吞吐', '开启模型量化', '减小最大上下文长度', '使用张量并行拆分模型'],
            'A',
            '增大 batch 会进一步推高显存占用，加剧 OOM。应通过量化、缩短上下文、并行切分或调整并发策略解决。',
            ['GPU 运维']),
          makeQuestion('ops-2-2', 'multiple', 'hard',
            '监控大模型推理服务时，应重点关注哪些指标？',
            ['首 token 延迟与 TPOT', 'GPU 显存与利用率', '请求排队长度与错误率', '服务器机箱颜色'],
            'ABC',
            '推理体验取决于延迟、吞吐与资源水位，排队与错误率反映服务健康度。机箱颜色与指标无关。',
            ['可观测性']),
          makeQuestion('ops-2-3', 'judge', 'easy',
            '生产环境的模型服务应配置健康检查与自动重启策略。',
            ['正确', '错误'], 'A',
            '健康检查可及时发现实例异常，配合重启与滚动发布保障服务可用性。',
            ['服务运维'])
        ])
      ]
    },
    {
      id: 'visual', name: 'AI视觉设计', icon: '🎨', color: '#ec4899', soft: '#fdf2f8',
      desc: '将 AIGC 融入视觉设计工作流，从提示词到成稿，提升创意产出效率。',
      stages: [
        stage('vis-1', '视觉设计基础', '🖌️', [
          makeQuestion('vis-1-1', 'single', 'easy',
            '在色彩理论中，色相环上相对 180° 的两种颜色称为？',
            ['互补色', '邻近色', '类似色', '单色'],
            'A',
            '互补色位于色相环两端，对比强烈，常用于强调与视觉焦点；邻近色则和谐统一。',
            ['色彩']),
          makeQuestion('vis-1-2', 'multiple', 'medium',
            '提升界面视觉层次感的常用手段包括？',
            ['建立字号与字重层级', '运用留白与间距', '所有元素使用同一颜色', '通过阴影与对比区分层级'],
            'ABD',
            '字号/字重、留白、阴影与对比都能构建信息层级。单一颜色会削弱区分度。',
            ['版式', 'UI']),
          makeQuestion('vis-1-3', 'judge', 'medium',
            '网站界面设计中，对比度不足会同时影响可读性与无障碍体验。',
            ['正确', '错误'], 'A',
            '低对比度文本在弱光或低视力场景下难以辨认，WCAG 建议正文对比度至少 4.5:1。',
            ['无障碍'])
        ]),
        stage('vis-2', 'AIGC 视觉工作流', '✨', [
          makeQuestion('vis-2-1', 'single', 'medium',
            '在图像生成提示词中，明确「构图、光线、材质、风格」通常会带来什么效果？',
            ['提升生成结果的可控性与一致性', '一定提高分辨率', '减少模型参数量', '自动完成版权登记'],
            'A',
            '结构化描述能显著降低生成随机性，让画面更贴近预期，并便于团队复用与迭代。',
            ['AIGC', 'Prompt']),
          makeQuestion('vis-2-2', 'multiple', 'medium',
            'AIGC 视觉产出交付前，通常需要检查哪些问题？',
            ['文字与细节是否正确', '是否符合品牌规范', '是否存在版权与合规风险', '生成速度是否最快'],
            'ABC',
            'AI 生成内容容易出现文字错乱、细节畸变，同时需核查品牌一致性与版权合规。生成速度不是交付质量项。',
            ['AIGC', '交付规范']),
          makeQuestion('vis-2-3', 'judge', 'easy',
            '使用图生图（img2img）时，重绘幅度（denoising strength）越高，结果与原图差异通常越大。',
            ['正确', '错误'], 'A',
            '重绘幅度控制加入的噪声比例，越高则模型改动越大，越低则越接近原图。',
            ['AIGC'])
        ])
      ]
    },
    {
      id: 'video', name: 'AI视频与短剧创作', icon: '🎬', color: '#f43f5e', soft: '#fff1f2',
      desc: 'AI 驱动的视频叙事与生产：分镜脚本、生成工具链与成片工作流。',
      stages: [
        stage('vid-1', '分镜与叙事', '🎞️', [
          makeQuestion('vid-1-1', 'single', 'easy',
            '短剧创作中，「三幕结构」通常指？',
            ['建置、对抗、结局', '开头、片尾曲、花絮', '近景、中景、远景', '脚本、拍摄、剪辑'],
            'A',
            '三幕结构由建置（Setup）、对抗（Confrontation）、结局（Resolution）构成，是经典叙事框架。',
            ['叙事结构']),
          makeQuestion('vid-1-2', 'multiple', 'medium',
            '一份可执行的分镜脚本通常需要包含？',
            ['镜号与景别', '画面描述与台词', '时长与转场', '服务器的 CPU 型号'],
            'ABC',
            '分镜脚本需描述镜头画面、景别、台词、时长与转场，作为拍摄或生成的依据。',
            ['分镜']),
          makeQuestion('vid-1-3', 'judge', 'medium',
            '短剧前 3 秒通常是决定用户是否继续观看的关键窗口。',
            ['正确', '错误'], 'A',
            '短视频平台的完播率高度依赖开头钩子，前 3 秒需快速建立冲突或悬念。',
            ['短剧创作'])
        ]),
        stage('vid-2', 'AI 视频生成工具链', '📹', [
          makeQuestion('vid-2-1', 'single', 'medium',
            'AI 视频生成中，保持角色一致性的常用手段是？',
            ['使用角色参考图与固定的风格描述', '每镜头随机更换提示词', '提高视频分辨率', '缩短成片时长'],
            'A',
            '角色参考图（Reference）+ 固定描述 + 种子控制，可显著提升跨镜头的一致性。',
            ['AIGC 视频']),
          makeQuestion('vid-2-2', 'multiple', 'hard',
            'AI 视频成片工作流通常包括哪些环节？',
            ['脚本与分镜', '素材生成与筛选', '配音配乐与剪辑', '无需人工审核直接发布'],
            'ABC',
            '完整链路包含策划、生成、筛选、配音剪辑与审核。发布前人工审核是必要环节。',
            ['工作流']),
          makeQuestion('vid-2-3', 'judge', 'easy',
            'AI 生成的视频可能存在物理规律错误与人物形变，需要逐镜头检查。',
            ['正确', '错误'], 'A',
            '当前生成模型在肢体、物体恒常性与物理交互上仍不稳定，需人工筛查与重生成。',
            ['质检'])
        ])
      ]
    }
  ];

  /* ---------- 扁平化索引 ---------- */
  const ALL_QUESTIONS = [];
  const STAGE_MAP = {};
  const SUBJECT_MAP = {};
  SUBJECTS.forEach(s => {
    SUBJECT_MAP[s.id] = s;
    s.stages.forEach(st => {
      STAGE_MAP[st.id] = Object.assign({ subjectId: s.id }, st);
      st.questions.forEach(q => {
        q.stageId = st.id;
        q.subjectId = s.id;
        q.stageName = st.name;
        q.subjectName = s.name;
        ALL_QUESTIONS.push(q);
      });
    });
  });
  const QUESTION_MAP = {};
  ALL_QUESTIONS.forEach(q => QUESTION_MAP[q.id] = q);

  /* ============================================================
     学习路线 / B站课程
     ============================================================ */
  const COVERS = [
    'linear-gradient(135deg,#6366f1,#a855f7)',
    'linear-gradient(135deg,#0ea5e9,#6366f1)',
    'linear-gradient(135deg,#10b981,#0ea5e9)',
    'linear-gradient(135deg,#f59e0b,#f43f5e)',
    'linear-gradient(135deg,#8b5cf6,#ec4899)',
    'linear-gradient(135deg,#14b8a6,#84cc16)',
    'linear-gradient(135deg,#f43f5e,#f59e0b)',
    'linear-gradient(135deg,#334155,#6366f1)'
  ];

  let courseSeq = 0;
  function course(id, title, subjectId, minutes, plays, bv) {
    const n = courseSeq++ % COVERS.length;
    return {
      id, title, subjectId, minutes,
      bv: bv || ('BV1' + Math.random().toString(36).slice(2, 10).toUpperCase()),
      plays: plays,
      cover: COVERS[n],
      coverUrl: '../assets/images/covers/cover-' + (n + 1) + '.svg',
      source: 'B站'
    };
  }
  function hash(str) { let h = 0; for (let i = 0; i < str.length; i++) h = (h * 31 + str.charCodeAt(i)) | 0; return h; }

  const COURSES = [
    course('c1', '大模型应用开发从 0 到 1：Prompt 工程实战', 'agent', 186, 124300, 'BV1GJ411x7h7'),
    course('c2', 'LangChain 与 Function Calling 全解', 'agent', 242, 98300, 'BV1sM4y1k7Yj'),
    course('c3', 'RAG 知识库实战：从切块到重排', 'agent', 168, 76200, 'BV1cL411E7qF'),
    course('c4', '多智能体系统设计与落地', 'agent', 201, 54100, 'BV1pN4y1J7Gm'),
    course('c5', 'Prompt 高级技巧与结构化输出', 'agent', 96, 43200, 'BV1zP411h7Xw'),
    course('c6', 'Transformer 原理逐行精讲', 'llm', 320, 231000, 'BV1pu411n7bE'),
    course('c7', 'LoRA 微调实战：从数据到部署', 'llm', 268, 154000, 'BV1xT4y1P7kR'),
    course('c8', 'vLLM 推理加速与生产部署', 'llm', 176, 88300, 'BV1eV4y1B7cY'),
    course('c9', '机器人运动学与 ROS2 入门', 'embodied', 290, 64200, 'BV1dD4y1N7uK'),
    course('c10', '强化学习在机器人控制中的应用', 'embodied', 224, 51800, 'BV1hM4y1x7aQ'),
    course('c11', 'FreeRTOS 内核原理与实战', 'embedded', 258, 110300, 'BV1gP4y1n7sZ'),
    course('c12', 'TinyML：在 MCU 上跑神经网络', 'embedded', 152, 47600, 'BV1mN4y1k7Lw'),
    course('c13', 'AI 产品测试方法论', 'testing', 134, 39500, 'BV1qS4y1t7Vc'),
    course('c14', '大模型评测与 LLM-as-a-Judge', 'testing', 118, 28700, 'BV1jR4y1e7Bp'),
    course('c15', 'Kubernetes 生产实践', 'ops', 310, 187000, 'BV1kG4y1p7mN'),
    course('c16', 'GPU 集群运维与模型服务监控', 'ops', 198, 72400, 'BV1yT4y1r7dH'),
    course('c17', 'AIGC 视觉设计工作流', 'visual', 142, 68900, 'BV1vF4y1s7Kx'),
    course('c18', 'Midjourney 商业级提示词', 'visual', 96, 51200, 'BV1wE4y1a7Zj'),
    course('c19', 'AI 短剧创作全流程', 'video', 176, 134000, 'BV1nC4y1b7Yq'),
    course('c20', 'Runway / 可灵视频生成实战', 'video', 128, 92300, 'BV1bV4y1c7Wm')
  ];
  const COURSE_MAP = {}; COURSES.forEach(c => COURSE_MAP[c.id] = c);

  const ROUTES = [
    { id: 'r1', subjectId: 'agent', name: 'AI 应用开发工程师成长路线', audience: '零基础转行 / 初级开发者', enabled: true,
      desc: '从大模型基础与 Prompt 工程出发，逐步掌握工具调用、RAG 知识库与多智能体协作，最终具备独立交付 AI 应用的能力。',
      courseIds: ['c1', 'c5', 'c2', 'c3', 'c4'] },
    { id: 'r2', subjectId: 'llm', name: '大模型算法工程师进阶路线', audience: '有深度学习基础 / 算法方向', enabled: true,
      desc: '深入理解 Transformer 与注意力机制，掌握 LoRA 微调与推理优化，打通从训练到生产部署的完整链路。',
      courseIds: ['c6', 'c7', 'c8'] },
    { id: 'r3', subjectId: 'embodied', name: '具身智能入门路线', audience: '机器人 / 自动化方向学生', enabled: true,
      desc: '覆盖机器人运动学、强化学习与 Sim2Real，建立具身智能系统的整体认知。',
      courseIds: ['c9', 'c10'] },
    { id: 'r4', subjectId: 'embedded', name: '嵌入式 AI 开发路线', audience: '电子 / 嵌入式工程师', enabled: true,
      desc: '从 RTOS 内核到 MCU 边缘推理，完成一个可落地的低功耗智能设备项目。',
      courseIds: ['c11', 'c12'] },
    { id: 'r5', subjectId: 'testing', name: 'AI 测试工程师路线', audience: '测试工程师转型', enabled: true,
      desc: '掌握测试用例设计方法，并建立面向大模型应用的评测体系。',
      courseIds: ['c13', 'c14'] },
    { id: 'r6', subjectId: 'ops', name: 'AI 运维工程师路线', audience: '运维 / DevOps 从业者', enabled: false,
      desc: '容器化到 GPU 集群，构建支撑大模型服务的稳定基础设施。',
      courseIds: ['c15', 'c16'] },
    { id: 'r7', subjectId: 'visual', name: 'AIGC 视觉设计师路线', audience: '设计师 / 内容创作者', enabled: true,
      desc: '把 AIGC 工具融入设计工作流，提升从创意到成稿的效率。',
      courseIds: ['c17', 'c18'] },
    { id: 'r8', subjectId: 'video', name: 'AI 短剧创作人路线', audience: '编导 / 短视频创作者', enabled: true,
      desc: '从分镜叙事到 AI 生成工具链，独立完成一部 AI 短剧。',
      courseIds: ['c19', 'c20'] }
  ];
  const ROUTE_MAP = {}; ROUTES.forEach(r => ROUTE_MAP[r.id] = r);

  /* ============================================================
     AI 面试题库（按技术方向）
     ============================================================ */
  const INTERVIEW_BANK = {
    agent: [
      { q: '请用一分钟介绍你理解的大模型应用开发，以及它和传统后端开发最大的差异。', keys: ['上下文', 'Prompt', '不确定性', '评测'] },
      { q: '什么是 RAG？如果让你为一家企业搭建内部知识库问答，你会如何设计整个链路？', keys: ['切块', '向量', '检索', '重排', '引用'] },
      { q: '讲讲 Function Calling 的工作机制。工具调用失败时你会如何处理？', keys: ['工具描述', '参数', '重试', '校验', '错误'] },
      { q: '在 Agent 项目中，你如何控制成本和延迟？', keys: ['缓存', '模型分级', '上下文', '并发', '流式'] },
      { q: '如何评估一个 AI 应用的效果？你会关注哪些指标？', keys: ['准确率', '召回', '人工评测', '埋点', '满意度'] },
      { q: '如果模型输出不稳定，线上出现幻觉，你会如何排查和治理？', keys: ['复现', '日志', '检索', '约束', '兜底'] }
    ],
    llm: [
      { q: '请解释 Self-Attention 的计算过程，以及为什么需要多头。', keys: ['Q', 'K', 'V', 'softmax', '子空间'] },
      { q: 'LoRA 微调的原理是什么？秩 r 和学习率你会如何选择？', keys: ['低秩', '冻结', '秩', '学习率', '过拟合'] },
      { q: '长文本推理时显存不够，你有哪些优化手段？', keys: ['量化', 'KV Cache', '并行', '分块', '卸载'] },
      { q: '如何设计一套模型效果的评测方案？', keys: ['测试集', '基线', '人工', '指标', '回归'] }
    ],
    embodied: [
      { q: '请说明正运动学与逆运动学的区别，并举例说明应用场景。', keys: ['关节角', '末端位姿', '解', '工作空间'] },
      { q: '强化学习中奖励函数设计需要注意什么？', keys: ['稀疏', '奖励劫持', '塑形', '收敛'] },
      { q: 'Sim2Real 的主要挑战是什么？你会如何缩小差距？', keys: ['域随机化', '系统辨识', '噪声', '延迟'] }
    ],
    embedded: [
      { q: 'RTOS 中任务调度与优先级反转是如何处理的？', keys: ['优先级', '继承', '互斥锁', '调度'] },
      { q: '在 MCU 上部署神经网络需要做哪些优化？', keys: ['量化', '剪枝', '算子', '内存'] },
      { q: '中断服务程序编写有哪些注意事项？', keys: ['简短', '队列', '可重入', '延迟'] }
    ],
    testing: [
      { q: '请介绍你熟悉的测试用例设计方法，并举例说明。', keys: ['等价类', '边界值', '场景', '因果图'] },
      { q: '大模型应用的测试与普通 Web 测试有什么不同？', keys: ['不确定性', '评测集', '人工', '幻觉', '回归'] },
      { q: '如何构建一套可持续的 AI 质量评测体系？', keys: ['指标', '自动化', '标注', '基线', '迭代'] }
    ],
    ops: [
      { q: '请说明容器与虚拟机的区别，以及容器的隔离原理。', keys: ['Namespace', 'Cgroups', '内核', '轻量'] },
      { q: 'GPU 推理服务出现 OOM，你会如何定位和解决？', keys: ['显存', 'batch', '量化', '上下文', '监控'] },
      { q: '如何设计大模型服务的监控与告警体系？', keys: ['延迟', '吞吐', '错误率', '显存', '告警'] }
    ],
    visual: [
      { q: '请谈谈你会如何用 AIGC 重构传统的视觉设计工作流。', keys: ['提示词', '参考图', '迭代', '规范', '效率'] },
      { q: 'AI 生成图像常见的质量问题有哪些？如何控制？', keys: ['一致性', '细节', '文字', '重绘', '筛选'] },
      { q: '如何在 AIGC 产出中保证品牌一致性？', keys: ['品牌规范', '色板', '参考', '风格', '审核'] }
    ],
    video: [
      { q: '请说说你理解的短剧叙事结构，以及如何设计前 3 秒的钩子。', keys: ['三幕', '冲突', '悬念', '节奏'] },
      { q: 'AI 视频生成中如何保证角色与场景的一致性？', keys: ['参考图', '种子', '描述', '分镜'] },
      { q: '一条 AI 短剧的完整生产流程是怎样的？', keys: ['脚本', '分镜', '生成', '配音', '剪辑'] }
    ]
  };

  /* ============================================================
     错题本种子数据
     ============================================================ */
  const SEED_MISTAKES = [
    { questionId: 'agent-1-2', wrongAnswer: 'ABC', count: 4, corrected: 1, date: '2026-11-04', mastered: false },
    { questionId: 'agent-1-6', wrongAnswer: 'ACD', count: 3, corrected: 2, date: '2026-11-03', mastered: false },
    { questionId: 'agent-1-4', wrongAnswer: 'B', count: 2, corrected: 0, date: '2026-11-02', mastered: false },
    { questionId: 'agent-1-8', wrongAnswer: 'B', count: 1, corrected: 3, date: '2026-10-28', mastered: true },
    { questionId: 'llm-1-2', wrongAnswer: 'AC', count: 2, corrected: 0, date: '2026-10-30', mastered: false },
    { questionId: 'llm-2-2', wrongAnswer: 'ABC', count: 1, corrected: 1, date: '2026-11-01', mastered: false }
  ];

  /* ============================================================
     面试记录种子数据
     ============================================================ */
  const SEED_RECORDS = [
    { id: 'ir1', subjectId: 'agent', subjectName: 'AI应用与智能体开发', score: 82, date: '2026-11-04 20:14', duration: '18分32秒', count: 8, difficulty: '中等' },
    { id: 'ir2', subjectId: 'agent', subjectName: 'AI应用与智能体开发', score: 76, date: '2026-10-31 21:02', duration: '22分10秒', count: 10, difficulty: '中等' },
    { id: 'ir3', subjectId: 'llm', subjectName: 'AI大模型开发', score: 68, date: '2026-10-27 19:40', duration: '14分05秒', count: 5, difficulty: '困难' },
    { id: 'ir4', subjectId: 'agent', subjectName: 'AI应用与智能体开发', score: 58, date: '2026-10-20 22:31', duration: '9分48秒', count: 3, difficulty: '简单' }
  ];

  /* ============================================================
     管理端数据
     ============================================================ */
  const ADMIN_USERS = [
    { id: 'u1', name: '超级管理员', phone: '13800000000', role: 'super_admin', roleName: '超级管理员', status: 'enabled', lastLogin: '2026-11-05 09:12', email: 'admin@yunshu.ai', builtin: true },
    { id: 'u2', name: '陈审核', phone: '13800138001', role: 'content_reviewer', roleName: '内容审核', status: 'enabled', lastLogin: '2026-11-04 17:26', email: 'review@yunshu.ai' },
    { id: 'u3', name: '林运营', phone: '13800138002', role: 'operator', roleName: '运营人员', status: 'enabled', lastLogin: '2026-11-05 08:47', email: 'ops@yunshu.ai' },
    { id: 'u4', name: '周运营', phone: '13800138003', role: 'operator', roleName: '运营人员', status: 'disabled', lastLogin: '2026-10-21 11:08', email: 'zhou@yunshu.ai' }
  ];

  const ROLES = [
    { id: 'role1', name: '超级管理员', code: 'super_admin', users: 1, desc: '系统内置唯一账号，拥有全部菜单与操作权限。', perms: 'all', builtin: true },
    { id: 'role2', name: '内容审核', code: 'content_reviewer', users: 1, desc: '只把关不生产：查看全部内容并审核题目，无新增/编辑/删除/导入导出权限。', perms: ['题库管理 · 查看', '题库管理 · 审核', '面试题库管理 · 查看'] },
    { id: 'role3', name: '运营人员', code: 'operator', users: 2, desc: '除系统管理外的全部操作，含内容维护与学员管理，不具备审核权限。', perms: ['题库管理 · 查看', '题库管理 · 新增', '题库管理 · 编辑', '学习路线 · 编辑', '学员管理 · 查看'] }
  ];

  const PERM_TREE = [
    { name: '题库管理', perms: ['查看', '新增', '编辑', '删除', '导入', '导出', '审核'] },
    { name: '面试题库管理', perms: ['查看', '上传', '编辑', '删除', '检索测试'] },
    { name: '用户管理', perms: ['查看', '新增', '编辑', '禁用'] },
    { name: '学习路线', perms: ['查看', '新增', '编辑', '删除'] },
    { name: '学员管理', perms: ['查看', '编辑', '删除'] },
    { name: '系统管理', perms: ['日志查看', '系统设置'] }
  ];

  const STUDENTS = [
    { id: 'st1', name: '林晓', phone: '13912345678', regDate: '2026-09-12', days: 42, questions: 386, interviews: 12, accuracy: 86, status: 'enabled', email: 'linxiao@example.com', lastLogin: '2026-11-05 08:20' },
    { id: 'st2', name: '王一鸣', phone: '13787654321', regDate: '2026-10-02', days: 21, questions: 142, interviews: 5, accuracy: 74, status: 'enabled', email: 'wym@example.com', lastLogin: '2026-11-04 22:11' },
    { id: 'st3', name: '赵敏', phone: '15811223344', regDate: '2026-10-18', days: 12, questions: 58, interviews: 2, accuracy: 69, status: 'enabled', email: 'zhaomin@example.com', lastLogin: '2026-11-03 19:45' },
    { id: 'st4', name: '孙浩', phone: '18655667788', regDate: '2026-08-30', days: 63, questions: 512, interviews: 18, accuracy: 91, status: 'enabled', email: 'sunhao@example.com', lastLogin: '2026-11-05 09:02' },
    { id: 'st5', name: '周雨桐', phone: '13122334455', regDate: '2026-09-25', days: 30, questions: 207, interviews: 7, accuracy: 80, status: 'disabled', email: 'zyt@example.com', lastLogin: '2026-10-19 10:30' },
    { id: 'st6', name: '吴刚', phone: '13566778899', regDate: '2026-10-29', days: 6, questions: 23, interviews: 0, accuracy: 61, status: 'enabled', email: 'wugang@example.com', lastLogin: '2026-11-02 16:12' }
  ];

  const LOGS = [
    { id: 'LOG-20261105-0091', user: '林运营', action: '编辑', target: '学习路线「AI 应用开发工程师成长路线」', ip: '192.168.12.34', time: '2026-11-05 09:42:11', detail: '调整课程顺序：LangChain 与 Function Calling 全解 前移至第 2 位' },
    { id: 'LOG-20261105-0090', user: '陈审核', action: '审核', target: '题目 #2841', ip: '192.168.12.51', time: '2026-11-05 09:20:37', detail: '审核通过，状态变更为「已上架」' },
    { id: 'LOG-20261105-0089', user: '超级管理员', action: '登录', target: '管理后台', ip: '192.168.12.8', time: '2026-11-05 09:12:03', detail: '账号密码登录成功' },
    { id: 'LOG-20261104-0218', user: '林运营', action: '新增', target: 'B站课程「多智能体系统设计与落地」', ip: '192.168.12.34', time: '2026-11-04 17:55:26', detail: '来源 B站，所属学科 AI应用与智能体开发' },
    { id: 'LOG-20261104-0217', user: '陈审核', action: '审核', target: '题目 #2836', ip: '192.168.12.51', time: '2026-11-04 16:31:02', detail: '审核不通过：选项 A 与题干不符' },
    { id: 'LOG-20261104-0216', user: '林运营', action: '禁用', target: '学员「周雨桐」', ip: '192.168.12.34', time: '2026-11-04 15:08:44', detail: '原因：异常刷题行为' },
    { id: 'LOG-20261103-0188', user: '林运营', action: '删除', target: '课程「旧版 Prompt 入门」', ip: '192.168.12.34', time: '2026-11-03 14:22:19', detail: '逻辑删除，可从回收站恢复' },
    { id: 'LOG-20261103-0187', user: '超级管理员', action: '新增', target: '后台用户「周运营」', ip: '192.168.12.8', time: '2026-11-03 10:05:51', detail: '角色：运营人员' }
  ];

  /* ---------- 管理端题库（表格展示） ---------- */
  const ADMIN_QUESTIONS = ALL_QUESTIONS.map((q, i) => ({
    id: 'Q' + String(2800 + i),
    stem: q.stem,
    stageId: q.stageId,
    stageName: q.stageName,
    subjectId: q.subjectId,
    subjectName: q.subjectName,
    type: q.type,
    difficulty: q.difficulty,
    status: i % 13 === 0 ? 'pending' : i % 17 === 0 ? 'rejected' : i % 11 === 0 ? 'offline' : i % 19 === 0 ? 'draft' : 'published',
    createdAt: `2026-10-${String(10 + (i % 25)).padStart(2, '0')} ${String(9 + (i % 9)).padStart(2, '0')}:${String(10 + (i % 49)).padStart(2, '0')}`,
    options: q.options,
    answer: q.answer,
    analysis: q.analysis,
    tags: q.tags,
    people: q.people,
    correct: Math.round(q.people * q.accuracy / 100),
    accuracy: q.accuracy
  }));

  /* ---------- 首页 7 天练习 ---------- */
  const WEEK_PRACTICE = [
    { label: '10/30', value: 18 }, { label: '10/31', value: 26 }, { label: '11/01', value: 12 },
    { label: '11/02', value: 34 }, { label: '11/03', value: 22 }, { label: '11/04', value: 41 },
    { label: '今日', value: 16, today: true }
  ];

  /* ---------- 用户增长 ---------- */
  const GROWTH = {
    7: [12, 18, 15, 24, 21, 31, 28],
    30: [8, 12, 9, 15, 18, 14, 22, 19, 25, 21, 28, 24, 30, 27, 33, 29, 36, 32, 40, 35, 38, 44, 41, 47, 43, 50, 46, 53, 49, 56],
    90: Array.from({ length: 90 }, (_, i) => Math.round(6 + i * 0.42 + Math.sin(i / 3) * 6 + (i % 7 === 0 ? 8 : 0)))
  };

  global.DB = {
    SUBJECTS, STAGE_MAP, SUBJECT_MAP, ALL_QUESTIONS, QUESTION_MAP,
    COURSES, COURSE_MAP, ROUTES, ROUTE_MAP,
    INTERVIEW_BANK, ADMIN_USERS, ROLES, PERM_TREE, STUDENTS, LOGS, ADMIN_QUESTIONS,
    SEED_MISTAKES, SEED_RECORDS, WEEK_PRACTICE, GROWTH, COVERS,
    QUESTION_TYPES: { single: '单选', multiple: '多选', judge: '判断', short: '简答' },
    DIFFICULTY: { easy: '简单', medium: '中等', hard: '困难' },
    STATUS: { published: '已上架', offline: '已下架', pending: '待审核', draft: '草稿', rejected: '已驳回' },
    DIRECTIONS: SUBJECTS.map(s => ({ id: s.id, name: s.name, icon: s.icon, color: s.color, soft: s.soft }))
  };
})(window);
