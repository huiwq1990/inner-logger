介绍
===

采用ClassLoader隔离，导入内置的sl4j和logback日志系统。确保绝不与相同JVM里其他的日志系统冲突(即使系统没有任何日志系统都可以保证日志可以输出)。支持指定独立的logback的配置文件进行Configure,不用担心配置文件冲突问题。
相同JVM中多个innerLogger通过appKey进行隔离，各自Configure也互相不影响。

适合各种客户端的程序，解决客户端部署到不同系统中可能出现的其他日志系统导致的日志冲突或者配置冲突等问题。

使用
===
引入依赖：

方式1：将打包后的inner-logger的jar 放到CLASSPATH中
方式2：将inner-logger deploy到maven仓库中


* 第一步 指定logback配置文件或者给出LogConfigure 具体实现类初始化日志系统（非必须如不指定会使用内置的loback配置文件）

指定配置文件方式以下com.alibaba.middleware.innerlog.LoggerFactory的两种方任选其一：

    /**
     * 该方法适用于直接给定classPath下logback配置文件classpath路径名称 配置文件来对日志做Configure
     * 
     * @param classResource
     */
    public static void initByClassResource(final String classResource,String appKey)

    /**
     * 该方法适用于直接给定文件系统下logback配置文件绝对路径 配置文件来对日志做Configure
     * 
     * @param classResource
     */
    public static void initByFileSystem(final String configFilePath,String appKey)

或者实现复杂的Configure，给出具体LogConfigure的实现类：

例如：

    LoggerFactory.doConfigure(new LogConfigure() {
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
            },"jingweiAppkey");

* 第二步获取logger对象

LoggerFactory.getLogger(String logName,String appKey) 或者 LoggerFactory.getLogger(Class<?> clazz,String appKey) 

使用例子：
===

    public static void main(String[] args)  {
     	    String metaqAppKey = "metaq";
            String jwAppKey = "jingwei";
            
            //classpath下有这个metaq-logback.xml的配置文件
            LoggerFactory.initByClassResource("metaq-logback.xml",metaqAppKey);
            //classpath下有这个jingwei-logback.xml的配置文件
            LoggerFactory.initByClassResource("jingwei-logback.xml",jwAppKey);
            
        	Logger jwLogger = LoggerFactory.getLogger(LoggerFactory.class, jwAppKey);
        	Logger metaqLogger = LoggerFactory.getLogger(LoggerFactory.class, metaqAppKey);
        	
        	jwLogger.setLevel(LogLevel.ERROR);
        	jwLogger.error("error!!");
        	metaqLogger.setLevel(LogLevel.WARN);
        	metaqLogger.warn("warn!!");
        }

提示：

由于LoggerFactory的接口都要求传入appKey来防止相同JVM不同的组件都使用了inner-log导致配置冲突,所以使用起来可能不是很方便。

所以建议自己包装一下LoggerFactory将appKey当做常量,这样暴露给外部的接口就不用在传入appKey。

例如：

    import com.alibaba.middleware.innerlog.LogConfigure;
    import com.alibaba.middleware.innerlog.Logger;
    import com.alibaba.middleware.innerlog.LoggerFactory;
    
    public class JwLoggerFactory {
    
        private final static String JW_LOG_APP_KEY = "JING-WEI";
    
        /**
         * 该方法适用于直接给定classPath下logback配置文件classpath路径名称 配置文件来对日志做Configure
         *
         * @param classResource
         */
        public static void initByClassResource(final String classResource) {
            LoggerFactory.initByClassResource(classResource, JW_LOG_APP_KEY);
        }
    
        /**
         * 该方法适用于直接给定文件系统下logback配置文件绝对路径 配置文件来对日志做Configure
         *
         * @param configFilePath
         */
        public static void initByFileSystem(final String configFilePath) {
            LoggerFactory.initByClassResource(configFilePath, JW_LOG_APP_KEY);
        }
    
        /**
         * 利用LogConfigure类来Configure日志系统, 需要在LogConfigure
         * 中实现根据给出logback配置文件的InputStream
         *
         * @param logConfigure
         */
        public synchronized static void doConfigure(LogConfigure logConfigure) {
            LoggerFactory.doConfigure(logConfigure, JW_LOG_APP_KEY);
        }
    
        /**
         * 根据logName和appKey获取logger
         * @param logName 日志名称
         * @return
         */
        public static Logger getLogger(String logName) {
            return LoggerFactory.getLogger(logName, JW_LOG_APP_KEY);
        }
    
        /**
         * 根据class和appKey获取logger
         * @param clazz 需要获取logger对应的class
         * @return
         */
        public static Logger getLogger(Class<?> clazz) {
            return LoggerFactory.getLogger(clazz, JW_LOG_APP_KEY);
        }
    }
