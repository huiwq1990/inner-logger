����
===

����ClassLoader���룬�������õ�sl4j��logback��־ϵͳ��ȷ����������ͬJVM����������־ϵͳ��ͻ(��ʹϵͳû���κ���־ϵͳ�����Ա�֤��־�������)��֧��ָ��������logback�������ļ�����Configure,���õ��������ļ���ͻ���⡣
��ͬJVM�ж��innerLoggerͨ��appKey���и��룬����ConfigureҲ���಻Ӱ�졣

�ʺϸ��ֿͻ��˵ĳ��򣬽���ͻ��˲��𵽲�ͬϵͳ�п��ܳ��ֵ�������־ϵͳ���µ���־��ͻ�������ó�ͻ�����⡣

ʹ��
===
����������

��ʽ1����������inner-logger��jar �ŵ�CLASSPATH��
��ʽ2����inner-logger deploy��maven�ֿ���


* ��һ�� ָ��logback�����ļ����߸���LogConfigure ����ʵ�����ʼ����־ϵͳ���Ǳ����粻ָ����ʹ�����õ�loback�����ļ���

ָ�������ļ���ʽ����com.alibaba.middleware.innerlog.LoggerFactory�����ַ���ѡ��һ��

    /**
     * �÷���������ֱ�Ӹ���classPath��logback�����ļ�classpath·������ �����ļ�������־��Configure
     * 
     * @param classResource
     */
    public static void initByClassResource(final String classResource,String appKey)

    /**
     * �÷���������ֱ�Ӹ����ļ�ϵͳ��logback�����ļ�����·�� �����ļ�������־��Configure
     * 
     * @param classResource
     */
    public static void initByFileSystem(final String configFilePath,String appKey)

����ʵ�ָ��ӵ�Configure����������LogConfigure��ʵ���ࣺ

���磺

    LoggerFactory.doConfigure(new LogConfigure() {
                public InputStream configure() {
                    InputStream inputStream = LoggerFactory.class.getClassLoader()
                            .getResourceAsStream("loglib/inner-jwtask-logback.xml");
                    if (null != inputStream && StringUtils.isNotBlank(taskName)) {
                        //��ȡ�������ļ�,��taskName�滻��־�ļ���������
                        String configContext = getStringFromInputStream(inputStream);
                        String newConfigContext = StringUtils
                                .replace(configContext, "${taskName}", taskName);
                        inputStream = new ByteArrayInputStream(newConfigContext.getBytes());
                    }
                    return inputStream;
                }
            },"jingweiAppkey");

* �ڶ�����ȡlogger����

LoggerFactory.getLogger(String logName,String appKey) ���� LoggerFactory.getLogger(Class<?> clazz,String appKey) 

ʹ�����ӣ�
===

    public static void main(String[] args)  {
     	    String metaqAppKey = "metaq";
            String jwAppKey = "jingwei";
            
            //classpath�������metaq-logback.xml�������ļ�
            LoggerFactory.initByClassResource("metaq-logback.xml",metaqAppKey);
            //classpath�������jingwei-logback.xml�������ļ�
            LoggerFactory.initByClassResource("jingwei-logback.xml",jwAppKey);
            
        	Logger jwLogger = LoggerFactory.getLogger(LoggerFactory.class, jwAppKey);
        	Logger metaqLogger = LoggerFactory.getLogger(LoggerFactory.class, metaqAppKey);
        	
        	jwLogger.setLevel(LogLevel.ERROR);
        	jwLogger.error("error!!");
        	metaqLogger.setLevel(LogLevel.WARN);
        	metaqLogger.warn("warn!!");
        }

��ʾ��

����LoggerFactory�Ľӿڶ�Ҫ����appKey����ֹ��ͬJVM��ͬ�������ʹ����inner-log�������ó�ͻ,����ʹ���������ܲ��Ǻܷ��㡣

���Խ����Լ���װһ��LoggerFactory��appKey��������,������¶���ⲿ�ĽӿھͲ����ڴ���appKey��

���磺

    import com.alibaba.middleware.innerlog.LogConfigure;
    import com.alibaba.middleware.innerlog.Logger;
    import com.alibaba.middleware.innerlog.LoggerFactory;
    
    public class JwLoggerFactory {
    
        private final static String JW_LOG_APP_KEY = "JING-WEI";
    
        /**
         * �÷���������ֱ�Ӹ���classPath��logback�����ļ�classpath·������ �����ļ�������־��Configure
         *
         * @param classResource
         */
        public static void initByClassResource(final String classResource) {
            LoggerFactory.initByClassResource(classResource, JW_LOG_APP_KEY);
        }
    
        /**
         * �÷���������ֱ�Ӹ����ļ�ϵͳ��logback�����ļ�����·�� �����ļ�������־��Configure
         *
         * @param configFilePath
         */
        public static void initByFileSystem(final String configFilePath) {
            LoggerFactory.initByClassResource(configFilePath, JW_LOG_APP_KEY);
        }
    
        /**
         * ����LogConfigure����Configure��־ϵͳ, ��Ҫ��LogConfigure
         * ��ʵ�ָ��ݸ���logback�����ļ���InputStream
         *
         * @param logConfigure
         */
        public synchronized static void doConfigure(LogConfigure logConfigure) {
            LoggerFactory.doConfigure(logConfigure, JW_LOG_APP_KEY);
        }
    
        /**
         * ����logName��appKey��ȡlogger
         * @param logName ��־����
         * @return
         */
        public static Logger getLogger(String logName) {
            return LoggerFactory.getLogger(logName, JW_LOG_APP_KEY);
        }
    
        /**
         * ����class��appKey��ȡlogger
         * @param clazz ��Ҫ��ȡlogger��Ӧ��class
         * @return
         */
        public static Logger getLogger(Class<?> clazz) {
            return LoggerFactory.getLogger(clazz, JW_LOG_APP_KEY);
        }
    }
