# 科百云田 · 苹果风设计系统（双端统一）

> 依据《科百云田小程序交互原型-1120》制定。token 定义在 `web_vue3/src/styles/theme.css` 与 `mini_uniapp/src/uni.scss`，命名一致、色值一致，双端共享同一设计语言。

## 1. 设计定位

- **用户**：种植户（含中老年）、园区管理员；任务场景为巡棚看数、控制设备、处理任务告警。
- **气质**：Apple HIG 的克制与层级 + 园艺绿品牌；清新、可信、适老。
- **记忆点**：品牌绿瓦片 + 毛玻璃页头 + 大字号适老模式。

## 2. 色板 token

| Token | 值 | 用途 |
|---|---|---|
| `--kb-brand` | `#17573F` | 主品牌绿：主按钮、选中态、数字强调 |
| `--kb-brand-tint` | `#1F7A56` | 主色 hover |
| `--kb-brand-bright` | `#35A476` | 图表线、进度 |
| `--kb-brand-soft` | `#BAE3CA` | 浅色强调底 |
| `--kb-brand-wash` | `#E9F4EC` | 成功/选中浅底 |
| `--kb-bg-page` | `#F5F7F4` | 页面底 |
| `--kb-bg-card` | `#FFFFFF` | 卡片 |
| `--kb-bg-sunken` | `#EFF5EF` | 分段控件底、表头 |
| `--kb-text` / `--kb-text-muted` | `#142C24` / `#52665D` | 主文 / 次文 |
| `--kb-danger(-bg)` | `#C24034` / `#FBECE9` | 超限红 |
| `--kb-warning(-bg)` | `#A96A1F` / `#FFF1DF` | 警告橙 |
| `--kb-offline` | `#8A9A91` | 离线灰 |
| `--kb-link` | `#0A84FF` | iOS 系统蓝，仅链接外跳 |
| `--kb-line` | `#D8E2D9` | 1px 分隔线 |

深色模式由主题设置弹层手动切换（`html[data-theme="dark"]`），未显式指定时跟随系统；浅色为默认档。

**Web 管理端外壳**：深色顶栏（`--kb-topbar-bg`）承产品级一级导航（大屏 / 物联网 / 系统管理 / 农事管理），当前产品用园艺绿高亮下划线；左侧白底菜单显示当前产品的授权菜单（`--kb-sidebar-width=208px`，当前项 `--kb-brand-wash`）；右上角工具区统一为圆形图标按钮 + 红点角标 + 头像和「用户名 | 机构」文本，帮助/主题/消息均用同款 popover 弹层。

## 3. 圆角 / 阴影 / 动效

- 圆角：卡片 16 / 瓦片 14 / 按钮 12 / 输入 10 / 弹层 20 / 胶囊 999。
- 阴影：卡片 `0 8px 24px rgba(20,44,36,.08)`；弹层 `0 12px 32px .12`。禁止重阴影。
- 动效：`240ms cubic-bezier(.22,1,.36,1)`（ease-out）；卡片按压 `scale(.96) + opacity .92`；页面转场淡入上移 6px。

## 4. 字体与适老大字号

- 字体栈：系统栈（PingFang SC 优先）；标题字距 -0.4px；数值用大号加粗强调。
- 标准档：heading 28 / subheading 17 / body 15 / caption 13 / 指标数值 27。
- 大字号档（小程序根节点挂 `.kb-large`）：body 15→18、caption 13→15、指标数值 27→31。
- Web 端适老通过 `documentElement.dataset.fs=large` + `html[data-fs=large] { --kb-fs-body: 18px; }` 切换。

## 5. 组件规范

- **页头毛玻璃**：web 用 `.kb-glass-header`（backdrop-filter）；小程序用自定义 NavBar + 渐变遮罩近似。
- **分段控件**：web 用 `.kb-segmented`（替代 el-radio-group 多按钮）；小程序自写 Segmented 组件。
- **实时瓦片三态**：`.kb-tile` + `--hot / --warn / --offline` 修饰类（正常=白底品牌绿数值）。
- **底部 4 tab**：看数据 / 控设备 / 消息 / 我的；消息 tab 带未读角标（红点 `#A93B2D`）。
- **toast**：底部居中深色胶囊（`#203B31`）。
- Element Plus 组件经 `--el-*` 覆盖自动继承品牌绿；无需逐组件改样式。

## 6. 双端一致性校验清单

1. 色值、圆角、阴影、动效曲线 token 命名与值逐一对照。
2. 同一页面（数据总览/设备控制/消息）双端信息架构一致，仅布局密度不同。
3. 瓦片三态、任务超时态、告警持续/已恢复的语义色两端相同。
4. 大字号模式两端均可用且不破坏布局。