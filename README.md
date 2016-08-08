##概要
> * 什么是Spring Cloud Eureka?
> * 使用Eureka获取服务调用
> * Eureka整合Spring Config Server
> * 构建Eureka Server集群


## 什么是Spring Cloud Eureka?
Spring Cloud Eureka 模块提供的功能是被动式的服务发现.
**什么是服务发现?**
服务发现就像聊天室一个,每个用户来的时候去服务器上注册,这样他的好友们就能看到你,你同时也将获取好友的上线列表.
在微服务中,服务就相当于聊天室的用户,而服务注册中心就像聊天室服务器一样,目前服务发现的解决方案有Eureka,Consul,Etcd,Zookeeper,SmartStack,等等.
![2555949490-5714a6ed90a1e_articlex][1]
本文就来讲讲Eureka,如图所示,Eureka Client通过HTTP(或者TCP,UDP)去Eureka Server注册和获取服务列表,为了高可用一般会有多个Eureka Server组成集群.Eureka会移除那些心跳检查未到达的服务.

## 使用Eureka获取服务调用
这节我们将构建一个Eureka Server,5个Eureka Client(分别提供主语,动词,量词,形容词,名词服务),再构建一个Sentence Eureka Client 来用前面五个服务造句.

1.创建mmb-eureka-server
- 添加依赖-spring-cloud-starter-parent,spring-cloud-starter-eureka-server(pom.xml)
```
 <parent>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-parent</artifactId>
        <version>Brixton.SR4</version>
        <relativePath/>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-eureka-server</artifactId>
        </dependency>
    </dependencies>
```
 - 配置应用信息-端口和应用名称 application.yml
```
server:
  port: 8010

spring:
  application:
    name: mmb-eureka-server
```
 - 启动服务
```
@SpringBootApplication
@EnableEurekaServer
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```
- 打开管理页面,检查是否成功

![图片描述][2]
2.创建mmb-eureka-client

 - 添加依赖-spring-cloud-starter-parent,spring-cloud-starter-eureka (pom.xml)
```
  <parent>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-parent</artifactId>
        <version>Brixton.SR4</version>
        <relativePath/>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-eureka</artifactId>
        </dependency>
    </dependencies>

```
 - 配置应用信息-eureka server信息,实际使用的words信息,端口号 (application.yml)
```
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8010/eureka/

words: 你,我,他

server:
  port: ${PORT:${SERVER_PORT:0}}
#  这个的意思是随机指定个没使用的端口
```
 - 配置启动信息-应用名称 (bootstrap.xml)
```
spring:
  application:
    name: mmb-eureka-client-subject
```
 - 添加Controller-随机获取words中的一条
```
@RestController
public class Controller {

   @Value("${words}") String words;

    @RequestMapping("/")
    public  String getWord() {
        String[] wordArray = words.split(",");
        int i = (int)Math.round(Math.random() * (wordArray.length - 1));
        return wordArray[i];
    }
}
```
 - 启动服务
```
@SpringBootApplication
@EnableEurekaClient
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

- 访问127.0.0.1/port(看日志可以得到各个应用的port) 看到words里的词就启动成功了,

其它的verb,acticle,adjective,noun工程类似,就把words,和spring.application.name改成对应的工程名字就好了

3.创建sentence工程
- 添加依赖-spring-cloud-starter-parent,spring-cloud-starter-eureka,spring-boot-starter-web,spring-boot-starter-actuator (pom.xml)
```
  <parent>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-parent</artifactId>
        <version>Brixton.SR4</version>
        <relativePath/>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-eureka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
```
 - 配置应用信息-eureka server和端口号 (application.yml)
```
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8010/eureka/

server:
  port: 8020
```
- 配置启动信息-应用名称 (bootstrap.yml)
```
spring:
  application:
    name: mmb-eureka-sentence
```
 - 添加Controller-用其他eureka-clients(subject,verb,acticle,adjective,noun)的各个服务造句
```
@RestController
public class Controller {

    @Autowired
    DiscoveryClient client;

    @RequestMapping("/sentence")
    public  String getSentence() {
        return
                getWord("mmb-eureka-client-subject") + " "
                        + getWord("MMB-EUREKA-CLIENT-VERB") + " "
                        + getWord("mmb-eureka-client-article") + " "
                        + getWord("mmb-eureka-client-adjective") + " "
                        + getWord("mmb-eureka-client-noun") + "."
                ;//大小写不区分
    }

    public String getWord(String service) {
        List<ServiceInstance> list = client.getInstances(service);
        if (list != null && list.size() > 0 ) {
            URI uri = list.get(0).getUri();
            if (uri !=null ) {
                return (new RestTemplate()).getForObject(uri,String.class);
            }
        }
        return null;
    }
}

