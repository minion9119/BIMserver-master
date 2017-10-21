package org.bimserver.bimbots;

public class BimBotsException extends Exception {

	private static final long serialVersionUID = -2477117682144897692L;

	public BimBotsException(String message) {
		super(message);
	}

	public BimBotsException(Exception e) {
		super(e);
	}
}
