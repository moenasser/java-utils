package com.mnasser.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Exposes the <code>shellOut</code> utilities allowing us
 * to create other sub processes on the system. 
 * 
 * This allows for a means of checking the std out, std err
 * and if there was a return status other than 0.
 * @author Moe
 *
 */
public class ShellUtils {

	public static final long PID;
	public static final String HOSTNAME;

	public static final Logger log = LoggerFactory.getLogger(ShellUtils.class);
	
	static {
		String[] pidHost = 
			java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@");
		PID = Long.parseLong(pidHost[0]);
		HOSTNAME = pidHost[1];
	}
	
    public static long getRandomLong() {
        long rand = (long) ((Math.random() * 100) * Math.pow(10, 10));
        return System.currentTimeMillis() + rand;
    }

    public static void runProcess(Logger log, String... args) {
        runProcess(log, null, null, args);
    }

    public static void runProcess(Logger log, File workDir, String... args) {
        runProcess(log, workDir, null, args);
    }
	
    public static void runProcess(Logger log, File workDir, Map<String, String> envMap, String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);   // merge stdout/stderr to one stream
            
            if (workDir != null) {
                pb.directory(workDir);
            }

            if (envMap != null) {
                Map<String, String> env = pb.environment();
                env.putAll(envMap);
            }
            
            Process p = pb.start();
            InputStream is = p.getInputStream();    // should have both stdout and stderr
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                log.info(line);
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    
    public static void runProcessThrowableAsync(final Logger log,final String... args) {  
    	new Thread(new Runnable() {
			@Override
			public void run() {
				runProcessThrowable(log,args);	
			}
		}).start();
    }
    
    public static Process runProcessThrowable(Logger log, String... args) {
    	
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            Process p = pb.start();
            InputStream is = p.getInputStream(); 
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                log.info(line);
            }
            br.close();
            is = p.getErrorStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            line = null;
            StringBuilder msg = new StringBuilder();
            while ((line = br.readLine()) != null) {
                msg.append(line).append("\n\t");
            }
            br.close();
    		if (msg.length() > 0) {
            	msg.setLength(msg.length()-2);
            	throw new RuntimeException(msg.toString());
            }

