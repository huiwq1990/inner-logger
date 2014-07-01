����
===

����ClassLoader���룬�������õ�sl4j��logback��־ϵͳ��ȷ����������ͬJVM����������־ϵͳ��ͻ��֧��ָ��������logback�������ļ�����Configure,���õ��������ļ���ͻ���⡣

ʹ��
===
����������

    <dependency>
        <groupId>com.alibaba.middleware</groupId>
        <artifactId>inner-logger</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>


* ��һ�� ָ��logback�����ļ����߸���LogConfigure ����ʵ�����ʼ����־ϵͳ���Ǳ����粻ָ����ʹ�����õ�loback�����ļ���

ָ�������ļ���ʽ����com.alibaba.middleware.innerlog.LogFactory�����ַ���ѡ��һ��

    /**
     * �÷���������ֱ�Ӹ���classPath��logback�����ļ�classpath·������ �����ļ�������־��Configure
     * 
     * @param classResource
     */
    public static void initByClassResource(final String classResource) {
        doConfigure(new DefaultLogConfigure(classResource));
    }

    /**
     * �÷���������ֱ�Ӹ����ļ�ϵͳ��logback�����ļ�����·�� �����ļ�������־��Configure
     * 
     * @param classResource
     */
    public static void initByFileSystem(final String configFilePath) {
        doConfigure(new DefaultLogConfigure(configFilePath, Boolean.TRUE));
    }

����ʵ�ָ��ӵ�Configure����������LogConfigure��ʵ���ࣺ

���磺

    LogFactory.doConfigure(new LogConfigure() {
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
                public String getSystemPropertyKey() {
                     return "jw.logback.config.file";
                }
            });

* �ڶ�����ȡlogger����

LogFactory.getLogger(String logName) ���� LogFactory.getLogger(Class<?> clazz) 

ʹ�����ӣ�
===

public static void main(String[] args)  {
        //classpath�������xxx-logback.xml�������ļ�
        LogFactory.initByClassResource("xxx-logback.xml");
        Logger logger = LogFactory.getLogger(��xxxLogName��);
        logger.setLevel(LogLevel.ERROR);
        logger.error("error!!");
        logger.setLevel(LogLevel.WARN);
        logger.warn("warn!!");
        logger.setLevel(LogLevel.INFO);
        logger.info("info!!");
    }