```
 - 启动服务
```
@SpringBootApplication
@EnableEurekaServer
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```
  - 先启动Eureka Server,再启动Eureka Client,在管理页面上看到服务都起成功时,访问127.0.0.1/8020/sentence 可以得到一个随机组成的句子
  

## Eureka整合Spring Config Server

 1. 在git的repository里添加application.yml
```
 eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8010/eureka/
```
 2. 启动实战(一)中的Spring Cloud Config Server
 3. 修改各个client的配置
    - application.yml移除属性eureka.client.serviceUrl.defaultZone
    - bootstrap.yml添加属性  spring.cloud.config.uri: http://localhost:8001
    - pom.xml添加依赖spring-cloud-config-client
 4. 依次启动Config Server,Eureka Server,Eureka Client,在管理页面上看到服务都起成功时,访问127.0.0.1/8020/sentence 可以得到一个随机组成的句子
  -
 5. 如果你想把words信息也放入repository呢?在application.yml中添加,如下信息,各个client启动的时候加上-Dspring.profiles.active对应到相应的启动参数就行了.
```
    ---
  spring:
    profiles: subject
  words: I,You,He,She,It
  
  ---
  spring:
    profiles: verb
  words: ran,knew,had,saw,bought

  ---
  spring:
    profiles: article
  words: a,the

  ---
  spring:
    profiles: adjective
  words: reasonable,leaky,suspicious,ordinary,unlikely

  ---
  spring:
    profiles: noun
  words: boat,book,vote,seat,backpack,partition,groundhog  
```

## 构建Eureka Server集群

- host文件中添加 (c:\WINDOWS\system32\drivers\etc\hosts). 

```
  127.0.0.1       eureka-primary
  127.0.0.1       eureka-secondary
  127.0.0.1       eureka-tertiary
```
- Eureka Server的application.yml添加多个profiles,和instanceId
```
 ---
spring:
  application:
    name: eureka-server-clustered   
  profiles: primary
server:
  port: 8011  
eureka:
  instance:
    hostname: eureka-primary       
  ---
spring:
  application:
    name: eureka-server-clustered      
  profiles: secondary
server:
  port: 8012
eureka:
  instance:
    hostname: eureka-secondary       
 ---
spring:
  application:
    name: eureka-server-clustered      
  profiles: tertiary
server:
  port: 8013
eureka:
  instance:
    hostname: eureka-tertiary       
```
- 此时Eureka Server 同时也是个Eureka Client,需要设置eureka.client.serviceUrl.defaultZone,值是另外两个,最终会是下面这样
```
---
spring:
  application:
    name: eureka-server-clustered   
  profiles: primary
server:
  port: 8011  
eureka:
  instance:
    hostname: eureka-primary       
  client:
    registerWithEureka: true
    fetchRegistry: true        
    serviceUrl:
      defaultZone: http://eureka-secondary:8012/eureka/,http://eureka-tertiary:8013/eureka/

---
spring:
  application:
    name: eureka-server-clustered      
  profiles: secondary
server:
  port: 8012
eureka:
  instance:
    hostname: eureka-secondary       
  client:
    registerWithEureka: true
    fetchRegistry: true        
    serviceUrl:
      defaultZone: http://eureka-tertiary:8013/eureka/,http://eureka-primary:8011/eureka/

---
spring:
  application:
    name: eureka-server-clustered      
  profiles: tertiary
server:
  port: 8013
eureka:
  instance:
    hostname: eureka-tertiary       
  client:
    registerWithEureka: true
    fetchRegistry: true    
    serviceUrl:
      defaultZone: http://eureka-primary:8011/eureka/,http://eureka-secondary:8012/eureka/      
```
- 以-Dspring.profiles.active=primary (and secondary, and tertiary)为启动参数分别启动Eureka Server
- 修改所有Eureka Client的eureka.client.serviceUrl.defaultZone值为http://eureka-primary:8011/eureka/,http://eureka-secondary:8012/eureka/,http://eureka-tertiary:8013/eureka/(逗号分隔,无空白),集群启动成功登录管理页面查看,如下图所示即成功
![图片描述][3]
- 再启动所有的Eureka Clients,查看http://localhost:8020/sentence 是否成功
- 为了测试容错性,关掉两个Eureka Client,重启若干个Eureka Client,观察启动是否报错,再去查看查看http://localhost:8020/sentence 是否成功


> 特别感谢 kennyk65
> Spring Cloud 中文用户组 31777218
> [Spring-Cloud-Config  官方文档-中文译本][4] (本人有参与,哈哈)
> [Spring Cloud Netflix 官网文档-中文译本][5]
> 本文实例github地址 [mmb-eureka][6]


  [1]: /img/bVzXZP
  [2]: /img/bVzX0q
  [3]: /img/bVzX0N
  [4]: http://www.xyuu.cn/spring-cloud-config-zhcn.html
  [5]: http://www.xyuu.cn/spring-cloud-netflix-zhcn.html
  [6]: https://github.com/mumubin/mmb-eureka