    		return p;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    //  Returns the output of this process call
    public static List<String> runProcessResultsThrowable(String... args) {
        try {
        	
            ProcessBuilder pb = new ProcessBuilder(args);
            Process p = pb.start();
            InputStream is = p.getInputStream(); 
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            List<String> results = new ArrayList<String>();
            while ((line = br.readLine()) != null) {
                results.add(line);
            }
            br.close();
            is = p.getErrorStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            line = null;
            StringBuilder msg = new StringBuilder();
            while ((line = br.readLine()) != null) {
                msg.append(line).append("\n\t");
            }
            br.close();
    		if (msg.length() > 0) {
            	msg.setLength(msg.length()-2);
            	throw new RuntimeException(msg.toString());
            }

    		return results;
    		
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Shells out your commands.  
     * Specifically calls 'sh -c arg'
     *  
     * @param arg
     * @return Any output from running the process. 
     * @throws RuntimeException if it detects anything from stderr
     */
    public static List<String> shellOut(String arg) {
    	return shellOut(null, arg);
    }
    public static List<String> shellOut(Logger log, String arg) {
        try {
        	
            ProcessBuilder pb = new ProcessBuilder("sh","-c",arg);
            Process p = pb.start();
            InputStream is = p.getInputStream(); 
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            List<String> results = new ArrayList<String>();
            while ((line = br.readLine()) != null) {
            	if( log != null )
            		log.info(line);
                results.add(line);
            }
            br.close();
            is = p.getErrorStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            line = null;
            StringBuilder msg = new StringBuilder();
            while ((line = br.readLine()) != null) {
                msg.append(line).append("\n\t");
            }
            br.close();
    		if (msg.length() > 0) {
            	msg.setLength(msg.length()-2);
            	throw new RuntimeException(msg.toString()+"\n$> "+arg+'\n');
            }

    		// Good shell programs should have an exit code. Check for non zero
    		int ret = 0;
    		
    		try { 	ret = p.waitFor();  } catch (InterruptedException e){}
    		
    		if( ret != 0 ){
    			throw new RuntimeException("Cmd exited with "+ret+" error code: "+ arg);
    		}
    		return results;
    		
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
	/**
	 * Attempts to return the PID of the current Java process
	 * on the give operating system. 
	 * 
	 * This only works because the management bean 
	 * returns 'pid@hostname'.
	 * 
	 * @return long PID of current java process running.
	 */
	public static long getPID() {
		return PID ;
	}

	/**
	 * Returns a unique string of the form 
	 * {@literal '<hostname>_<timestamp>_<pid>' }.
	 * 
	 * <b>NOTE:</b>  Calling this method successively within
	 * the same millisecond will produce the same string. 
	 * 
	 * @return String with hostname, timestamp, pid.
	 * @throws IOException
	 */
	public static String getUUID() throws IOException{
		return getSiteHostName() + '_' + System.currentTimeMillis() + '_' + getPID();
	}
	
    /**
     * Returns hostname of this machine. 
     * 
     * @return Hostname of current machine;
     */
	public static String getSiteHostName()  {
		return HOSTNAME;
	}
    
    @SuppressWarnings("unused")
	private static String makeUnqualified(String host) {
		return host.split("\\.")[0];
	}

	public static boolean isNumeric(String num){
    	if ( num == null || num.length() == 0 )
    		return false;
    	for( char c : num.toCharArray()){
    		if( c < '0' || c > '9')
    			return false;
    	}
    	return true;
    }
    

	public static boolean isGZ(String s) {
		int idx = s.lastIndexOf('.');
		if (idx >= 0) {
			return s.substring(idx+1, idx+3).toLowerCase().equals("gz");
		}
		return false;
	}
	public static boolean isZip(String s) {
		int idx = s.lastIndexOf('.');
		if (idx >= 0) {
			return s.substring(idx+1, idx+4).toLowerCase().equals("zip");
		}
		return false;
	}
	
	
	public static boolean isTar(String file) {
		Set<String> set =  new HashSet<String>(Arrays.asList(file.toLowerCase()
				.split("\\.",-1)));
		return set.contains("tar") || set.contains("tgz");
	}
	
	/**
	 * recursively deletes files when jvm shuts down
	 * @param file
	 */
	public static void registerForDelete(File file) {
        file.deleteOnExit();
        File[] subList = file.listFiles();
        if (subList != null) {
            for (File subFile : subList) {
                registerForDelete(subFile);
            }
        }
    }
	
    public static void main(String[] args) throws Exception {
    	/**
		System.out.println(isNumeric(null));
		System.out.println(isNumeric(""));
		System.out.println(isNumeric("231"));
		System.out.println(isNumeric("aqsd"));
		System.out.println(isNumeric("123 456"));
		System.out.println(java.lang.management.ManagementFactory.getRuntimeMXBean().getName());
		System.out.println(getPID());
		System.out.println(getSiteHostName());
		System.out.println(getUUID());
		System.out.println(getUUID());
		System.out.println(getUUID());
		System.out.println(System.currentTimeMillis());
		System.out.println(System.currentTimeMillis());
		System.out.println(System.currentTimeMillis());
		***/
		
		/***
		List<String> out = shellOut("/home/mnasser/data_dump.rebuild_cdb /home/mnasser/data.incremental.ws_acct.dat  /tmp/TEMP_CDB_NAME");
		String records = null;
		int size = 0;
		for( String cdb : out){
			System.out.println(cdb);
			List<String> stats = runProcessResultsThrowable("cdb","-s",cdb);
			for( String stat : stats){
				System.out.println(stat);
			}
			records = stats.get(0).split("\\:")[1].trim();
			System.out.println(records);
			size += Integer.parseInt(records);
		}
		System.out.println("Total records : " +  size);
		***/
		
		//	for( String record : shellOut("cdb -s /tmp/TEMP_CDB_NAME.shrd7.cdb  |  head -1 |  awk '{print $4}'")){
		//		System.out.println(record);
		//	}

		for( String record : shellOut("/psapp/sbin/rebuild_cdb_from_dump ")){
			System.out.println(record);
		}
    	
		// Thread.sleep(1000*5);  // check ps aux | grep java ; or jps
	}

}
