介绍
===

采用ClassLoader隔离，导入内置的sl4j和logback日志系统。确保绝不与相同JVM里其他的日志系统冲突。支持指定独立的logback的配置文件进行Configure,不用担心配置文件冲突问题。

使用
===
引入依赖：

    <dependency>
        <groupId>com.alibaba.middleware</groupId>
        <artifactId>inner-logger</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>


* 第一步 指定logback配置文件或者给出LogConfigure 具体实现类初始化日志系统（非必须如不指定会使用内置的loback配置文件）

指定配置文件方式以下com.alibaba.middleware.innerlog.LogFactory的两种方任选其一：

    /**
     * 该方法适用于直接给定classPath下logback配置文件classpath路径名称 配置文件来对日志做Configure
     * 
     * @param classResource
     */
    public static void initByClassResource(final String classResource) {
        doConfigure(new DefaultLogConfigure(classResource));
    }

    /**
     * 该方法适用于直接给定文件系统下logback配置文件绝对路径 配置文件来对日志做Configure
     * 
     * @param classResource
     */
    public static void initByFileSystem(final String configFilePath) {
        doConfigure(new DefaultLogConfigure(configFilePath, Boolean.TRUE));
    }

或者实现复杂的Configure，给出具体LogConfigure的实现类：

例如：

    LogFactory.doConfigure(new LogConfigure() {
                public InputStream configure() {
                    InputStream inputStream = LoggerFactory.class.getClassLoader()
                            .getResourceAsStream("loglib/inner-jwtask-logback.xml");
                    if (null != inputStream && StringUtils.isNotBlank(taskName)) {
                        //获取到配置文件,用taskName替换日志文件配置内容
                        String configContext = getStringFromInputStream(inputStream);
                        String newConfigContext = StringUtils
                                .replace(configContext, "${taskName}", taskName);
                        inputStream = new ByteArrayInputStream(newConfigContext.getBytes());
                    }
                    return inputStream;
                }
                public String getSystemPropertyKey() {
                     return "jw.logback.config.file";
                }
            });

* 第二步获取logger对象

LogFactory.getLogger(String logName) 或者 LogFactory.getLogger(Class<?> clazz) 

使用例子：
===

public static void main(String[] args)  {
        //classpath下有这个xxx-logback.xml的配置文件
        LogFactory.initByClassResource("xxx-logback.xml");
        Logger logger = LogFactory.getLogger(“xxxLogName”);
        logger.setLevel(LogLevel.ERROR);
        logger.error("error!!");
        logger.setLevel(LogLevel.WARN);
        logger.warn("warn!!");
        logger.setLevel(LogLevel.INFO);
        logger.info("info!!");
    }

