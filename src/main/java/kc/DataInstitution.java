package kc;

import uk.ac.imperial.einst.Institution;

public class DataInstitution implements Institution {

	final String name;

	String payRole = "initiator";

	DataInstitution(String name) {
		super();
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public String getPayRole() {
		return payRole;
	}

}
