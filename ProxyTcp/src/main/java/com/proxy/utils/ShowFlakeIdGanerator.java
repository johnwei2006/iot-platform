package com.proxy.utils;

public class ShowFlakeIdGanerator {

    public static void main(String[] args) {
    	
        ShowFlakeIdGanerator snowFlake = new ShowFlakeIdGanerator(2, 3);

        for (int i = 0; i < (1<<4); i++) {
            //10进制
        	long id = snowFlake.nextRandId(8);
            System.out.println(id);
            byte[] value = long2Byte(id);
            System.out.println("Long Hex:" + bytesToHex(value));

            byte[] value2 = int2Byte(id);
            System.out.println("Int Hex:" + bytesToHex(value2));
            
//            byte[] value3 = short2Byte(id);
//            System.out.println("Short Hex:" + bytesToHex(value3));
            System.out.println("===============================");
        }
    }

    /**
     * 起始的时间戳
     */
    private final static long START_TIMESTAMP = 1480166465631L;

    /**
     * 每一部分占用的位数
     */
    private final static long SEQUENCE_BIT = 8;   //序列号占用的位数
    private final static long MACHINE_BIT = 5;     //机器标识占用的位数
    private final static long DATA_CENTER_BIT = 5; //数据中心占用的位数

    /**
     * 每一部分的最大值
     */
    private final static long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);
    private final static long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
    private final static long MAX_DATA_CENTER_NUM = -1L ^ (-1L << DATA_CENTER_BIT);

    /**
     * 每一部分向左的位移
     */
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long DATA_CENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTAMP_LEFT = DATA_CENTER_LEFT + DATA_CENTER_BIT;

    private long dataCenterId;  //数据中心
    private long machineId;     //机器标识
    private long sequence = 0L; //序列号
    private long lastTimeStamp = -1L;  //上一次时间戳

    private long getNextMill() {
        long mill = getNewTimeStamp();
        while (mill <= lastTimeStamp) {
            mill = getNewTimeStamp();
        }
        return mill;
    }

    private long getNewTimeStamp() {
        return System.currentTimeMillis();
    }

    /**
     * 根据指定的数据中心ID和机器标志ID生成指定的序列号
     *
     * @param dataCenterId 数据中心ID
     * @param machineId    机器标志ID
     */
    public ShowFlakeIdGanerator(long dataCenterId, long machineId) {
        if (dataCenterId > MAX_DATA_CENTER_NUM || dataCenterId < 0) {
            throw new IllegalArgumentException("DtaCenterId can't be greater than MAX_DATA_CENTER_NUM or less than 0！");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("MachineId can't be greater than MAX_MACHINE_NUM or less than 0！");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
    }

    public synchronized int nextRandId(int length) {
    	int min=1;
    	int max = 10;
    	for(int i = 1; i < length; i++)
    		max *=10;
  	  	return (int) (Math.random()*(max-min)+min); 
    }
    /**
     * 产生下一个ID
     *
     * @return
     */
    public synchronized long nextId() {
        long currTimeStamp = getNewTimeStamp();
        if (currTimeStamp < lastTimeStamp) {
            throw new RuntimeException("Clock moved backwards.  Refusing to generate id");
        }

        if (currTimeStamp == lastTimeStamp) {
            //相同毫秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            //同一毫秒的序列数已经达到最大
            if (sequence == 0L) {
                currTimeStamp = getNextMill();
            }
        } else {
            //不同毫秒内，序列号置为0
            sequence = 0L;
        }

        lastTimeStamp = currTimeStamp;

        return (currTimeStamp - START_TIMESTAMP) << SEQUENCE_BIT //时间戳部分
                | dataCenterId << DATA_CENTER_LEFT       //数据中心部分
                | machineId << MACHINE_LEFT             //机器标识部分
                | sequence;                             //序列号部分
    }

    
    public static byte[] long2Byte(long x) { 
    	byte[] bb = new byte[8];
        bb[ 0] = (byte) (x >> 56); 
        bb[ 1] = (byte) (x >> 48); 
        bb[ 2] = (byte) (x >> 40); 
        bb[ 3] = (byte) (x >> 32); 
        bb[ 4] = (byte) (x >> 24); 
        bb[ 5] = (byte) (x >> 16); 
        bb[ 6] = (byte) (x >> 8); 
        bb[ 7] = (byte) (x >> 0); 
        return bb;
  } 
    
    public static byte[] int2Byte(long x) { 
    	byte[] bb = new byte[4];
        bb[ 0] = (byte) (x >> 24); 
        bb[ 1] = (byte) (x >> 16); 
        bb[ 2] = (byte) (x >> 8); 
        bb[ 3] = (byte) (x >> 0); 
        return bb;
  }
    
    public static byte[] short2Byte(long x) { 
    	byte[] bb = new byte[2];
        bb[ 0] = (byte) (x >> 8); 
        bb[ 1] = (byte) (x >> 0); 
        return bb;
  } 

    public static String bytesToHex(byte[] bytes) {  
        StringBuffer sb = new StringBuffer();  
        for(int i = 0; i < bytes.length; i++) {  
            String hex = Integer.toHexString(bytes[i] & 0xFF);  
            if(hex.length() < 2){  
                sb.append(0);  
            }  
            sb.append(hex);  
        }  
        return sb.toString();  
    }
}
