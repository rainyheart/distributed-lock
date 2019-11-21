# distributed-lock

This is a open source project for distributed lock. I have implemented the Zookeeper implementation based on the distributed-lock-api project. Any contribution is welcome!

## How to use the distributed lock

Step 1:

Add below 2 dependency to your project pom.xml file, please refer to the release version in the packages.

``` xml
<dependency>
    <groupId>org.rainyheart</groupId>
    <artifactId>distributed-lock-api</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>org.rainyheart</groupId>
    <artifactId>distributed-lock-zk-impl</artifactId>
    <version>1.0.0</version>
</dependency>
```

Step 2:

Import the distributed-lock-spring.xml into your spring configuration (Either a or b listed below is ok)

a) web.xml:

```xml
<context-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>spring-confs-in-your-project.xml,classpath*:/distributed-lock-spring.xml</param-value>
</context-param>
```

b) spring.xml:

```xml
<import resource="classpath*:distributed-lock-spring.xml"/>
```

Step 3:

Add the zookeeper.properties to the folder under classpath:profiles/${Env}/

Please take distributed-lock-zk-impl/src/test/resources/profiles/test/zookeeper.properties for reference

Step 4:

Make sure your spring configuration is using below proxy method:

```xml
<aop:aspectj-autoproxy proxy-target-class="true"/>
```

Step 5 (optional):

Log4j Appender: DistributedLockApiImplLog

```xml
<appender name="DistributedLockApiImplLog" class="org.apache.log4j.DailyRollingFileAppender">
    <param name="File" value="/path/to/log/distributedLockApiImpl.log" />
    <param name="Append" value="true" />
    <param name="Threshold" value="debug" />
    <param name="DatePattern" value="'.'yyyy-MM-dd-HH'.log'" />
    <layout class="org.apache.log4j.PatternLayout">
        <param name="ConversionPattern" value="%-d{yyyy-MM-dd HH:mm:ss} [%t] [%c]-[%p] - %m%n" />
    </layout>
</appender>
```

Logger DistributedLockApiImpl & ZooKeeperManager:

```xml
<logger name="org.rainyheart.api.impl.DistributedLockApiImpl">
    <level value="info" />
    <appender-ref ref="DistributedLockApiImplLog"></appender-ref>
</logger>

<logger name="org.rainyheart.distributed.lock.thridparty.zk.ZooKeeperManager">
    <level value="info" />
    <appender-ref ref="DistributedLockApiImplLog"></appender-ref>
</logger>
```

## Example to use the distributed lock in 2 ways

a) The most simple way is to use the annotation "@DistributedLock" in your java method, refer to: DistributedLockTestController and DistributedLockTestControllerTest

b) The customized way is to inject the DistributedLockApi into your spring proxy classes, then call lock() / tryLock() / unlock() methods, refer to: ZookeeperManagerTest