package com.ds.jvm.agent;

import java.io.IOException;
import java.lang.management.MemoryUsage;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.ds.jvm.agent.AgentConstants.CMD_APPLY_RULES;
//import static com.ds.jvm.agent.AgentConstants.CMD_DISCONNECT;
import static com.ds.jvm.agent.AgentConstants.CMD_GC;
import static com.ds.jvm.agent.AgentConstants.CMD_GET_MEMORY_INFO;
import static com.ds.jvm.agent.AgentConstants.CMD_GET_RUNTIME_INFO;
import static com.ds.jvm.agent.AgentConstants.CMD_GET_THREAD_INFO;
import static com.ds.jvm.agent.AgentConstants.CMD_LIST_CLASSES;
import static com.ds.jvm.agent.AgentConstants.CMD_RESET_STATS;
import static com.ds.jvm.agent.AgentConstants.CMD_SET_THREAD_MONITORING;
import static com.ds.jvm.agent.AgentConstants.CMD_SNAPSHOT;
import static com.ds.jvm.agent.AgentConstants.COMMAND_ACK;
import static com.ds.jvm.agent.AgentConstants.STATUS_UNKNOWN_CMD;

import com.ds.jvm.agent.Config;



public class HttpServer {
    
	
	private ServerSocketChannel serverSocketChannel=null;
	private ExecutorService executorService;
	//private static final int POOL_MULTIPLE=4;
	
	public HttpServer() throws IOException{
		executorService=Executors.newFixedThreadPool(1);
		serverSocketChannel=ServerSocketChannel.open();
		serverSocketChannel.socket().setReuseAddress(true);
		serverSocketChannel.socket().bind(new InetSocketAddress(Config.port));
		
	}
	
