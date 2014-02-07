package kc;

import uk.ac.imperial.einst.Institution;

public class DataInstitution implements Institution {

	final String name;

	DataInstitution(String name) {
		super();
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

}
