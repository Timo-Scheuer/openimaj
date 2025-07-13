module org.openimaj.citation {
	requires java.compiler;
	requires java.instrument;
	requires jbibtex;
	requires org.javassist;
	requires org.apache.logging.log4j;
	requires org.openimaj.aop;
	
	exports org.openimaj.citation.annotation;
}