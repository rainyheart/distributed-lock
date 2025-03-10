# Distributed Lock Configuration Guide

## ZooKeeper Configuration Parameters

### Core Configuration
| Parameter | Description | Default Value | Example |
|-----------|-------------|---------------|---------|
| hostPort | ZooKeeper server address and port | None | localhost:2181 |
| sessionTimeout | Session timeout in milliseconds | 30000 | 30000 |
| appName | Application name | None | myapp |
| clientConnectCount | Client reconnection attempts | 3 | 3 |
| adminAuth | Administrator authentication info | None | admin:password |

### Environment Variables
| Variable | Description | Default Value | Example |
|----------|-------------|---------------|---------|
| DISTRIBUTED_LOCK_THREAD_INTERVAL | Lock wait interval (ms) | 500 | 500 |

### Advanced Configuration
Different configurations for different environments:
```properties
# Development Environment
hostPort=localhost:2181
sessionTimeout=30000
clientConnectCount=3

# Production Environment
hostPort=zk1:2181,zk2:2181,zk3:2181
sessionTimeout=60000
clientConnectCount=5
```

## Configuration Best Practices
1. Session Timeout Settings:
   - Development: 30 seconds is sufficient
   - Production: Recommend 60 seconds or more

2. Reconnection Count Settings:
   - Development: 3 attempts is sufficient
   - Production: Recommend 5 or more attempts

3. ZooKeeper Cluster:
   - Production environment should use 3 or more nodes
   - Configure multiple ZooKeeper server addresses, separated by commas

4. Authentication Configuration:
   - Authentication must be configured in production
   - Passwords should be sufficiently complex
