# Distributed Lock 配置参数说明

## ZooKeeper配置参数

### 核心配置
| 参数名 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| hostPort | ZooKeeper服务器地址和端口 | 无 | localhost:2181 |
| sessionTimeout | 会话超时时间(毫秒) | 30000 | 30000 |
| appName | 应用名称 | 无 | myapp |
| clientConnectCount | 客户端重连次数 | 3 | 3 |
| adminAuth | 管理员认证信息 | 无 | admin:password |

### 环境变量配置
| 环境变量 | 说明 | 默认值 | 示例 |
|----------|------|--------|------|
| DISTRIBUTED_LOCK_THREAD_INTERVAL | 锁等待间隔(毫秒) | 500 | 500 |

### 高级配置
建议在不同环境下使用不同的配置：
```properties
# 开发环境
hostPort=localhost:2181
sessionTimeout=30000
clientConnectCount=3

# 生产环境
hostPort=zk1:2181,zk2:2181,zk3:2181
sessionTimeout=60000
clientConnectCount=5
```

## 配置最佳实践
1. 会话超时设置：
   - 开发环境：30秒足够
   - 生产环境：建议60秒以上

2. 重连次数设置：
   - 开发环境：3次足够
   - 生产环境：建议5次以上

3. ZooKeeper集群：
   - 生产环境建议使用3个或以上节点的集群
   - 配置多个ZooKeeper服务器地址，用逗号分隔

4. 认证配置：
   - 生产环境必须配置认证信息
   - 密码需要足够复杂
