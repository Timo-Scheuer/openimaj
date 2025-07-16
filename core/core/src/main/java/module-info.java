module org.openimaj.core {
	requires java.prefs;
	
	requires ant;
	requires colt;
	requires com.esotericsoftware.kryo;
	requires transitive core;
	requires jal;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;
	requires org.apache.commons.vfs2;
	requires org.apache.httpcomponents.httpclient;
	requires org.apache.httpcomponents.httpcore;
	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.core;
	requires org.jsoup;
	requires org.objenesis;
	requires transitive org.openimaj.citation;
	
	exports org.openimaj.data;
	exports org.openimaj.io;
	exports org.openimaj.util;
	exports org.openimaj.util.array;
	exports org.openimaj.util.comparator;
	exports org.openimaj.util.function;
	exports org.openimaj.util.math;
	exports org.openimaj.util.pair;
	exports org.openimaj.util.tree;
}