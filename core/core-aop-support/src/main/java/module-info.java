module org.openimaj.aop {
	requires java.instrument;
	requires java.management;
	requires jdk.attach;
	requires transitive org.javassist;
	requires org.apache.logging.log4j.core;
	
	exports org.openimaj.aop;
	exports org.openimaj.aop.agent;
}