package org.mokyang.android;

public class Const {
	enum Phase {
		BETA, RELEASE;
	}

	static public Phase phase = Phase.BETA;
	static public boolean DEBUG = (phase != Phase.RELEASE);
}
