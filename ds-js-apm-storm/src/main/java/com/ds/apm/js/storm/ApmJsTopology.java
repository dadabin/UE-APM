package com.ds.apm.js.storm;

import backtype.storm.topology.TopologyBuilder;

import java.util.Map;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class ApmJsTopology {
	
	
	
	public static void main(String[] args)throws Exception{
		TopologyBuilder builder=new TopologyBuilder();
		Config conf=new Config();
		conf.setDebug(true);
		String name=ApmJsTopology.class.getSimpleName();
		
		String zks01="";
		String zks02="";
		String zks03="";
		String topic="";
		String zkRoot="";
		String id="";
		String zks="";
		
		builder.setSpout("spout", new ApmJsKafkaSpout(zks, topic, "ttt"),2);
		
		//统计地域访问量
		builder.setBolt("areaSplitter",new AreaSplitter(),2 ).shuffleGrouping("spout");
		builder.setBolt("areaCounter", new AreaCounter(),2).fieldsGrouping("areaSplitter", new Fields("area"));
		
		//统计浏览器
		builder.setBolt("browsersSplitter", new BrowsersSplitter(),2).shuffleGrouping("spout");
		builder.setBolt("browsersCounter", new BrowsersCounter(),2).fieldsGrouping("browsersSplitter",new Fields("browsersType"));
		
		//错误页面统计量
		builder.setBolt("jsErrorSplitter", new JsErrorSplitter(),2).shuffleGrouping("spout");
		builder.setBolt("jsErrorCounter", new JsErrorCounter(),2).fieldsGrouping("jsErrorSplitter", new Fields("page_url"));
		
		
		
		
		
		
	}
	
	//================地区
	
	public static class AreaSplitter extends BaseRichBolt{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void execute(Tuple arg0) {
			// TODO Auto-generated method stub
			
		}

		public void prepare(Map arg0, TopologyContext arg1, OutputCollector arg2) {
			// TODO Auto-generated method stub
			
		}

		public void declareOutputFields(OutputFieldsDeclarer arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public static class AreaCounter extends BaseRichBolt{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void execute(Tuple arg0) {
			// TODO Auto-generated method stub
			
		}

		public void prepare(Map arg0, TopologyContext arg1, OutputCollector arg2) {
			// TODO Auto-generated method stub
			
		}

		public void declareOutputFields(OutputFieldsDeclarer arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	//================浏览器统计
	public static class BrowsersSplitter extends BaseRichBolt{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void execute(Tuple arg0) {
			// TODO Auto-generated method stub
			
		}

		public void prepare(Map arg0, TopologyContext arg1, OutputCollector arg2) {
			// TODO Auto-generated method stub
			
		}

		public void declareOutputFields(OutputFieldsDeclarer arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public static class BrowsersCounter extends BaseRichBolt{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void execute(Tuple arg0) {
			// TODO Auto-generated method stub
			
		}

		public void prepare(Map arg0, TopologyContext arg1, OutputCollector arg2) {
			// TODO Auto-generated method stub
			
		}

		public void declareOutputFields(OutputFieldsDeclarer arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}

	//================页面错误统计
	
	public static class JsErrorSplitter extends BaseRichBolt{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void execute(Tuple arg0) {
			// TODO Auto-generated method stub
			
		}

		public void prepare(Map arg0, TopologyContext arg1, OutputCollector arg2) {
			// TODO Auto-generated method stub
			
		}

		public void declareOutputFields(OutputFieldsDeclarer arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}

	public static class JsErrorCounter extends BaseRichBolt{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void execute(Tuple arg0) {
			// TODO Auto-generated method stub
			
		}

		public void prepare(Map arg0, TopologyContext arg1, OutputCollector arg2) {
			// TODO Auto-generated method stub
			
		}

		public void declareOutputFields(OutputFieldsDeclarer arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}

	//===============页面请求次数
	public static class PageTopNSplitter extends BaseRichBolt{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void execute(Tuple arg0) {
			// TODO Auto-generated method stub
			
		}

		public void prepare(Map arg0, TopologyContext arg1, OutputCollector arg2) {
			// TODO Auto-generated method stub
			
		}

		public void declareOutputFields(OutputFieldsDeclarer arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}

	public static class PageTopNCounter extends BaseRichBolt{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void execute(Tuple arg0) {
			// TODO Auto-generated method stub
			
		}

		public void prepare(Map arg0, TopologyContext arg1, OutputCollector arg2) {
			// TODO Auto-generated method stub
			
		}

		public void declareOutputFields(OutputFieldsDeclarer arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	
	//===============
	public static class ResponseAvailableTimeSplitter extends BaseRichBolt{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void execute(Tuple arg0) {
			// TODO Auto-generated method stub
			
		}

		public void prepare(Map arg0, TopologyContext arg1, OutputCollector arg2) {
			// TODO Auto-generated method stub
			
		}

		public void declareOutputFields(OutputFieldsDeclarer arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public static class ResponseAvailableTimeCounter extends BaseRichBolt{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public void execute(Tuple arg0) {
			// TODO Auto-generated method stub
			
		}

		public void prepare(Map arg0, TopologyContext arg1, OutputCollector arg2) {
			// TODO Auto-generated method stub
			
		}

		public void declareOutputFields(OutputFieldsDeclarer arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	
	
	
	
	

	
}