	public void service(){
		
		while(true){
			SocketChannel socketChannel=null;
			try{
				socketChannel=serverSocketChannel.accept();
				executorService.execute(new Handler(socketChannel));
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
	
	
	class Handler implements Runnable{
		
		private SocketChannel socketChannel;
		public Handler(SocketChannel socketChannel){
			this.socketChannel=socketChannel;
		}

		@Override
		public void run() {
			handle(socketChannel);
		}
		
		public void handle(SocketChannel socketChannel){
			try{
				Socket socket=socketChannel.socket();
				System.out.println("");
				
				ByteBuffer buffer=ByteBuffer.allocate(1024);
				socketChannel.read(buffer);
				buffer.flip();
				String request=decode(buffer);
				
				StringBuffer sb=new StringBuffer("HTTP/1.1 200 OK\r\n");
				sb.append("Content-Type:text/html\r\n\r\n");
				int c_cmd=-1;
				
				int end=request.indexOf("\r\n");
				if(end!=-1){
					String firstLineOfRequest=request.substring(0, end);
					socketChannel.write(encode(sb.toString()));
					firstLineOfRequest=firstLineOfRequest.substring(request.indexOf("/")+1,request.indexOf("/")+2);
				    try{
				    	c_cmd=Integer.parseInt(firstLineOfRequest.trim());
				    }catch(Exception e){
				    	
				    }
				}
				
				switch(c_cmd){
				case CMD_GC:
					System.gc();
					Log.print(0, "system.gc...");
					socketChannel.write(encode(COMMAND_ACK+"\n"));
					break;
				case CMD_SNAPSHOT:
					socketChannel.write(encode(COMMAND_ACK+"\n"));
					StringBuilder snapshotStrBuilder=new StringBuilder("");
					ThreadProfiler.createSnapshot(snapshotStrBuilder);
					socketChannel.write(encode(snapshotStrBuilder.toString()));
					Log.print(0, "CMD_SNAPSHOT");
					break;
					
				case CMD_RESET_STATS:
					 ThreadProfiler.resetStats();
					 socketChannel.write(encode(COMMAND_ACK+"\n"));
					 Log.print(0, "cmd Reset Stats");
					break;
					
				case CMD_APPLY_RULES:
					socketChannel.write(encode(COMMAND_ACK+"\n"));
					String opts="";
					String rules="";
					final int[] progress=new int[1];
					Transformer.Callback callback = new Transformer.Callback() {
                        public void notifyClassTransformed(String className,
                                                           int backSequence,
                                                           int bachSize) {
                            // This will be called from the transformer thread: be careful
                            progress[0] = backSequence;
                        }
                    };
                    int n = Agent.startNewSession(opts, rules, callback);
                    
                    StringBuilder applyRulesStrBuilder=new StringBuilder("");
                    applyRulesStrBuilder.append(n+"\n");
                    applyRulesStrBuilder.append("@["+progress.length+"\n");
                    while(progress[0]<n){
                    	try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    	applyRulesStrBuilder.append(progress[0]+"\n");
                    }
                    applyRulesStrBuilder.append("-1\n");
                    synchronized (Agent.waitConnectionLock) { 
                        Agent.waitConnectionLock.notifyAll();
                    }

					break;
				case CMD_LIST_CLASSES:
					socketChannel.write(encode(COMMAND_ACK+""));
					Class[] classes=Agent.getLoadedClasses(true);
					StringBuilder strBuilder=new StringBuilder(classes.length+"\n");
					synchronized (Agent.modifiedClassNames){
						for(int i=0;i<classes.length;i++){
							strBuilder.append(classes[i].getName()+"\n");
							 final boolean instrumented = Agent.modifiedClassNames
		                                .contains(classes[i].getName());
							 strBuilder.append(instrumented+"\n");
						}
					}
					socketChannel.write(encode(strBuilder.toString()));
					break;
				case CMD_GET_RUNTIME_INFO:
					socketChannel.write(encode(COMMAND_ACK+"\n"));
					StringBuilder runtimeStrBuilder=new StringBuilder("");
					runtimeStrBuilder.append(Agent.rtbean.getBootClassPath()+"\n");
					runtimeStrBuilder.append(Agent.rtbean.getClassPath()+"\n");
					List<String> inputArguments=Agent.rtbean.getInputArguments();
					runtimeStrBuilder.append("@L"+inputArguments.size()+"\n");
					for(String v:inputArguments){
						runtimeStrBuilder.append(v+"\n");
					}
					runtimeStrBuilder.append(Agent.rtbean.getLibraryPath()+"\n");
					runtimeStrBuilder.append(Agent.rtbean.getName()+"\n");
					runtimeStrBuilder.append(Agent.rtbean.getVmName()+"\n");
					runtimeStrBuilder.append(Agent.rtbean.getStartTime()+"\n");
					runtimeStrBuilder.append(Agent.rtbean.getUptime()+"\n");
					Map<String,String> map=Agent.rtbean.getSystemProperties();
					runtimeStrBuilder.append("@M"+map.size()+"\n");
					for(String v:map.keySet()){
						runtimeStrBuilder.append(v+":"+map.get(v)+"\n");
					}
					socketChannel.write(encode(runtimeStrBuilder.toString()));
					break;
				case CMD_GET_MEMORY_INFO:
					socketChannel.write(encode(COMMAND_ACK+"\n"));
					MemoryUsage memoryUsage01=Agent.membean.getHeapMemoryUsage();
					MemoryUsage memoryUsage02=Agent.membean.getNonHeapMemoryUsage();
					String heapMemoryUsage="heap_memory_usage init:"+memoryUsage01.getInit()+",used:"+memoryUsage01.getUsed()+",committed:"+memoryUsage01.getCommitted()+",max:"+memoryUsage01.getMax()+"\n";
					String noHeapMemoryUsage="no_heap_memory_usage init:"+memoryUsage02.getInit()+",used:"+memoryUsage02.getUsed()+",committed:"+memoryUsage02.getCommitted()+",max:"+memoryUsage02.getMax()+"\n";
					String objectPendingFinalizationCount="object_pending_finalization_count "+Agent.membean.getObjectPendingFinalizationCount()+"\n";
					socketChannel.write(encode(heapMemoryUsage+noHeapMemoryUsage+objectPendingFinalizationCount+"\n"));
					break;
				case CMD_GET_THREAD_INFO :
					socketChannel.write(encode(COMMAND_ACK+"\n"));
					long[] ids={};
					int maxDepth=100;
					ids=Agent.threadbean.getAllThreadIds();
					
					ThreadInfo[] tInfos=ServerUtil.makeSerializable(Agent.threadbean.getThreadInfo(ids, maxDepth));

					StringBuilder threadInfoStrBuffer=new StringBuilder();
					socketChannel.write(encode("@["+tInfos.length+"\n"));
					for(ThreadInfo threadInfo:tInfos){
						threadInfoStrBuffer.append(threadInfo.toString()+"\n");
					}
					socketChannel.write(encode(threadInfoStrBuffer.toString()));
					
					break;
					
				case CMD_SET_THREAD_MONITORING :
					boolean[] flags={true,true};
					boolean[] support= new boolean[]{
                            Agent.threadbean.isThreadContentionMonitoringSupported(),
                            Agent.threadbean.isThreadCpuTimeSupported()};
					if (support[0]) {
                        Agent.threadbean.setThreadContentionMonitoringEnabled(flags[0]);
                    }
                    if (support[1]) {
                        Agent.threadbean.setThreadCpuTimeEnabled(flags[1]);
                    }
                    socketChannel.write(encode(COMMAND_ACK+"\n"));

					break;
					
				default :
					socketChannel.write(encode(STATUS_UNKNOWN_CMD+"\n"));
					break;
				}
				
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				try{
					if(socketChannel!=null){
						socketChannel.close();
					}
					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		
		private Charset charset = Charset.forName("GBK");

		public String decode(ByteBuffer buffer) { // ½âÂë
			CharBuffer charBuffer = charset.decode(buffer);
			return charBuffer.toString();
		}

		public ByteBuffer encode(String str) {// ±àÂë
			return charset.encode(str);
		}
		
	}


	

}
