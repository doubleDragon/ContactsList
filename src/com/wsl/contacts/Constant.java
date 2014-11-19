package com.wsl.contacts;

public class Constant {
	
	private volatile boolean mStatusServiceWork;

	private static class SingletonHolder {
		private static final Constant INSTANCE = new Constant();
	}

	private Constant() {
	}

	public static final Constant getInstance() {
		return SingletonHolder.INSTANCE;
	}
	
	public boolean isSyncServiceStatus() {
		return mStatusServiceWork;
	}
	
	public void setSyncServiceStatus(boolean res) {
		mStatusServiceWork = res;
	}
}
